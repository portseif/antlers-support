package com.antlers.support.highlighting

import com.intellij.lexer.Lexer
import com.intellij.openapi.editor.colors.TextAttributesKey
import com.intellij.openapi.fileTypes.SyntaxHighlighterBase
import com.intellij.psi.tree.IElementType
import com.antlers.support.lexer.AntlersLexerAdapter
import com.antlers.support.lexer.AntlersTokenSets
import com.antlers.support.lexer.AntlersTokenTypes

class AntlersSyntaxHighlighter : SyntaxHighlighterBase() {
    override fun getHighlightingLexer(): Lexer = AntlersLexerAdapter()

    override fun getTokenHighlights(tokenType: IElementType): Array<TextAttributesKey> {
        return HIGHLIGHTS[tokenType]
            ?: when {
                AntlersTokenSets.KEYWORDS.contains(tokenType) -> KEYWORD_KEYS
                AntlersTokenSets.NUMBERS.contains(tokenType) -> NUMBER_KEYS
                AntlersTokenSets.OPERATORS.contains(tokenType) -> OPERATOR_KEYS
                else -> TextAttributesKey.EMPTY_ARRAY
            }
    }

    companion object {
        // Pre-built keys arrays (allocated once)
        private val DELIMITER_KEYS = pack(AntlersHighlighterColors.DELIMITER)
        private val COMMENT_KEYS = pack(AntlersHighlighterColors.COMMENT)
        private val PHP_CONTENT_KEYS = pack(AntlersHighlighterColors.PHP_CONTENT)
        private val KEYWORD_KEYS = pack(AntlersHighlighterColors.KEYWORD)
        private val IDENTIFIER_KEYS = pack(AntlersHighlighterColors.IDENTIFIER)
        private val STRING_KEYS = pack(AntlersHighlighterColors.STRING)
        private val NUMBER_KEYS = pack(AntlersHighlighterColors.NUMBER)
        private val PIPE_KEYS = pack(AntlersHighlighterColors.PIPE)
        private val OPERATOR_KEYS = pack(AntlersHighlighterColors.OPERATOR)
        private val PUNCTUATION_KEYS = pack(AntlersHighlighterColors.PUNCTUATION)
        private val BAD_CHARACTER_KEYS = pack(AntlersHighlighterColors.BAD_CHARACTER)

        // O(1) map lookup for specific token types.
        // TokenSet-based lookups (keywords, numbers, operators) fall through to the when block.
        private val HIGHLIGHTS: Map<IElementType, Array<TextAttributesKey>> = buildMap {
            // Delimiters
            put(AntlersTokenTypes.ANTLERS_OPEN, DELIMITER_KEYS)
            put(AntlersTokenTypes.ANTLERS_CLOSE, DELIMITER_KEYS)
            put(AntlersTokenTypes.TAG_SELF_CLOSE, DELIMITER_KEYS)
            put(AntlersTokenTypes.PHP_RAW_OPEN, DELIMITER_KEYS)
            put(AntlersTokenTypes.PHP_RAW_CLOSE, DELIMITER_KEYS)
            put(AntlersTokenTypes.PHP_ECHO_OPEN, DELIMITER_KEYS)
            put(AntlersTokenTypes.PHP_ECHO_CLOSE, DELIMITER_KEYS)

            // Comments
            put(AntlersTokenTypes.COMMENT_OPEN, COMMENT_KEYS)
            put(AntlersTokenTypes.COMMENT_CLOSE, COMMENT_KEYS)
            put(AntlersTokenTypes.COMMENT_CONTENT, COMMENT_KEYS)

            // PHP content
            put(AntlersTokenTypes.PHP_RAW_CONTENT, PHP_CONTENT_KEYS)
            put(AntlersTokenTypes.PHP_ECHO_CONTENT, PHP_CONTENT_KEYS)

            // Identifiers
            put(AntlersTokenTypes.IDENTIFIER, IDENTIFIER_KEYS)

            // Strings
            put(AntlersTokenTypes.STRING_DQ, STRING_KEYS)
            put(AntlersTokenTypes.STRING_SQ, STRING_KEYS)

            // Pipe
            put(AntlersTokenTypes.OP_PIPE, PIPE_KEYS)

            // Punctuation
            put(AntlersTokenTypes.COLON, PUNCTUATION_KEYS)
            put(AntlersTokenTypes.DOT, PUNCTUATION_KEYS)
            put(AntlersTokenTypes.COMMA, PUNCTUATION_KEYS)
            put(AntlersTokenTypes.SEMICOLON, PUNCTUATION_KEYS)
            put(AntlersTokenTypes.LPAREN, PUNCTUATION_KEYS)
            put(AntlersTokenTypes.RPAREN, PUNCTUATION_KEYS)
            put(AntlersTokenTypes.LBRACKET, PUNCTUATION_KEYS)
            put(AntlersTokenTypes.RBRACKET, PUNCTUATION_KEYS)

            // Bad character
            put(AntlersTokenTypes.BAD_CHARACTER, BAD_CHARACTER_KEYS)
        }
    }
}
