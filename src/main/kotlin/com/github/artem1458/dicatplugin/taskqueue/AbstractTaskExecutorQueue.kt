package com.github.artem1458.dicatplugin.taskqueue

import com.intellij.openapi.diagnostic.Logger
import java.time.Duration
import java.util.concurrent.atomic.AtomicBoolean

abstract class AbstractTaskExecutorQueue<T>(
  private val executionInterval: Duration
) : ITaskExecutorQueue<T> {

  abstract fun executeAll(tasks: Iterable<T>)

  protected val logger = Logger.getInstance(javaClass)

  private val queue = mutableListOf<T>()

  @Volatile
  private var executionThread: CancelableThread? = null

  @Synchronized
  override fun add(task: T) {
    queue.add(task)
  }

  @Synchronized
  override fun add(tasks: List<T>) {
    queue.addAll(tasks)
  }

  override fun start() {
    if (executionThread != null)
      throw IllegalStateException("Trying to start not cancelled queue")

    val newThread = CancelableThread(executionInterval)

    newThread.start()

    executionThread = newThread
  }

  @Synchronized
  override fun stop() {
    val thread = executionThread ?: return

    thread.cancel()

    while (thread.isAlive) {
      Thread.sleep(100)
    }

    clear()
  }

  override fun clear() {
    queue.clear()
  }

  private inner class CancelableThread(private val executionInterval: Duration) : Thread() {

    init {
      name = "DICat task queue thread"
    }

    private val cancelled = AtomicBoolean(false)

    override fun run() {
      while (!cancelled.get()) {
        if (cancelled.get()) {
          break
        }

        try {
          try {
            val tasks = getAllTasksAndClearQueue()

            executeAll(tasks)
          } catch (error: Exception) {
            logger.error("Consume task error.", error)
          }
        } catch (error: Exception) {
          if (cancelled.get()) {
            break
          }

          logger.error(error)
        }

        sleep(executionInterval.toMillis())
      }
    }

    fun cancel() {
      cancelled.set(true)
    }
  }

  @Synchronized
  private fun getAllTasksAndClearQueue(): List<T> = queue.toList().also { queue.clear() }
}
