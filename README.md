# archipelago-viewer-app

Android app that watches a running [Archipelago](https://archipelago.gg) multiworld room. It
connects to a slot as a tracker (it never sends checks) and shows the room's messages live: items
sent and received, hints, chat and join/leave events, with player, item and location names
resolved and colored like in the Archipelago text client.

## Features

- Several multiworlds with several slots each, saved on the device. Every slot connects as a
  tracker and receives text; room-wide messages appear once in the feed.
- Overview with connection state, new items for your slots and the latest one. Swipe a multiworld
  to the left to delete it; one switch pauses all connections.
- Feed per multiworld with filters (all, for me, from me, chat, item types), classic or structured
  layout, stored history and a chat input that sends as one of your slots.
- Multiworld and slot settings, including a server check and the other slots of the room. Slot
  changes of a saved multiworld are stored right away.
- Hints per multiworld, read live from the server: a list filtered by direction and slot, and a
  sortable table in landscape. The hint status is shown, not changed.
- Received items per slot, newest first, with the item type filter of the feed; items that arrived
  since the tab was last opened are marked as new.
- Background service for multiworlds kept in the background, with notifications per slot for items
  (off, silent or with sound per item type), new hints and chat mentions. While the app is visible
  it posts no notifications.
- Reconnects with backoff, pauses without a network and reconnects as soon as it is back.

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
