package com.antlers.support.editor

import com.antlers.support.AntlersBlockTags
import com.intellij.codeInsight.editorActions.TypedHandlerDelegate
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.EditorModificationUtil
import com.intellij.openapi.fileTypes.FileType
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFile
import com.antlers.support.settings.AntlersSettings

class AntlersTypedHandler : TypedHandlerDelegate() {
    override fun charTyped(c: Char, project: Project, editor: Editor, file: PsiFile): Result {
        if (!file.name.contains(".antlers.")) return Result.CONTINUE

        return when (c) {
            '{' -> handleOpenBrace(editor, file)
            '/' -> handleSlash(editor, file)
            else -> Result.CONTINUE
        }
    }

    private fun handleOpenBrace(editor: Editor, file: PsiFile): Result {
        if (!AntlersSettings.getInstance().state.enableAutoCloseDelimiters) return Result.CONTINUE

        val offset = editor.caretModel.offset
        val document = editor.document
        val chars = document.charsSequence

        if (offset < 2) return Result.CONTINUE

        // Check if we just completed {{
        if (chars[offset - 2] != '{') return Result.CONTINUE

        // Don't auto-close if }} already follows
        if (offset + 1 < chars.length && chars[offset] == '}' && chars[offset + 1] == '}') {
            return Result.CONTINUE
        }

        // Remove stray } from built-in brace pairing of the first {
        if (offset < chars.length && chars[offset] == '}') {
            document.deleteString(offset, offset + 1)
        }

        // Insert "  }}" with cursor between the spaces
        EditorModificationUtil.insertStringAtCaret(editor, "  }}", false, true, 1)
        return Result.STOP
    }

    /**
     * When `/` is typed inside `{{ }}`, auto-complete the closing tag name
     * based on the nearest unclosed block tag.
     */
    private fun handleSlash(editor: Editor, file: PsiFile): Result {
        if (!AntlersSettings.getInstance().state.enableAutoCloseDelimiters) return Result.CONTINUE

        val offset = editor.caretModel.offset
        val document = editor.document
        val chars = document.charsSequence

        // Check if we're inside {{ / }} — the / was just typed, so look for {{ before it
        // Pattern: {{ (optional whitespace) / (caret is here)
        var checkIdx = offset - 2  // skip back past the /
        while (checkIdx >= 0 && chars[checkIdx].isWhitespace()) checkIdx--
        if (checkIdx < 1) return Result.CONTINUE
        if (chars[checkIdx] != '{' || chars[checkIdx - 1] != '{') return Result.CONTINUE

        // Find the nearest unclosed block tag by scanning backwards
        val tagName = findNearestUnclosedTag(chars, checkIdx - 1) ?: return Result.CONTINUE

        // Check if }} follows (from auto-closer)
        var afterIdx = offset
        while (afterIdx < chars.length && chars[afterIdx].isWhitespace()) afterIdx++
        val hasClosingBraces = afterIdx + 1 < chars.length &&
            chars[afterIdx] == '}' && chars[afterIdx + 1] == '}'

        if (hasClosingBraces) {
            // Insert just the tag name + space before the existing }}
            EditorModificationUtil.insertStringAtCaret(editor, "$tagName ", false, true)
        } else {
            // Insert tag name + space + }}
            EditorModificationUtil.insertStringAtCaret(editor, "$tagName }}", false, true)
        }

        return Result.STOP
    }

    /**
     * Scans backward through the document text to find the nearest unclosed block tag.
     * Uses a simple stack: closing tags push, opening block tags pop or return.
     */
    private fun findNearestUnclosedTag(chars: CharSequence, beforeOffset: Int): String? {
        val closedTags = mutableListOf<String>()  // stack of closed tag root names
        val tagPattern = Regex("""\{\{\s*(/?)(\s*\w[\w:/-]*)\s*[^}]*}}""")

        // Find all tag occurrences before the offset
        val text = chars.subSequence(0, beforeOffset).toString()
        val matches = tagPattern.findAll(text).toList()

        // Walk backwards through matches
        for (i in matches.lastIndex downTo 0) {
            val match = matches[i]
            val isClosing = match.groupValues[1] == "/"
            val tagName = match.groupValues[2].trim()
            val rootName = tagName.substringBefore(':')

            if (isClosing) {
                closedTags.add(rootName)
            } else if (AntlersBlockTags.isBlockTag(tagName)) {
                val closeIdx = closedTags.indexOfLast { it == rootName }
                if (closeIdx >= 0) {
                    closedTags.removeAt(closeIdx)
                } else {
                    // This is the nearest unclosed block tag
                    return tagName
                }
            }
        }

        return null
    }
}
