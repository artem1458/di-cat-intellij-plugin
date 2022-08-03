package com.github.artem1458.dicatplugin.linemarker

import com.github.artem1458.dicatplugin.DICatStatsRepository
import com.github.artem1458.dicatplugin.FileUtils
import com.github.artem1458.dicatplugin.models.processfiles.ProcessFilesResponse
import com.github.artem1458.dicatplugin.models.processfiles.statistics.BaseStatistics
import com.github.artem1458.dicatplugin.models.processfiles.statistics.BeanDeclarationLinkStatistics
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
    if (!element.isValid || element !is LeafPsiElement) return
    val processFilesResponse = element.project.service<DICatStatsRepository>().getCurrentSync() ?: return

    val modificationStamp = FileUtils.getModificationStamp(element.containingFile.originalFile)
    val filePath = FileUtils.getFilePath(element.containingFile.originalFile)

    if (processFilesResponse.modificationStamps[filePath] != modificationStamp) return

    val beanDeclarations = mutableListOf<BeanDeclarationLinkStatistics>()

    fillStats(
      stats = processFilesResponse,
      filePath = filePath,
      beanDeclarations = beanDeclarations
    )

    val beanDeclarationLineMarker = element.project.service<DICatBeanDeclarationLineMarkerProvider>()

    beanDeclarationLineMarker.handle(element, result, beanDeclarations)
  }

  private fun fillStats(
    stats: ProcessFilesResponse,
    filePath: String,
    beanDeclarations: MutableList<BeanDeclarationLinkStatistics>,
  ) {
    stats.statistics.forEach { baseStatistics ->
      when (baseStatistics.type) {
        BaseStatistics.StatisticsType.BEAN_DEPENDENCIES -> {}

        BaseStatistics.StatisticsType.BEAN_DECLARATION_LINK ->
          handleBeanDeclarationLink(filePath, baseStatistics.payload, beanDeclarations)
      }
    }
  }

  private fun handleBeanDeclarationLink(
    filePath: String,
    payload: String,
    beanDeclarations: MutableList<BeanDeclarationLinkStatistics>
  ) {
    objectMapper.fromJson(payload, BeanDeclarationLinkStatistics::class.java).run {
      if (linkPosition.path == filePath)
        beanDeclarations.add(this)
    }
  }
}
