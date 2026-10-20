# Kotlin style

All Kotlin code follows Google's
[Android Kotlin style guide](https://developer.android.com/kotlin/style-guide). The guide is the
source of truth; this page summarizes the rules that come up most often.

Formatting is enforced with [ktlint](https://pinterest.github.io/ktlint/) in its `android_studio`
code style (configured in `.editorconfig`) through Spotless:

```sh
./gradlew spotlessCheck   # fails on violations
./gradlew spotlessApply   # fixes what can be fixed automatically
```

## Source files

- UTF-8 encoding, file names in PascalCase ending in `.kt`.
- A file with a single top-level class is named after that class.
- Order: license header (optional), file annotations, `package`, imports, top-level declarations.
- No wildcard imports. Imports are sorted ASCII, without blank lines between them.

## Formatting

- Maximum line length is 100 characters (package and import lines are exempt).
- Indent with 4 spaces, continuation indent with 4 spaces. No tabs.
- Braces follow K&R style. Braces are required for `if`, `for`, `when` branches, `do` and `while`
  unless the whole `if`/`else` fits on one line.
- One statement per line, no semicolons.
- Trailing commas are allowed (and used by ktlint) in multi-line declarations and calls.
- When a function signature does not fit on one line, put each parameter on its own line.
- Expression functions that do not fit on one line wrap after `=`.

## Naming

- Packages: lowercase, no underscores (`de.example.apviewer.protocol`).
- Classes and objects: PascalCase.
- Functions and properties: camelCase. Test functions may use backticks with spaces.
- Constants (`const val` and top-level/object `val`s holding deeply immutable data):
  `SCREAMING_SNAKE_CASE`.
- Backing properties start with an underscore: `private val _state` / `val state`.
- Composable functions that return `Unit` are PascalCase nouns (Compose API guidelines).
- Acronyms are written as words: `HttpClient`, `JsonParser`, not `HTTPClient`.

## Documentation

- KDoc (`/** … */`) for public API where the name alone is not enough.
- The summary fragment starts with a third-person verb or a noun phrase
  ("Returns …", "Connection state of …").
- Use block tags (`@param`, `@return`, `@throws`) only when they add information.

## Language and comments

- Code, identifiers and comments are in English.
- Comments explain why, not what.
