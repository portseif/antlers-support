package com.antlers.support.navigation

import com.antlers.support.AntlersLanguage
import com.antlers.support.file.AntlersFile
import com.antlers.support.psi.AntlersAntlersTag
import com.intellij.ide.navigationToolbar.StructureAwareNavBarModelExtension
import com.intellij.lang.Language

/**
 * Shows tag context in the navigation bar breadcrumb when the caret is positioned
 * inside an Antlers `{{ ... }}` tag delimiter.
 *
 * Because Antlers has a **flat PSI structure** (all tags are siblings, not nested
 * parents of the HTML content between them), the navbar can only show context for
 * elements that are truly hierarchically nested in the PSI tree — i.e., nodes
 * *within* a single `{{ ... }}` tag. When the caret is in template text between
 * tags the breadcrumb shows just the file name, which is the normal IDE behaviour.
 *
 * Example breadcrumbs:
 *  - Caret inside `{{ collection:blog }}` → `_blog.antlers.html  >  {{ collection:blog }}`
 *  - Caret inside `{{ if title }}` → `_blog.antlers.html  >  {{ if }}`
 *  - Caret in template HTML between tags → `_blog.antlers.html`
 */
class AntlersStructureAwareNavbar : StructureAwareNavBarModelExtension() {

    override val language: Language = AntlersLanguage.INSTANCE

    override fun getPresentableText(obj: Any?): String? = when (obj) {
        is AntlersFile -> obj.name

        is AntlersAntlersTag -> {
            val tagExpr  = obj.tagExpression
            val condTag  = obj.conditionalTag
            val closeTag = obj.closingTag
            when {
                tagExpr  != null               -> "{{ ${tagExpr.tagName.text} }}"
                condTag?.keywordIf     != null -> "{{ if }}"
                condTag?.keywordElseif != null -> "{{ elseif }}"
                condTag?.keywordUnless != null -> "{{ unless }}"
                condTag?.keywordElse   != null -> "{{ else }}"
                closeTag != null               -> "{{ /${closeTag.tagName?.text ?: "..."} }}"
                else                           -> null
            }
        }

        else -> null
    }
}
