package com.antlers.support.intentions

import com.intellij.codeInsight.intention.IntentionAction
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.psi.PsiFile
import java.io.IOException
import java.nio.file.Path

class ExtractPartialIntention : IntentionAction {
    override fun getText(): String = "Extract to Antlers partial"
    override fun getFamilyName(): String = "Antlers"
    override fun startInWriteAction(): Boolean = false

    override fun isAvailable(project: Project, editor: Editor?, file: PsiFile?): Boolean {
        if (editor == null || file == null) return false
        val virtualFile = file.virtualFile ?: return false
        if (!virtualFile.name.contains(".antlers.")) return false
        return editor.selectionModel.hasSelection()
    }

    override fun invoke(project: Project, editor: Editor?, file: PsiFile?) {
        if (editor == null || file == null) return
        val selectionModel = editor.selectionModel
        if (!selectionModel.hasSelection()) return

        val selectedText = selectionModel.selectedText ?: return
        val basePath = project.basePath ?: return

        val partialName = Messages.showInputDialog(
            project,
            "Partial name (e.g. components/hero):",
            "Extract to Antlers Partial",
            Messages.getQuestionIcon()
        ) ?: return

        if (partialName.isBlank()) {
            Messages.showErrorDialog(project, "Enter a valid partial name.", "Extract to Antlers Partial")
            return
        }

        // Resolve target path: resources/views/partials/{name}.antlers.html
        val viewsDir = findViewsDirectory(basePath)
        if (viewsDir == null) {
            Messages.showErrorDialog(
                project,
                "Cannot find resources/views/ directory in your project.",
                "Extract to Antlers Partial"
            )
            return
        }

        val partialPath = "partials/$partialName"
        val dirPath = partialPath.substringBeforeLast('/', "partials")
        val fileName = partialPath.substringAfterLast('/') + ".antlers.html"

        try {
            WriteCommandAction.runWriteCommandAction(project, "Extract to Antlers Partial", null, Runnable {
                val targetDir = VfsUtil.createDirectoryIfMissing("$viewsDir/$dirPath")
                    ?: error("Unable to create directory: $dirPath")

                val existing = targetDir.findChild(fileName)
                if (existing != null) {
                    Messages.showErrorDialog(
                        project,
                        "Partial already exists: $partialPath",
                        "Extract to Antlers Partial"
                    )
                    return@Runnable
                }

                // Create the partial file with the selected content
                val newFile = targetDir.createChildData(this, fileName)
                VfsUtil.saveText(newFile, selectedText)

                // Replace the selection with a partial tag
                val document = editor.document
                val start = selectionModel.selectionStart
                val end = selectionModel.selectionEnd
                val replacement = "{{ partial:$partialName }}"
                document.replaceString(start, end, replacement)
                selectionModel.removeSelection()
                editor.caretModel.moveToOffset(start + replacement.length)

                // Open the new partial file
                FileEditorManager.getInstance(project).openFile(newFile, true)
            })
        } catch (e: IOException) {
            Messages.showErrorDialog(
                project,
                e.message ?: "Failed to create partial.",
                "Extract to Antlers Partial"
            )
        } catch (e: IllegalStateException) {
            Messages.showErrorDialog(
                project,
                e.message ?: "Failed to create partial.",
                "Extract to Antlers Partial"
            )
        }
    }

    private fun findViewsDirectory(basePath: String): String? {
        val viewsPath = Path.of(basePath, "resources", "views")
        return if (viewsPath.toFile().isDirectory) viewsPath.toString() else null
    }
}
