package com.github.artem1458.dicatplugin

import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiFile

object FileUtils {

  fun getFilePath(psiFile: PsiFile): String = getFilePath(psiFile.originalFile.virtualFile)
  fun getFilePath(virtualFile: VirtualFile): String = virtualFile.path

  fun getFileContent(virtualFile: VirtualFile): String = String(virtualFile.contentsToByteArray())
  fun getFileContent(psiFile: PsiFile): String = psiFile.originalFile.text

  fun isValidFile(psiFile: PsiFile): Boolean = !psiFile.isDirectory && psiFile.isPhysical
}
