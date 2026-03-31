package com.antlers.support.navigation

import com.antlers.support.partials.AntlersPartialPaths
import com.intellij.navigation.ChooseByNameContributorEx
import com.intellij.navigation.NavigationItem
import com.intellij.psi.PsiManager
import com.intellij.psi.search.FilenameIndex
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.util.indexing.IdFilter
import com.intellij.util.Processor
import com.intellij.util.indexing.FindSymbolParameters

/**
 * Registers Antlers partial files as Go To Symbol targets so users can navigate
 * to any partial by name via **Navigate | Symbol** (Cmd+Alt+O).
 *
 * Only files under `resources/views/` are indexed — the same narrow scope used
 * by partial completion and Cmd+click navigation. This prevents the contributor
 * from scanning the whole project and triggering freezes in large codebases.
 */
class AntlersGotoSymbolContributor : ChooseByNameContributorEx {

    override fun processNames(
        processor: Processor<in String>,
        scope: GlobalSearchScope,
        filter: IdFilter?
    ) {
        val project = scope.project ?: return
        val viewsScope = AntlersPartialPaths.searchScope(project)
        val seen = mutableSetOf<String>()

        for (ext in AntlersPartialPaths.extensions()) {
            for (file in FilenameIndex.getAllFilesByExt(project, ext, viewsScope)) {
                val path = AntlersPartialPaths.lookupPath(file) ?: continue
                if (seen.add(path)) {
                    processor.process(path)
                }
            }
        }
    }

    override fun processElementsWithName(
        name: String,
        processor: Processor<in NavigationItem>,
        parameters: FindSymbolParameters
    ) {
        val project = parameters.project
        val psiManager = PsiManager.getInstance(project)
        val viewsScope = AntlersPartialPaths.searchScope(project)

        for (ext in AntlersPartialPaths.extensions()) {
            for (file in FilenameIndex.getAllFilesByExt(project, ext, viewsScope)) {
                val path = AntlersPartialPaths.lookupPath(file) ?: continue
                if (path == name) {
                    val psiFile = psiManager.findFile(file) ?: continue
                    processor.process(psiFile)
                }
            }
        }
    }
}
