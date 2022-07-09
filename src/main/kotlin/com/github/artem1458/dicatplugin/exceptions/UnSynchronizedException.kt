package com.github.artem1458.dicatplugin.exceptions

data class UnSynchronizedException(
  override val message: String
) : RuntimeException()
