package com.antlers.support.actions

import com.antlers.support.AntlersIcons
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.vfs.VfsUtil
import java.io.IOException

class CreateStatamicModifierAction : DumbAwareAction(
    "Create Statamic Modifier...",
    "Create a Statamic modifier class scaffold under app/Modifiers.",
    AntlersIcons.FILE
) {
    private val dialogTitle = "Create Statamic Modifier..."

    override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.BGT

    override fun update(event: AnActionEvent) {
        event.presentation.isEnabledAndVisible = actionContext(event).hasLaravelStructure
    }

    override fun actionPerformed(event: AnActionEvent) {
        val project = event.project ?: return
        val basePath = actionContext(event).basePath ?: return
        val rawName = Messages.showInputDialog(
            project,
            "Modifier class name:",
            dialogTitle,
            Messages.getQuestionIcon()
        ) ?: return

        val className = StatamicSnippetTemplates.normalizeModifierClassName(rawName)
        if (className == null) {
            Messages.showErrorDialog(project, "Enter a valid modifier class name.", dialogTitle)
            return
        }

        try {
            WriteCommandAction.runWriteCommandAction(project, dialogTitle, null, Runnable {
                val modifierDir = VfsUtil.createDirectoryIfMissing(basePath.resolve("app/Modifiers").toString())
                    ?: error("Unable to create app/Modifiers")
                val fileName = "$className.php"
                val existing = modifierDir.findChild(fileName)
                if (existing != null) {
                    FileEditorManager.getInstance(project).openFile(existing, true)
                    return@Runnable
                }
                val file = modifierDir.createChildData(this, fileName)
                VfsUtil.saveText(file, StatamicSnippetTemplates.buildModifierClass(className))
                FileEditorManager.getInstance(project).openFile(file, true)
            })
        } catch (exception: IOException) {
            Messages.showErrorDialog(project, exception.message ?: "Failed to create modifier.", dialogTitle)
        } catch (exception: IllegalStateException) {
            Messages.showErrorDialog(project, exception.message ?: "Failed to create modifier.", dialogTitle)
        }
    }
}
