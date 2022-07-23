package com.github.artem1458.dicatplugin.components

import com.github.artem1458.dicatplugin.models.processfiles.ProcessFilesResponse
import com.intellij.openapi.Disposable
import java.util.concurrent.CompletableFuture

class StatsRepository : Disposable {
  @Volatile
  private var currentData = CompletableFuture<ProcessFilesResponse>()

  @Volatile
  private var nextData = CompletableFuture<ProcessFilesResponse>()

  @Synchronized
  fun updateData(data: ProcessFilesResponse) {
    if (!currentData.isDone) {
      currentData.complete(data)

      return
    }

    currentData = nextData
    nextData = CompletableFuture()

    currentData.complete(data)
  }

  @Synchronized
  fun getData(): CompletableFuture<ProcessFilesResponse> = currentData

  @Synchronized
  fun getNextData(): CompletableFuture<ProcessFilesResponse> = nextData

  @Synchronized
  override fun dispose() {
    currentData.cancel(true)
    nextData.cancel(true)
  }
}
