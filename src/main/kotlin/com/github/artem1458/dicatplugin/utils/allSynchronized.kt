package com.github.artem1458.dicatplugin.utils

fun <R> allSynchronized(lock: Any, vararg locks: Any, block: () -> R): R =
  _allSynchronized(lock, *locks, block = block)

private fun <R> _allSynchronized(vararg locks: Any, block: () -> R): R {
  return if (locks.isEmpty())
    block()
  else {
    synchronized(locks.first()) {
      _allSynchronized(*locks.slice(1 until locks.size).toTypedArray(), block = block)
    }
  }
}
