package com.github.artem1458.dicatplugin.models.fs

typealias Path = String
typealias Content = String

interface FileSystemCommandPayload {
  val type: FSCommandType

  enum class FSCommandType {
    ADD,
    DELETE,
  }

  data class Delete(
    val paths: MutableList<Path> = mutableListOf()
  ) : FileSystemCommandPayload {
    override val type = FSCommandType.DELETE
  }

  data class Add(
    val files: MutableMap<Path, Content> = mutableMapOf()
  ) : FileSystemCommandPayload {
    override val type = FSCommandType.ADD
  }
}

data class BatchFileSystemCommandPayload(
  val commands: List<FileSystemCommandPayload>
)
