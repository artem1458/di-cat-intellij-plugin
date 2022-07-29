package com.github.artem1458.dicatplugin

import com.intellij.openapi.Disposable
import com.intellij.openapi.editor.Document
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiFile

class DICatModificationStampTracker(
  val project: Project
): Disposable {

  private val stamps = mutableMapOf<String, Long>()

  fun inc(document: Document) {
    val psiFile = PsiDocumentManager.getInstance(project).getPsiFile(document) ?: return

    inc(FileUtils.getFilePath(psiFile))
  }
  fun inc(virtualFile: VirtualFile) {
    inc(FileUtils.getFilePath(virtualFile))
  }
  fun inc(path: String) {
    synchronized(stamps) {
      val prev = stamps.getOrDefault(path, 0)

      stamps[path] = prev + 1
    }
  }

  fun get(virtualFile: VirtualFile): Long? = get(FileUtils.getFilePath(virtualFile))
  fun get(psiFile: PsiFile): Long? = get(FileUtils.getFilePath(psiFile))
  fun get(path: String): Long? = stamps[path]

  fun clear(virtualFile: VirtualFile) = clear(FileUtils.getFilePath(virtualFile))
  fun clear(path: String) {
    synchronized(stamps) {
      stamps.remove(path)
    }
  }

  override fun dispose() {
    synchronized(stamps) {
      stamps.clear()
    }
  }
}
