# archipelago-monitor-app

Android app ("AP Monitor") that monitors several [Archipelago](https://archipelago.gg) multiworlds
with several slots each, in the foreground and in the background, and notifies about items, hints
and chat mentions.

## Conventions (mandatory)

Read and follow these before writing code or committing:

- [Kotlin code style](docs/conventions/kotlin-style.md): Google's Android Kotlin style guide.
- [Commit messages](docs/conventions/commit-messages.md): Conventional Commits, plain text without emoji.
- [Attribution](docs/conventions/attribution.md): no co-authoring, no AI attribution lines.

## Product

- [Brief](docs/product/brief.md): scope and technical findings.
- [Design](docs/product/design.md): design rules and tokens.

## Project layout

- `protocol/`: pure Kotlin/JVM module with the Archipelago network protocol (packet models,
  WebSocket client, message rendering). No Android dependencies, so it is testable on the JVM.
- `app/`: Android app (Jetpack Compose UI, ViewModels) built on top of `protocol`.

## Commands

- `./gradlew :protocol:test`: run the protocol unit tests (no Android SDK needed).
- `./gradlew spotlessCheck` / `./gradlew spotlessApply`: check or fix formatting with ktlint.
- `./gradlew :app:assembleDebug`: build the app (needs the Android SDK).
