package com.github.artem1458.dicatplugin.taskqueue

import com.intellij.openapi.diagnostic.Logger
import java.time.Duration
import java.time.LocalDateTime
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.concurrent.thread

abstract class AbstractTaskExecutorQueue<T>(
  private val executionInterval: Duration
) : ITaskExecutorQueue<T> {

  abstract fun executeAll(tasks: Iterable<T>)

  protected val LOGGER = Logger.getInstance(javaClass)

  private val queue = mutableListOf<T>()

  @Volatile
  private var executionThread: CancelableThread? = null

  @Volatile
  private var nextStartTime = LocalDateTime.now()

  private val hasQueueChanges = AtomicBoolean(false)

  @Synchronized
  override fun add(task: T) {
    queue.add(task)
    nextStartTime = LocalDateTime.now().plus(executionInterval)
    hasQueueChanges.set(true)
  }

  @Synchronized
  override fun add(tasks: List<T>) {
    queue.addAll(tasks)
    nextStartTime = LocalDateTime.now().plus(executionInterval)
    hasQueueChanges.set(true)
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

  @Synchronized
  private fun getAllTasksAndClearQueue(): List<T> = queue.toList()
    .also { queue.clear() }
    .also { hasQueueChanges.set(false) }

  private inner class CancelableThread(private val executionInterval: Duration) : Thread() {

    init {
      name = "DICat task queue"
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
