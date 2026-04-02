# Statamic Toolkit

A JetBrains IDE plugin providing syntax highlighting and editor support for the [Antlers](https://statamic.dev/frontend/antlers) template language used by [Statamic CMS](https://statamic.com).

## Features

- **Syntax Highlighting** for `.antlers.html` and `.antlers.php` files
  - Variables, tags, modifiers, operators, keywords
  - String and number literals
  - Comments (`{{# #}}`)
  - PHP regions (`{{? ?}}`, `{{$ $}}`)
  - Semantic colors for tag names, parameter names, and more
- **HTML Intelligence Preserved** -- full HTML/CSS/JS support within template files via IntelliJ's Template Language framework
- **Code Completion** -- Statamic tags, modifiers, and variables with descriptions from the official docs
- **Hover Documentation** -- quick docs with examples and links to statamic.dev
- **PHP Intelligence** -- full PHP support inside `{{? ?}}` and `{{$ $}}` blocks with highlighting, completion, and formatting from your PhpStorm settings
- **Alpine.js Support** -- JavaScript intelligence inside Alpine attributes (`x-data`, `@click`, `x-bind`, etc.) with method navigation back to `x-data`
- **Partial Navigation** -- Cmd-click on `partial:name` to jump to the partial file
- **Antlers Language Server** -- integrated Stillat LSP for formatting and diagnostics (bundled, requires Node.js)
- **Tag Parameter Completion** -- suggests official parameters (`from=`, `limit=`, `sort=`, etc.) for common Statamic tags
- **Scope-Aware Variables** -- suggests `title`, `slug`, `url`, `first`, `last`, `count`, etc. inside collection/nav/taxonomy loops
- **Collection Handle Completion** -- `{{ collection: }}` suggests handles from flat-file or Eloquent driver (queries database via artisan)
- **Go-To Custom Tags/Modifiers** -- Cmd-click navigates to PHP classes in `app/Tags/` and `app/Modifiers/`
- **Extract to Partial** -- select code, Alt+Enter to extract into a new partial file
- **Structure View** -- Antlers tags + HTML landmarks (`<header>`, `<main>`, `<section>`, etc.) in document order with nested tag pair children
- **Formatting** -- template-aware formatting with block tag indentation for `collection`, `nav`, `cache`, `foreach`, `entries`, `groups`, `items`, and more
- **Auto-Close Tags** -- typing `{{ /` auto-completes the closing tag name; Enter after block tags auto-indents
- **Brace Matching** -- highlights matching `{{ }}` pairs
- **Block Commenting** -- toggle comments with `{{# #}}` via Ctrl+/ (Cmd+/)
- **Typing Aids** -- auto-closing braces, smart quotes, and smart enter handling
- **Status Bar Widget** -- shows Statamic indexing status with resource counts, driver detection, and auto-index toggle
- **Statamic Menu** -- top-level menu with controller, tag, and modifier generators plus content query snippets
- **Customizable Colors** -- Settings > Editor > Color Scheme > Antlers, including underlined partial paths

## Supported Syntax

| Construct | Example |
|-----------|---------|
| Variables | `{{ title }}`, `{{ post:title }}` |
| Tags | `{{ collection:blog limit="5" }}...{{ /collection:blog }}` |
| Modifiers | `{{ title \| upper \| truncate(50) }}` |
| Conditionals | `{{ if }}`, `{{ elseif }}`, `{{ else }}`, `{{ unless }}` |
| Operators | `==`, `!=`, `&&`, `\|\|`, `??`, `?:`, `+`, `-`, `*`, `/` |
| Assignment | `{{ total = price * quantity }}` |
| Comments | `{{# This is a comment #}}` |
| PHP | `{{? $var = doSomething(); ?}}`, `{{$ route('home') $}}` |
| Self-closing | `{{ partial:hero /}}` |
| Escaped | `@{{ not_parsed }}` |

## Installation

### From JetBrains Marketplace
*Coming soon*

### From Source

```bash
git clone https://github.com/portseif/statamic-toolkit-phpstorm.git
cd statamic-toolkit-phpstorm
./gradlew runIde
```

This launches a sandboxed IDE instance with the plugin installed.

## Requirements

- IntelliJ IDEA / PhpStorm 2024.2+
- JDK 21

## Building

```bash
./gradlew build          # Build the plugin
./gradlew test           # Run tests
./gradlew runIde         # Launch sandbox IDE
./gradlew buildPlugin    # Package as .zip for distribution
```

## Roadmap

- [x] Syntax highlighting
- [x] Brace matching
- [x] Block commenting
- [x] HTML/CSS/JS intelligence in template files
- [x] Code completion for Statamic tags and modifiers
- [x] Go-to-definition for partials
- [x] Formatting support
- [x] Structure view
- [x] Alpine.js support
- [x] PHP intelligence in PHP blocks
- [x] Hover documentation
- [x] Statamic generators (controllers, tags, modifiers)
- [x] Configurable settings panel
- [x] Code folding for tag pairs
- [x] Tag parameter completion
- [x] Scope-aware variable completion
- [x] Go-to custom tag/modifier definition
- [x] Extract to partial refactoring
- [x] Antlers Language Server integration
- [x] Eloquent driver support
- [x] Status bar indexing widget
- [ ] Live templates / snippets
- [ ] Blueprint field completion

## Disclaimer

This plugin is not affiliated with, endorsed by, or officially connected to Statamic. "Statamic" and the Statamic logo are trademarks of [Statamic](https://statamic.com).

## License

MIT
