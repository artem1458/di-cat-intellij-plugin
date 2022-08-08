package com.github.artem1458.dicatplugin

import com.github.artem1458.dicatplugin.models.processfiles.ProcessFilesResponse
import com.github.artem1458.dicatplugin.utils.allSynchronized
import com.intellij.openapi.Disposable
import com.intellij.openapi.diagnostic.Logger
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Future

class DICatResponseHolder : Disposable {

  private val LOGGER = Logger.getInstance(javaClass)

  @Volatile
  private var currentData: ProcessFilesResponse = ProcessFilesResponse.EMPTY

  @Volatile
  private var previousData: ProcessFilesResponse = currentData

  private val nextFutures = mutableSetOf<CompletableFuture<ProcessFilesResponse>>()

  @Synchronized
  fun updateData(data: ProcessFilesResponse) {
    allSynchronized(currentData, nextFutures, previousData) {
      previousData = currentData
      currentData = data
      nextFutures.forEach {
        if (!it.isDone) it.complete(data)
      }
      nextFutures.clear()
    }
  }

  fun getCurrentSync(): ProcessFilesResponse? = synchronized(currentData) {
    if (currentData.isEmpty()) null
    else currentData
  }

  fun getPreviousSync(): ProcessFilesResponse? = synchronized(previousData) {
    if (previousData.isEmpty()) null
    else previousData
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
