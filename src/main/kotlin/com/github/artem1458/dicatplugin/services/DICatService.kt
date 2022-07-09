package com.github.artem1458.dicatplugin.services

import com.github.artem1458.dicatplugin.models.ServiceCommand
import com.github.artem1458.dicatplugin.models.ServiceResponse
import com.github.artem1458.dicatplugin.process.DICatProcessBuilder
import com.intellij.execution.process.OSProcessHandler
import com.intellij.openapi.Disposable
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project

class DICatService(
  private val project: Project
) : Disposable {

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

    //TODO handle async

    val result =  processListener.writeAndFlush(command).get()

    return result
  }

  fun sendCommandAsync(command: ServiceCommand<*>) {
    val processListener = processListener ?: throw IllegalStateException("Process listener is not initialized")

    processListener.writeAndFlush(command).thenAcceptAsync(::handleResponse)
  }

  private fun handleResponse(response: ServiceResponse) {
    response
  }

  override fun dispose() {
    val commandExecutorService = project.service<CommandExecutorService>()

    terminateProcess()
    commandExecutorService.stop()
  }

  private fun terminateProcess() {
    val processHandler = processHandler ?: return

    processHandler.destroyProcess()
    processHandler.process.waitFor()

    this.processHandler = null
  }
}
