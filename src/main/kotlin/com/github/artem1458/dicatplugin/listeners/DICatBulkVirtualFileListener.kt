package com.github.artem1458.dicatplugin.listeners

import com.github.artem1458.dicatplugin.models.ServiceCommand
import com.github.artem1458.dicatplugin.models.fs.FileSystemCommandPayload
import com.github.artem1458.dicatplugin.services.CommandExecutorService
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.newvfs.BulkFileListener
import com.intellij.openapi.vfs.newvfs.events.VFileContentChangeEvent
import com.intellij.openapi.vfs.newvfs.events.VFileCopyEvent
import com.intellij.openapi.vfs.newvfs.events.VFileCreateEvent
import com.intellij.openapi.vfs.newvfs.events.VFileDeleteEvent
import com.intellij.openapi.vfs.newvfs.events.VFileEvent
import com.intellij.openapi.vfs.newvfs.events.VFileMoveEvent
import com.intellij.openapi.vfs.newvfs.events.VFilePropertyChangeEvent

class DICatBulkVirtualFileListener(
  private val project: Project
) : BulkFileListener {

  override fun after(events: MutableList<out VFileEvent>) {
    val commandExecutorService = project.service<CommandExecutorService>()

    val addFilesCommand = FileSystemCommandPayload.Add()
    val deleteFilesCommand = FileSystemCommandPayload.Delete()

    events.forEach {
      when (it) {
        is VFilePropertyChangeEvent -> onFileUpdate(addFilesCommand, it)
        is VFileContentChangeEvent -> onFileUpdate(addFilesCommand, it)
        is VFileCopyEvent -> onFileUpdate(addFilesCommand, it)
        is VFileCreateEvent -> onFileUpdate(addFilesCommand, it)

        is VFileMoveEvent -> onFileMove(addFilesCommand, deleteFilesCommand, it)

        is VFileDeleteEvent -> onFileDelete(deleteFilesCommand, it)
      }
    }

    val addFilesServiceCommand =
      if (addFilesCommand.files.isNotEmpty()) ServiceCommand.FS(addFilesCommand) else null
    val deleteFilesServiceCommand =
      if (deleteFilesCommand.paths.isNotEmpty()) ServiceCommand.FS(deleteFilesCommand) else null

    commandExecutorService.add(
      listOfNotNull(
        addFilesServiceCommand,
        deleteFilesServiceCommand
      )
    )
  }

  private fun onFileUpdate(command: FileSystemCommandPayload.Add, event: VFileEvent) {
    val file = event.file ?: return

    if (isUnderProjectDir(file.path))
      command.files[file.path] = String(file.contentsToByteArray())
  }

  private fun onFileDelete(command: FileSystemCommandPayload.Delete, event: VFileDeleteEvent) {
    command.paths.add(event.path)
  }

  private fun onFileMove(
    addCommand: FileSystemCommandPayload.Add,
    deleteCommand: FileSystemCommandPayload.Delete,
    event: VFileMoveEvent
  ) {
    if (isUnderProjectDir(event.oldPath))
      deleteCommand.paths.remove(event.oldPath)

    if (isUnderProjectDir(event.newPath))
      addCommand.files[event.newPath] = String(event.file.contentsToByteArray())
  }

  private fun isUnderProjectDir(
    childPath: String
  ): Boolean {
    return project.basePath?.let { childPath.startsWith(it) } ?: false
  }
}
