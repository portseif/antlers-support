package com.antlers.support.settings

import com.intellij.openapi.options.Configurable
import com.intellij.ui.TitledSeparator
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
    private var hoverDocumentation: JBCheckBox? = null
    private var alpineJsInjection: JBCheckBox? = null
    private var phpInjection: JBCheckBox? = null
    private var semanticHighlighting: JBCheckBox? = null

    /**
     * Binds a single checkbox to its corresponding [AntlersSettings.State] property.
     * The three boilerplate methods (isModified / apply / reset) iterate this list
     * instead of repeating the same pattern for every checkbox.
     */
    private data class CheckboxField(
        val box: () -> JBCheckBox?,
        val read: (AntlersSettings.State) -> Boolean,
        val write: (AntlersSettings.State, Boolean) -> Unit
    )

    // Populated lazily so the box lambdas resolve after createComponent() runs.
    private val fields: List<CheckboxField> by lazy {
        listOf(
            CheckboxField({ autoCloseDelimiters },  { it.enableAutoCloseDelimiters },  { s, v -> s.enableAutoCloseDelimiters  = v }),
            CheckboxField({ autoCloseQuotes },      { it.enableAutoCloseQuotes },      { s, v -> s.enableAutoCloseQuotes      = v }),
            CheckboxField({ semanticHighlighting }, { it.enableSemanticHighlighting }, { s, v -> s.enableSemanticHighlighting = v }),
            CheckboxField({ tagCompletion },        { it.enableTagCompletion },        { s, v -> s.enableTagCompletion        = v }),
            CheckboxField({ modifierCompletion },   { it.enableModifierCompletion },   { s, v -> s.enableModifierCompletion   = v }),
            CheckboxField({ variableCompletion },   { it.enableVariableCompletion },   { s, v -> s.enableVariableCompletion   = v }),
            CheckboxField({ partialNavigation },    { it.enablePartialNavigation },    { s, v -> s.enablePartialNavigation    = v }),
            CheckboxField({ hoverDocumentation },   { it.enableHoverDocumentation },   { s, v -> s.enableHoverDocumentation   = v }),
            CheckboxField({ phpInjection },         { it.enablePhpInjection },         { s, v -> s.enablePhpInjection         = v }),
            CheckboxField({ alpineJsInjection },    { it.enableAlpineJsInjection },    { s, v -> s.enableAlpineJsInjection    = v }),
        )
    }

    override fun getDisplayName(): String = "Statamic"

    override fun createComponent(): JComponent {
        autoCloseDelimiters = JBCheckBox("Auto-close {{ }} delimiters")
        autoCloseQuotes = JBCheckBox("Auto-close quotes inside expressions")
        tagCompletion = JBCheckBox("Tag name completion")
        modifierCompletion = JBCheckBox("Modifier completion (after |)")
        variableCompletion = JBCheckBox("Variable completion")
        partialNavigation = JBCheckBox("Cmd+click navigation to partials")
        hoverDocumentation = JBCheckBox("Show Statamic documentation on hover")
        alpineJsInjection = JBCheckBox("Alpine.js intelligence in Antlers templates")
        phpInjection = JBCheckBox("PHP intelligence in {{? ?}} and {{$ $}} blocks")
        semanticHighlighting = JBCheckBox("Semantic highlighting for tag names and parameters")

        panel = FormBuilder.createFormBuilder()
            .addComponent(TitledSeparator("Editor"))
            .addComponent(autoCloseDelimiters!!)
            .addComponent(autoCloseQuotes!!)
            .addComponent(semanticHighlighting!!)
            .addComponent(TitledSeparator("Completion"))
            .addComponent(tagCompletion!!)
            .addComponent(modifierCompletion!!)
            .addComponent(variableCompletion!!)
            .addComponent(TitledSeparator("Navigation & Documentation"))
            .addComponent(partialNavigation!!)
            .addComponent(hoverDocumentation!!)
            .addComponent(TitledSeparator("Language Injection"))
            .addComponent(phpInjection!!)
            .addComponent(alpineJsInjection!!)
            .addComponentFillVertically(JPanel(), 0)
            .panel

        return panel!!
    }

    override fun isModified(): Boolean {
        val state = AntlersSettings.getInstance().state
        return fields.any { f -> f.box()?.isSelected != f.read(state) }
    }

    override fun apply() {
        val state = AntlersSettings.getInstance().state
        fields.forEach { f -> f.write(state, f.box()?.isSelected ?: true) }
    }

    override fun reset() {
        val state = AntlersSettings.getInstance().state
        fields.forEach { f -> f.box()?.isSelected = f.read(state) }
    }
}
