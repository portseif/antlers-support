package com.antlers.support.inspections

import com.antlers.support.AntlersBlockTags
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
 * Simple bare identifiers (no `:`, `/`, or parameters) are suppressed because
 * they are almost always variables or contextual loop fields like `title`,
 * `entries`, `groups`, `items`, etc.
 *
 * The inspection is toggleable via Settings > Editor > Inspections > Antlers.
 */
class AntlersUnknownTagInspection : LocalInspectionTool() {

    override fun getDisplayName(): String = "Unknown Antlers tag"
    override fun getGroupDisplayName(): String = "Antlers"
    override fun getShortName(): String = "AntlersUnknownTag"

    override fun isEnabledByDefault(): Boolean = false

    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor {
        return object : AntlersVisitor() {
            override fun visitTagExpression(tag: AntlersTagExpression) {
                val tagNameElement = tag.tagName
                val name = tagNameElement.text

                // Skip known catalog tags
                if (StatamicCatalog.isKnownTag(name)) return

                // Skip known block tags (entries, groups, items, etc.)
                if (AntlersBlockTags.isBlockTag(name)) return

                // Skip simple bare identifiers — these are almost always variables
                // or contextual fields (title, content, slug, date, url, etc.)
                if (!name.contains(':') && !name.contains('/')) return

                holder.registerProblem(
                    tagNameElement,
                    "Unknown tag '$name' — not found in Statamic catalog",
                    ProblemHighlightType.WEAK_WARNING
                )
            }
        }
    }
}
