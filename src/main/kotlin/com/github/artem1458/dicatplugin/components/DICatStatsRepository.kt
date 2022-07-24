package com.github.artem1458.dicatplugin.components

import com.github.artem1458.dicatplugin.models.processfiles.ProcessFilesResponse
import com.intellij.openapi.Disposable
import com.intellij.openapi.diagnostic.Logger
import org.jetbrains.rpc.LOG
import java.util.concurrent.CompletableFuture

class DICatStatsRepository : Disposable {
  private val LOGGER = Logger.getInstance(javaClass)

  @Volatile
  private var current = CompletableFuture<ProcessFilesResponse>()

  @Volatile
  private var next = CompletableFuture<ProcessFilesResponse>()

  @Synchronized
  fun updateData(data: ProcessFilesResponse) {
    LOGGER.info("Updating data, modificationStamps: ${data.modificationStamps}")

    if (!current.isDone) {
      LOGGER.info("Future is not done, completing current")
      current.complete(data)

      return
    }

    LOGGER.info("Current future is done, making current = next, and completing new current")

    synchronized(current) {
      synchronized(next) {
        current = next
        next = CompletableFuture()

        current.complete(data)
      }
    }
  }

  fun getCurrent(): CompletableFuture<ProcessFilesResponse> = current

  fun getNext(): CompletableFuture<ProcessFilesResponse> = next

  override fun dispose() {
    synchronized(current) {
      synchronized(next) {
        current.cancel(true)
        next.cancel(true)
      }
    }
  }
}
