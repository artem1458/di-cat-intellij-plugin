package com.github.artem1458.dicatplugin.process

import com.github.artem1458.dicatplugin.PathUtils
import com.github.artem1458.dicatplugin.exceptions.DICatServiceNotFoundException
import com.github.artem1458.dicatplugin.exceptions.NodeJSNotFoundException
import com.github.artem1458.dicatplugin.exceptions.NotFoundException
import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.execution.process.OSProcessHandler
import com.intellij.execution.process.ProcessHandlerFactory
import com.intellij.javascript.nodejs.interpreter.NodeJsInterpreterManager
import com.intellij.openapi.project.Project
import com.intellij.util.io.BaseDataReader.SleepingPolicy
import com.intellij.util.io.BaseOutputReader
import java.util.concurrent.Future

object DICatProcessBuilder {

  fun build(project: Project): OSProcessHandler {
    val nodeJsInterpreter = NodeJsInterpreterManager.getInstance(project).interpreter
      ?: throw NodeJSNotFoundException()
    val projectPath = project.basePath
      ?: throw NotFoundException("Project Base Path not found")
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

    override fun readerOptions() = MyReaderOptions()

    private class MyReaderOptions : BaseOutputReader.Options() {
      override fun splitToLines(): Boolean = false
      override fun sendIncompleteLines(): Boolean = false

//      override fun policy(): SleepingPolicy = SleepingPolicy.NON_BLOCKING
    }
  }
}
