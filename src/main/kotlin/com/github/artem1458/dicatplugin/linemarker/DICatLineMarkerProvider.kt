package com.github.artem1458.dicatplugin.linemarker

import com.github.artem1458.dicatplugin.DICatModificationStampTracker
import com.github.artem1458.dicatplugin.DICatResponseHolder
import com.github.artem1458.dicatplugin.FileUtils
import com.github.artem1458.dicatplugin.models.processfiles.ProcessFilesResponse
import com.github.artem1458.dicatplugin.models.processfiles.statistics.BaseStatistics
import com.github.artem1458.dicatplugin.models.processfiles.statistics.LinkStatistics
import com.google.gson.Gson
import com.intellij.codeInsight.daemon.RelatedItemLineMarkerInfo
import com.intellij.codeInsight.daemon.RelatedItemLineMarkerProvider
import com.intellij.openapi.components.service
import com.intellij.psi.PsiElement
import com.intellij.psi.impl.source.tree.LeafPsiElement

class DICatLineMarkerProvider : RelatedItemLineMarkerProvider() {

  private val objectMapper = Gson()

  override fun collectNavigationMarkers(
    element: PsiElement,
    result: MutableCollection<in RelatedItemLineMarkerInfo<*>>
  ) {
    val modificationStampTracker = element.project.service<DICatModificationStampTracker>()
    if (!element.isValid || element !is LeafPsiElement) return
    val processFilesResponse = element.project.service<DICatResponseHolder>().getCurrentSync() ?: return

    val projectModificationStamp = modificationStampTracker.get()
    val filePath = FileUtils.getFilePath(element.containingFile.originalFile)

    if (processFilesResponse.projectModificationStamp != projectModificationStamp) return

    val linkStatistics = mutableListOf<LinkStatistics>()

    fillStats(
      stats = processFilesResponse,
      filePath = filePath,
      linkStatistics = linkStatistics
    )

    val linkLineMarkerProvider = element.project.service<DICatLinkLineMarkerProvider>()

    linkLineMarkerProvider.handle(element, result, linkStatistics)
  }

  private fun fillStats(
    stats: ProcessFilesResponse,
    filePath: String,
    linkStatistics: MutableList<LinkStatistics>,
  ) {
    stats.statistics.forEach { baseStatistics ->
      when (baseStatistics.type) {
        BaseStatistics.StatisticsType.LINK ->
          handleLink(filePath, baseStatistics.payload, linkStatistics)

        else -> {}
      }
    }
  }

  private fun handleLink(
    filePath: String,
    payload: String,
    linkStatistics: MutableList<LinkStatistics>
  ) {
    objectMapper.fromJson(payload, LinkStatistics::class.java).run {
      if (fromPosition.path == filePath) linkStatistics.add(this)
    }
  }
}
