package com.github.artem1458.dicatplugin.listeners

import com.github.artem1458.dicatplugin.DICatModificationStampTracker
import com.github.artem1458.dicatplugin.FileUtils
import com.github.artem1458.dicatplugin.models.ServiceCommand
import com.github.artem1458.dicatplugin.models.fs.FileSystemCommandPayload
import com.github.artem1458.dicatplugin.services.DICatCommandExecutorService
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
    val commandExecutorService = project.service<DICatCommandExecutorService>()

    val fsCommandPayloads = mutableListOf<FileSystemCommandPayload>()

    events.forEach { event ->
      event.file?.let { file ->
        if (file.isDirectory) return@forEach
      }

      when (event) {
        is VFilePropertyChangeEvent -> onFileUpdate(fsCommandPayloads, event)
        is VFileContentChangeEvent -> onFileUpdate(fsCommandPayloads, event)
        is VFileCopyEvent -> onFileUpdate(fsCommandPayloads, event)
        is VFileCreateEvent -> onFileUpdate(fsCommandPayloads, event)

        is VFileMoveEvent -> onFileMove(fsCommandPayloads, event)

        is VFileDeleteEvent -> onFileDelete(fsCommandPayloads, event)
      }
    }

    commandExecutorService.add(fsCommandPayloads.map { ServiceCommand.FS(it) })
  }

  private fun onFileUpdate(payloads: MutableList<FileSystemCommandPayload>, event: VFileEvent) {
    val file = event.file ?: return

    if (isUnderProjectDir(file.path))
      payloads.add(
        FileSystemCommandPayload.Add(
          path = FileUtils.getFilePath(file),
          content = FileUtils.getFileContent(file),
        )
      )
  }

  private fun onFileDelete(payloads: MutableList<FileSystemCommandPayload>, event: VFileDeleteEvent) {
    payloads.add(FileSystemCommandPayload.Delete(path = FileUtils.getFilePath(event.file)))
  }

  private fun onFileMove(
    payloads: MutableList<FileSystemCommandPayload>,
    event: VFileMoveEvent
  ) {
    val isOldUnderProjectDir = isUnderProjectDir(event.oldPath)
    val isNewUnderProjectDir = isUnderProjectDir(event.newPath)

    when {
      isOldUnderProjectDir && isNewUnderProjectDir -> {
        payloads.add(
          FileSystemCommandPayload.Move(
            oldPath = event.oldPath,
            newPath = event.newPath,
            content = FileUtils.getFileContent(event.file),
          )
        )
      }

      isOldUnderProjectDir -> {
        payloads.add(FileSystemCommandPayload.Delete(event.oldPath))
      }

      isNewUnderProjectDir -> {
        payloads.add(
          FileSystemCommandPayload.Add(
            path = event.newPath,
            content = FileUtils.getFileContent(event.file),
          )
        )
      }
    }
  }

  private fun isUnderProjectDir(
    childPath: String
  ): Boolean {
    return project.basePath?.let { childPath.startsWith(it) } ?: false
  }
}
