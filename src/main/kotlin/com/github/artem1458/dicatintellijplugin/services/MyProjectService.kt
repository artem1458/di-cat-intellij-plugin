package com.github.artem1458.dicatintellijplugin.services

import com.intellij.openapi.project.Project
import com.github.artem1458.dicatintellijplugin.MyBundle

class MyProjectService(project: Project) {

    init {
        println(MyBundle.message("projectService", project.name))
    }
}
