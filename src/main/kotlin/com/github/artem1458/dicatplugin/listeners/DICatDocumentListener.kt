package com.github.artem1458.dicatplugin.listeners

import com.github.artem1458.dicatplugin.PsiUtils
import com.github.artem1458.dicatplugin.models.ServiceCommand
import com.github.artem1458.dicatplugin.models.fs.FileSystemCommandPayload
import com.github.artem1458.dicatplugin.services.DICatCommandExecutorService
import com.intellij.openapi.Disposable
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.editor.Document
import com.intellij.openapi.editor.EditorFactory
import com.intellij.openapi.editor.event.BulkAwareDocumentListener
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiDocumentManager


//Unused
class DICatDocumentListener(
  project: Project
) : Disposable {

  private val listenerInstance = Listener(project)

  fun listen() {
    EditorFactory.getInstance().eventMulticaster.addDocumentListener(listenerInstance, this)
  }

  override fun dispose() {
    EditorFactory.getInstance().eventMulticaster.removeDocumentListener(listenerInstance)
  }

  private class Listener(
    private val project: Project
  ) : BulkAwareDocumentListener.Simple {

    private val LOGGER = Logger.getInstance(DICatDocumentListener::class.java)

    override fun afterDocumentChange(document: Document) {
      val commandExecutorService = project.service<DICatCommandExecutorService>()
      val psiFile = PsiDocumentManager.getInstance(project).getPsiFile(document) ?: return
      val filePath = psiFile.originalFile.virtualFile.path

      val command = ServiceCommand.FS(
        payload = FileSystemCommandPayload.Add(
          path = filePath,
          content = psiFile.originalFile.text,
          modificationStamp = PsiUtils.getModificationStamp(psiFile),
        )
      )

      LOGGER.info("Sending file with psiModStamp: ${psiFile.modificationStamp}, psiOriginalModStamp ${PsiUtils.getModificationStamp(psiFile)}}")
      commandExecutorService.add(command)
    }
  }
}
