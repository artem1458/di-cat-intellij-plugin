package com.github.artem1458.dicatplugin

import com.github.artem1458.dicatplugin.models.processfiles.ProcessFilesResponse
import com.github.artem1458.dicatplugin.utils.logger
import com.intellij.codeInspection.ex.ExternalAnnotatorBatchInspection
import com.intellij.lang.annotation.AnnotationHolder
import com.intellij.lang.annotation.ExternalAnnotator
import com.intellij.lang.annotation.HighlightSeverity
import com.intellij.openapi.components.service
import com.intellij.openapi.editor.Editor
import com.intellij.psi.PsiFile
import java.util.concurrent.CancellationException
import java.util.concurrent.Future

class DICatExternalAnnotator :
  ExternalAnnotator<DICatExternalAnnotator.DICatCollectedInfo, DICatExternalAnnotator.DICatAnnotationResultType>(),
  ExternalAnnotatorBatchInspection {

  private val LOGGER = logger()

  override fun getShortName(): String = "DI Cat"

  override fun collectInformation(file: PsiFile, editor: Editor, hasErrors: Boolean) =
    DICatCollectedInfo(
      filePath = FileUtils.getFilePath(file),
      psiFile = file
    ).also {
      LOGGER.info("collectInformation(): Info collected for file: ${it.filePath}")
    }

  override fun doAnnotate(collectedInfo: DICatCollectedInfo?): DICatAnnotationResultType? {
    LOGGER.info("doAnnotate(): Starting annotation process")
    collectedInfo ?: return null
    val project = collectedInfo.psiFile.project
    val responseHolder = project.service<DICatResponseHolder>()

    return myAnnotate(collectedInfo, responseHolder.getCurrent())
  }

  private fun myAnnotate(
    collectedInfo: DICatCollectedInfo,
    futureProcessFilesResponse: Future<ProcessFilesResponse>,
  ): DICatAnnotationResultType? {
    val processFilesResponse = runCatching {
      futureProcessFilesResponse.get()
    }.getOrElse {
      if (it !is CancellationException) {
        LOGGER.error(it)
      } else {
        LOGGER.info("Future is canceled", it)
      }

      return null.also {
        LOGGER.info("annotate(): Skipping annotation process, returning null")
      }
    }

    val currentModificationStamp = collectedInfo.psiFile.project.service<DICatModificationStampTracker>().get()
    val responseModificationStamp = processFilesResponse.projectModificationStamp

    LOGGER.info(
      "annotate(): " +
        "currentModificationStamp: $currentModificationStamp, " +
        "responseModificationStamp: $responseModificationStamp, "
    )

    if (responseModificationStamp == currentModificationStamp) {
      LOGGER.info("annotate(): responseModificationStamp == currentModificationStamp. file: ${collectedInfo.filePath}")
      return DICatAnnotationResultType.buildFromServiceResponse(processFilesResponse, collectedInfo)
    }

    if (responseModificationStamp > currentModificationStamp)
      LOGGER.error(IllegalStateException("Modification stamp from service response is higher than local. file: ${collectedInfo.filePath}"))
        .also { return null }

    LOGGER.info("annotate(): waiting more new timestamp. file: ${collectedInfo.filePath}")
    val project = collectedInfo.psiFile.project
    val responseHolder = project.service<DICatResponseHolder>()

    return myAnnotate(collectedInfo, responseHolder.getNext())
  }

  override fun apply(psiFile: PsiFile, annotationResult: DICatAnnotationResultType?, holder: AnnotationHolder) {
    LOGGER.info("apply(): applying annotation for file: ${FileUtils.getFilePath(psiFile)}")

    val messages = annotationResult?.messages
      ?: return Unit.also { LOGGER.info("apply(): have no messages to process, file: ${FileUtils.getFilePath(psiFile)}") }

    messages.forEach { compilationMessage ->
      when (compilationMessage.type) {
        ProcessFilesResponse.MessageType.INFO -> TODO()
        ProcessFilesResponse.MessageType.WARNING -> TODO()

        ProcessFilesResponse.MessageType.ERROR -> {
          LOGGER.info("apply(): Processing message with type ERROR, file: ${FileUtils.getFilePath(psiFile)}")
          if (!DICatPsiUtils.isValidRangeInFile(psiFile, compilationMessage.position))
            return@forEach Unit
              .also { LOGGER.error("Response position is not valid for file: ${FileUtils.getFilePath(psiFile)}") }

          val message = compilationMessage.details
            ?.let { details -> """${compilationMessage.code}: ${compilationMessage.description} $details""" }
            ?: """${compilationMessage.code}: ${compilationMessage.description}"""

          LOGGER.info("apply(): Adding new annotation with message: $message, file: ${FileUtils.getFilePath(psiFile)}")

          holder.newAnnotation(HighlightSeverity.ERROR, message)
            .range(compilationMessage.position.asTextRange())
            .create()
        }
      }
    }
  }

  data class DICatCollectedInfo(
    val filePath: String,
    val psiFile: PsiFile
  )

  data class DICatAnnotationResultType(
    val messages: List<ProcessFilesResponse.CompilationMessage>,
  ) {

    companion object {

      fun buildFromServiceResponse(response: ProcessFilesResponse, collectedInfo: DICatCollectedInfo) =
        DICatAnnotationResultType(
          messages = response.compilationMessages.filter { it.filePath == collectedInfo.filePath },
        )
    }
  }
}
