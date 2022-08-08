package com.github.artem1458.dicatplugin.models.processfiles.statistics

data class BaseStatistics(
  val type: StatisticsType,
  val payload: String
) {

  enum class StatisticsType {
    LINK,
  }
}
