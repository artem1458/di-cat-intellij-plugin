package com.github.artem1458.dicatplugin

import com.intellij.openapi.Disposable
import com.intellij.openapi.project.Project
import java.util.concurrent.atomic.AtomicLong

class DICatModificationStampTracker(
  val project: Project
) : Disposable {

  private val projectStamp = AtomicLong(0L)

  fun inc() {
    projectStamp.incrementAndGet()
  }

  fun get(): Long = projectStamp.get()

  override fun dispose() {
    projectStamp.set(0)
  }
}
