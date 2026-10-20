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
- Item sent by one of my slots: background `rgba(240, 106, 232, 0.05)`.
- Rows: 6 px × 8 px padding, 6 px radius, 4 px gap.

## Screens

| Artboard      | Content                                                                          |
| ------------- | -------------------------------------------------------------------------------- |
| Main          | Multiworld overview: background switch with slot count, list of multiworlds with name, slots, status text, last item; button "Multiworld" |
| Feed          | Text client: header with multiworld and status, tabs Feed / Hints, filter segments (All, For me, By me, Chat) and item type filter button, log in classic (monospace one line) or structured layout, input with "send as" slot and message field |
| Hints         | Direction segments (I receive, I find), slot dropdown, "hide found" switch, list with item, status (menu for the receiver), receiver, finder, location, entrance |
| HintsWide     | Landscape table with sortable columns item, receiver, location, finder, entrance, status |
| MwEdit        | Connection (name, address, port, room password, reachability test), my slots, other slots reported by the server, behavior (connect on start, keep in background, feed retention 1/7/30 days/unlimited), remove |
| SlotSettings  | Slot status, access (slot name, password), feed highlight, notifications per item type (off / silent / sound), extras (own world items, new hints, chat mentions), synchronization (last reported item index, "count from now"), remove |
| Notifications | Ongoing service notification, grouped item notifications with item name colored, hint notification with "mark as priority" action |
| Legende       | Color legend |
