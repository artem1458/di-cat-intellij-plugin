package com.github.artem1458.dicatplugin.configfile

import com.intellij.icons.AllIcons
import com.intellij.json.JsonLanguage
import com.intellij.openapi.fileTypes.LanguageFileType
import com.intellij.openapi.vfs.VirtualFile
import javax.swing.Icon

class DICatConfigFileType : LanguageFileType(JsonLanguage.INSTANCE) {
  companion object {
    val INSTANCE = DICatConfigFileType()
    val FILE_NAMES = setOf(
      ".dicatrc",
      ".dicatrc.json"
    )

    fun isDICatConfig(file: VirtualFile): Boolean =
      file.fileType is DICatConfigFileType || FILE_NAMES.contains(file.presentableName)
  }

  override fun isSecondary(): Boolean = true

  override fun getName(): String = "DICat Config"

  override fun getDescription(): String = "DICat configuration file"

  override fun getDefaultExtension(): String = ""
  override fun getIcon(): Icon = AllIcons.FileTypes.Json
}
