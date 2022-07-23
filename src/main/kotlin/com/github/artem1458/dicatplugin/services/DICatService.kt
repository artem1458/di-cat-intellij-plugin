package com.github.artem1458.dicatplugin.services

import com.github.artem1458.dicatplugin.models.ServiceCommand
import com.github.artem1458.dicatplugin.models.ServiceResponse
import com.github.artem1458.dicatplugin.models.processfiles.ProcessFilesResponse
import com.github.artem1458.dicatplugin.process.DICatProcessBuilder
import com.github.artem1458.dicatplugin.components.StatsRepository
import com.github.artem1458.dicatplugin.utils.DebouncedExecutor
import com.google.gson.Gson
import com.intellij.codeInsight.daemon.DaemonCodeAnalyzer
import com.intellij.execution.process.OSProcessHandler
import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.editor.EditorFactory
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiManager
import java.time.Duration

class DICatService(
  private val project: Project
) : Disposable {

  private val LOGGER = Logger.getInstance(javaClass)
  private val objectMapper = Gson()
  private var processHandler: OSProcessHandler? = null
  private var processListener: DICatProcessListener? = null

  fun run() {
    val commandExecutorService = project.service<CommandExecutorService>()
    val diCatProcess = DICatProcessBuilder.build(project)

    val diCatProcessListener = DICatProcessListener(diCatProcess)
    diCatProcess.addProcessListener(diCatProcessListener)

    diCatProcess.startNotify()
    commandExecutorService.start()

    processHandler = diCatProcess
    processListener = diCatProcessListener

    //Remove
    commandExecutorService.add(ServiceCommand.ProcessFiles())
  }

  fun restart() {
    val commandExecutorService = project.service<CommandExecutorService>()

    terminateProcess()
    commandExecutorService.stop()

    run()
  }

  @Synchronized
  fun sendCommand(command: ServiceCommand<*>): ServiceResponse {
    val processListener = processListener ?: throw IllegalStateException("Process listener is not initialized")

    val response = processListener.writeAndFlush(command).get()

    return response.also(::handleResponse)
  }

  private fun handleResponse(response: ServiceResponse) {
    response.payload ?: return

    if (project.isDisposed) return

    val repository = project.getComponent(StatsRepository::class.java)

    if (response.type == ServiceResponse.ResponseType.PROCESS_FILES) {
      val processFilesResponse = objectMapper.fromJson(response.payload, ProcessFilesResponse::class.java)

      repository.updateData(processFilesResponse)
      restartDaemonCodeAnalyzer()
    }
  }

  private fun restartDaemonCodeAnalyzer() {
    LOGGER.info("Running invokeLater")
    ApplicationManager.getApplication().invokeLater {
      val psiManager = PsiManager.getInstance(project)
      val daemonCodeAnalyzer = DaemonCodeAnalyzer.getInstance(project)

      FileEditorManager.getInstance(project).allEditors.forEach { editor ->
        LOGGER.info("Restarting Daemon Code Analyzer")
        editor.file?.let(psiManager::findFile)?.let(daemonCodeAnalyzer::restart)
      }
    }
  }
//
//  val restartDaemonCodeAnalyzer = DebouncedExecutor(Duration.ofSeconds(2)) {
//    LOGGER.info("Running invokeLater")
//    ApplicationManager.getApplication().invokeLater {
//      val psiManager = PsiManager.getInstance(project)
//      val daemonCodeAnalyzer = DaemonCodeAnalyzer.getInstance(project)
//
//      FileEditorManager.getInstance(project).allEditors.forEach { editor ->
//        LOGGER.info("Restarting Daemon Code Analyzer")
//        editor.file?.let(psiManager::findFile)?.let(daemonCodeAnalyzer::restart)
//      }
//    }
//  }

  override fun dispose() {
    terminateProcess()
  }

  private fun terminateProcess() {
    val processHandler = processHandler ?: return

    processHandler.destroyProcess()
    processHandler.process.waitFor()

    this.processHandler = null
  }
}
