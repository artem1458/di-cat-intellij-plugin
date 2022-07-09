package com.github.artem1458.dicatplugin.models.processfiles

data class ProcessFilesResponse(
  val compilationMessages: List<CompilationMessage>
) {

  data class CompilationMessage(
    val details: String?,
    val code: MessageCode,
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
  )

  enum class MessageType {
    INFO,
    WARNING,
    ERROR
  }

  enum class MessageCode {
    DICAT0,
    DICAT1,
    DICAT2,
    DICAT3,
    DICAT4,
    DICAT5,
    DICAT6,
    DICAT7,
    DICAT8,
    DICAT9,
    DICAT10,
    DICAT11,
    DICAT12,
    DICAT13,
    DICAT14,
    DICAT15,
    DICAT16,
  }
}
