package io.github.d4rthr4tz3.apviewer.ui.hints

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.d4rthr4tz3.apviewer.R
import io.github.d4rthr4tz3.apviewer.protocol.model.HintStatus
import io.github.d4rthr4tz3.apviewer.protocol.model.ItemFlags
import io.github.d4rthr4tz3.apviewer.protocol.session.MultiworldHint
import io.github.d4rthr4tz3.apviewer.protocol.session.SlotHint
import io.github.d4rthr4tz3.apviewer.protocol.store.ItemClass
import io.github.d4rthr4tz3.apviewer.ui.components.ApIconButton
import io.github.d4rthr4tz3.apviewer.ui.components.SegmentedControl
import io.github.d4rthr4tz3.apviewer.ui.feed.itemClassStyle
import io.github.d4rthr4tz3.apviewer.ui.theme.ApColors
import io.github.d4rthr4tz3.apviewer.ui.theme.ApViewerTheme
import io.github.d4rthr4tz3.apviewer.ui.theme.ContentColors

private const val FOUND_ALPHA = 0.55f

/** Hint list of the Hints tab: direction, slot and visibility filters above the hints. */
@Composable
fun HintsList(
    state: HintsState,
    onDirectionChange: (HintDirection) -> Unit,
    onSlotChange: (String?) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            SegmentedControl(
                options = listOf(
                    HintDirection.RECEIVE to stringResource(R.string.hints_receive),
                    HintDirection.FIND to stringResource(R.string.hints_find),
                ),
                selected = state.direction,
                onSelect = onDirectionChange,
                modifier = Modifier.fillMaxWidth(),
            )
            HintSlotPicker(
                slotNames = state.slotNames,
                selected = state.slot,
                onSelect = onSlotChange,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        HorizontalDivider(color = ApColors.Divider)
        if (state.visible.isEmpty()) {
            Box(
                modifier = Modifier.weight(1f).fillMaxWidth().padding(24.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = stringResource(
                        if (state.connected) R.string.hints_empty else R.string.hints_offline,
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    color = ApColors.TextTertiary,
                )
            }
        } else {
            // Each tab is a list of its own that starts at the top; with a shared list state, a
            // hint shown in both tabs would keep its position and scroll the other list.
            key(state.direction) {
                LazyColumn(modifier = Modifier.weight(1f).fillMaxWidth()) {
                    items(state.visible, key = { it.key }) { hint ->
                        HintRow(hint = hint)
                        HorizontalDivider(color = ApColors.Divider)
                    }
                }
            }
        }
    }
}

/** Stable list key: the server identifies a hint by its finding slot and location. */
private val MultiworldHint.key: String get() = "${hint.findingSlot}:${hint.locationId}"

@Composable
private fun HintSlotPicker(
    slotNames: List<String>,
    selected: String?,
    onSelect: (String?) -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }
    val allSlots = stringResource(R.string.hints_all_slots)
    Box(modifier = modifier) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(40.dp)
                .background(ApColors.Surface, MaterialTheme.shapes.small)
                .border(1.dp, ApColors.InputBorder, MaterialTheme.shapes.small)
                .clickable(role = Role.DropdownList) { expanded = true }
                .padding(start = 10.dp, end = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = selected ?: allSlots,
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.bodyMedium,
                color = ApColors.TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Icon(
                painter = painterResource(R.drawable.ic_arrow_drop_down),
                contentDescription = null,
                tint = ApColors.TextSecondary,
            )
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            containerColor = ApColors.Surface,
        ) {
            (listOf<String?>(null) + slotNames).forEach { name ->
                DropdownMenuItem(
                    text = { Text(name ?: allSlots, color = ApColors.TextPrimary) },
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
private fun HintRow(hint: MultiworldHint) {
    val data = hint.hint
    val itemStyle = itemClassStyle(ItemClass.of(data.itemFlags))
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .alpha(if (data.status == HintStatus.FOUND) FOUND_ALPHA else 1f)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = data.itemName,
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.bodyLarge.copy(fontSize = 15.sp),
                color = itemStyle.color,
                fontWeight = itemStyle.weight,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Box(modifier = Modifier.width(104.dp), contentAlignment = Alignment.CenterEnd) {
                Text(
                    text = hintStatusLabel(data.status),
                    style = MaterialTheme.typography.labelMedium,
                    color = hintStatusColor(data.status),
                )
            }
        }
        Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
            HintDetail(
                label = stringResource(R.string.hints_receiver),
                value = data.receiverName,
                color = playerColor(hint.receiverIsOwn),
            )
            HintDetail(
                label = stringResource(R.string.hints_finder),
                value = data.finderName,
                color = playerColor(hint.finderIsOwn),
            )
            HintDetail(
                label = stringResource(R.string.hints_location),
                value = data.locationName,
                color = ContentColors.Location,
            )
            if (data.entrance.isNotEmpty()) {
                HintDetail(
                    label = stringResource(R.string.hints_entrance),
                    value = data.entrance,
                    color = ContentColors.Entrance,
                )
            }
        }
    }
}

@Composable
private fun HintDetail(label: String, value: String, color: Color) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = label,
            modifier = Modifier.width(76.dp),
            style = MaterialTheme.typography.bodySmall,
            color = ApColors.TextTertiary,
        )
        Text(
            text = value,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodySmall,
            color = color,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/** Landscape table of all hints, sortable by every column. */
@Composable
fun HintsTable(
    name: String,
    state: HintsState,
    onSortChange: (HintColumn) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxSize().padding(start = 8.dp, end = 24.dp, bottom = 8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth().height(52.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ApIconButton(
                icon = R.drawable.ic_arrow_back,
                contentDescription = stringResource(R.string.action_back),
                onClick = onBack,
            )
            Text(
                text = stringResource(R.string.tab_hints),
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.titleSmall.copy(fontSize = 16.sp),
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = name,
                style = MaterialTheme.typography.bodySmall,
                color = ApColors.TextTertiary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Column(modifier = Modifier.padding(start = 16.dp)) {
            Row(modifier = Modifier.fillMaxWidth().height(36.dp)) {
                HintColumn.entries.forEach { column ->
                    HeaderCell(
                        column = column,
                        sorted = column == state.sortColumn,
                        onClick = { onSortChange(column) },
                    )
                }
            }
            HorizontalDivider(color = ApColors.InputBorder)
            if (state.table.isEmpty()) {
                Text(
                    text = stringResource(
                        if (state.connected) R.string.hints_empty else R.string.hints_offline,
                    ),
                    modifier = Modifier.padding(vertical = 16.dp, horizontal = 8.dp),
                    style = MaterialTheme.typography.bodyMedium,
                    color = ApColors.TextTertiary,
                )
            }
            LazyColumn(contentPadding = PaddingValues(bottom = 8.dp)) {
                items(state.table, key = { it.key }) { hint ->
                    TableRow(hint)
                    HorizontalDivider(color = ApColors.Divider)
                }
            }
        }
    }
}

private val HintColumn.weight: Float
    get() = when (this) {
        HintColumn.ITEM -> 0.19f
        HintColumn.RECEIVER -> 0.16f
        HintColumn.LOCATION -> 0.25f
        HintColumn.FINDER -> 0.14f
        HintColumn.ENTRANCE -> 0.15f
        HintColumn.STATUS -> 0.11f
    }

@Composable
private fun columnLabel(column: HintColumn): String = stringResource(
    when (column) {
        HintColumn.ITEM -> R.string.hints_item
        HintColumn.RECEIVER -> R.string.hints_receiver
        HintColumn.LOCATION -> R.string.hints_location
        HintColumn.FINDER -> R.string.hints_finder
        HintColumn.ENTRANCE -> R.string.hints_entrance
        HintColumn.STATUS -> R.string.hints_status
    },
)

@Composable
private fun RowScope.HeaderCell(column: HintColumn, sorted: Boolean, onClick: () -> Unit) {
    val label = columnLabel(column)
    val description = stringResource(R.string.hints_sort_by, label)
    Row(
        modifier = Modifier
            .weight(column.weight)
            .height(36.dp)
            .clickable(onClickLabel = description, onClick = onClick)
            .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Medium,
            color = if (sorted) ApColors.TextPrimary else ApColors.TextTertiary,
            maxLines = 1,
        )
        if (sorted) {
            Icon(
                painter = painterResource(R.drawable.ic_arrow_downward),
                contentDescription = null,
                tint = ApColors.TextPrimary,
                modifier = Modifier.size(16.dp),
            )
        }
    }
}

@Composable
private fun TableRow(hint: MultiworldHint) {
    val data = hint.hint
    val itemStyle = itemClassStyle(ItemClass.of(data.itemFlags))
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(34.dp)
            .alpha(if (data.status == HintStatus.FOUND) FOUND_ALPHA else 1f),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TableCell(HintColumn.ITEM, data.itemName, itemStyle.color, itemStyle.weight)
        TableCell(HintColumn.RECEIVER, data.receiverName, playerColor(hint.receiverIsOwn))
        TableCell(HintColumn.LOCATION, data.locationName, ContentColors.Location)
        TableCell(HintColumn.FINDER, data.finderName, playerColor(hint.finderIsOwn))
        TableCell(HintColumn.ENTRANCE, data.entrance, ContentColors.Entrance)
        TableCell(
            HintColumn.STATUS,
            hintStatusLabel(data.status),
            hintStatusColor(data.status),
            FontWeight.Medium,
        )
    }
}

@Composable
private fun RowScope.TableCell(
    column: HintColumn,
    text: String,
    color: Color,
    weight: FontWeight = FontWeight.Normal,
) {
    Text(
        text = text,
        modifier = Modifier.weight(column.weight).padding(horizontal = 8.dp),
        style = MaterialTheme.typography.bodySmall,
        color = color,
        fontWeight = weight,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
    )
}

private fun playerColor(own: Boolean): Color =
    if (own) ContentColors.OwnSlot else ContentColors.OtherPlayer

/** Name of a hint [status]. */
@Composable
fun hintStatusLabel(status: HintStatus): String = stringResource(
    when (status) {
        HintStatus.PRIORITY -> R.string.hint_status_priority
        HintStatus.UNSPECIFIED -> R.string.hint_status_open
        HintStatus.NO_PRIORITY -> R.string.hint_status_no_priority
        HintStatus.AVOID -> R.string.hint_status_avoid
        HintStatus.FOUND -> R.string.hint_status_found
    },
)

/** Color of a hint [status], as in the design's legend. */
fun hintStatusColor(status: HintStatus): Color = when (status) {
    HintStatus.PRIORITY -> ContentColors.HintPriority
    HintStatus.UNSPECIFIED -> ContentColors.HintOpen
    HintStatus.NO_PRIORITY -> ContentColors.HintNoNeed
    HintStatus.AVOID -> ContentColors.HintAvoid
    HintStatus.FOUND -> ContentColors.HintFound
}

private val previewHints = listOf(
    Triple("Hookshot", "Misery Mire - Big Chest", HintStatus.PRIORITY),
    Triple("Moon Pearl", "Subrosia: Temple of Seasons", HintStatus.PRIORITY),
    Triple("Ice Trap", "Hyrule Castle - Zelda's Chest", HintStatus.AVOID),
    Triple("Fire Rod", "Subrosia: Buried Bomb Flower", HintStatus.FOUND),
).mapIndexed { index, (item, location, status) ->
    MultiworldHint(
        hint = SlotHint(
            receivingSlot = 1,
            receiverName = "D4rth_Keymizer",
            findingSlot = 2,
            finderName = "ManneALTTP",
            itemId = index.toLong(),
            itemName = item,
            itemFlags = if (item == "Ice Trap") ItemFlags.TRAP else ItemFlags.PROGRESSION,
            locationId = index.toLong(),
            locationName = location,
            entrance = if (index == 0) "Desert Palace Entrance (North)" else "",
            status = status,
        ),
        receiverIsOwn = true,
        finderIsOwn = false,
    )
}

@Preview(backgroundColor = 0xFF141414, showBackground = true, widthDp = 390, heightDp = 600)
@Composable
private fun HintsListPreview() {
    ApViewerTheme {
        HintsList(
            state = HintsState(
                connected = true,
                slotNames = listOf("D4rth_Keymizer", "D4rth_Doorsmizer"),
                visible = previewHints,
            ),
            onDirectionChange = {},
            onSlotChange = {},
        )
    }
}

@Preview(backgroundColor = 0xFF141414, showBackground = true, widthDp = 844, heightDp = 390)
@Composable
private fun HintsTablePreview() {
    ApViewerTheme {
        HintsTable(
            name = "Zelda-Async Oktober",
            state = HintsState(connected = true, table = previewHints),
            onSortChange = {},
            onBack = {},
        )
    }
}
