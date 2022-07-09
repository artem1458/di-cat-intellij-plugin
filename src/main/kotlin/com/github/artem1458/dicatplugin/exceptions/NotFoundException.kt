package com.github.artem1458.dicatplugin.exceptions

data class NotFoundException(
  override val message: String
) : RuntimeException()
