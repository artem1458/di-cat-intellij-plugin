package com.github.artem1458.dicatplugin.services

import com.github.artem1458.dicatplugin.models.FSServiceCommand
import com.github.artem1458.dicatplugin.models.ProcessFilesServiceCommand
import com.github.artem1458.dicatplugin.models.ServiceCommand
import com.github.artem1458.dicatplugin.models.fs.BatchFileSystemCommandPayload
import com.github.artem1458.dicatplugin.taskqueue.AbstractTaskExecutorQueue
import com.intellij.openapi.Disposable
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import java.time.Duration

class CommandExecutorService(
  private val project: Project
) : AbstractTaskExecutorQueue<ServiceCommand<*>>(Duration.ofSeconds(2)), Disposable {

  override fun dispose() {
    stop()
  }

  override fun executeAll(tasks: Iterable<ServiceCommand<*>>) {
    val fsCommands = mutableListOf<FSServiceCommand>()
    val processFilesCommands = mutableListOf<ProcessFilesServiceCommand>()

    tasks.forEach { task ->
      when (task.type) {
        ServiceCommand.CommandType.FS -> fsCommands.add(task as FSServiceCommand)
        ServiceCommand.CommandType.PROCESS_FILES -> processFilesCommands.add(task as ProcessFilesServiceCommand)
      }
    }

    if (fsCommands.isEmpty())
      return

    val fsServiceCommand = ServiceCommand.BatchFS(
      payload = BatchFileSystemCommandPayload(
        commands = fsCommands.map { command -> command.payload }
      )
    )

    executeCommand(fsServiceCommand)

    val processFilesCommand = ServiceCommand.ProcessFiles()

    executeCommand(processFilesCommand)
  }

  private fun executeCommand(command: ServiceCommand<*>) {
    val diCatService = project.service<DICatService>()

    diCatService.sendCommand(command)
  }
}
