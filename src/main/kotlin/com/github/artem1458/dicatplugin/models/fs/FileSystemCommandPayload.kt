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
    //If true - should ignore modificationStamp in annotator. Determines are this file was just opened and psi structure was not ready yet
    val isCold: Boolean = false
  ) : FileSystemCommandPayload {
    override val type = FSCommandType.ADD
  }
}

data class BatchFileSystemCommandPayload(
  val commands: List<FileSystemCommandPayload>
)
