package com.antlers.support.editor

import com.antlers.support.lexer.AntlersLexerAdapter
import com.antlers.support.lexer.AntlersTokenSets
import com.antlers.support.psi.AntlersModifier
import com.antlers.support.psi.AntlersTagName
import com.intellij.lang.cacheBuilder.DefaultWordsScanner
import com.intellij.lang.findUsages.FindUsagesProvider
import com.intellij.psi.PsiElement
import com.intellij.psi.tree.TokenSet

/**
 * Enables **Edit > Find Usages** for Antlers tag names and modifiers.
 *
 * The [DefaultWordsScanner] indexes [AntlersTokenSets.IDENTIFIERS] tokens across all
 * Antlers files so the IDE can locate every usage of a given identifier without
 * a full project scan at query time.
 */
class AntlersFindUsagesProvider : FindUsagesProvider {

    override fun getWordsScanner() = DefaultWordsScanner(
        AntlersLexerAdapter(),
        AntlersTokenSets.IDENTIFIERS,  // words to index
        AntlersTokenSets.COMMENTS,     // skip comment content
        TokenSet.EMPTY                 // literals not indexed separately
    )

    override fun canFindUsagesFor(element: PsiElement): Boolean =
        element is AntlersTagName || element is AntlersModifier

    override fun getHelpId(element: PsiElement): String? = null

    override fun getType(element: PsiElement): String = when (element) {
        is AntlersTagName -> "Antlers tag"
        is AntlersModifier -> "Antlers modifier"
        else -> ""
    }

    override fun getDescriptiveName(element: PsiElement): String = when (element) {
        is AntlersTagName  -> element.text
        is AntlersModifier -> element.identifier?.text ?: element.text
        else               -> element.text
    }

    override fun getNodeText(element: PsiElement, useFullName: Boolean): String = element.text
}
