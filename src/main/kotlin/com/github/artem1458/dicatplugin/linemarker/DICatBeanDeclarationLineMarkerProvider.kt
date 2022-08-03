package com.github.artem1458.dicatplugin.linemarker

import com.github.artem1458.dicatplugin.DICatPsiUtils
import com.github.artem1458.dicatplugin.models.processfiles.statistics.BeanDeclarationLinkStatistics
import com.intellij.codeInsight.daemon.RelatedItemLineMarkerInfo
import com.intellij.codeInsight.navigation.NavigationUtil
import com.intellij.lang.javascript.TypeScriptFileType
import com.intellij.navigation.GotoRelatedItem
import com.intellij.openapi.application.ReadAction
import com.intellij.openapi.editor.markup.GutterIconRenderer
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.popup.util.BaseListPopupStep
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiManager
import com.intellij.ui.awt.RelativePoint
import com.intellij.ui.popup.list.ListPopupImpl
import java.nio.file.Path

class DICatBeanDeclarationLineMarkerProvider(
  val project: Project
) {

  fun handle(
    element: PsiElement,
    result: MutableCollection<in RelatedItemLineMarkerInfo<*>>,
    beanDeclarationLinks: List<BeanDeclarationLinkStatistics>,
  ) {
    handleBeanPosition(element, result, beanDeclarationLinks)
  }

  private fun handleBeanPosition(
    element: PsiElement,
    result: MutableCollection<in RelatedItemLineMarkerInfo<*>>,
    declarations: List<BeanDeclarationLinkStatistics>,
  ) {
    val gotoList = ReadAction.compute<List<GotoRelatedItem>, Nothing> {
      declarations.mapNotNull {
        if (!DICatPsiUtils.isMatchingPsiElementByRange(element, it.linkPosition.nodePosition))
          return@mapNotNull null

        buildGotoRelatedItem(it.contextPosition)
      }
    }

    if (gotoList.isEmpty()) return

    val lineMarkerInfo = buildRelatedItemLineMarkerInfo(element, gotoList)

    result.add(lineMarkerInfo)
  }

  private fun buildGotoRelatedItem(positionDescriptor: BeanDeclarationLinkStatistics.PositionDescriptor): GotoRelatedItem? {
    val virtualFile = VirtualFileManager.getInstance().findFileByNioPath(Path.of(positionDescriptor.path))
      ?: return null
    val psiFile = PsiManager.getInstance(project).findFile(virtualFile)
      ?: return null
    val psiElement = psiFile.findElementAt(positionDescriptor.nodePosition.startOffset)
      ?: return null
    val matchingParent = DICatPsiUtils.getParentMatchingPosition(psiElement, positionDescriptor.nodePosition)
      ?: return null

    return GotoRelatedItem(matchingParent)
  }

  private fun buildRelatedItemLineMarkerInfo(
    element: PsiElement,
    gotoList: List<GotoRelatedItem>
  ): RelatedItemLineMarkerInfo<*> {
    return RelatedItemLineMarkerInfo(
      element,
      element.textRange,
      TypeScriptFileType.INSTANCE.icon!!,
      { "Navigate to Bean declarations" },
      { mouseEvent, _ ->
        if (gotoList.size == 1)
          gotoList.first().navigate()
        else
//          val popupStep = BaseListPopupStep<PsiElement>()
//          val popup = MyListPopup<PsiElement>()
          NavigationUtil.getRelatedItemsPopup(gotoList, "NAVIGATE_TO_BEAN_DECLARATIONS")
            .show(RelativePoint(mouseEvent))
      },
      GutterIconRenderer.Alignment.RIGHT,
      { gotoList }
    )
  }

  private inner class MyListPopupStep : BaseListPopupStep<BeanDeclarationLinkStatistics>() {

  }

  private inner class MyListPopup(
    listPopupStep: MyListPopupStep
  ) : ListPopupImpl(project, listPopupStep) {
  }
}
