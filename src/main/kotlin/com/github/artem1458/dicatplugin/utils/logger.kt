package com.github.artem1458.dicatplugin.utils

import com.intellij.openapi.diagnostic.Logger

inline fun <reified R : Any> R.logger(): Logger = Logger.getInstance(this.javaClass) 

