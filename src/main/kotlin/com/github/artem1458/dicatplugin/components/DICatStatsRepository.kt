package com.github.artem1458.dicatplugin.components

import com.github.artem1458.dicatplugin.models.processfiles.ProcessFilesResponse
import com.intellij.openapi.Disposable
import com.intellij.openapi.diagnostic.Logger
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Future

class DICatStatsRepository : Disposable {
  private val LOGGER = Logger.getInstance(javaClass)

  @Volatile
  private var currentData: ProcessFilesResponse = ProcessFilesResponse.EMPTY

  private val nextFutures = mutableSetOf<CompletableFuture<ProcessFilesResponse>>()

  @Synchronized
  fun updateData(data: ProcessFilesResponse) {
    LOGGER.info("Updating data, modificationStamps: ${data.modificationStamps}")

    synchronized(currentData) {
      synchronized(nextFutures) {
        currentData = data
        nextFutures.forEach {
          if (!it.isDone) it.complete(data)
        }
        nextFutures.clear()
      }
    }
  }

  fun getCurrent(): Future<ProcessFilesResponse> = synchronized(currentData) {
    if (currentData.isEmpty()) getNext()
    else CompletableFuture.completedFuture(currentData)
  }

  fun getNext(): Future<ProcessFilesResponse> =
    synchronized(nextFutures) {
      CompletableFuture<ProcessFilesResponse>()
        .also(nextFutures::add)
    }

  override fun dispose() {
    synchronized(nextFutures) {
      nextFutures.forEach { it.cancel(true) }
    }
  }
}
