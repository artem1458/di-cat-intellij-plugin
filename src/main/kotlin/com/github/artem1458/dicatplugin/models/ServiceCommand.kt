package com.github.artem1458.dicatplugin.models

import com.github.artem1458.dicatplugin.models.fs.BatchFileSystemCommandPayload
import com.github.artem1458.dicatplugin.models.fs.FileSystemCommandPayload
import com.github.artem1458.dicatplugin.models.processfiles.ProcessFilesCommandPayload

data class ServiceCommand<Payload>(
  val type: CommandType,
  val payload: Payload,
) {

  companion object {
    fun FS(payload: FileSystemCommandPayload): FSServiceCommand = ServiceCommand(
      type = CommandType.FS,
      payload = payload
    )

    fun BatchFS(payload: BatchFileSystemCommandPayload): BatchFSServiceCommand = ServiceCommand(
      type = CommandType.FS,
      payload = payload
    )

    fun ProcessFiles(payload: ProcessFilesCommandPayload): ProcessFilesServiceCommand = ServiceCommand(
      type = CommandType.PROCESS_FILES,
      payload = payload
    )
  }

  enum class CommandType {
    FS,
    PROCESS_FILES
  }
}

typealias FSServiceCommand = ServiceCommand<FileSystemCommandPayload>
typealias BatchFSServiceCommand = ServiceCommand<BatchFileSystemCommandPayload>
typealias ProcessFilesServiceCommand = ServiceCommand<ProcessFilesCommandPayload>

