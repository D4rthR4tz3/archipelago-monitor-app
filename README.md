# archipelago-monitor-app

AP Monitor is an Android app that keeps an eye on your [Archipelago](https://archipelago.gg)
multiworlds. Add as many multiworlds as you like, each with your own slots; the app connects every
slot as a tracker next to your game client and notifies you when one of your slots receives an item,
someone hints an item for you or a chat message names you. Each multiworld has a live text client
with items, hints, chat and join/leave events, names resolved and colored like in the Archipelago
text client, plus a hint list and the items each slot received.

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

## Notifications

Notifications need all of the following:

- **Permission:** allow notifications when the app asks on first start (Android 13 and newer), or
  later in the system settings. The channels "Items with sound", "Items without sound", "New hints"
  and "Chat mentions" must stay on.
- **Background connection:** "Keep connected in the background" is on in the multiworld settings
  (the default). Multiworlds without it disconnect as soon as you leave the app. While at least one
  multiworld stays connected, an ongoing status notification shows the connected slots.
- **Connections active:** the switch on the overview is on. "Pause" in the status notification
  turns it off.
- **Slot settings:** each slot decides per item type whether to notify (off, silent or with sound;
  by default progression with sound, useful items silent, filler and traps off) and whether new
  hints and chat mentions notify (both on by default). Items a slot finds in its own world notify
  only if "Items from your own world" is on.
- **Network:** without a network the app waits and reconnects as soon as one is back.

While the app is visible it posts no notifications; the feed shows everything. Hints created while
a slot was not connected do not notify, and items notify only once, even after a reconnect.

Some manufacturers stop background apps despite the ongoing notification. If notifications stop
arriving, set the app's battery usage to "Unrestricted" in the system settings.

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
