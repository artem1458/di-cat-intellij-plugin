package com.github.artem1458.dicatplugin.listeners

import com.github.artem1458.dicatplugin.FileUtils
import com.github.artem1458.dicatplugin.models.ServiceCommand
import com.github.artem1458.dicatplugin.models.fs.FileSystemCommandPayload
import com.github.artem1458.dicatplugin.services.DICatCommandExecutorService
import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.ReadAction
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiManager
import com.intellij.psi.util.PsiModificationTracker

class DICatPsiModificationTrackerListener(
  private val project: Project
) : Disposable {

  private val listenerInstance = Listener(project)

  fun listen() {
    ApplicationManager.getApplication().messageBus.connect(this)
      .subscribe(PsiModificationTracker.TOPIC, listenerInstance)
  }

  override fun dispose() {}

  private val LOGGER = Logger.getInstance(javaClass)

  private inner class Listener(
    private val project: Project
  ) : PsiModificationTracker.Listener {

    override fun modificationCountChanged() {
      ReadAction.run<Nothing> {
        val psiManager = PsiManager.getInstance(project)
        val commandExecutorService = project.service<DICatCommandExecutorService>()

        val psiFiles = FileEditorManager.getInstance(project).allEditors.mapNotNull { editor ->
          editor.file?.let(psiManager::findFile)
        }

        val fsCommands = psiFiles.filter(FileUtils::isValidFile).map { psiFile ->
          ServiceCommand.FS(
            payload = FileSystemCommandPayload.Add(
              path = FileUtils.getFilePath(psiFile),
              content = FileUtils.getFileContent(psiFile),
              modificationStamp = FileUtils.getModificationStamp(psiFile)
            )
          )
        }

        commandExecutorService.add(fsCommands)
      }
    }
  }
}
