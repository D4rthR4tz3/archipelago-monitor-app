package io.github.d4rthr4tz3.apviewer.ui.items

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.d4rthr4tz3.apviewer.R
import io.github.d4rthr4tz3.apviewer.protocol.model.ItemFlags
import io.github.d4rthr4tz3.apviewer.protocol.model.NetworkItem
import io.github.d4rthr4tz3.apviewer.protocol.session.ReceivedItem
import io.github.d4rthr4tz3.apviewer.protocol.store.ItemClass
import io.github.d4rthr4tz3.apviewer.ui.feed.ItemFilterButton
import io.github.d4rthr4tz3.apviewer.ui.feed.itemClassStyle
import io.github.d4rthr4tz3.apviewer.ui.formatClock
import io.github.d4rthr4tz3.apviewer.ui.theme.ApColors
import io.github.d4rthr4tz3.apviewer.ui.theme.ApViewerTheme
import io.github.d4rthr4tz3.apviewer.ui.theme.ContentColors
import io.github.d4rthr4tz3.apviewer.ui.theme.GeistMono

/**
 * Items one of the user's slots received, newest first, limited to [itemClasses]. While [active],
 * the slot's items count as seen through [onSeen]; [onLeave] runs when the list stops being active
 * or is hidden. A list that only peeks in during a swipe is not active.
 */
@Composable
fun ItemsList(
    state: ItemsState,
    active: Boolean,
    itemClasses: Set<ItemClass>,
    onSlotChange: (String) -> Unit,
    onOpenItemFilter: () -> Unit,
    onSeen: (slotName: String, index: Int) -> Unit,
    onLeave: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val latestIndex = state.rows.firstOrNull()?.item?.index ?: -1
    LaunchedEffect(state.slot, latestIndex, active) {
        if (active) state.slot?.let { onSeen(it, latestIndex) }
    }
    DisposableEffect(active) {
        onDispose {
            if (active) onLeave()
        }
    }
    val visible = remember(state.rows, itemClasses) {
        state.rows.filter { it.itemClass in itemClasses }
    }
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ItemSlotPicker(
                slotNames = state.slotNames,
                newCounts = state.newCounts,
                selected = state.slot,
                onSelect = onSlotChange,
                modifier = Modifier.weight(1f),
            )
            ItemFilterButton(
                active = itemClasses.size < ItemClass.entries.size,
                onClick = onOpenItemFilter,
            )
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = pluralStringResource(
                    R.plurals.items_received,
                    state.rows.size,
                    state.rows.size,
                ),
                style = MaterialTheme.typography.bodySmall,
                color = ApColors.TextSecondary,
            )
            Text(
                text = pluralStringResource(
                    R.plurals.overview_unread,
                    state.newCount,
                    state.newCount,
                ),
                style = MaterialTheme.typography.bodySmall,
                color = ApColors.TextPrimary,
            )
        }
        HorizontalDivider(color = ApColors.Divider)
        if (visible.isEmpty()) {
            Box(
                modifier = Modifier.weight(1f).fillMaxWidth().padding(24.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = stringResource(
                        when {
                            state.rows.isNotEmpty() -> R.string.items_filtered
                            state.connected -> R.string.items_empty
                            else -> R.string.items_offline
                        },
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    color = ApColors.TextTertiary,
                )
            }
        } else {
            // Each slot gets a list of its own that starts at the top. Item indices repeat across
            // slots, so a shared list state would keep the previous slot's position by its key.
            key(state.slot) {
                LazyColumn(modifier = Modifier.weight(1f).fillMaxWidth()) {
                    items(visible, key = { it.item.index }) { row ->
                        ItemListRow(row = row)
                        HorizontalDivider(color = ApColors.Divider)
                    }
                }
            }
        }
    }
}

@Composable
private fun ItemSlotPicker(
    slotNames: List<String>,
    newCounts: Map<String, Int>,
    selected: String?,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }
    Box(modifier = modifier) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(40.dp)
                .background(ApColors.Surface, MaterialTheme.shapes.small)
                .border(1.dp, ApColors.InputBorder, MaterialTheme.shapes.small)
                .clickable(role = Role.DropdownList, enabled = slotNames.size > 1) {
                    expanded = true
                }
                .padding(start = 10.dp, end = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = selected.orEmpty(),
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.bodyMedium,
                color = ApColors.TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (slotNames.size > 1) {
                Icon(
                    painter = painterResource(R.drawable.ic_arrow_drop_down),
                    contentDescription = null,
                    tint = ApColors.TextSecondary,
                )
            }
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            containerColor = ApColors.Surface,
        ) {
            slotNames.forEach { name ->
                val newCount = newCounts[name] ?: 0
                DropdownMenuItem(
                    text = {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(name, color = ApColors.TextPrimary)
                            if (newCount > 0) {
                                Text(
                                    text = stringResource(R.string.new_count, newCount),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = ApColors.TextSecondary,
                                )
                            }
                        }
                    },
                    onClick = {
                        onSelect(name)
                        expanded = false
                    },
                )
            }
        }
    }
}

@Composable
private fun ItemListRow(row: ItemRow) {
    val style = itemClassStyle(row.itemClass)
    val sender = if (row.fromSelf) stringResource(R.string.feed_themselves) else row.item.senderName
    val origin = buildAnnotatedString {
        append(stringResource(R.string.items_from_prefix))
        withStyle(
            SpanStyle(
                color = if (row.fromSelf) ApColors.TextTertiary else ContentColors.OtherPlayer,
            ),
        ) { append(sender) }
        append(", ")
        withStyle(SpanStyle(color = ContentColors.Location)) { append(row.item.locationName) }
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(if (row.isNew) ContentColors.HighlightForMe else ApColors.Background)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        // Number, item and time share the baseline of the item's line, as in the feed.
        Text(
            // The server counts from 0; people count from 1.
            text = stringResource(R.string.items_index, row.item.index + 1),
            modifier = Modifier.width(40.dp).alignByBaseline(),
            style = MonoLabelStyle,
            color = ApColors.TextTertiary,
        )
        Column(
            modifier = Modifier.weight(1f).alignByBaseline(),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Row {
                Text(
                    text = row.item.itemName,
                    modifier = Modifier.weight(1f).alignByBaseline(),
                    style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 18.sp),
                    fontWeight = style.weight,
                    color = style.color,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                row.item.receivedAtMillis?.let { time ->
                    Text(
                        text = formatClock(time),
                        modifier = Modifier.padding(start = 10.dp).alignByBaseline(),
                        style = MonoLabelStyle,
                        color = ApColors.TextTertiary,
                    )
                }
            }
            Text(
                text = origin,
                style = MaterialTheme.typography.labelSmall.copy(lineHeight = 16.sp),
                color = ApColors.TextTertiary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

/** Same size as the feed's time column. */
private val MonoLabelStyle = TextStyle(fontFamily = GeistMono, fontSize = 11.sp, lineHeight = 18.sp)

@Preview(backgroundColor = 0xFF141414, showBackground = true, widthDp = 390, heightDp = 600)
@Composable
private fun ItemsListPreview() {
    fun row(index: Int, name: String, flags: Int, sender: String, location: String, new: Boolean) =
        ItemRow(
            item = ReceivedItem(
                index = index,
                item = NetworkItem(item = index.toLong(), location = 1, player = 2, flags = flags),
                itemName = name,
                senderName = sender,
                locationName = location,
                isNew = new,
                receivedAtMillis = 1_760_000_000_000L - (141 - index) * 600_000L,
            ),
            itemClass = ItemClass.of(flags),
            fromSelf = sender == "D4rth_Keymizer",
            isNew = new,
        )
    ApViewerTheme {
        ItemsList(
            state = ItemsState(
                connected = true,
                slotNames = listOf("D4rth_Keymizer", "D4rth_Doorsmizer"),
                newCounts = mapOf("D4rth_Keymizer" to 2, "D4rth_Doorsmizer" to 5),
                slot = "D4rth_Keymizer",
                rows = listOf(
                    row(141, "Small Heart", 0, "hellow-oos", "Chest in Summer Cave", true),
                    row(
                        140,
                        "Small Key (Skull Woods)",
                        ItemFlags.PROGRESSION,
                        "ManneALTTP",
                        "Sewers",
                        true,
                    ),
                    row(
                        139,
                        "Arrows (10)",
                        0,
                        "D4rth_Keymizer",
                        "Eastern Palace - Compass Chest",
                        false,
                    ),
                ),
                newCount = 2,
            ),
            active = true,
            itemClasses = ItemClass.entries.toSet(),
            onSlotChange = {},
            onOpenItemFilter = {},
            onSeen = { _, _ -> },
            onLeave = {},
        )
    }
}
