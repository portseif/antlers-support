# CLAUDE.md

This file provides guidance to Claude Code (`claude.ai/code`) when working in this repository.

## Project Overview

**Statamic Toolkit** is a JetBrains IDE plugin for the [Antlers](https://statamic.dev/frontend/antlers) template language in Statamic CMS.

- Marketplace/plugin display name: **Statamic Toolkit**
- Language name: **Antlers**
- Current development target: **PhpStorm** (`platformType=PS` in `gradle.properties`)
- Language-level registrations (file type, parser, highlighter, color scheme, code style) use **Antlers**
- The settings panel under Languages & Frameworks uses **Statamic**

## Build and Packaging

```bash
./gradlew build          # Generate lexer/parser from grammars + compile + package
./gradlew runIde         # Launch sandbox IDE with plugin installed
./gradlew buildPlugin    # Package plugin for distribution
./gradlew test           # Run tests (JUnit 4)
./gradlew test --tests com.antlers.support.formatting.AntlersFormattingPostProcessorTest  # Focused formatter regressions
```

Grammar-Kit code generation from `.flex` and `.bnf` files runs automatically before compilation. There is no separate generation step.

Runtime plugin dependencies must be declared in both places:

- `build.gradle.kts` via `bundledPlugin()`
- `plugin.xml` via `<depends>`

If either side is missing, features can silently fail. Optional dependencies must use `<depends optional="true" config-file="...">`.

## Core Architecture

### Dual PSI Tree

Antlers files contain both Antlers expressions (`{{ }}`) and HTML/CSS/JS, so the plugin uses IntelliJ's Template Language Framework with `MultiplePsiFilesPerDocumentFileViewProvider`.

Each file maintains two PSI trees:

- **Antlers tree** (base language): tags, expressions, conditionals, modifiers
- **HTML tree** (template data language): full HTML/CSS/JS intelligence

`AntlersFileViewProvider` overrides `findElementAt()` and `findReferenceAt()` to prefer the HTML tree for template data regions and fall back to the Antlers tree. This enables CSS class navigation, JS references, and similar editor features inside template files.

The `TemplateDataElementType` outer fragment must use `OuterLanguageElementType`, not a plain PSI element type. Using the wrong outer element causes the Template Language Framework to throw `Wrong element created by ASTFactory` when switching files or reparsing mixed Antlers/HTML content.

### Grammar and Code Generation

Two Grammar-Kit grammars in `grammars/` generate code into `src/main/gen/`:

1. `Antlers.flex` (JFlex) generates `_AntlersLexer.java`
2. `Antlers.bnf` (Grammar-Kit BNF) generates `AntlersParser.java` plus PSI element types/interfaces/implementations

The lexer is stateful and uses these states:

- `YYINITIAL`
- `ANTLERS_EXPR`
- `COMMENT`
- `PHP_RAW`
- `PHP_ECHO`
- `DQ_STRING`
- `SQ_STRING`
- `NOPARSE`

### Token Identity Bridge

The parser and lexer must share the same `IElementType` instances.

`AntlersTokenTypes.factory()` uses reflection to resolve BNF token names to existing lexer token fields. The BNF `tokens` block must list bare token names, not string values, so the factory receives names like `ANTLERS_OPEN` instead of display strings like `{{`.

## Editor Features

### Highlighting

`AntlersEditorHighlighter` extends `LayeredLexerEditorHighlighter`:

- Base layer: `AntlersSyntaxHighlighter`
- Layer for `TEMPLATE_TEXT`: HTML/CSS/JS syntax highlighter

Antlers-specific semantic colors such as tag names and parameter names come from `AntlersHighlightingAnnotator`, not from the lexer highlighter alone.

### Alpine.js Integration

Alpine support is implemented with:

- `AntlersAlpineAttributeInjector` (`MultiHostInjector`)
- `AntlersAlpineReferenceContributor`
- `AntlersAlpineReferenceResolver`

Rules:

- `x-data` is injected as a JS expression and wrapped with `(` `)` so object literals parse correctly.
- Event-style attributes such as `@click`, `x-on:*`, `x-init`, and `x-effect` are injected as statements.
- `x-for` is **not** injected as JavaScript because Alpine's `(item, index) in items` syntax is not valid JS and creates parser noise.
- `x-for` loop aliases are resolved manually in the Alpine reference resolver so descendant expressions can still navigate and avoid false unresolved warnings.
- Cmd-click on Alpine method calls should resolve through normal PSI references first. `AntlersGotoDeclarationHandler` remains a fallback for Antlers-specific navigation such as partials.
- `AntlersAlpineReferenceContributor` must stay cheap. Do not eagerly call the full resolver from `getReferencesByElement()`.
- `AntlersAlpineReferenceResolver` caches the injected `x-data` object literal lookup per `XmlAttributeValue`. Do not re-materialize injected PSI while walking ancestors.

### Auto-closing Delimiters

`AntlersTypedHandler` auto-closes `{{ }}` when the user types `{{`. The handler inserts `  }}` (two spaces + closing braces) and places the caret between the spaces, so the document becomes `{{ | }}` with the cursor in the middle.

Rules:

- When auto-close is enabled (the default), typing `{{` always produces `{{  }}` with the cursor between the two spaces. Any feature that generates Antlers output (live templates, snippets, intentions) must account for the `}}` already being present after the cursor.
- The handler checks for an existing `}}` immediately after the caret to avoid double-closing.
- The handler also removes a stray `}` left by IntelliJ's built-in single-brace pairing before inserting its own `  }}`.
- The feature is toggled by `AntlersSettings.enableAutoCloseDelimiters`.

### PHP Injection

PHP intelligence inside `{{? ?}}` (raw PHP) and `{{$ $}}` (echo PHP) blocks is implemented via `AntlersPhpInjector`, a `MultiHostInjector` registered as an optional dependency on `com.jetbrains.php`.

Rules:

- The BNF rules for `phpRawBlock` and `phpEchoBlock` use `AntlersPhpBlockMixin`, which implements `PsiLanguageInjectionHost`.
- `{{? ... ?}}` content is injected with prefix `<?php ` and suffix ` ?>`.
- `{{$ ... $}}` content is injected with prefix `<?php echo ` and suffix `; ?>`.
- The injector is registered in `antlers-php.xml`, loaded via `<depends optional="true" config-file="antlers-php.xml">com.jetbrains.php</depends>`.
- Formatting inside PHP blocks follows the user's PHP code style settings.

### Formatting

Formatting uses `TemplateLanguageFormattingModelBuilder`, not a plain formatting model builder.

`AntlersFormattingModelBuilder` must special-case `OuterLanguageElementType` nodes and delegate them back to `SimpleTemplateLanguageFormattingModelBuilder`. Otherwise mixed-template formatting breaks around template data boundaries.

`AntlersBlock` (a `TemplateLanguageBlock`) holds a `SpacingBuilder` that enforces token-level spacing rules:

- One space inside `{{ }}` delimiters
- One space around operators
- No space around `=` in parameters
- No space around `:` in modifier args
- No space around `/`

The `SpacingBuilder` is instantiated once per block in the constructor, and `getSpacing()` is called before the super-class fallback.

`Reformat Code` uses two layers:

- `AntlersFormattingModelBuilder` / `AntlersBlock` for token spacing
- `AntlersConditionalPostFormatProcessor` for standalone Antlers control/tag line indentation after the normal formatter runs

The post-format processor exists because the template-language formatter alone cannot reliably align `{{ if }}` / `{{ else }}` / `{{ /if }}` in mixed Antlers/HTML files.

`AntlersConditionalPostFormatProcessor` now uses a structural depth stack over both standalone Antlers lines and standalone HTML lines. It treats single-line HTML open/close tags and single-line standalone Antlers tags as one nesting model, so surrounding HTML parents (`<div>`, `<main>`, `<header>`, etc.) and Antlers control-flow lines indent consistently in the same pass.

The processor should only create extra indentation for real nesting constructs:

- HTML parent structure
- Antlers control-flow constructs such as `if`, `unless`, `switch`, `else`, and `elseif`

Flat sequences of standalone tags such as consecutive `{{ partial:... }}` lines must stay aligned with each other. Do not let a previously-added continuation indent cascade through sequential standalone tags.

#### Known formatting limitations and rules

**`{{ else }}` / `{{ elseif }}` alignment**  
The template-language formatter alone cannot align these tags correctly in mixed Antlers/HTML files because they are often merged under `DataLanguageBlockWrapper` nodes that carry surrounding HTML indentation. The plugin handles the common `Reformat Code` case with `AntlersConditionalPostFormatProcessor`, which realigns standalone Antlers control/tag lines after formatting. If you need richer indentation for arbitrary mixed HTML content between branches, that would still require a more custom formatting model.

**Standalone-line scope**  
`AntlersConditionalPostFormatProcessor` intentionally operates on lines where the trimmed line text is exactly one standalone Antlers tag, plus simple standalone HTML open/close/self-closing lines. It is designed for `Reformat Code` cleanup of mixed Antlers/HTML structure, not arbitrary inline HTML fragments or multi-node line layouts.

**`OP_DIVIDE` spacing**  
Use `.around(OP_DIVIDE).none()` in the `SpacingBuilder`. Do not use `.spaces(1)`, and do not omit the rule. In Antlers, `/` is more often a path separator such as `partial:partials/sections/hero` than an arithmetic operator. `.none()` actively removes spaces during Reformat Code; omitting the rule returns `null` and leaves whitespace unchanged.

**Closing-tag grammar fix**  
`{{ /if }}` used to fail because `tagName` accepted only `IDENTIFIER`, while `if` is lexed as `KEYWORD_IF`. The fix is a private `tagNameAtom` rule in `Antlers.bnf` that accepts `IDENTIFIER | KEYWORD_IF | KEYWORD_UNLESS | KEYWORD_SWITCH`. After that change, `{{ /if }}` parses as `closingTag` instead of `conditionalTag`, so `AntlersFoldingBuilder` must map `if -> COND_IF` and `unless -> COND_UNLESS` to match the stack keys used by opening conditionals.

### Code Folding

`AntlersFoldingBuilder` extends `FoldingBuilderEx` and implements `DumbAware`.

It uses a stack of `OpenFold(tag, key)` entries to match opening and closing tags in document order. The Antlers PSI tree is flat, so walking `root.children` with a stack is the only viable matching strategy.

Rules:

- Regular tag pairs use `tagExpr.tagName.text` as the stack key.
- Conditional pairs use synthetic keys `COND_IF` and `COND_UNLESS` to avoid collisions with real tags named `if`.
- Because `{{ /if }}` and `{{ /unless }}` parse as `closingTag`, the closing branch remaps `if -> COND_IF` and `unless -> COND_UNLESS` before stack lookup.
- `getPlaceholderText()` should show the full expression/condition text, trimmed and truncated at 60 characters, so folded blocks read like `{{ if site:environment === 'production' }}...`.

## Data, Completion, and Documentation

### Statamic Catalog and Hover Docs

Official Statamic tags, modifiers, and variables are generated rather than maintained by hand.

- Source of truth: `scripts/generate_statamic_catalog.py`
- Generated output: `src/main/kotlin/com/antlers/support/statamic/StatamicCatalogGenerated.kt`
- Runtime lookup layer: `StatamicCatalog`

This catalog powers:

- Antlers completion (`StatamicData`, `AntlersCompletionContributor`)
- Hover and quick documentation (`AntlersDocumentationProvider`)
- Official descriptions, examples, and URLs for tags, modifiers, and variables

Variable-vs-tag hover resolution is intentionally conservative:

- Simple bare identifiers with no parameters can resolve as variables
- Namespaced forms such as `nav:foo` and `current_user:email` fall back to root tag/variable handles as needed

### Completion Pre-building

`StatamicData` keeps these as lazy, pre-built structures:

- `TAG_ELEMENTS`
- `MODIFIER_ELEMENTS`
- `VARIABLE_ELEMENTS`
- `SUB_TAG_ELEMENT_MAP`

`AntlersCompletionContributor` must call `result.addAllElements(StatamicData.XXX_ELEMENTS)`. Do not revert to building `LookupElement` instances per keystroke.

## IDE Integration Patterns

### Settings

Feature toggles live in `AntlersSettings`, an application-level `PersistentStateComponent`, and are exposed at Settings > Languages & Frameworks > Statamic via `AntlersSettingsConfigurable`.

Every major feature should have a toggle. When adding a new toggleable feature:

1. Add `var enableXxx: Boolean = true` to `AntlersSettings.State`
2. Add a `JBCheckBox` in `AntlersSettingsConfigurable` under the appropriate `TitledSeparator`
3. Wire it through `isModified`, `apply`, and `reset`
4. Guard the feature entry point with `if (!AntlersSettings.getInstance().state.enableXxx) return`

Current settings sections:

- Editor
- Completion
- Navigation & Documentation
- Language Injection

#### Settings configurable binding pattern

`AntlersSettingsConfigurable` uses a `CheckboxField(box, read, write)` data class to bind each `JBCheckBox` to its getter and setter in `AntlersSettings.State`.

The `fields: List<CheckboxField>` is built once. `isModified`, `apply`, and `reset` each collapse to a single `any` or `forEach` over that list. When adding a new toggle, add one entry to `fields` instead of updating three separate methods.

### Statamic Menu

The plugin exposes a top-level **Statamic** menu in the main menu bar for PHP-side Statamic workflows.

Rules:

- Content query snippets insert code at the caret and are enabled only when the active editor is a PHP file.
- The `Content Queries` submenu should remain visible in Laravel/Statamic projects even when the active tab is Antlers, so the menu does not look broken.
- Controller, tag, and modifier generators create files under `app/Http/Controllers`, `app/Tags`, and `app/Modifiers`.
- Generators should open an existing file instead of overwriting it if the class already exists.

Project-aware menu visibility is handled by lightweight action groups such as `StatamicProjectActionGroup` and `StatamicPhpInsertActionGroup`, not by duplicating enable/disable logic in every child action.

### Navigation Bar Integration

`AntlersStructureAwareNavbar` extends `StructureAwareNavBarModelExtension`.

Rules:

- The abstract member is a Kotlin property, not a Java method. Use `override val language: Language = AntlersLanguage.INSTANCE`, not `override fun getLanguage()`.
- `getIcon()` does not exist on `AbstractNavBarModelExtension`; do not add it.
- The Antlers PSI tree is flat, so the navbar can show only within-`{{ }}` context. It cannot show tag-parent hierarchy for template content between tags.

### Find Usages and Symbols

`AntlersFindUsagesProvider` uses `DefaultWordsScanner` from `com.intellij.lang.cacheBuilder`, not `com.intellij.psi.search`.

Related type locations:

- `ChooseByNameContributorEx` is in `com.intellij.navigation`
- `IdFilter` is in `com.intellij.util.indexing`
- `FindSymbolParameters` is in `com.intellij.util.indexing`

## Performance and Stability

The plugin has several editor hot paths where small mistakes can freeze PhpStorm.

Rules:

- Partial navigation and completion must use `AntlersPartialPaths.searchScope(project)` rooted at `resources/views`, not `GlobalSearchScope.allScope(project)`.
- Do not add fallback `FilenameIndex` scans that search the whole project by filename only from goto handlers.
- JFlex states with custom start conditions need explicit `<<EOF>>` handling. Truncated Antlers blocks should reset to `YYINITIAL` and return `BAD_CHARACTER`.
- `AntlersFileViewProvider.supportsIncrementalReparse()` is intentionally `false`. Do not flip it without a reproducible case and validation against mixed Antlers/HTML PSI correctness.
- If the IDE freezes again, collect a thread dump or CPU snapshot before making more speculative performance changes.

## Grammar and Parser Patterns

### Adding a New Keyword

1. Add a lexer rule in `Antlers.flex` with lookahead: `"keyword" / [^a-zA-Z0-9_]`
2. Add a token field in `AntlersTokenTypes.kt`: `@JvmField val KEYWORD_X = AntlersTokenType("KEYWORD_X")`
3. Add the token to `AntlersTokenSets.KEYWORDS`
4. Add the bare token name to the `tokens` block in `Antlers.bnf` and reference it in grammar rules
5. Run `./gradlew build` to regenerate everything

### BNF Error Recovery

- `pin=1` means "commit after matching the first token" and prevents backtracking.
- `recoverWhile` skips tokens until a predicate matches. Do not apply it to rules that can fail cleanly without consuming tokens, or it will create false errors.
- `private` rules do not generate PSI nodes and are useful for dispatch/grouping.
- If a grammar rule accepts only `IDENTIFIER` but the lexer produces a keyword token such as `KEYWORD_IF` for the same text, the parser throws `IDENTIFIER expected, got 'if'`. The fix is a private `tagNameAtom` rule that lists `IDENTIFIER | KEYWORD_IF | KEYWORD_UNLESS | ...` and is used wherever the original `IDENTIFIER` was. This is preferable to making those keywords context-sensitive in the lexer.

## Release and Maintenance

### Release Notes / What's New

Plugin update notes come from `CHANGELOG.md`, not handwritten `<change-notes>` in `plugin.xml`.

- `build.gradle.kts` extracts all version sections from `CHANGELOG.md` and feeds them into `pluginConfiguration.changeNotes`
- The converter maps `## [x.y.z]` headings to `<h2>`, `- ` items to `<ul><li>`, and inline `` ` `` to `<code>`
- `build/tmp/patchPluginXml/plugin.xml` is the fastest place to verify the generated `<change-notes>` block before publishing

### Release Process

Every version bump must update all of the following:

1. `pluginVersion` in `gradle.properties`
2. A new `## [x.y.z]` section at the top of `CHANGELOG.md` with user-facing bullet points
3. `README.md` features list and roadmap if the release adds visible features
4. `CLAUDE.md` if the release adds new subsystems or new patterns worth preserving

## File Conventions

- Hand-written code: `src/main/kotlin/`
- Generated code: `src/main/gen/` (gitignored, regenerated on build)
- Grammar sources: `grammars/Antlers.flex`, `grammars/Antlers.bnf`
- Plugin manifest: `src/main/resources/META-INF/plugin.xml`
- Color schemes: `src/main/resources/colorSchemes/`
