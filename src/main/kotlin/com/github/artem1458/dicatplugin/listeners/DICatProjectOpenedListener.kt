package com.github.artem1458.dicatplugin.listeners

import com.github.artem1458.dicatplugin.services.DICatCommandExecutorService
import com.github.artem1458.dicatplugin.services.DICatService
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ProjectManagerListener

internal class DICatProjectOpenedListener : ProjectManagerListener {

  override fun projectOpened(project: Project) {
    val diCatService = project.service<DICatService>()
    val psiTreeChangeListener = project.service<DICatPsiTreeChangeListener>()
    val editorOpenedListener = project.service<DICatEditorOpenedListener>()
    val commandExecutorService = project.service<DICatCommandExecutorService>()
    val documentListener = project.service<DICatDocumentListener>()
    val DICatPsiModificationTrackerListener = project.service<DICatPsiModificationTrackerListener>()

    commandExecutorService.start()
    documentListener.listen()
//    DICatPsiModificationTrackerListener.listen()
//    psiTreeChangeListener.listen()
    editorOpenedListener.listen()
    diCatService.start()
  }
}
