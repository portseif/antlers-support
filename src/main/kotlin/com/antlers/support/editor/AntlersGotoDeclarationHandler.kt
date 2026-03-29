package com.antlers.support.editor

import com.intellij.codeInsight.navigation.actions.GotoDeclarationHandler
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiManager
import com.intellij.psi.search.FilenameIndex
import com.intellij.psi.search.GlobalSearchScope
import com.antlers.support.AntlersLanguage
import com.antlers.support.lexer.AntlersTokenTypes

class AntlersGotoDeclarationHandler : GotoDeclarationHandler {
    override fun getGotoDeclarationTargets(
        sourceElement: PsiElement?,
        offset: Int,
        editor: Editor
    ): Array<PsiElement>? {
        if (sourceElement == null) return null
        if (sourceElement.language != AntlersLanguage.INSTANCE) return null

        val partialPath = resolvePartialPath(sourceElement) ?: return null
        val targets = findPartialFiles(sourceElement.project, partialPath)
        return if (targets.isNotEmpty()) targets.toTypedArray() else null
    }

    private fun resolvePartialPath(element: PsiElement): String? {
        val elementType = element.node?.elementType ?: return null

        // Only handle identifiers and colons within Antlers expressions
        if (elementType != AntlersTokenTypes.IDENTIFIER &&
            elementType != AntlersTokenTypes.COLON &&
            elementType != AntlersTokenTypes.OP_DIVIDE
        ) return null

        // Walk backwards/forwards to collect the full tag expression: partial:path/to/file
        val parent = element.parent ?: return null
        val children = parent.children
        if (children.isEmpty()) {
            // Flat token structure — walk siblings instead
            return resolveFromSiblings(element)
        }
        return null
    }

    private fun resolveFromSiblings(element: PsiElement): String? {
        // Find the start of the tag expression by walking backwards
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

        // Now collect the full expression forward
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

        // Check if this is a partial tag: partial:path/to/name
        if (!expression.startsWith("partial:")) return null
        val path = expression.removePrefix("partial:")
        return if (path.isNotEmpty()) path else null
    }

    private fun findPartialFiles(project: Project, partialPath: String): List<PsiElement> {
        val psiManager = PsiManager.getInstance(project)
        val results = mutableListOf<PsiElement>()
        val scope = GlobalSearchScope.projectScope(project)

        // The partial path like "partials/sections/hero" maps to a view file
        // Try common extensions: .antlers.html, .antlers.php, .blade.php, .html
        val extensions = listOf("antlers.html", "antlers.php", "blade.php", "html")
        val fileName = partialPath.substringAfterLast("/")

        for (extension in extensions) {
            val fullFileName = "$fileName.$extension"
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
        // Check if the file's path ends with the expected partial path
        // e.g., partialPath = "partials/sections/hero"
        // file path should contain "partials/sections/hero.antlers.html"
        val normalizedPath = partialPath.replace("/", "/")
        val filePath = file.path
        return filePath.contains(normalizedPath)
    }
}
