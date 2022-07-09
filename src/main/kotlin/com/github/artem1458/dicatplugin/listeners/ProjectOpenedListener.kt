package com.github.artem1458.dicatplugin.listeners

import com.github.artem1458.dicatplugin.services.DICatService
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ProjectManagerListener

internal class ProjectOpenedListener : ProjectManagerListener {

  override fun projectOpened(project: Project) {
    val diCatService = project.service<DICatService>()
    project.service<DICatDocumentListener>()

    diCatService.run()
  }
}
