package com.github.artem1458.dicatplugin.utils

import com.intellij.openapi.diagnostic.Logger
import java.time.Duration
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit


class DebouncedExecutor(
  private val delay: Duration,
  private val task: Runnable,
) {

  private val LOGGER = Logger.getInstance(javaClass)

  private val executor = Executors.newSingleThreadScheduledExecutor {
    Thread(it, javaClass.name).apply { isDaemon = true }
  }

  private var future: ScheduledFuture<*>? = null

  operator fun invoke() {
    LOGGER.info("Scheduling task")

    val myFuture = future
    if (myFuture != null && !myFuture.isDone) {
      LOGGER.info("Canceling exist task")
      myFuture.cancel(false)
    }

    executor.schedule(task, delay.toMillis(), TimeUnit.MILLISECONDS).also {
      future = it
    }
  }
}
