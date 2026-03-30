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

class CreateStatamicTagAction : DumbAwareAction(
    "Create Statamic Tag...",
    "Create a Statamic tag class scaffold under app/Tags.",
    AntlersIcons.FILE
) {
    private val dialogTitle = "Create Statamic Tag..."

    override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.BGT

    override fun update(event: AnActionEvent) {
        event.presentation.isEnabledAndVisible = actionContext(event).hasLaravelStructure
    }

    override fun actionPerformed(event: AnActionEvent) {
        val project = event.project ?: return
        val basePath = actionContext(event).basePath ?: return
        val rawName = Messages.showInputDialog(
            project,
            "Tag class name:",
            dialogTitle,
            Messages.getQuestionIcon()
        ) ?: return

        val className = StatamicSnippetTemplates.normalizeTagClassName(rawName)
        if (className == null) {
            Messages.showErrorDialog(project, "Enter a valid tag class name.", dialogTitle)
            return
        }

        try {
            WriteCommandAction.runWriteCommandAction(project, dialogTitle, null, Runnable {
                val tagDir = VfsUtil.createDirectoryIfMissing(basePath.resolve("app/Tags").toString())
                    ?: error("Unable to create app/Tags")
                val fileName = "$className.php"
                val existing = tagDir.findChild(fileName)
                if (existing != null) {
                    FileEditorManager.getInstance(project).openFile(existing, true)
                    return@Runnable
                }
                val file = tagDir.createChildData(this, fileName)
                VfsUtil.saveText(file, StatamicSnippetTemplates.buildTagClass(className))
                FileEditorManager.getInstance(project).openFile(file, true)
            })
        } catch (exception: IOException) {
            Messages.showErrorDialog(project, exception.message ?: "Failed to create tag.", dialogTitle)
        } catch (exception: IllegalStateException) {
            Messages.showErrorDialog(project, exception.message ?: "Failed to create tag.", dialogTitle)
        }
    }
}
