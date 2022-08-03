package com.github.artem1458.dicatplugin.models.processfiles.statistics

import com.github.artem1458.dicatplugin.models.processfiles.NodePosition

data class BeanDeclarationLinkStatistics(
  val linkPosition: PositionDescriptor,
  val contextPosition: PositionDescriptor,
  val contextName: String,
  val beanNameInContext: String,
) {

  data class PositionDescriptor(
    val path: String,
    val nodePosition: NodePosition,
  )
}
