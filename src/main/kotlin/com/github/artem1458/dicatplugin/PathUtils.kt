package com.github.artem1458.dicatplugin

import com.intellij.openapi.project.Project
import java.nio.file.Path

object PathUtils {
  fun getDICatNodeModulesPath(project: Project): Path? {
    //TODO add configurable path to di-cat path
    return project.basePath?.let { Path.of("""$it/node_modules/dependency-injection-cat""") }?.toAbsolutePath()
  }

  fun getDICatNodeJSServicePath(project: Project): Path? {
    return getDICatNodeModulesPath(project)?.let { Path.of(it.toString(), "service") }
  }
}
