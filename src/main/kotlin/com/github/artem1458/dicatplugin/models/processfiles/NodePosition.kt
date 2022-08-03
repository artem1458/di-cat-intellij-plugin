package com.github.artem1458.dicatplugin.models.processfiles

import com.intellij.openapi.util.TextRange

data class NodePosition(
  val line: Int,
  val startColumn: Int,
  val endColumn: Int,
  val startOffset: Int,
  val endOffset: Int
) {

  fun asTextRange(): TextRange = TextRange.create(startOffset, endOffset)
}
