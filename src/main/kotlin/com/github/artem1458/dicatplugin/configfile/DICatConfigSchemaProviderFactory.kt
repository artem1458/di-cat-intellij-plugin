package com.github.artem1458.dicatplugin.configfile

import com.github.artem1458.dicatplugin.PathUtils
import com.intellij.lang.javascript.EmbeddedJsonSchemaFileProvider
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileManager
import com.jetbrains.jsonSchema.extension.JsonSchemaFileProvider
import com.jetbrains.jsonSchema.extension.JsonSchemaProviderFactory
import java.nio.file.Path

class DICatConfigSchemaProviderFactory : JsonSchemaProviderFactory {

  override fun getProviders(project: Project): MutableList<JsonSchemaFileProvider> {
    return mutableListOf(createProvider(project))
  }

  private fun createProvider(project: Project): JsonSchemaFileProvider {
    val dicatPath = PathUtils.getDICatNodeModulesPath(project) ?: throw RuntimeException("tratata")
    val schemaPath = Path.of(dicatPath.toString(), "config/schema.json")
    val schemaFile = VirtualFileManager.getInstance()
      .findFileByNioPath(schemaPath) ?: throw RuntimeException("tratatata")

    val provider = SchemaFileProvider(
      schemaFile
    )

    return provider
  }

  private class SchemaFileProvider(
    virtualFile: VirtualFile
  ) : EmbeddedJsonSchemaFileProvider(virtualFile) {
    override fun getPresentableName(): String = DICatConfigFileType.INSTANCE.name
    override fun isUserVisible(): Boolean = true
    override fun isAvailable(file: VirtualFile): Boolean {
      val fileType = file.fileType

      return fileType is DICatConfigFileType
    }
  }
}
