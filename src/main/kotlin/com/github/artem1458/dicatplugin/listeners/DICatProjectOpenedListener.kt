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
    val commandExecutorService = project.service<DICatCommandExecutorService>()
//    val editorOpenedListener = project.service<DICatEditorOpenedListener>()
    val documentListener = project.service<DICatDocumentListener>()

//    daemonCodeAnalyzerListener.listen()
    psiTreeChangeListener.listen()
    commandExecutorService.start()
    diCatService.start()
//    documentListener.listen()
//    DICatPsiModificationTrackerListener.listen()
//    editorOpenedListener.listen()
  }
}
