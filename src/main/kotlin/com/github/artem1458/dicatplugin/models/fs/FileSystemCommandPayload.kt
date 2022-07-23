package com.github.artem1458.dicatplugin.models.fs

interface FileSystemCommandPayload {
  val type: FSCommandType

  enum class FSCommandType {
    ADD,
    DELETE,
    MOVE,
  }

  data class Delete(
    val path: String
  ) : FileSystemCommandPayload {
    override val type = FSCommandType.DELETE
  }

  data class Move(
    val oldPath: String,
    val newPath: String,
    val content: String,
    val modificationStamp: Long?
  ) : FileSystemCommandPayload {
    override val type = FSCommandType.MOVE
  }

  data class Add(
    val path: String,
    val content: String,
    val modificationStamp: Long?,
  ) : FileSystemCommandPayload {
    override val type = FSCommandType.ADD
  }
}

data class BatchFileSystemCommandPayload(
  val commands: List<FileSystemCommandPayload>
)
