package com.antlers.support.editor

import com.antlers.support.AntlersBlockTags
import com.intellij.codeInsight.editorActions.enter.EnterHandlerDelegate
import com.intellij.codeInsight.editorActions.enter.EnterHandlerDelegateAdapter
import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.actionSystem.EditorActionHandler
import com.intellij.openapi.util.Ref
import com.intellij.psi.PsiFile

class AntlersEnterHandler : EnterHandlerDelegateAdapter() {
    override fun preprocessEnter(
        file: PsiFile,
        editor: Editor,
        caretOffset: Ref<Int>,
        caretAdvance: Ref<Int>,
        dataContext: DataContext,
        originalHandler: EditorActionHandler?
    ): EnterHandlerDelegate.Result {
        if (!file.name.contains(".antlers.")) return EnterHandlerDelegate.Result.Continue

        val offset = caretOffset.get()
        val document = editor.document
        val chars = document.charsSequence

        if (offset < 2 || offset + 2 >= chars.length) return EnterHandlerDelegate.Result.Continue

        // Check if caret is between {{ tag }}|{{ /tag }}
        // Look backwards for }}
        if (!findTokenBefore(chars, offset, "}}")) return EnterHandlerDelegate.Result.Continue

        // Look forwards for {{ /
        val afterOpenIdx = indexOfSkippingWhitespace(chars, offset, "{{")
        if (afterOpenIdx < 0) return EnterHandlerDelegate.Result.Continue

        // Check that {{ is followed by optional whitespace then /
        var checkIdx = afterOpenIdx + 2
        while (checkIdx < chars.length && chars[checkIdx].isWhitespace()) checkIdx++
        if (checkIdx >= chars.length || chars[checkIdx] != '/') return EnterHandlerDelegate.Result.Continue

        // Insert extra newline for the closing tag
        caretAdvance.set(1)
        return EnterHandlerDelegate.Result.Default
    }

    override fun postProcessEnter(
        file: PsiFile,
        editor: Editor,
        dataContext: DataContext
    ): EnterHandlerDelegate.Result {
        if (!file.name.contains(".antlers.")) return EnterHandlerDelegate.Result.Continue

        val document = editor.document
        val caretOffset = editor.caretModel.offset
        val caretLine = document.getLineNumber(caretOffset)
        if (caretLine == 0) return EnterHandlerDelegate.Result.Continue

        // Check the previous line for a block tag opener
        val prevLine = caretLine - 1
        val prevLineStart = document.getLineStartOffset(prevLine)
        val prevLineEnd = document.getLineEndOffset(prevLine)
        val prevLineText = document.charsSequence.subSequence(prevLineStart, prevLineEnd).toString().trim()

        val tagName = extractBlockTagName(prevLineText)
            ?: return EnterHandlerDelegate.Result.Continue

        if (!AntlersBlockTags.isBlockTag(tagName)) return EnterHandlerDelegate.Result.Continue

        // Add one indent level to the current line
        val codeStyleSettings = com.intellij.psi.codeStyle.CodeStyleSettingsManager.getInstance(file.project).currentSettings
        val indentOptions = codeStyleSettings.getIndentOptionsByFile(file)
        val indent = if (indentOptions.USE_TAB_CHARACTER) "\t" else " ".repeat(indentOptions.INDENT_SIZE.coerceAtLeast(1))

        // Get the previous line's indentation and add one level
        val prevIndent = document.charsSequence.subSequence(prevLineStart, prevLineEnd)
            .toString().takeWhile { it == ' ' || it == '\t' }
        val desiredIndent = prevIndent + indent

        // Get current line's indentation
        val curLineStart = document.getLineStartOffset(caretLine)
        val curLineEnd = document.getLineEndOffset(caretLine)
        val curLineText = document.charsSequence.subSequence(curLineStart, curLineEnd).toString()
        val curIndent = curLineText.takeWhile { it == ' ' || it == '\t' }

        if (curIndent != desiredIndent) {
            document.replaceString(curLineStart, curLineStart + curIndent.length, desiredIndent)
            editor.caretModel.moveToOffset(curLineStart + desiredIndent.length)
        }

        return EnterHandlerDelegate.Result.Continue
    }

    /**
     * Extracts the tag name from a line that looks like a block tag opener.
     * E.g., "{{ collection:blog from="posts" }}" returns "collection:blog"
     */
    private fun extractBlockTagName(trimmedLine: String): String? {
        if (!trimmedLine.startsWith("{{")) return null
        if (trimmedLine.startsWith("{{#")) return null  // comment
        if (trimmedLine.startsWith("{{?")) return null  // PHP raw
        if (trimmedLine.startsWith("{{$")) return null  // PHP echo

        val inner = trimmedLine.removePrefix("{{").trimStart()
        if (inner.startsWith("/")) return null  // closing tag

        // Extract the tag name (up to first space, pipe, or }})
        val nameEnd = inner.indexOfFirst { it == ' ' || it == '|' || it == '}' }
        val name = if (nameEnd > 0) inner.substring(0, nameEnd) else inner.removeSuffix("}}")
        return name.trim().ifEmpty { null }
    }

    private fun findTokenBefore(chars: CharSequence, offset: Int, token: String): Boolean {
        var idx = offset - 1
        while (idx >= 0 && chars[idx].isWhitespace()) idx--
        if (idx < token.length - 1) return false
        val start = idx - token.length + 1
        return chars.subSequence(start, idx + 1).toString() == token
    }

    private fun indexOfSkippingWhitespace(chars: CharSequence, offset: Int, token: String): Int {
        var idx = offset
        while (idx < chars.length && chars[idx].isWhitespace()) idx++
        if (idx + token.length > chars.length) return -1
        return if (chars.subSequence(idx, idx + token.length).toString() == token) idx else -1
    }
}
