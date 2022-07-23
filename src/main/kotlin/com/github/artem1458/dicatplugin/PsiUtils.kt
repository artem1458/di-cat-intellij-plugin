package com.github.artem1458.dicatplugin

import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiManager

object PsiUtils {

  fun getModificationStamp(psiFile: PsiFile): Long = psiFile.originalFile.modificationStamp
  fun getModificationStamp(virtualFile: VirtualFile, project: Project): Long? {
    val psiManager = PsiManager.getInstance(project)

    if (psiManager.isDisposed)
      return null
    else
      return psiManager.findFile(virtualFile)?.originalFile?.modificationStamp
  }
}
