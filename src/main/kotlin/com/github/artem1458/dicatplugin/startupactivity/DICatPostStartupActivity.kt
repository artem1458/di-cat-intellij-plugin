package com.github.artem1458.dicatplugin.startupactivity

import com.github.artem1458.dicatplugin.FileUtils
import com.github.artem1458.dicatplugin.models.FSServiceCommand
import com.github.artem1458.dicatplugin.models.fs.FileSystemCommandPayload
import com.github.artem1458.dicatplugin.services.DICatCommandExecutorService
import com.intellij.openapi.application.ReadAction
import com.intellij.openapi.components.service
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.StartupActivity
import com.intellij.psi.PsiManager

//Unused
class DICatPostStartupActivity : StartupActivity.Background {

  override fun runActivity(project: Project) {
    val psiManager = PsiManager.getInstance(project)
    val commandExecutorService = project.service<DICatCommandExecutorService>()

    ReadAction.run<Nothing> {
      val fsCommands = FileEditorManager.getInstance(project).allEditors.mapNotNull { editor ->
        editor.file?.let(psiManager::findFile)?.let { psiFile ->
          FSServiceCommand.FS(
            FileSystemCommandPayload.Add(
              path = FileUtils.getFilePath(psiFile),
              content = FileUtils.getFileContent(psiFile),
              modificationStamp = FileUtils.getModificationStamp(psiFile),
              isCold = true
            )
          )
        }
      }

      commandExecutorService.add(fsCommands)
    }
  }
}
