package com.antlers.support.actions

import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.DefaultActionGroup

class StatamicProjectActionGroup : DefaultActionGroup() {
    override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.BGT

    override fun update(event: AnActionEvent) {
        val context = actionContext(event)
        event.presentation.isVisible = context.hasLaravelStructure
        event.presentation.isEnabled = context.hasLaravelStructure
    }
}

class StatamicPhpInsertActionGroup : DefaultActionGroup() {
    override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.BGT

    override fun update(event: AnActionEvent) {
        val context = actionContext(event)
        event.presentation.isVisible = context.hasLaravelStructure
        event.presentation.isEnabled = context.hasLaravelStructure
    }
}
