package com.github.artem1458.dicatplugin.exceptions

class NodeJSNotFoundException : RuntimeException() {
  override val message: String = "NodeJS interpreter not found"
}
