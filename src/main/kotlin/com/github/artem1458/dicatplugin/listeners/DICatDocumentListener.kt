package com.github.artem1458.dicatplugin.listeners

import com.github.artem1458.dicatplugin.models.ServiceCommand
import com.github.artem1458.dicatplugin.models.fs.FileSystemCommandPayload
import com.github.artem1458.dicatplugin.services.CommandExecutorService
import com.github.artem1458.dicatplugin.services.DICatService
import com.intellij.openapi.Disposable
import com.intellij.openapi.components.service
import com.intellij.openapi.editor.Document
import com.intellij.openapi.editor.EditorFactory
import com.intellij.openapi.editor.event.BulkAwareDocumentListener
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiDocumentManager

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

    override fun afterDocumentChange(document: Document) {
      val commandExecutorService = project.service<CommandExecutorService>()
      val filePath = PsiDocumentManager.getInstance(project).getPsiFile(document)?.originalFile?.virtualFile?.path
        ?: return

      val command = ServiceCommand.FS(
        payload = FileSystemCommandPayload.Add(mutableMapOf(filePath to document.text))
      )

      commandExecutorService.add(listOf(command))
    }
  }
}
