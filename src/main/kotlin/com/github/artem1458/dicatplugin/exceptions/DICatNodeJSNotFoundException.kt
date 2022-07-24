package com.github.artem1458.dicatplugin.exceptions

class DICatNodeJSNotFoundException : RuntimeException() {
  override val message: String = "NodeJS interpreter not found"
}
