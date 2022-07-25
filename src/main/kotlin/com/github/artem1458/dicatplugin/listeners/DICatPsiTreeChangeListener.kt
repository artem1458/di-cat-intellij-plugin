package com.github.artem1458.dicatplugin.listeners

import com.github.artem1458.dicatplugin.PsiUtils
import com.github.artem1458.dicatplugin.models.ServiceCommand
import com.github.artem1458.dicatplugin.models.fs.FileSystemCommandPayload
import com.github.artem1458.dicatplugin.services.DICatCommandExecutorService
import com.intellij.openapi.Disposable
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiManager
import com.intellij.psi.PsiTreeChangeEvent
import com.intellij.psi.PsiTreeChangeListener

class DICatPsiTreeChangeListener(
  private val project: Project
) : Disposable {

  private val listenerInstance = Listener(project)

  fun listen() {
    PsiManager.getInstance(project).addPsiTreeChangeListener(listenerInstance, this)
  }

  override fun dispose() {
    PsiManager.getInstance(project).removePsiTreeChangeListener(listenerInstance)
  }

  private class Listener(
    private val project: Project
  ) : PsiTreeChangeListener {

    private val LOGGER = Logger.getInstance(DICatPsiTreeChangeListener::class.java)

    private fun onEvent(event: PsiTreeChangeEvent) {
      val commandExecutorService = project.service<DICatCommandExecutorService>()
      val psiFile = event.file ?: return
      val isValid = PsiUtils.isValidFile(psiFile)

      if (!isValid) return

      val command = ServiceCommand.FS(
        payload = FileSystemCommandPayload.Add(
          path = PsiUtils.getFilePath(psiFile),
          content = PsiUtils.getFileContent(psiFile),
          modificationStamp = PsiUtils.getModificationStamp(psiFile)
        )
      )

      commandExecutorService.add(command)
    }

    override fun childAdded(event: PsiTreeChangeEvent) = onEvent(event)
    override fun childRemoved(event: PsiTreeChangeEvent) = onEvent(event)
    override fun childReplaced(event: PsiTreeChangeEvent) = onEvent(event)
    override fun childrenChanged(event: PsiTreeChangeEvent) = onEvent(event)
    override fun childMoved(event: PsiTreeChangeEvent) = onEvent(event)
    override fun propertyChanged(event: PsiTreeChangeEvent) = onEvent(event)

    override fun beforeChildAddition(event: PsiTreeChangeEvent) {}
    override fun beforeChildRemoval(event: PsiTreeChangeEvent) {}
    override fun beforeChildReplacement(event: PsiTreeChangeEvent) {}
    override fun beforeChildMovement(event: PsiTreeChangeEvent) {}
    override fun beforeChildrenChange(event: PsiTreeChangeEvent) {}
    override fun beforePropertyChange(event: PsiTreeChangeEvent) {}
  }
}
