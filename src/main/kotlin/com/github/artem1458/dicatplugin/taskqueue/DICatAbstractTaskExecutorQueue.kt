package com.github.artem1458.dicatplugin.taskqueue

import com.github.artem1458.dicatplugin.utils.logger
import java.time.Duration
import java.time.LocalDateTime
import java.util.concurrent.atomic.AtomicBoolean

abstract class DICatAbstractTaskExecutorQueue<T>(
  private val executionInterval: Duration
) : ITaskExecutorQueue<T> {

  abstract fun executeAll(tasks: Iterable<T>)

  private val LOGGER = logger()

  private val queue = mutableListOf<T>()

  @Volatile
  private var executionThread: CancelableThread? = null

  @Volatile
  private var nextStartTime = LocalDateTime.now()

  private val hasQueueChanges = AtomicBoolean(false)

  override fun add(task: T) {
    synchronized(queue) {
      queue.add(task)
      postAdd()
    }
  }

  override fun add(tasks: List<T>) {
    if (tasks.isEmpty()) return

    synchronized(queue) {
      queue.addAll(tasks)
      postAdd()
    }
  }

  private fun postAdd() {
    synchronized(nextStartTime) {
      nextStartTime = LocalDateTime.now().plus(executionInterval)
      hasQueueChanges.set(true)
    }
  }

  override fun start() {
    if (executionThread != null)
      throw IllegalStateException("Trying to start not cancelled queue")

    val newThread = CancelableThread(executionInterval)

    executionThread = newThread.also { it.start() }
  }

  override fun stop() {
    val thread = executionThread

    if (thread != null) {
      thread.cancel()

      while (thread.isAlive) {
        Thread.sleep(100)
      }
    }

    clear()
  }

  override fun clear() {
    synchronized(queue) {
      queue.clear()
    }
  }

  private fun getAllTasksAndClearQueue(): List<T> = synchronized(queue) {
    queue.toList()
      .also { queue.clear() }
      .also { hasQueueChanges.set(false) }
  }

  private inner class CancelableThread(private val executionInterval: Duration) : Thread() {

    init {
      name = "DICat task queue"
      isDaemon = true
    }

    private val cancelled = AtomicBoolean(false)

    override fun run() {
      while (!cancelled.get()) {
        if (cancelled.get()) {
          break
        }

        sleep(executionInterval.toMillis())

        if (!hasQueueChanges.get() || nextStartTime > LocalDateTime.now()) {
          continue
        }

        try {

          try {
            val tasks = getAllTasksAndClearQueue()

            executeAll(tasks)
          } catch (error: Exception) {
            LOGGER.error("Consume task error.", error)
          }

        } catch (error: Exception) {
          if (cancelled.get()) {
            break
          }

          LOGGER.error(error)
        }
      }
    }

    fun cancel() {
      cancelled.set(true)
    }
  }
}
