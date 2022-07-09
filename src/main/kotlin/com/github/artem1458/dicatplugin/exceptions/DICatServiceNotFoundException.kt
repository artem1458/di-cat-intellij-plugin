package com.github.artem1458.dicatplugin.exceptions

class DICatServiceNotFoundException : RuntimeException() {
  override val message: String = "DI Cat service path not found"
}
