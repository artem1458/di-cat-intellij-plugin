package com.github.artem1458.dicatplugin

import com.intellij.DynamicBundle
import org.jetbrains.annotations.NonNls
import org.jetbrains.annotations.PropertyKey

@NonNls
private const val BUNDLE = "messages.DICatBundle"

object DICatBundle : DynamicBundle(BUNDLE) {

  @Suppress("SpreadOperator")
  @JvmStatic
  fun message(@PropertyKey(resourceBundle = BUNDLE) key: String, vararg params: Any) =
    messageOrNull(key, *params) ?: "DI_CAT_NO_CONTENT"

  @Suppress("SpreadOperator", "unused")
  @JvmStatic
  fun messagePointer(@PropertyKey(resourceBundle = BUNDLE) key: String, vararg params: Any) =
    getLazyMessage(key, *params)
}
