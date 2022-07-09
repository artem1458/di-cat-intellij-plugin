package com.github.artem1458.dicatplugin.extensions

import com.fasterxml.jackson.databind.ObjectMapper

inline fun <reified T>ObjectMapper.readValue(value: String): T = this.readValue(value, T::class.java)
