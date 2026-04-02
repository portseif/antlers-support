package com.antlers.support.lsp

import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.platform.lsp.api.ProjectWideLspServerDescriptor
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption

class AntlersLspServerDescriptor(project: Project) :
    ProjectWideLspServerDescriptor(project, "Antlers Language Server") {

    override fun isSupportedFile(file: VirtualFile): Boolean {
        return file.name.contains(".antlers.")
    }

    override fun createCommandLine(): GeneralCommandLine {
        val serverJs = extractBundledServer()
        val nodePath = findNodeJs()

        return GeneralCommandLine(nodePath, serverJs.toString(), "--stdio").apply {
            withWorkDirectory(project.basePath)
        }
    }

    /**
     * Extracts the bundled antlersls.js from the plugin JAR to a temp directory.
     * The file is cached so subsequent starts don't re-extract.
     */
    private fun extractBundledServer(): Path {
        val targetDir = Path.of(System.getProperty("java.io.tmpdir"), "antlers-lsp")
        val targetFile = targetDir.resolve("antlersls.js")

        if (Files.exists(targetFile)) return targetFile

        Files.createDirectories(targetDir)
        val resource = javaClass.getResourceAsStream("/language-server/antlersls.js")
            ?: error("Bundled Antlers language server not found in plugin resources")

        resource.use { input ->
            Files.copy(input, targetFile, StandardCopyOption.REPLACE_EXISTING)
        }

        return targetFile
    }

    /**
     * Finds the Node.js binary. Tries PhpStorm's configured interpreter first,
     * then falls back to 'node' on the system PATH.
     */
    private fun findNodeJs(): String {
        // Try PhpStorm's Node.js interpreter manager
        try {
            val interpreterClass = Class.forName("com.intellij.javascript.nodejs.interpreter.NodeJsInterpreterManager")
            val getInstance = interpreterClass.getMethod("getInstance", Project::class.java)
            val manager = getInstance.invoke(null, project)
            val getInterpreterRef = manager.javaClass.getMethod("getInterpreterRef")
            val ref = getInterpreterRef.invoke(manager)
            val resolve = ref.javaClass.getMethod("resolve", Project::class.java)
            val interpreter = resolve.invoke(ref, project)
            if (interpreter != null) {
                val referenceName = interpreter.javaClass.getMethod("getReferenceName")
                val path = referenceName.invoke(interpreter) as? String
                if (path != null && File(path).exists()) return path
            }
        } catch (_: Exception) {
            // Node.js plugin not available or interpreter not configured
        }

        // Fall back to 'node' on PATH
        return "node"
    }

    override fun getLanguageId(file: VirtualFile): String = "antlers"

    // Disable LSP features that our native PSI already handles well
    override val lspHoverSupport: Boolean get() = false
    override val lspGoToDefinitionSupport: Boolean get() = false
}
