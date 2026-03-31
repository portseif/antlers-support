package com.antlers.support.completion

import com.intellij.codeInsight.completion.*
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.patterns.PlatformPatterns
import com.intellij.psi.search.FilenameIndex
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.util.ProcessingContext
import com.antlers.support.AntlersIcons
import com.antlers.support.AntlersLanguage
import com.antlers.support.lexer.AntlersTokenTypes
import com.antlers.support.partials.AntlersPartialPaths
import com.antlers.support.settings.AntlersSettings

class AntlersCompletionContributor : CompletionContributor() {
    init {
        extend(
            CompletionType.BASIC,
            PlatformPatterns.psiElement().withLanguage(AntlersLanguage.INSTANCE),
            AntlersCompletionProvider()
        )
    }
}

private class AntlersCompletionProvider : CompletionProvider<CompletionParameters>() {
    override fun addCompletions(
        parameters: CompletionParameters,
        context: ProcessingContext,
        result: CompletionResultSet
    ) {
        val position = parameters.position
        val settings = AntlersSettings.getInstance().state

        val prevLeaf = PsiTreeUtil.prevVisibleLeaf(position)
        val prevType = prevLeaf?.node?.elementType

        // After | pipe — suggest modifiers (pre-built, no allocations)
        if (prevType == AntlersTokenTypes.OP_PIPE) {
            if (!settings.enableModifierCompletion) return
            result.addAllElements(StatamicData.MODIFIER_ELEMENTS)
            return
        }

        // After colon — check context for tag scopes or partials
        if (prevType == AntlersTokenTypes.COLON) {
            val colonPrev = PsiTreeUtil.prevVisibleLeaf(prevLeaf!!)
            val tagName = colonPrev?.text

            // After partial: — suggest partial file paths
            if (tagName == "partial") {
                addPartialCompletions(parameters, result)
                return
            }

            // After tag: — suggest sub-tags (pre-built per namespace)
            if (tagName != null && settings.enableTagCompletion) {
                val subTagElements = StatamicData.getSubTagElements(tagName)
                if (subTagElements.isNotEmpty()) {
                    result.addAllElements(subTagElements)
                    return
                }
            }
        }

        // General completions: tags + variables (pre-built, no allocations)
        if (settings.enableTagCompletion) {
            result.addAllElements(StatamicData.TAG_ELEMENTS)
        }

        if (settings.enableVariableCompletion) {
            result.addAllElements(StatamicData.VARIABLE_ELEMENTS)
        }
    }

    private fun addPartialCompletions(parameters: CompletionParameters, result: CompletionResultSet) {
        val project = parameters.position.project
        val scope = AntlersPartialPaths.searchScope(project)
        val seen = mutableSetOf<String>()

        for (ext in AntlersPartialPaths.extensions()) {
            val files = FilenameIndex.getAllFilesByExt(project, ext, scope)
            for (file in files) {
                val partialPath = AntlersPartialPaths.lookupPath(file) ?: continue
                if (seen.add(partialPath)) {
                    result.addElement(
                        LookupElementBuilder.create(partialPath)
                            .withTypeText(file.name, true)
                            .withIcon(AntlersIcons.FILE)
                    )
                }
            }
        }
    }
}
