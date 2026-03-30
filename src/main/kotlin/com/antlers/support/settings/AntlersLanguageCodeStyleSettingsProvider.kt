package com.antlers.support.settings

import com.intellij.lang.Language
import com.intellij.psi.codeStyle.LanguageCodeStyleSettingsProvider
import com.antlers.support.AntlersLanguage

class AntlersLanguageCodeStyleSettingsProvider : LanguageCodeStyleSettingsProvider() {
    override fun getLanguage(): Language = AntlersLanguage.INSTANCE

    override fun getCodeSample(settingsType: SettingsType): String {
        return """
            {{ collection:blog limit="5" as="posts" }}
                <h2>{{ title }}</h2>
                <p>{{ content | truncate:200 }}</p>
                {{ if featured }}
                    <span>Featured</span>
                {{ /if }}
            {{ /collection:blog }}
        """.trimIndent()
    }
}
