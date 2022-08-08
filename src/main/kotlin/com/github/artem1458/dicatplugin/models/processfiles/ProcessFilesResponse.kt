package com.github.artem1458.dicatplugin.models.processfiles

import com.github.artem1458.dicatplugin.models.processfiles.statistics.BaseStatistics

data class ProcessFilesResponse(
  val compilationMessages: List<CompilationMessage>,
  val projectModificationStamp: Long,
  val statistics: List<BaseStatistics>,
  val affectedFiles: Set<String>
) {

  companion object {
    val EMPTY = ProcessFilesResponse(
      compilationMessages = emptyList(),
      statistics = emptyList(),
      projectModificationStamp = 0,
      affectedFiles = emptySet()
    )
  }

  fun isEmpty(): Boolean = this === EMPTY

  data class CompilationMessage(
    val details: String?,
    val code: String,
    val type: MessageType,
    val description: String,
    val position: NodePosition,
    val contextDetails: ContextDetails?,
    val filePath: String,
    val nodeText: String,
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
