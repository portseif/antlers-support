# Changelog

All notable changes to this plugin should be documented in this file.

## [0.3.0] - 2026-03-29
- Added BNF grammar for full Grammar-Kit parser generation with proper PSI tree structure.
- Added auto-generated Statamic catalog powering tag, modifier, and variable completion with hover documentation sourced from the official docs.
- Added `Tools > Statamic` menu with controller, tag, and modifier generators plus content query snippet insertion in PhpStorm.
- Added changelog-driven release notes in plugin.xml.

## [0.2.0] - 2026-03-28
- Added template-aware formatting for mixed Antlers/HTML files.
- Added layered editor highlighter with Antlers base layer and HTML/CSS/JS syntax layer.
- Added semantic highlighting annotator for tag names, parameter names, and other Antlers constructs.
- Added Alpine.js support with JavaScript injection in Alpine attributes and reference resolution back to `x-data` methods.
- Added Statamic tag and modifier code completion.
- Added structure view for Antlers files.
- Added typing aids for auto-closing braces, quotes, and smart enter handling.
- Added code style settings page.

## [0.1.0] - 2026-03-27
- Initial release with Antlers language support for PhpStorm.
- Added JFlex lexer with syntax highlighting for Antlers expressions, comments, and PHP blocks.
- Added dual PSI tree via Template Language Framework for full HTML/CSS/JS intelligence inside Antlers files.
- Added brace matching and commenter support.
- Added partial navigation via Cmd-click on `partial:name` tags.
- Added dark and light theme icons using Statamic S mark.
