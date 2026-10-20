# Roadmap

Build order towards the [brief](brief.md). Each step is one pull request.

## Current state (after the scaffold)

- `protocol`: packet codec, OkHttp socket, tracker handshake for one slot, name resolution,
  `PrintJSON` rendering, wss/ws fallback.
- `app`: one connection at a time, connect form and live feed in default Material 3 styling
  (dynamic colors, light and dark).

## Gaps to the brief and design

| Area           | Scaffold                         | Target                                                   |
| -------------- | -------------------------------- | -------------------------------------------------------- |
| Connections    | One slot                         | n multiworlds × n slots; one text connection per multiworld, the rest with `NoText` |
| Robustness     | No reconnect                     | Reconnect with backoff, ping, per-message compression    |
| Item tracking  | None                             | Last `ReceivedItems` index per (server, seed, slot), persisted |
| Names          | Data package fetched every time  | Cached per game, invalidated by `RoomInfo` checksums     |
| Text client    | Read-only                        | `Say` for chat and commands, "send as" slot              |
| Hints          | None                             | Hint list per multiworld with read-only status |
| Persistence    | None                             | Room (feed, hints, retention), DataStore (multiworlds, slots, settings) |
| Background     | None                             | Foreground service (`specialUse`), notifications per item type (off / silent / sound) |
| Look           | Material defaults, dynamic color | Fixed dark theme, design tokens, Geist, Material Symbols |

## Steps

Steps 1 to 3 are done. The background service comes last, once the app works in the foreground
(decided 2026-10-20).

1. **Design foundation**: theme tokens, Geist fonts, fixed dark theme; feed rows restyled to the
   design (classic layout, highlights, 4 px gap).
2. **Protocol for many slots**: per-multiworld session with one text connection and `NoText` for the
   others, reconnect with backoff, `ReceivedItems` index tracking, data package cache, `Say`.
3. **Persistence**: DataStore for multiworlds and slots, Room for feed, hints and item indices.
4. **Screens**: overview, multiworld edit, slot settings, feed with filters and input.
5. **Hints**: hint list and wide table, status shown read-only.
6. **Open design points** (after they are designed): received items tab, item type filter sheet,
   empty, loading and error states.
7. **Background service and notifications**: foreground service for all active slots, channels per
   item type, grouped item notifications, hint notifications, ongoing status notification.
