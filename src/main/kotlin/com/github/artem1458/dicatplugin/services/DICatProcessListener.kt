package com.github.artem1458.dicatplugin.services

import com.github.artem1458.dicatplugin.models.ServiceResponse
import com.google.gson.Gson
import com.intellij.execution.process.OSProcessHandler
import com.intellij.execution.process.ProcessEvent
import com.intellij.execution.process.ProcessListener
import com.intellij.execution.process.ProcessOutputType
import com.intellij.execution.process.ProcessOutputTypes
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.util.Key
import java.io.BufferedWriter
import java.util.concurrent.CompletableFuture
import java.util.concurrent.atomic.AtomicBoolean

class DICatProcessListener(
  private val processHandler: OSProcessHandler,
) : ProcessListener {

  private val objectMapper = Gson()

  private val LOGGER = Logger.getInstance(javaClass)
  private val bufferedWriter: BufferedWriter = processHandler.processInput.bufferedWriter()
  private var future: CompletableFuture<ServiceResponse>? = null
  private val initialized = AtomicBoolean(false)

  override fun startNotified(event: ProcessEvent) = Unit

  @Synchronized
  override fun processTerminated(event: ProcessEvent) {
    bufferedWriter.close()
    initialized.set(false)
    future?.complete(ServiceResponse(ServiceResponse.ResponseType.EXIT, null))
  }

  @Synchronized
  override fun onTextAvailable(event: ProcessEvent, outputType: Key<*>) {
    if (ProcessOutputType.isStdout(outputType)) {
      val text = event.text.trim()

      try {
        val response = objectMapper.fromJson(text, ServiceResponse::class.java)

        if (response.type == ServiceResponse.ResponseType.INIT) {
          initialized.set(true)

          return
        }

        future?.complete(response)
      } catch (err: Throwable) {
        LOGGER.error(err)
        //TODO
        future?.complete(ServiceResponse(ServiceResponse.ResponseType.ERROR, null))
      }
    }
  }

  @Synchronized
  fun <T> writeAndFlush(data: T): CompletableFuture<ServiceResponse> {
    while (!initialized.get()) {
      Thread.sleep(100)
    }

    bufferedWriter.write(objectMapper.toJson(data))
    bufferedWriter.newLine()
    bufferedWriter.flush()

    return CompletableFuture<ServiceResponse>().also { future = it }
  }
}
