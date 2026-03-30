package com.antlers.support.settings

import com.intellij.openapi.options.Configurable
import com.intellij.ui.components.JBCheckBox
import com.intellij.util.ui.FormBuilder
import javax.swing.JComponent
import javax.swing.JPanel

class AntlersSettingsConfigurable : Configurable {
    private var panel: JPanel? = null
    private var autoCloseDelimiters: JBCheckBox? = null
    private var autoCloseQuotes: JBCheckBox? = null
    private var tagCompletion: JBCheckBox? = null
    private var modifierCompletion: JBCheckBox? = null
    private var variableCompletion: JBCheckBox? = null
    private var partialNavigation: JBCheckBox? = null

    override fun getDisplayName(): String = "Antlers"

    override fun createComponent(): JComponent {
        autoCloseDelimiters = JBCheckBox("Auto-close {{ }} delimiters")
        autoCloseQuotes = JBCheckBox("Auto-close quotes inside expressions")
        tagCompletion = JBCheckBox("Tag name completion")
        modifierCompletion = JBCheckBox("Modifier completion (after |)")
        variableCompletion = JBCheckBox("Variable completion")
        partialNavigation = JBCheckBox("Cmd+click navigation to partials")

        panel = FormBuilder.createFormBuilder()
            .addSeparator()
            .addComponent(autoCloseDelimiters!!)
            .addComponent(autoCloseQuotes!!)
            .addSeparator()
            .addComponent(tagCompletion!!)
            .addComponent(modifierCompletion!!)
            .addComponent(variableCompletion!!)
            .addSeparator()
            .addComponent(partialNavigation!!)
            .addComponentFillVertically(JPanel(), 0)
            .panel

        return panel!!
    }

    override fun isModified(): Boolean {
        val settings = AntlersSettings.getInstance().state
        return autoCloseDelimiters?.isSelected != settings.enableAutoCloseDelimiters
            || autoCloseQuotes?.isSelected != settings.enableAutoCloseQuotes
            || tagCompletion?.isSelected != settings.enableTagCompletion
            || modifierCompletion?.isSelected != settings.enableModifierCompletion
            || variableCompletion?.isSelected != settings.enableVariableCompletion
            || partialNavigation?.isSelected != settings.enablePartialNavigation
    }

    override fun apply() {
        val settings = AntlersSettings.getInstance().state
        settings.enableAutoCloseDelimiters = autoCloseDelimiters?.isSelected ?: true
        settings.enableAutoCloseQuotes = autoCloseQuotes?.isSelected ?: true
        settings.enableTagCompletion = tagCompletion?.isSelected ?: true
        settings.enableModifierCompletion = modifierCompletion?.isSelected ?: true
        settings.enableVariableCompletion = variableCompletion?.isSelected ?: true
        settings.enablePartialNavigation = partialNavigation?.isSelected ?: true
    }

    override fun reset() {
        val settings = AntlersSettings.getInstance().state
        autoCloseDelimiters?.isSelected = settings.enableAutoCloseDelimiters
        autoCloseQuotes?.isSelected = settings.enableAutoCloseQuotes
        tagCompletion?.isSelected = settings.enableTagCompletion
        modifierCompletion?.isSelected = settings.enableModifierCompletion
        variableCompletion?.isSelected = settings.enableVariableCompletion
        partialNavigation?.isSelected = settings.enablePartialNavigation
    }
}
