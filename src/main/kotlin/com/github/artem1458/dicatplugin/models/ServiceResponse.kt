package com.github.artem1458.dicatplugin.models


data class ServiceResponse(
  val type: ResponseType,
  val payload: String?,
) {

  enum class ResponseType {
    FS,
    PROCESS_FILES,

    INIT,
    EXIT,
    ERROR,
  }
}
