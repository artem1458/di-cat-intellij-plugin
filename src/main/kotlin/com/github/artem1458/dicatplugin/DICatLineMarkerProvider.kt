package com.github.artem1458.dicatplugin

import com.intellij.codeInsight.daemon.RelatedItemLineMarkerInfo
import com.intellij.codeInsight.daemon.RelatedItemLineMarkerProvider
import com.intellij.codeInsight.navigation.NavigationGutterIconBuilder
import com.intellij.openapi.util.IconLoader
import com.intellij.psi.PsiElement

class DICatLineMarkerProvider : RelatedItemLineMarkerProvider() {
  override fun collectNavigationMarkers(
    element: PsiElement,
    result: MutableCollection<in RelatedItemLineMarkerInfo<*>>
  ) {
    super.collectNavigationMarkers(element, result)
  }
}
