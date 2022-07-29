package com.github.artem1458.dicatplugin.listeners

import com.github.artem1458.dicatplugin.FileUtils
import com.github.artem1458.dicatplugin.models.FSServiceCommand
import com.github.artem1458.dicatplugin.models.fs.FileSystemCommandPayload
import com.github.artem1458.dicatplugin.services.DICatCommandExecutorService
import com.intellij.application.subscribe
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.FileEditorManagerListener
import com.intellij.openapi.fileEditor.FileEditorManagerListener.FILE_EDITOR_MANAGER
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiManager

class DICatEditorOpenedListener(
  private val project: Project
) {

  private val listenerInstance = Listener(project)

  fun listen() {
    FILE_EDITOR_MANAGER.subscribe(null, listenerInstance)
  }

  private class Listener(
    private val project: Project
  ) : FileEditorManagerListener {

    private val LOGGER = Logger.getInstance(DICatEditorOpenedListener::class.java)

    override fun fileOpened(source: FileEditorManager, file: VirtualFile) {
      val commandExecutorService = project.service<DICatCommandExecutorService>()

      PsiManager.getInstance(project).findFile(file)?.let { psiFile ->
        commandExecutorService.add(FSServiceCommand.FS(FileSystemCommandPayload.Add(
          path = FileUtils.getFilePath(psiFile),
          content = FileUtils.getFileContent(psiFile),
          modificationStamp = FileUtils.getModificationStamp(psiFile),
          isCold = true
        )))
      }
    }
  }
}
