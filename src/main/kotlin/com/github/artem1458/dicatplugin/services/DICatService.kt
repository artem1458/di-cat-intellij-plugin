package com.github.artem1458.dicatplugin.services

import com.github.artem1458.dicatplugin.DICatDaemonCodeAnalyzerRestarter
import com.github.artem1458.dicatplugin.DICatModificationStampTracker
import com.github.artem1458.dicatplugin.DICatResponseHolder
import com.github.artem1458.dicatplugin.models.ServiceCommand
import com.github.artem1458.dicatplugin.models.ServiceResponse
import com.github.artem1458.dicatplugin.models.processfiles.ProcessFilesCommandPayload
import com.github.artem1458.dicatplugin.models.processfiles.ProcessFilesResponse
import com.github.artem1458.dicatplugin.process.DICatProcessBuilder
import com.github.artem1458.dicatplugin.utils.logger
import com.google.gson.Gson
import com.intellij.execution.process.OSProcessHandler
import com.intellij.openapi.Disposable
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project

class DICatService(
  private val project: Project
) : Disposable {

  private val LOGGER = logger()
  private val objectMapper = Gson()
  private var processHandler: OSProcessHandler? = null
  private var processListener: DICatProcessListener? = null

  fun start() {
    val modificationStampTracker = project.service<DICatModificationStampTracker>()
    LOGGER.info("Starting DICat service")
    val diCatProcess = DICatProcessBuilder.build(project)

    val diCatProcessListener = DICatProcessListener(diCatProcess)
    diCatProcess.addProcessListener(diCatProcessListener)

    LOGGER.info("Starting DICat process (startNotify)")
    diCatProcess.startNotify()

    processHandler = diCatProcess
    processListener = diCatProcessListener

    LOGGER.info("Adding Process files command")
    project.service<DICatCommandExecutorService>().add(
      ServiceCommand.ProcessFiles(ProcessFilesCommandPayload(projectModificationStamp = modificationStampTracker.get()))
    )
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
        val repository = project.service<DICatResponseHolder>()

        val processFilesResponse = objectMapper.fromJson(response.payload, ProcessFilesResponse::class.java)

        repository.updateData(processFilesResponse)

        DICatDaemonCodeAnalyzerRestarter.restart(project)
      }

      ServiceResponse.ResponseType.ERROR -> {
        LOGGER.info(response.payload)
      }

      ServiceResponse.ResponseType.FS -> {}
      ServiceResponse.ResponseType.INIT -> {}
      ServiceResponse.ResponseType.EXIT -> {}
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
