package com.github.artem1458.dicatplugin.models.processfiles

data class ProcessFilesResponse(
  val compilationMessages: List<CompilationMessage>,
  val modificationStamps: Map<String, Long>,
  val coldFilePaths: Set<String>
) {

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

  data class NodePosition(
    val line: Int,
    val startColumn: Int,
    val endColumn: Int,
    val startOffset: Int,
    val endOffset: Int
  )

  enum class MessageType {
    INFO,
    WARNING,
    ERROR
  }
}
