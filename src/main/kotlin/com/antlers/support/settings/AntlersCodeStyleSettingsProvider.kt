package com.antlers.support.settings

import com.intellij.application.options.CodeStyleAbstractConfigurable
import com.intellij.application.options.CodeStyleAbstractPanel
import com.intellij.application.options.TabbedLanguageCodeStylePanel
import com.intellij.psi.codeStyle.CodeStyleConfigurable
import com.intellij.psi.codeStyle.CodeStyleSettings
import com.intellij.psi.codeStyle.CodeStyleSettingsProvider
import com.intellij.psi.codeStyle.CustomCodeStyleSettings
import com.intellij.lang.Language
import com.antlers.support.AntlersLanguage

class AntlersCodeStyleSettingsProvider : CodeStyleSettingsProvider() {
    override fun getLanguage(): Language = AntlersLanguage.INSTANCE

    override fun createCustomSettings(settings: CodeStyleSettings): CustomCodeStyleSettings {
        return AntlersCodeStyleSettings(settings)
    }

    override fun getConfigurableDisplayName(): String = "Antlers"

    override fun createConfigurable(
        settings: CodeStyleSettings,
        modelSettings: CodeStyleSettings
    ): CodeStyleConfigurable {
        return object : CodeStyleAbstractConfigurable(settings, modelSettings, configurableDisplayName) {
            override fun createPanel(settings: CodeStyleSettings): CodeStyleAbstractPanel {
                return AntlersCodeStylePanel(currentSettings, settings)
            }
        }
    }

    private class AntlersCodeStylePanel(
        currentSettings: CodeStyleSettings,
        settings: CodeStyleSettings
    ) : TabbedLanguageCodeStylePanel(AntlersLanguage.INSTANCE, currentSettings, settings)
}
