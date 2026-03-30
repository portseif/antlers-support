package com.antlers.support.actions

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class StatamicSnippetTemplatesTest {
    @Test
    fun exposesExpectedContentQueryTemplates() {
        assertEquals(
            listOf(
                "Entry Query",
                "Single Entry Query",
                "Paginated Entry Query",
                "Global Set Lookup"
            ),
            StatamicSnippetTemplates.contentQueries.map { it.title }
        )
    }

    @Test
    fun normalizesControllerClassNames() {
        assertEquals("ExampleController", StatamicSnippetTemplates.normalizeControllerClassName("Example"))
        assertEquals("BlogPostsController", StatamicSnippetTemplates.normalizeControllerClassName("blog posts"))
        assertEquals("ExampleController", StatamicSnippetTemplates.normalizeControllerClassName("ExampleController"))
        assertNull(StatamicSnippetTemplates.normalizeControllerClassName("///"))
    }

    @Test
    fun buildsBasicControllerTemplate() {
        val template = StatamicSnippetTemplates.buildBasicController("ExampleController")

        assertTrue(template.contains("class ExampleController extends Controller"))
        assertTrue(template.contains("return view('myview', \$data);"))
    }

    @Test
    fun buildsAntlersViewControllerTemplate() {
        val template = StatamicSnippetTemplates.buildAntlersViewController("ExampleController")

        assertTrue(template.contains("use Statamic\\View\\View;"))
        assertTrue(template.contains("->template('myview')"))
        assertTrue(template.contains("->layout('mylayout')"))
    }

    @Test
    fun normalizesTagClassNames() {
        assertEquals("HeroBars", StatamicSnippetTemplates.normalizeTagClassName("hero bars"))
        assertEquals("MyTag", StatamicSnippetTemplates.normalizeTagClassName("MyTag"))
        assertNull(StatamicSnippetTemplates.normalizeTagClassName("///"))
    }

    @Test
    fun normalizesModifierClassNames() {
        assertEquals("Repeat", StatamicSnippetTemplates.normalizeModifierClassName("repeat"))
        assertEquals("UpperCaseWords", StatamicSnippetTemplates.normalizeModifierClassName("upper case words"))
        assertNull(StatamicSnippetTemplates.normalizeModifierClassName("///"))
    }

    @Test
    fun buildsTagClassTemplate() {
        val template = StatamicSnippetTemplates.buildTagClass("HeroBars")

        assertTrue(template.contains("namespace App\\Tags;"))
        assertTrue(template.contains("use Statamic\\Tags\\Tags;"))
        assertTrue(template.contains("class HeroBars extends Tags"))
        assertTrue(template.contains("return 'Hello from the HeroBars tag.';"))
    }

    @Test
    fun buildsModifierClassTemplate() {
        val template = StatamicSnippetTemplates.buildModifierClass("Repeat")

        assertTrue(template.contains("namespace App\\Modifiers;"))
        assertTrue(template.contains("use Statamic\\Modifiers\\Modifier;"))
        assertTrue(template.contains("class Repeat extends Modifier"))
        assertTrue(template.contains("public function index(\$value, \$params, \$context)"))
        assertTrue(template.contains("return \$value;"))
    }
}
