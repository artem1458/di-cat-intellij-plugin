package com.github.artem1458.dicatplugin.listeners

import com.github.artem1458.dicatplugin.PsiUtils
import com.github.artem1458.dicatplugin.models.ServiceCommand
import com.github.artem1458.dicatplugin.models.fs.FileSystemCommandPayload
import com.github.artem1458.dicatplugin.services.CommandExecutorService
import com.github.artem1458.dicatplugin.utils.DebouncedExecutor
import com.intellij.codeInsight.daemon.DaemonCodeAnalyzer
import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.editor.Document
import com.intellij.openapi.editor.EditorFactory
import com.intellij.openapi.editor.event.BulkAwareDocumentListener
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiManager
import java.time.Duration

class DICatDocumentListener(
  project: Project
) : Disposable {

  private val listenerInstance = Listener(project)

  init {
    EditorFactory.getInstance().eventMulticaster.addDocumentListener(listenerInstance, this)
  }

  override fun dispose() {
    EditorFactory.getInstance().eventMulticaster.removeDocumentListener(listenerInstance)
  }

  private class Listener(
    private val project: Project
  ) : BulkAwareDocumentListener.Simple {
    private val LOGGER = Logger.getInstance(javaClass)

//    val restartDaemonCodeAnalyzer = DebouncedExecutor(Duration.ofSeconds(2)) {
//      LOGGER.info("Running invokeLater")
//      ApplicationManager.getApplication().runReadAction {
//        val psiManager = PsiManager.getInstance(project)
//        val daemonCodeAnalyzer = DaemonCodeAnalyzer.getInstance(project)
//
//        FileEditorManager.getInstance(project).allEditors.forEach { editor ->
//          LOGGER.info("Restarting Daemon Code Analyzer")
//          editor.file?.let(psiManager::findFile)?.let(daemonCodeAnalyzer::restart)
//        }
//      }
//    }

    override fun afterDocumentChange(document: Document) {
      val commandExecutorService = project.service<CommandExecutorService>()
      val psiFile = PsiDocumentManager.getInstance(project).getPsiFile(document) ?: return
      val filePath = psiFile.originalFile.virtualFile.path

      val command = ServiceCommand.FS(
        payload = FileSystemCommandPayload.Add(
          path = filePath,
          content = psiFile.originalFile.text,
          modificationStamp = PsiUtils.getModificationStamp(psiFile),
        )
      )

      commandExecutorService.add(command)
//      restartDaemonCodeAnalyzer()
    }
  }
}
