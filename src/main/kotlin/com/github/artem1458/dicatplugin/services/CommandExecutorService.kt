package com.github.artem1458.dicatplugin.services

import com.github.artem1458.dicatplugin.models.BatchFSServiceCommand
import com.github.artem1458.dicatplugin.models.FSServiceCommand
import com.github.artem1458.dicatplugin.models.ProcessFilesServiceCommand
import com.github.artem1458.dicatplugin.models.ServiceCommand
import com.github.artem1458.dicatplugin.models.fs.BatchFileSystemCommandPayload
import com.github.artem1458.dicatplugin.models.fs.FileSystemCommandPayload
import com.github.artem1458.dicatplugin.taskqueue.AbstractTaskExecutorQueue
import com.intellij.openapi.Disposable
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import java.time.Duration

class CommandExecutorService(
  private val project: Project
) : AbstractTaskExecutorQueue<ServiceCommand<*>>(Duration.ofSeconds(1)), Disposable {

  override fun dispose() {
    stop()
  }

  override fun executeAll(tasks: Iterable<ServiceCommand<*>>) {
    val fsCommands = mutableListOf<FSServiceCommand>()

    tasks.forEach { task ->
      when (task.type) {
        ServiceCommand.CommandType.FS -> fsCommands.add(task as FSServiceCommand)
        else -> {}
      }
    }

    if (fsCommands.isEmpty())
      return

    val fsServiceCommand = buildBatchFSCommand(fsCommands)

    executeCommand(fsServiceCommand)

    val processFilesCommand = ServiceCommand.ProcessFiles()

    executeCommand(processFilesCommand)
  }

  private fun executeCommand(command: ServiceCommand<*>) {
    val diCatService = project.service<DICatService>()

    diCatService.sendCommand(command)
  }

  private fun buildBatchFSCommand(fsCommands: List<FSServiceCommand>): BatchFSServiceCommand {
    val squashedCommands = mutableMapOf<String, FileSystemCommandPayload>()

    fsCommands.forEach {
      when (it.payload) {
        is FileSystemCommandPayload.Delete -> squashedCommands[it.payload.path] = it.payload
        is FileSystemCommandPayload.Add -> squashedCommands[it.payload.path] = it.payload
        is FileSystemCommandPayload.Move -> squashedCommands[it.payload.oldPath + it.payload.newPath] = it.payload
      }
    }

    return ServiceCommand.BatchFS(BatchFileSystemCommandPayload(squashedCommands.values.toList()))
  }
}
