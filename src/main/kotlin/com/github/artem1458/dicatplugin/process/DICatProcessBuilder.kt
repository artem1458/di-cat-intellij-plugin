package com.github.artem1458.dicatplugin.process

import com.github.artem1458.dicatplugin.exceptions.DICatNodeJSNotFoundException
import com.github.artem1458.dicatplugin.exceptions.DICatNotFoundException
import com.github.artem1458.dicatplugin.exceptions.DICatServiceNotFoundException
import com.github.artem1458.dicatplugin.utils.PathUtils
import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.execution.process.OSProcessHandler
import com.intellij.javascript.nodejs.interpreter.NodeJsInterpreterManager
import com.intellij.openapi.project.Project
import com.intellij.util.io.BaseOutputReader

object DICatProcessBuilder {

  fun build(project: Project): OSProcessHandler {
    val nodeJsInterpreter = NodeJsInterpreterManager.getInstance(project).interpreter
      ?: throw DICatNodeJSNotFoundException()
    val projectPath = project.basePath
      ?: throw DICatNotFoundException("Project Base Path not found")
    val nodePath = nodeJsInterpreter.presentableName
    val diCatServicePath = PathUtils.getDICatNodeJSServicePath(project)
      ?: throw DICatServiceNotFoundException()

    val commandLine = GeneralCommandLine()
    commandLine.setWorkDirectory(projectPath)
    commandLine.exePath = nodePath

    //TODO add node arguments to configuration
    commandLine.addParameter(diCatServicePath.toString())

    return MyOSProcessHandler(commandLine)
  }

  private class MyOSProcessHandler(
    commandLine: GeneralCommandLine,
  ) : OSProcessHandler(commandLine) {

    override fun readerOptions(): BaseOutputReader.Options = BaseOutputReader.Options.forMostlySilentProcess()
  }
}
