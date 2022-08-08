package com.github.artem1458.dicatplugin.models.processfiles.statistics

import com.github.artem1458.dicatplugin.models.processfiles.NodePosition

data class LinkStatistics(
  val linkType: LinkType,
  val fromPosition: LinkPositionDescriptor,
  val toPosition: LinkPositionDescriptor,
  val presentableName: String,
) {

  data class LinkPositionDescriptor(
    val path: String,
    val nodePosition: NodePosition,
  )

  enum class LinkType {
    BEAN_DECLARATION,
  }
}
