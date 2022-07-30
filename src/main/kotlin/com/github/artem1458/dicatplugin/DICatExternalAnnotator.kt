package com.github.artem1458.dicatplugin

import com.github.artem1458.dicatplugin.components.DICatStatsRepository
import com.github.artem1458.dicatplugin.models.processfiles.ProcessFilesResponse
import com.intellij.codeInspection.ex.ExternalAnnotatorBatchInspection
import com.intellij.lang.annotation.AnnotationHolder
import com.intellij.lang.annotation.ExternalAnnotator
import com.intellij.lang.annotation.HighlightSeverity
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiFile
import java.util.concurrent.CancellationException
import java.util.concurrent.Future

class DICatExternalAnnotator :
  ExternalAnnotator<DICatExternalAnnotator.DICatCollectedInfo, DICatExternalAnnotator.DICatAnnotationResultType>(),
  ExternalAnnotatorBatchInspection {

  private val LOGGER = Logger.getInstance(javaClass)

  override fun getShortName(): String = "DI Cat"

  override fun collectInformation(file: PsiFile, editor: Editor, hasErrors: Boolean) =
    DICatCollectedInfo(
      filePath = FileUtils.getFilePath(file),
      psiFile = file
    ).also {
      LOGGER.info("Info collected for file: ${it.filePath}")
    }

  override fun doAnnotate(collectedInfo: DICatCollectedInfo?): DICatAnnotationResultType? {
    LOGGER.info("doAnnotate(): Starting annotation process")
    collectedInfo ?: return null
    val project = collectedInfo.psiFile.project
    val statsRepository = project.service<DICatStatsRepository>()

    return myAnnotate(collectedInfo, statsRepository.getCurrent())
  }

  private fun myAnnotate(
    collectedInfo: DICatCollectedInfo,
    futureProcessFilesResponse: Future<ProcessFilesResponse>,
  ): DICatAnnotationResultType? {
    LOGGER.info("annotate(): Starting annotation process for file: ${collectedInfo.filePath}")
    val processFilesResponse = runCatching {
      futureProcessFilesResponse.get()
    }.getOrElse {
      if (it !is CancellationException) {
        LOGGER.error(it)
      } else {
        LOGGER.info("Future is canceled", it)
      }

      return null.also{
        LOGGER.info("annotate(): Skipping annotation process, returning null")
      }
    }

    val currentModificationStamp = FileUtils.getModificationStamp(collectedInfo.psiFile)
    val responseModificationStamp = processFilesResponse.modificationStamps[collectedInfo.filePath]

    LOGGER.info("annotate(): " +
            "currentModificationStamp: $currentModificationStamp, " +
            "responseModificationStamp: $responseModificationStamp, "
    )

    if (responseModificationStamp == currentModificationStamp) {
      LOGGER.info("annotate(): applying annotation. file: ${collectedInfo.filePath}")
      return DICatAnnotationResultType.buildFromServiceResponse(processFilesResponse, collectedInfo)
    }

    if (responseModificationStamp !== null && currentModificationStamp !== null) {
      if(responseModificationStamp > currentModificationStamp)
        LOGGER.error(IllegalStateException("Modification stamp from service response is bigger that local. file: ${collectedInfo.filePath}"))
          .also { return null }
    }

    LOGGER.info("annotate(): waiting more new timestamp. file: ${collectedInfo.filePath}")
    val project = collectedInfo.psiFile.project
    val statsRepository = project.service<DICatStatsRepository>()

    return myAnnotate(collectedInfo, statsRepository.getNext())
  }

  override fun apply(psiFile: PsiFile, annotationResult: DICatAnnotationResultType?, holder: AnnotationHolder) {
    LOGGER.info("apply(): applying annotation for file: ${FileUtils.getFilePath(psiFile)}")
    annotationResult?.messages?.forEach { compilationMessage ->
      when (compilationMessage.type) {
        ProcessFilesResponse.MessageType.INFO -> TODO()
        ProcessFilesResponse.MessageType.WARNING -> TODO()

        ProcessFilesResponse.MessageType.ERROR -> {
          val message = compilationMessage.details
            ?.let { details -> """${compilationMessage.code}: ${compilationMessage.description} $details""" }
            ?: compilationMessage.description

          holder.newAnnotation(HighlightSeverity.ERROR, message)
            .range(TextRange(compilationMessage.position.startOffset, compilationMessage.position.endOffset))
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
