package com.github.artem1458.dicatplugin.services

import com.github.artem1458.dicatplugin.models.ServiceResponse
import com.google.gson.Gson
import com.intellij.execution.process.OSProcessHandler
import com.intellij.execution.process.ProcessEvent
import com.intellij.execution.process.ProcessListener
import com.intellij.execution.process.ProcessOutputType
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
  private val initialized = AtomicBoolean(false)

  @Volatile
  private var responseFuture: CompletableFuture<ServiceResponse>? = null

  override fun startNotified(event: ProcessEvent) = Unit

  override fun processTerminated(event: ProcessEvent) {
    bufferedWriter.close()
    responseFuture?.let { synchronized(it) { it.cancel(true) } }
  }

  override fun onTextAvailable(event: ProcessEvent, outputType: Key<*>) {
    if (ProcessOutputType.isStdout(outputType)) {
      handleResponse(event.text)
    }
  }

  @Volatile
  private var responseBuffer: String = ""

  private fun handleResponse(text: String) {

    val fullOutput = synchronized(responseBuffer) {
      if (text.endsWith('\n')) {
        return@synchronized (responseBuffer + text).also {
          responseBuffer = ""
        }
      } else {
        responseBuffer += text

        return@synchronized null
      }
    }

    fullOutput ?: return

    try {
      val response = objectMapper.fromJson(fullOutput, ServiceResponse::class.java)

      if (response.type == ServiceResponse.ResponseType.INIT) {
        initialized.set(true)

        return
      }

      responseFuture?.let{
        synchronized(it) {it.complete(response)}
      }
    } catch (err: Throwable) {
      LOGGER.error(err)
      //TODO
      responseFuture?.let{
        synchronized(it) {it.complete(ServiceResponse(ServiceResponse.ResponseType.ERROR, null))}
      }
    }
  }

  fun <T> writeAndFlush(data: T): CompletableFuture<ServiceResponse> {
    while (!initialized.get()) {
      Thread.sleep(100)
    }

    responseFuture?.let {
      synchronized(it) {
        if (!it.isDone) throw IllegalStateException("Can not flush data, previous response is not received")
      }
    }

    bufferedWriter.write(objectMapper.toJson(data))
    bufferedWriter.newLine()
    bufferedWriter.flush()

    val newResponseFuture = CompletableFuture<ServiceResponse>()
    responseFuture = newResponseFuture

    return newResponseFuture
  }
}
