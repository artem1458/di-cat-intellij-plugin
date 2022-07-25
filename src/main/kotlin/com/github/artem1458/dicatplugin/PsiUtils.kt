package com.github.artem1458.dicatplugin

import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiManager

object PsiUtils {

  fun getModificationStamp(psiFile: PsiFile): Long = psiFile.manager.modificationTracker.modificationCount
  fun getModificationStamp(virtualFile: VirtualFile, project: Project): Long? {
    val psiManager = PsiManager.getInstance(project)

    return if (psiManager.isDisposed)
      null
    else
      psiManager.findFile(virtualFile)?.manager?.modificationTracker?.modificationCount
  }

  fun getFilePath(psiFile: PsiFile): String = psiFile.originalFile.virtualFile.path
  fun getFileContent(psiFile: PsiFile): String = psiFile.originalFile.text

  fun isValidFile(psiFile: PsiFile): Boolean = !psiFile.isDirectory && psiFile.isPhysical
}
