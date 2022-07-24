package com.github.artem1458.dicatplugin.exceptions

data class DICatNotFoundException(
  override val message: String
) : RuntimeException()
