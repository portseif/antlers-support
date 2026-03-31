package com.antlers.support.inspections

import com.antlers.support.psi.AntlersTagExpression
import com.antlers.support.psi.AntlersVisitor
import com.antlers.support.statamic.StatamicCatalog
import com.intellij.codeInspection.LocalInspectionTool
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.psi.PsiElementVisitor

/**
 * Warns when an Antlers tag name is not found in [StatamicCatalog].
 *
 * Uses [ProblemHighlightType.WEAK_WARNING] since projects routinely define custom
 * tags — an unknown name is suspicious but not always an error.
 *
 * [StatamicCatalog.isKnownTag] handles both full names (`nav:breadcrumbs`) and
 * root-only lookups (`nav`) so namespaced sub-tags are not falsely flagged.
 *
 * The inspection is toggleable via Settings > Editor > Inspections > Antlers.
 */
class AntlersUnknownTagInspection : LocalInspectionTool() {

    override fun getDisplayName(): String = "Unknown Antlers tag"
    override fun getGroupDisplayName(): String = "Antlers"
    override fun getShortName(): String = "AntlersUnknownTag"

    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor {
        return object : AntlersVisitor() {
            override fun visitTagExpression(tag: AntlersTagExpression) {
                val tagNameElement = tag.tagName
                val name = tagNameElement.text

                if (!StatamicCatalog.isKnownTag(name)) {
                    holder.registerProblem(
                        tagNameElement,
                        "Unknown tag '$name' — not found in Statamic catalog",
                        ProblemHighlightType.WEAK_WARNING
                    )
                }
            }
        }
    }
}
