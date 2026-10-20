# archipelago-viewer-app

Android app that watches a running [Archipelago](https://archipelago.gg) multiworld room. It
connects to a slot as a tracker (it never sends checks) and shows the room's messages live: items
sent and received, hints, chat and join/leave events, with player, item and location names
resolved and colored like in the Archipelago text client.

## Status

Early scaffold. What works so far:

- Connect form for server address, slot name and optional password.
- Connection via `wss://` with fallback to `ws://`, default port 38281.
- Live message feed of the room with resolved names and item classification colors.

Planned next: background connection with notifications, filters (own slot, progression items),
hint overview, saved connection profiles and caching of the data package.

## Modules

| Module      | Content                                                                      |
| ----------- | ---------------------------------------------------------------------------- |
| `protocol/` | Pure Kotlin/JVM: packet models, codec, OkHttp WebSocket client, message feed |
| `app/`      | Android app: Jetpack Compose UI and ViewModel on top of `protocol`           |

## Building

Requirements: JDK 17 or newer, Android SDK with platform 37 (for the app module).

```sh
./gradlew :protocol:test       # protocol unit tests, no Android SDK needed
./gradlew :app:assembleDebug   # debug APK
./gradlew spotlessCheck        # ktlint formatting check
```

## Contributing

See [CLAUDE.md](CLAUDE.md) and [docs/conventions](docs/conventions) for code style, commit message
and attribution rules.
