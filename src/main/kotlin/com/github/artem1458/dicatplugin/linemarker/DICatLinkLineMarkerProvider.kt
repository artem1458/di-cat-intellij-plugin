package com.github.artem1458.dicatplugin.linemarker

import com.github.artem1458.dicatplugin.DICatBundle
import com.github.artem1458.dicatplugin.DICatPsiUtils
import com.github.artem1458.dicatplugin.models.processfiles.statistics.LinkStatistics
import com.intellij.codeInsight.daemon.RelatedItemLineMarkerInfo
import com.intellij.codeInsight.navigation.NavigationUtil
import com.intellij.lang.javascript.TypeScriptFileType
import com.intellij.navigation.GotoRelatedItem
import com.intellij.openapi.application.ReadAction
import com.intellij.openapi.editor.markup.GutterIconRenderer
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.openapi.ui.popup.ListPopupStep
import com.intellij.openapi.ui.popup.ListSeparator
import com.intellij.openapi.ui.popup.MnemonicNavigationFilter
import com.intellij.openapi.ui.popup.PopupStep
import com.intellij.openapi.ui.popup.SpeedSearchFilter
import com.intellij.openapi.ui.popup.util.BaseListPopupStep
import com.intellij.openapi.ui.popup.util.BaseStep
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiManager
import com.intellij.ui.awt.RelativePoint
import com.intellij.ui.popup.list.ListPopupImpl
import java.nio.file.Path
import javax.swing.Icon

class DICatLinkLineMarkerProvider(
  val project: Project
) {

  fun handle(
    element: PsiElement,
    result: MutableCollection<in RelatedItemLineMarkerInfo<*>>,
    linkStatistics: List<LinkStatistics>,
  ) {
    val gotoList = ReadAction.compute<List<Pair<LinkStatistics, MyGotoRelatedItem>>, Nothing> {
      linkStatistics.mapNotNull { linkStatistic ->
        if (!DICatPsiUtils.isMatchingPsiElementByRange(element, linkStatistic.fromPosition.nodePosition))
          return@mapNotNull null

        buildGotoRelatedItem(linkStatistic)?.let { linkStatistic to it }
      }
    }

    if (gotoList.isEmpty()) return

    gotoList.groupBy({ it.first.linkType }, { it.second }).forEach { (linkType, gotoItems) ->
      val sortedGotoItems = gotoItems.sortedBy { it.presentableName }

      result.add(buildRelatedItemLineMarkerInfo(element, linkType, sortedGotoItems))
    }
  }

  private fun buildGotoRelatedItem(linkStatistic: LinkStatistics): MyGotoRelatedItem? {
    val virtualFile = VirtualFileManager.getInstance().findFileByNioPath(Path.of(linkStatistic.toPosition.path))
      ?: return null
    val psiFile = PsiManager.getInstance(project).findFile(virtualFile)
      ?: return null
    val psiElement = psiFile.findElementAt(linkStatistic.toPosition.nodePosition.startOffset)
      ?: return null
    val matchingParent = DICatPsiUtils.getParentMatchingPosition(psiElement, linkStatistic.toPosition.nodePosition)
      ?: return null

    return MyGotoRelatedItem(matchingParent, linkStatistic.presentableName)
  }

  private fun buildRelatedItemLineMarkerInfo(
    element: PsiElement,
    linkType: LinkStatistics.LinkType,
    gotoList: List<MyGotoRelatedItem>
  ): RelatedItemLineMarkerInfo<*> {
    val hoveringTitle = if (gotoList.size > 1)
      DICatBundle.message("lineMarkerLinkType_${linkType.name}_plural")
    else
      DICatBundle.message("lineMarkerLinkType_${linkType.name}")
    val popupTitle = DICatBundle.message("lineMarkerLinkType_${linkType.name}_popupTitle")

    return RelatedItemLineMarkerInfo(
      element,
      element.textRange,
      TypeScriptFileType.INSTANCE.icon!!,
      { hoveringTitle },
      { mouseEvent, _ ->
        if (gotoList.size == 1)
          gotoList.first().navigate()
        else {
          JBPopupFactory.getInstance().createListPopup(
            MyListPopupStep(
              popupTitle,
              gotoList
            )
          )
          .show(RelativePoint(mouseEvent))
        }
      },
      GutterIconRenderer.Alignment.RIGHT,
      { gotoList }
    )
  }

  private inner class MyListPopupStep(
    private val myTitle: String,
    private val items: List<MyGotoRelatedItem>,
  ) : ListPopupStep<MyGotoRelatedItem>, BaseStep<MyGotoRelatedItem>() {

    override fun getTitle(): String = myTitle

    override fun getValues(): MutableList<MyGotoRelatedItem> = items.toMutableList()

    override fun getDefaultOptionIndex(): Int = 0

    override fun getTextFor(value: MyGotoRelatedItem?): String = value?.presentableName ?: "-"

    //TODO
    override fun getIconFor(value: MyGotoRelatedItem?): Icon? = null

    override fun onChosen(selectedValue: MyGotoRelatedItem?, finalChoice: Boolean): PopupStep<*>? {
      selectedValue?.navigate()

      return PopupStep.FINAL_CHOICE
    }

    override fun isAutoSelectionEnabled(): Boolean = false

    override fun canceled() {}

    override fun isSelectable(value: MyGotoRelatedItem?): Boolean = true
    override fun isMnemonicsNavigationEnabled(): Boolean = true
    override fun getMnemonicNavigationFilter(): MnemonicNavigationFilter<MyGotoRelatedItem> = this

    override fun isSpeedSearchEnabled(): Boolean = true
    override fun getSpeedSearchFilter(): SpeedSearchFilter<MyGotoRelatedItem> = this

    override fun getSeparatorAbove(value: MyGotoRelatedItem?): ListSeparator? = null
    override fun getFinalRunnable(): Runnable? = null
    override fun hasSubstep(selectedValue: MyGotoRelatedItem?): Boolean = false
  }

  private class MyGotoRelatedItem(
    element: PsiElement,
    val presentableName: String
  ) : GotoRelatedItem(element)
}
