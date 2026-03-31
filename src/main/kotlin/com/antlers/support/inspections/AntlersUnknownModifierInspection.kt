package com.antlers.support.inspections

import com.antlers.support.psi.AntlersModifier
import com.antlers.support.psi.AntlersVisitor
import com.antlers.support.statamic.StatamicCatalog
import com.intellij.codeInspection.LocalInspectionTool
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.psi.PsiElementVisitor

/**
 * Warns when a modifier name is not found in [StatamicCatalog].
 *
 * Uses [ProblemHighlightType.WEAK_WARNING] because projects can define custom
 * modifiers — an unknown name is suspicious but not necessarily wrong.
 *
 * The inspection is toggleable via Settings > Editor > Inspections > Antlers.
 */
class AntlersUnknownModifierInspection : LocalInspectionTool() {

    override fun getDisplayName(): String = "Unknown Antlers modifier"
    override fun getGroupDisplayName(): String = "Antlers"
    override fun getShortName(): String = "AntlersUnknownModifier"

    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor {
        return object : AntlersVisitor() {
            override fun visitModifier(modifier: AntlersModifier) {
                val nameElement = modifier.identifier ?: return
                val name = nameElement.text

                if (StatamicCatalog.findModifier(name) == null) {
                    holder.registerProblem(
                        nameElement,
                        "Unknown modifier '$name' — not found in Statamic catalog",
                        ProblemHighlightType.WEAK_WARNING
                    )
                }
            }
        }
    }
}
