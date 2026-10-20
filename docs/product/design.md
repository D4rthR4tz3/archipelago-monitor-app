# Design

This page records the design rules and tokens that apply to every screen.

## Rules

- Color only where it makes content readable: feed, hints and item names in notifications.
  Settings, navigation and slot selection stay neutral gray.
- No horizontal scrolling. Filters are segmented controls, dropdowns or a bottom sheet.
- Nothing may shift when switching views or filters: use fixed grids.
- Feed rows always have a 4 px gap, highlighted rows included.
- No typical AI look: no pills, badges, status dots, outlined cards or gradients. Lists use divider
  lines. Status is plain text; color only for problems.
- Shapes: 8 px radius for controls, 12 px for surfaces.
- Type: Geist and Geist Mono. Icons: Material Symbols (outlined).
- Dark neutral theme only.

## Tokens

### Surfaces and text

| Token           | Value     | Use                               |
| --------------- | --------- | --------------------------------- |
| background      | `#141414` | Screen background                 |
| surface         | `#1E1E1E` | Panels, inputs, segmented control |
| divider         | `#262626` | List dividers                     |
| input border    | `#2E2E2E` | Text fields, selects              |
| segment active  | `#3A3A3A` | Selected segment, switch track off |
| text primary    | `#EDEDED` | Titles, body                      |
| text secondary  | `#A8A8A8` | Subtitles, inactive tabs          |
| text tertiary   | `#8C8C8C` | Timestamps, labels, hints         |
| warning text    | `#E3B341` | Status "reconnecting"             |

### Item types

| Type        | Color     | Weight   |
| ----------- | --------- | -------- |
| Progression | `#B58CFF` | semibold |
| Useful      | `#7AA2F7` | regular  |
| Filler      | `#4FD6D6` | regular  |
| Trap        | `#FF7A50` | semibold |

### Names and places

| Element       | Color     |
| ------------- | --------- |
| Own slot      | `#F06AE8` |
| Other players | `#D6D6D6` |
| Location      | `#6CCB8E` |
| Entrance      | `#A4B4C4` |

### Hint status

| Status    | Protocol value | Color     | Note           |
| --------- | -------------- | --------- | -------------- |
| Priority  | 30             | `#E8C35A` |                |
| Open      | 0              | `#B0B0B0` |                |
| Avoid     | 20             | `#F07C7C` |                |
| No need   | 10             | `#8C8C8C` |                |
| Found     | 40             | `#6CCB8E` | Row at 55 % opacity |

### Feed highlight

- Item for one of my slots: background `rgba(240, 106, 232, 0.12)`.
- Item sent by one of my slots: no background.
- Hint: no background; an info icon below the time, centered on the hint and location lines.
- Received items are numbered from 1 in the Items tab.
- Rows: 6 px × 8 px padding, 6 px radius, 4 px gap.

## Screens

| Artboard      | Content                                                                          |
| ------------- | -------------------------------------------------------------------------------- |
| Main          | Multiworld overview: background switch with slot count, list of multiworlds with name, slots, status text, last item; button "Multiworld" |
| Feed          | Text client: header with multiworld and status, tabs Feed / Hints, filter segments (All, For me, By me, Chat) and item type filter button, log in structured (default) or classic (monospace one line) layout, input with "send as" slot and message field |
| Hints         | Direction segments (For me, To find), slot dropdown, "hide found" switch, list with item, status (read-only, as set when the hint was created), receiver, finder, location, entrance |
| HintsWide     | Landscape table with sortable columns item, receiver, location, finder, entrance, status |
| MwEdit        | Connection (name, address, port, room password, reachability test), my slots, other slots reported by the server, behavior (connect on start, keep in background, feed retention 1/7/30 days/unlimited), remove |
| SlotSettings  | Slot status, access (slot name; the password is per room), feed highlight, notifications per item type (off / silent / sound), extras (own world items, new hints, chat mentions), synchronization (last reported item index, "count from now"), remove |
| Notifications | Ongoing service notification, grouped item notifications with item name colored, hint notification (no "mark as priority" action, the hint status is read-only) |
| Legende       | Color legend |

## Launcher icon

Motif "Feed": three feed rows, each with an item marker (own slot, progression, filler) on the
background `#141414`. Source: [icon.svg](icon.svg), a 108 dp adaptive icon layer; launchers show
the inner 72 dp and the artwork stays inside the 66 dp safe zone. The app uses the vector
resources `ic_launcher_foreground` and `ic_launcher_monochrome` (themed icons) via
`mipmap-anydpi`. The PNGs in `mipmap-*dpi` and `ic_launcher-playstore.png` (512 px) are rendered
from the SVG; re-render them when the icon changes.
