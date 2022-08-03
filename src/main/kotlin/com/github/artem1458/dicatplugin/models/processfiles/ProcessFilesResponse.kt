package com.github.artem1458.dicatplugin.models.processfiles

import com.github.artem1458.dicatplugin.models.processfiles.statistics.BaseStatistics

data class ProcessFilesResponse(
  val compilationMessages: List<CompilationMessage>,
  val modificationStamps: Map<String, Long>,
  val statistics: List<BaseStatistics>
) {

  companion object {
    val EMPTY = ProcessFilesResponse(
      compilationMessages = emptyList(),
      modificationStamps = emptyMap(),
      statistics = emptyList()
    )
  }

  fun isEmpty(): Boolean = this === EMPTY

  fun hasSomethingByPath(path: String): Boolean =
    compilationMessages.any { it.filePath == path }

  data class CompilationMessage(
    val details: String?,
    val code: String,
    val type: MessageType,
    val description: String,
    val position: NodePosition,
    val contextDetails: ContextDetails?,
    val filePath: String,
  )

  data class ContextDetails(
    val name: String,
    val path: String,
    val namePosition: NodePosition,
  )

  enum class MessageType {
    INFO,
    WARNING,
    ERROR
  }
}
