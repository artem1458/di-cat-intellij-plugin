package com.github.artem1458.dicatplugin.listeners

import com.github.artem1458.dicatplugin.services.DICatCommandExecutorService
import com.github.artem1458.dicatplugin.services.DICatService
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ProjectManagerListener

internal class ProjectOpenedListener : ProjectManagerListener {

  override fun projectOpened(project: Project) {
    val diCatService = project.service<DICatService>()
    val psiTreeChangeListener = project.service<DICatPsiTreeChangeListener>()
    val editorOpenedListener = project.service<DICatEditorOpenedListener>()
    val commandExecutorService = project.service<DICatCommandExecutorService>()

    commandExecutorService.start()
    psiTreeChangeListener.listen()
    editorOpenedListener.listen()
    diCatService.start()
  }
}
