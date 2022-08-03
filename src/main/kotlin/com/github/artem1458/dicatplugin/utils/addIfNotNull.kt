package com.github.artem1458.dicatplugin.utils

fun <T> MutableCollection<T>.addIfNotNull(e: T?) {
  e?.let(::add)
}
