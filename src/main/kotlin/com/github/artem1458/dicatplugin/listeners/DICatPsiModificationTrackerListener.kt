package com.github.artem1458.dicatplugin.listeners

import com.intellij.openapi.project.Project
import com.intellij.psi.util.PsiModificationTracker

class DICatPsiModificationTrackerListener {

  private class Listener(
    private val project: Project
  ) : PsiModificationTracker.Listener {

    override fun modificationCountChanged() {
      TODO("Not yet implemented")
    }
  }
}
