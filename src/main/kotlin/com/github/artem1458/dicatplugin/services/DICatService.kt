package com.github.artem1458.dicatplugin.services

import com.github.artem1458.dicatplugin.FileUtils
import com.github.artem1458.dicatplugin.DICatStatsRepository
import com.github.artem1458.dicatplugin.models.ServiceCommand
import com.github.artem1458.dicatplugin.models.ServiceResponse
import com.github.artem1458.dicatplugin.models.processfiles.ProcessFilesResponse
import com.github.artem1458.dicatplugin.process.DICatProcessBuilder
import com.google.gson.Gson
import com.intellij.codeInsight.daemon.DaemonCodeAnalyzer
import com.intellij.execution.process.OSProcessHandler
import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ReadAction
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiManager

class DICatService(
  private val project: Project
) : Disposable {

  private val LOGGER = Logger.getInstance(javaClass)
  private val objectMapper = Gson()
  private var processHandler: OSProcessHandler? = null
  private var processListener: DICatProcessListener? = null

  fun start() {
    LOGGER.info("Starting DICat service")
    val diCatProcess = DICatProcessBuilder.build(project)

    val diCatProcessListener = DICatProcessListener(diCatProcess)
    diCatProcess.addProcessListener(diCatProcessListener)

    LOGGER.info("Starting DICat process (startNotify)")
    diCatProcess.startNotify()

    processHandler = diCatProcess
    processListener = diCatProcessListener

    LOGGER.info("Adding Process files command")
    project.service<DICatCommandExecutorService>().add(ServiceCommand.ProcessFiles())
  }

  fun sendCommand(command: ServiceCommand<*>): ServiceResponse {
    LOGGER.info("sendCommand(): Sending command with type: ${command.type}")
    val processListener = processListener ?: throw IllegalStateException("Process listener is not initialized")

    val response = processListener.writeAndFlush(command).get()

    return response.also(::handleResponse)
  }

  private fun handleResponse(response: ServiceResponse) {
    if (project.isDisposed) return Unit.also { LOGGER.info("Project disposed, ignorring response") }

    LOGGER.info("Handling response with type: ${response.type}")
    response.payload ?: return Unit.also { LOGGER.info("Response has no payload, skipping") }

    when (response.type) {
      ServiceResponse.ResponseType.PROCESS_FILES -> {
        val repository = project.service<DICatStatsRepository>()

        val processFilesResponse = objectMapper.fromJson(response.payload, ProcessFilesResponse::class.java)

        LOGGER.info("Response timestamps: ${processFilesResponse.modificationStamps}")

        repository.updateData(processFilesResponse)
        restartDaemonCodeAnalyzer(processFilesResponse)
      }
      ServiceResponse.ResponseType.ERROR -> {
        LOGGER.info(response.payload)
      }
      ServiceResponse.ResponseType.FS -> {}
      ServiceResponse.ResponseType.INIT -> {}
      ServiceResponse.ResponseType.EXIT -> {}
    }
  }

  private fun restartDaemonCodeAnalyzer(processFilesResponse: ProcessFilesResponse) {
    ReadAction.run<Nothing> {
      LOGGER.info("Scheduling restart of daemonCodeAnalyzer")

      val psiManager = PsiManager.getInstance(project)
      val daemonCodeAnalyzer = DaemonCodeAnalyzer.getInstance(project)

      FileEditorManager.getInstance(project).allEditors.forEach { editor ->
        val file = editor.file
          ?: return@forEach Unit.also { LOGGER.info("VFile from editor not found, skipping restart of daemonCodeAnalyzer") }

        val psiFile = psiManager.findFile(file)
          ?: return@forEach Unit.also { LOGGER.info("PsiFile for editor not found, skipping restart of daemonCodeAnalyzer") }

        if (!FileUtils.isValidFile(psiFile))
          return@forEach Unit.also { LOGGER.info("Skipping restart of daemonCodeAnalyzer, file not valid: ${file.path}") }

        //TODO Restart only for affected files (compare previousResponse and new response)

        LOGGER.info("Restarting daemonCodeAnalyzer for file: ${file.path}, modificationStamp: ${processFilesResponse.modificationStamps[file.path]}")

        daemonCodeAnalyzer.restart(psiFile)
      }
    }
  }

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
