# archipelago-viewer-app

Android app that watches a running [Archipelago](https://archipelago.gg) multiworld room. It
connects to a slot as a tracker (it never sends checks) and shows the room's messages live: items
sent and received, hints, chat and join/leave events, with player, item and location names
resolved and colored like in the Archipelago text client.

## Status

What works so far:

- Several multiworlds with several slots each, saved on the device. Every slot connects as a
  tracker and receives text; room-wide messages appear once in the feed.
- Overview with connection state, new items for your slots and the latest one.
- Feed per multiworld with filters (all, for me, from me, chat, item types), classic or structured
  layout, stored history and a chat input that sends as one of your slots.
- Multiworld and slot settings, including a server check and the other slots of the room.
- Hints per multiworld, read live from the server: a list filtered by direction and slot, with
  a status menu for hints you receive, and a sortable table in landscape.

Planned next: the open design points, then the background service with notifications. See
[docs/product/roadmap.md](docs/product/roadmap.md).

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
