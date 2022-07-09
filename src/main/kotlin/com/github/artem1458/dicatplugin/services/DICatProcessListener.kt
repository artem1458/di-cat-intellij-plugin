package com.github.artem1458.dicatplugin.services

import com.github.artem1458.dicatplugin.models.ServiceResponse
import com.google.gson.Gson
import com.intellij.execution.process.OSProcessHandler
import com.intellij.execution.process.ProcessEvent
import com.intellij.execution.process.ProcessListener
import com.intellij.execution.process.ProcessOutputTypes
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.util.Key
import java.io.BufferedWriter
import java.io.OutputStreamWriter
import java.util.concurrent.CompletableFuture

class DICatProcessListener(
  private val processHandler: OSProcessHandler,
) : ProcessListener {

  private val objectMapper = Gson()

  private val logger: Logger = Logger.getInstance(javaClass)
  private val processStdin: OutputStreamWriter = processHandler.processInput.writer()
  private var future: CompletableFuture<ServiceResponse>? = null

  override fun startNotified(event: ProcessEvent) = Unit

  override fun processTerminated(event: ProcessEvent) {
    processStdin.close()
  }

  @Synchronized
  override fun onTextAvailable(event: ProcessEvent, outputType: Key<*>) {
    if (outputType == ProcessOutputTypes.STDOUT) {
      val text = event.text.trim()

      try {
        val response = objectMapper.fromJson(text, ServiceResponse::class.java)

        future?.complete(response)
      } catch (err: Throwable) {
        logger.error(err)
      }
    }
  }

  fun <T> writeAndFlush(data: T): CompletableFuture<ServiceResponse> {
    processStdin.write(objectMapper.toJson(data))
    processStdin.write("\n")
    processStdin.flush()

    return CompletableFuture<ServiceResponse>().also { future = it }
  }
}
