# Product brief: AP Monitor

Handoff notes from the initial research and design work (written by Sven, 2026-10-20). This is the
source of truth for scope and technical direction. Design rules are in [design.md](design.md), the
build order in [roadmap.md](roadmap.md).

## Goal

Private Android app that connects to Archipelago servers and sends notifications as soon as one of
my slots receives an item.

- Several multiworlds (address, port, optional room password), each with n slots of mine.
- Connections keep running in the background.
- Text client per multiworld: feed of all exchanged items, chat, commands.
- Hints per multiworld: item, receiver, location, finder, entrance, status.
- Findings for me and by me are highlighted in the feed.
- Items are colored by type (progression, useful, filler, trap).

## Technical findings

- Protocol: WebSocket with JSON. Sequence: `RoomInfo`, `GetDataPackage`, `Connect`,
  `Connected`/`ConnectionRefused`, then `ReceivedItems`.
- One connection per slot with tag `Tracker` (or `TextOnly`) and an empty game: no game validation,
  runs in parallel to the real game client.
- Only one connection per multiworld receives text; all others add the tag `NoText`.
- No notification spam: store the last item index per (server, seed, slot) and only report higher
  indices.
- IDs to names via the data package; the game of each player comes from `slot_info`. Cache the data
  package per game and invalidate it with the checksums from `RoomInfo`.
- Feed from `PrintJSON` (`data` parts: `player_id`, `item_id`, `location_id`, ...). Chat and
  commands via `Say`. Hint status can only be changed by the receiver (`UpdateHint`).
- Item flags: `0b001` progression, `0b010` useful, `0b100` trap (none: filler).
- Android: foreground service (`foregroundServiceType="specialUse"`), `POST_NOTIFICATIONS` from
  Android 13, reconnect with backoff plus ping, try `wss://` before `ws://`, support per-message
  compression. Notification channels per item type (off / silent / sound).
- Stack: Kotlin, Jetpack Compose, OkHttp WebSocket, kotlinx.serialization, Room (feed, hints),
  DataStore (profiles). Alternative: Archipelago.MultiClient.Java.
- Reference client: [mooinglemur/axolotl](https://github.com/mooinglemur/axolotl) (color logic,
  feed, hints).

## Open points

- "Received items" tab per slot.
- Item type filter for the feed (sheet) and status menu for hints are not designed yet.
- Empty, loading and error states are not designed yet.
