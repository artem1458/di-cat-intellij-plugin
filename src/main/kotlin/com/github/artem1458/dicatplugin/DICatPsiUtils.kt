package com.github.artem1458.dicatplugin

import com.github.artem1458.dicatplugin.models.processfiles.NodePosition
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile

object DICatPsiUtils {

  fun isMatchingPsiElementByRange(
    element: PsiElement,
    position: NodePosition
  ) = element.textRange == position.asTextRange()

  fun isValidRangeInFile(file: PsiFile, nodePosition: NodePosition): Boolean = isValidRangeInFile(file, nodePosition.asTextRange())
  fun isValidRangeInFile(file: PsiFile, range: TextRange): Boolean = file.textRange.contains(range)

  fun getParentMatchingPosition(element: PsiElement?, nodePosition: NodePosition): PsiElement? {
    val startOffset = element?.textRange?.startOffset ?: return null

    if (startOffset > nodePosition.startOffset)
      return null

    if (isMatchingPsiElementByRange(element, nodePosition))
      return element

    return getParentMatchingPosition(element.parent, nodePosition)
  }
}
