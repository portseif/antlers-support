package com.antlers.support.structure

import com.intellij.ide.projectView.PresentationData
import com.intellij.ide.structureView.StructureViewTreeElement
import com.intellij.ide.util.treeView.smartTree.SortableTreeElement
import com.intellij.ide.util.treeView.smartTree.TreeElement
import com.intellij.navigation.ItemPresentation
import com.intellij.psi.NavigatablePsiElement
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.psi.xml.XmlTag
import com.intellij.icons.AllIcons
import com.antlers.support.AntlersBlockTags
import com.antlers.support.AntlersIcons
import com.antlers.support.AntlersLanguage
import com.antlers.support.psi.*

/**
 * A virtual structure node that can hold pre-computed children.
 * Used to build a nested tree from the flat Antlers PSI.
 */
class AntlersStructureViewElement private constructor(
    private val element: PsiElement,
    private val virtualChildren: List<TreeElement>?
) : StructureViewTreeElement, SortableTreeElement {

    constructor(element: PsiElement) : this(element, null)

    companion object {
        private val LANDMARK_TAGS = setOf(
            "header", "main", "nav", "section", "footer", "article", "aside"
        )

        fun withChildren(element: PsiElement, children: List<TreeElement>): AntlersStructureViewElement {
            return AntlersStructureViewElement(element, children)
        }
    }

    override fun getValue(): Any = element

    override fun navigate(requestFocus: Boolean) {
        (element as? NavigatablePsiElement)?.navigate(requestFocus)
    }

    override fun canNavigate(): Boolean = (element as? NavigatablePsiElement)?.canNavigate() == true

    override fun canNavigateToSource(): Boolean =
        (element as? NavigatablePsiElement)?.canNavigateToSource() == true

    override fun getAlphaSortKey(): String = getPresentation().presentableText ?: ""

    override fun getPresentation(): ItemPresentation {
        return when (element) {
            is AntlersAntlersTag -> PresentationData(
                getTagPresentation(), null, AntlersIcons.FILE, null
            )
            is AntlersConditionalTag -> PresentationData(
                getConditionalPresentation(), null, AntlersIcons.FILE, null
            )
            is AntlersComment -> PresentationData(
                "{{# comment #}}", null, AntlersIcons.FILE, null
            )
            is AntlersNoparseBlock -> PresentationData(
                "{{ noparse }}", null, AntlersIcons.FILE, null
            )
            is XmlTag -> PresentationData(
                getHtmlLandmarkPresentation(element as XmlTag),
                null,
                AllIcons.Xml.Html_id,
                null
            )
            else -> PresentationData(
                element.containingFile?.name ?: "Antlers", null, AntlersIcons.FILE, null
            )
        }
    }

    override fun getChildren(): Array<TreeElement> {
        // If we have pre-computed virtual children, use them
        if (virtualChildren != null) return virtualChildren.toTypedArray()

        // For root file elements, build a nested tree from both PSI trees
        if (element is PsiFile) {
            return buildNestedTree(element).toTypedArray()
        }

        // For HTML landmarks, recurse into child landmarks
        if (element is XmlTag) {
            val children = mutableListOf<TreeElement>()
            for (child in element.children) {
                if (child is XmlTag && child.name.lowercase() in LANDMARK_TAGS) {
                    children.add(AntlersStructureViewElement(child))
                }
            }
            return children.toTypedArray()
        }

        return TreeElement.EMPTY_ARRAY
    }

    /**
     * Builds a nested tree from the flat Antlers PSI by matching opening/closing
     * tag pairs and nesting content between them as children.
     */
    private fun buildNestedTree(psiFile: PsiFile): List<TreeElement> {
        // Collect all structural elements (Antlers + HTML) sorted by offset
        val flatElements = mutableListOf<Pair<Int, PsiElement>>()

        for (child in psiFile.children) {
            when (child) {
                is AntlersAntlersTag,
                is AntlersConditionalTag,
                is AntlersComment,
                is AntlersNoparseBlock -> {
                    flatElements.add(child.textRange.startOffset to child)
                }
            }
        }

        // Collect HTML landmarks
        val viewProvider = psiFile.viewProvider
        val templateLang = viewProvider.languages.firstOrNull { !it.isKindOf(AntlersLanguage.INSTANCE) }
        if (templateLang != null) {
            val htmlFile = viewProvider.getPsi(templateLang)
            if (htmlFile != null) {
                PsiTreeUtil.processElements(htmlFile, XmlTag::class.java) { tag ->
                    if (tag.name.lowercase() in LANDMARK_TAGS && isTopLevelLandmark(tag)) {
                        flatElements.add(tag.textRange.startOffset to tag)
                    }
                    true
                }
            }
        }

        flatElements.sortBy { it.first }

        // Build nested structure using a stack
        return nestElements(flatElements.map { it.second })
    }

    private fun nestElements(elements: List<PsiElement>): List<TreeElement> {
        val result = mutableListOf<TreeElement>()
        val stack = ArrayDeque<Pair<String, MutableList<TreeElement>>>()
        // stack entries: (opening tag root name, children accumulated so far)

        for (element in elements) {
            if (element is AntlersAntlersTag) {
                val closingName = element.closingTag?.tagName?.text
                if (closingName != null) {
                    // This is a closing tag — pop the stack
                    val closeRoot = closingName.substringBefore(':')
                    val idx = stack.indexOfLast { it.first == closeRoot }
                    if (idx >= 0) {
                        // Pop everything from idx onwards, the children belong to the opener
                        while (stack.size > idx + 1) {
                            val (_, orphanChildren) = stack.removeLast()
                            stack.last().second.addAll(orphanChildren)
                        }
                        val (_, children) = stack.removeLast()
                        // Don't add closing tag to the tree — it's implicit
                        // The opener already has all its children
                        val target = if (stack.isNotEmpty()) stack.last().second else result
                        // Find the opener element that was already added and replace with nested version
                        // Actually, the opener is the last element in the parent's children list matching this name
                        // We need to find and wrap it
                        replaceLastOpenerWithNested(target, closeRoot, children)
                    }
                    continue
                }

                val tagExpr = element.tagExpression
                val tagName = tagExpr?.tagName?.text
                val conditionalTag = element.conditionalTag

                if (conditionalTag != null) {
                    // Conditionals: if/unless open a scope
                    val keyword = conditionalTag.firstChild?.text
                    if (keyword == "if" || keyword == "unless") {
                        val target = if (stack.isNotEmpty()) stack.last().second else result
                        target.add(AntlersStructureViewElement(element))
                        stack.addLast(keyword to mutableListOf())
                    } else if (keyword == "endif" || keyword == "endunless") {
                        val matchKey = if (keyword == "endif") "if" else "unless"
                        val idx = stack.indexOfLast { it.first == matchKey }
                        if (idx >= 0) {
                            while (stack.size > idx + 1) {
                                val (_, orphanChildren) = stack.removeLast()
                                stack.last().second.addAll(orphanChildren)
                            }
                            val (_, children) = stack.removeLast()
                            val target = if (stack.isNotEmpty()) stack.last().second else result
                            replaceLastConditionalWithNested(target, matchKey, children)
                        }
                    } else {
                        // else/elseif — just add as a child
                        val target = if (stack.isNotEmpty()) stack.last().second else result
                        target.add(AntlersStructureViewElement(element))
                    }
                    continue
                }

                if (tagName != null && AntlersBlockTags.isBlockTag(tagName)) {
                    // Block tag opener — push onto stack
                    val target = if (stack.isNotEmpty()) stack.last().second else result
                    target.add(AntlersStructureViewElement(element))
                    stack.addLast(tagName.substringBefore(':') to mutableListOf())
                } else {
                    // Regular tag — add to current scope
                    val target = if (stack.isNotEmpty()) stack.last().second else result
                    target.add(AntlersStructureViewElement(element))
                }
            } else {
                // HTML landmarks, comments, noparse — add to current scope
                val target = if (stack.isNotEmpty()) stack.last().second else result
                target.add(AntlersStructureViewElement(element))
            }
        }

        // Flush any unclosed scopes
        while (stack.isNotEmpty()) {
            val (_, children) = stack.removeLast()
            val target = if (stack.isNotEmpty()) stack.last().second else result
            target.addAll(children)
        }

        return result
    }

    /**
     * Finds the last element in the list whose tag name matches the given root name,
     * and replaces it with a version that has nested children.
     */
    private fun replaceLastOpenerWithNested(
        list: MutableList<TreeElement>,
        closeRoot: String,
        children: List<TreeElement>
    ) {
        for (i in list.lastIndex downTo 0) {
            val el = list[i]
            if (el is AntlersStructureViewElement && el.element is AntlersAntlersTag) {
                val tagExpr = PsiTreeUtil.findChildOfType(el.element, AntlersTagExpression::class.java)
                val tagName = PsiTreeUtil.findChildOfType(tagExpr ?: el.element, AntlersTagName::class.java)?.text
                if (tagName != null && tagName.substringBefore(':') == closeRoot) {
                    list[i] = withChildren(el.element, children)
                    return
                }
            }
        }
        // If no opener found, add orphan children to the list
        list.addAll(children)
    }

    private fun replaceLastConditionalWithNested(
        list: MutableList<TreeElement>,
        keyword: String,
        children: List<TreeElement>
    ) {
        for (i in list.lastIndex downTo 0) {
            val el = list[i]
            if (el is AntlersStructureViewElement && el.element is AntlersAntlersTag) {
                val cond = (el.element as AntlersAntlersTag).conditionalTag
                if (cond != null && cond.firstChild?.text == keyword) {
                    list[i] = withChildren(el.element, children)
                    return
                }
            }
        }
        list.addAll(children)
    }

    private fun isTopLevelLandmark(tag: XmlTag): Boolean {
        var parent = tag.parentTag
        while (parent != null) {
            if (parent.name.lowercase() in LANDMARK_TAGS) return false
            parent = parent.parentTag
        }
        return true
    }

    private fun getHtmlLandmarkPresentation(tag: XmlTag): String {
        val name = tag.name.lowercase()
        val id = tag.getAttributeValue("id")
        val cls = tag.getAttributeValue("class")

        return buildString {
            append("<$name")
            if (id != null) append(" id=\"$id\"")
            else if (cls != null) {
                val short = cls.split("\\s+".toRegex()).take(2).joinToString(" ")
                append(" class=\"$short\"")
            }
            append(">")
        }
    }

    private fun getTagPresentation(): String {
        val tag = element as AntlersAntlersTag
        val tagExpr = PsiTreeUtil.findChildOfType(tag, AntlersTagExpression::class.java)
        val tagName = PsiTreeUtil.findChildOfType(tagExpr ?: tag, AntlersTagName::class.java)
        val closingTag = PsiTreeUtil.findChildOfType(tag, AntlersClosingTag::class.java)

        val name = tagName?.text ?: closingTag?.let {
            val innerName = PsiTreeUtil.findChildOfType(it, AntlersTagName::class.java)
            "/${innerName?.text ?: "..."}"
        } ?: "..."

        return "{{ $name }}"
    }

    private fun getConditionalPresentation(): String {
        val cond = element as AntlersConditionalTag
        val firstChild = cond.firstChild ?: return "{{ ... }}"
        val keyword = firstChild.text
        return "{{ $keyword }}"
    }
}
