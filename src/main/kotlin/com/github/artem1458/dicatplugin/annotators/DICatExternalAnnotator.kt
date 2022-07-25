package com.github.artem1458.dicatplugin.annotators

import com.github.artem1458.dicatplugin.PsiUtils
import com.github.artem1458.dicatplugin.components.DICatStatsRepository
import com.github.artem1458.dicatplugin.models.processfiles.ProcessFilesResponse
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.codeInspection.ex.ExternalAnnotatorBatchInspection
import com.intellij.lang.PsiBuilder
import com.intellij.lang.annotation.AnnotationHolder
import com.intellij.lang.annotation.ExternalAnnotator
import com.intellij.lang.annotation.HighlightSeverity
import com.intellij.lang.annotation.ProblemGroup
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
      filePath = file.virtualFile.path,
      psiFile = file
    )

  override fun doAnnotate(collectedInfo: DICatCollectedInfo?): DICatAnnotationResultType? {
    collectedInfo ?: return null
    val project = collectedInfo.psiFile.project
    val statsRepository = project.getComponent(DICatStatsRepository::class.java)

    return annotate(collectedInfo, statsRepository.getCurrent())
  }

  private fun annotate(
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

      return null
    }

    val currentModificationStamp = PsiUtils.getModificationStamp(collectedInfo.psiFile)
    val responseModificationStamp = processFilesResponse.modificationStamps[collectedInfo.filePath]
    val isCold = processFilesResponse.coldFilePaths.contains(collectedInfo.filePath)

    LOGGER.info("annotate(): " +
            "currentModificationStamp: $currentModificationStamp, " +
            "responseModificationStamp: $responseModificationStamp, " +
            "isCold: $isCold"
    )

    if (responseModificationStamp == currentModificationStamp || isCold) {
      LOGGER.info("annotate(): applying annotation")
      return DICatAnnotationResultType.buildFromServiceResponse(processFilesResponse, collectedInfo)
    }

    responseModificationStamp?.let {
      if(responseModificationStamp > currentModificationStamp)
        LOGGER.error(IllegalStateException("Modification stamp from service response is bigger that local"))
        .also { return null }
    }

    LOGGER.info("annotate(): waiting more new timestamp")
    val project = collectedInfo.psiFile.project
    val statsRepository = project.getComponent(DICatStatsRepository::class.java)

    return annotate(collectedInfo, statsRepository.getNext())
  }

  override fun apply(psiFile: PsiFile, annotationResult: DICatAnnotationResultType?, holder: AnnotationHolder) {
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
            .needsUpdateOnTyping(true)
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
