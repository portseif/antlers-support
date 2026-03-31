package com.antlers.support.editor

import com.intellij.codeInsight.navigation.actions.GotoDeclarationHandler
import com.intellij.lang.injection.InjectedLanguageManager
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiManager
import com.intellij.psi.search.FilenameIndex
import com.antlers.support.AntlersLanguage
import com.antlers.support.lexer.AntlersTokenTypes
import com.antlers.support.partials.AntlersPartialPaths
import com.antlers.support.settings.AntlersSettings

class AntlersGotoDeclarationHandler : GotoDeclarationHandler {
    override fun getGotoDeclarationTargets(
        sourceElement: PsiElement?,
        offset: Int,
        editor: Editor
    ): Array<PsiElement>? {
        if (sourceElement == null) return null

        val virtualFile = antlersVirtualFile(sourceElement) ?: return null
        if (!virtualFile.name.contains(".antlers.")) return null

        if (!AntlersSettings.getInstance().state.enablePartialNavigation) return null

        val project = sourceElement.project

        // Get the element from the Antlers PSI tree explicitly,
        // since the platform may give us an element from the HTML tree
        val viewProvider = InjectedLanguageManager.getInstance(project)
            .getTopLevelFile(sourceElement)
            .viewProvider
        val antlersFile = viewProvider.getPsi(AntlersLanguage.INSTANCE) ?: return null
        val antlersElement = antlersFile.findElementAt(offset) ?: return null

        val partialPath = resolvePartialPath(antlersElement) ?: return null
        val targets = findPartialFiles(project, partialPath)
        return if (targets.isNotEmpty()) targets.toTypedArray() else null
    }

    private fun antlersVirtualFile(sourceElement: PsiElement): VirtualFile? {
        val injectedLanguageManager = InjectedLanguageManager.getInstance(sourceElement.project)
        return injectedLanguageManager.getTopLevelFile(sourceElement).virtualFile
            ?: sourceElement.containingFile?.virtualFile
    }

    private fun resolvePartialPath(element: PsiElement): String? {
        val elementType = element.node?.elementType ?: return null

        if (elementType != AntlersTokenTypes.IDENTIFIER &&
            elementType != AntlersTokenTypes.COLON &&
            elementType != AntlersTokenTypes.OP_DIVIDE
        ) return null

        return resolveFromSiblings(element)
    }

    private fun resolveFromSiblings(element: PsiElement): String? {
        // Walk backwards to find the start of the tag expression
        var current: PsiElement? = element
        while (current?.prevSibling != null) {
            val prev = current.prevSibling
            val prevType = prev.node?.elementType
            if (prevType == AntlersTokenTypes.IDENTIFIER ||
                prevType == AntlersTokenTypes.COLON ||
                prevType == AntlersTokenTypes.OP_DIVIDE
            ) {
                current = prev
            } else {
                break
            }
        }

        // Collect the full expression forward
        val expressionBuilder = StringBuilder()
        var node: PsiElement? = current
        while (node != null) {
            val nodeType = node.node?.elementType
            if (nodeType == AntlersTokenTypes.IDENTIFIER ||
                nodeType == AntlersTokenTypes.COLON ||
                nodeType == AntlersTokenTypes.OP_DIVIDE
            ) {
                expressionBuilder.append(node.text)
                node = node.nextSibling
            } else {
                break
            }
        }

        val expression = expressionBuilder.toString()

        if (!expression.startsWith("partial:")) return null
        val path = expression.removePrefix("partial:")
        return if (path.isNotEmpty()) path else null
    }

    private fun findPartialFiles(project: Project, partialPath: String): List<PsiElement> {
        val psiManager = PsiManager.getInstance(project)
        val results = mutableListOf<PsiElement>()
        val scope = AntlersPartialPaths.searchScope(project)

        // Search by exact filename with path matching
        for (fullFileName in AntlersPartialPaths.candidateFileNames(partialPath)) {
            val files = FilenameIndex.getVirtualFilesByName(fullFileName, scope)
            for (file in files) {
                if (matchesPartialPath(file, partialPath)) {
                    psiManager.findFile(file)?.let { results.add(it) }
                }
            }
        }

        return results
    }

    private fun matchesPartialPath(file: VirtualFile, partialPath: String): Boolean {
        return AntlersPartialPaths.matches(file, partialPath)
    }
}
