package com.github.artem1458.dicatplugin

import com.intellij.codeInspection.InspectionManager
import com.intellij.codeInspection.LocalInspectionTool
import com.intellij.codeInspection.LocalInspectionToolSession
import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.codeInspection.ex.PairedUnfairLocalInspectionTool
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiElementVisitor
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiNamedElement

class DICatInspection : LocalInspectionTool(), PairedUnfairLocalInspectionTool {
  override fun getInspectionForBatchShortName(): String = "DI Cat"

  override fun runForWholeFile(): Boolean {
    return super.runForWholeFile()
  }

  override fun checkFile(file: PsiFile, manager: InspectionManager, isOnTheFly: Boolean): Array<ProblemDescriptor>? {
    return super.checkFile(file, manager, isOnTheFly)
  }

  override fun buildVisitor(
    holder: ProblemsHolder,
    isOnTheFly: Boolean,
    session: LocalInspectionToolSession
  ): PsiElementVisitor {
    return super.buildVisitor(holder, isOnTheFly, session)
  }

  override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor {
    return super.buildVisitor(holder, isOnTheFly)
  }

  override fun getProblemElement(psiElement: PsiElement): PsiNamedElement? {
    return super.getProblemElement(psiElement)
  }

  override fun inspectionStarted(session: LocalInspectionToolSession, isOnTheFly: Boolean) {
    super.inspectionStarted(session, isOnTheFly)
  }

  override fun inspectionFinished(session: LocalInspectionToolSession, problemsHolder: ProblemsHolder) {
    super.inspectionFinished(session, problemsHolder)
  }

  override fun processFile(file: PsiFile, manager: InspectionManager): MutableList<ProblemDescriptor> {
    return super.processFile(file, manager)
  }
}
