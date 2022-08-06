package com.github.artem1458.dicatplugin.services

import com.github.artem1458.dicatplugin.DICatModificationStampTracker
import com.github.artem1458.dicatplugin.models.BatchFSServiceCommand
import com.github.artem1458.dicatplugin.models.FSServiceCommand
import com.github.artem1458.dicatplugin.models.ProcessFilesServiceCommand
import com.github.artem1458.dicatplugin.models.ServiceCommand
import com.github.artem1458.dicatplugin.models.fs.BatchFileSystemCommandPayload
import com.github.artem1458.dicatplugin.models.fs.FileSystemCommandPayload
import com.github.artem1458.dicatplugin.models.processfiles.ProcessFilesCommandPayload
import com.github.artem1458.dicatplugin.taskqueue.DICatAbstractTaskExecutorQueue
import com.intellij.openapi.Disposable
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import java.time.Duration

class DICatCommandExecutorService(
  private val project: Project
) : DICatAbstractTaskExecutorQueue<ServiceCommand<*>>(Duration.ofSeconds(2)), Disposable {

  override fun dispose() {
    stop()
  }

  override fun executeAll(tasks: Iterable<ServiceCommand<*>>) {
    val modificationStampTracker = project.service<DICatModificationStampTracker>()
    val fsCommands = mutableListOf<FSServiceCommand>()
    val processFilesCommands = mutableListOf<ProcessFilesServiceCommand>()

    tasks.forEach { task ->
      when (task.type) {
        ServiceCommand.CommandType.FS -> fsCommands.add(task as FSServiceCommand)
        ServiceCommand.CommandType.PROCESS_FILES -> processFilesCommands.add(task as ProcessFilesServiceCommand)
        else -> {}
      }
    }

    val fsServiceCommand = buildBatchFSCommand(fsCommands)

    if (fsServiceCommand.payload.commands.isNotEmpty())
      executeCommand(fsServiceCommand)

    val processFilesCommand = ServiceCommand.ProcessFiles(ProcessFilesCommandPayload(
      projectModificationStamp = modificationStampTracker.get()
    ))

    executeCommand(processFilesCommand)
  }

  private fun executeCommand(command: ServiceCommand<*>) {
    val diCatService = project.service<DICatService>()

    //TODO If will throw an exception push tasks back to queue
    diCatService.sendCommand(command)
  }

  private fun buildBatchFSCommand(fsCommands: List<FSServiceCommand>): BatchFSServiceCommand {
    val addedPaths = mutableSetOf<String>()
    val squashedCommands = mutableListOf<FileSystemCommandPayload>()

    fsCommands.forEach { command ->
      when (command.payload) {
        is FileSystemCommandPayload.Delete -> {

          if (addedPaths.contains(command.payload.path)) {
            removeCommandFromListByPath(squashedCommands, command.payload.path)
          }

          squashedCommands.add(command.payload)
        }
        is FileSystemCommandPayload.Add -> {

          if (addedPaths.contains(command.payload.path)) {
            removeCommandFromListByPath(squashedCommands, command.payload.path)
          }

          squashedCommands.add(command.payload)
        }
        is FileSystemCommandPayload.Move -> squashedCommands.add(command.payload)
      }
    }

    return ServiceCommand.BatchFS(BatchFileSystemCommandPayload(squashedCommands))
  }

  private fun removeCommandFromListByPath(list: MutableList<FileSystemCommandPayload>, path: String) {
    list.removeIf {
      when(it) {
        is FileSystemCommandPayload.Delete -> it.path == path
        is FileSystemCommandPayload.Add -> it.path == path
        else -> false
      }
    }
  }
}
