package com.github.artem1458.dicatplugin

import com.github.artem1458.dicatplugin.utils.logger
import com.intellij.codeInsight.daemon.DaemonCodeAnalyzer
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.service
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiManager

object DICatDaemonCodeAnalyzerRestarter {

  private val LOGGER = logger()

  fun restart(project: Project) {
    LOGGER.info("Scheduling restart of daemonCodeAnalyzer")

    val filesToRestart = getFilesToRestart(project)

    ApplicationManager.getApplication().invokeLater {
      val psiManager = PsiManager.getInstance(project)
      val daemonCodeAnalyzer = DaemonCodeAnalyzer.getInstance(project)

      FileEditorManager.getInstance(project).allEditors.forEach { editor ->
        if (!filesToRestart.contains(FileUtils.getFilePath(editor.file)))
          return@forEach

        val file = editor.file
          ?: return@forEach Unit.also { LOGGER.info("VFile from editor not found, skipping restart of daemonCodeAnalyzer") }

        val psiFile = psiManager.findFile(file)
          ?: return@forEach Unit.also { LOGGER.info("PsiFile for editor not found, skipping restart of daemonCodeAnalyzer") }

        if (!FileUtils.isValidFile(psiFile))
          return@forEach Unit.also { LOGGER.info("Skipping restart of daemonCodeAnalyzer, file not valid: ${file.path}") }

        LOGGER.info("Restarting daemonCodeAnalyzer for file: ${file.path}")

        daemonCodeAnalyzer.restart(psiFile)
      }
    }
  }

  private fun getFilesToRestart(project: Project): Set<String> {
    val responseHolder = project.service<DICatResponseHolder>()
    val previouslyAffectedFiles = responseHolder.getPreviousSync()?.affectedFiles ?: emptySet()
    val currentlyAffectedFiles = responseHolder.getCurrentSync()?.affectedFiles ?: emptySet()

    return previouslyAffectedFiles + currentlyAffectedFiles
  }
}
