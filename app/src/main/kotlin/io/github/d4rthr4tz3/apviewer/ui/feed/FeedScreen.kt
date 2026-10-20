package io.github.d4rthr4tz3.apviewer.ui.feed

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.d4rthr4tz3.apviewer.R
import io.github.d4rthr4tz3.apviewer.protocol.message.FeedFilter
import io.github.d4rthr4tz3.apviewer.protocol.message.FeedHighlight
import io.github.d4rthr4tz3.apviewer.protocol.message.FeedMessage
import io.github.d4rthr4tz3.apviewer.protocol.message.MessageSegment
import io.github.d4rthr4tz3.apviewer.protocol.message.SegmentKind
import io.github.d4rthr4tz3.apviewer.protocol.model.ItemFlags
import io.github.d4rthr4tz3.apviewer.protocol.model.NetworkItem
import io.github.d4rthr4tz3.apviewer.protocol.monitor.FeedEntry
import io.github.d4rthr4tz3.apviewer.protocol.session.MultiworldStatus
import io.github.d4rthr4tz3.apviewer.protocol.store.FeedLayout
import io.github.d4rthr4tz3.apviewer.protocol.store.ItemClass
import io.github.d4rthr4tz3.apviewer.ui.components.ApIconButton
import io.github.d4rthr4tz3.apviewer.ui.components.ApTextField
import io.github.d4rthr4tz3.apviewer.ui.components.PrimaryButton
import io.github.d4rthr4tz3.apviewer.ui.components.ScreenHeader
import io.github.d4rthr4tz3.apviewer.ui.components.SegmentedControl
import io.github.d4rthr4tz3.apviewer.ui.components.SwitchRow
import io.github.d4rthr4tz3.apviewer.ui.containerViewModel
import io.github.d4rthr4tz3.apviewer.ui.hints.HintsList
import io.github.d4rthr4tz3.apviewer.ui.hints.HintsTable
import io.github.d4rthr4tz3.apviewer.ui.hints.HintsViewModel
import io.github.d4rthr4tz3.apviewer.ui.statusDetail
import io.github.d4rthr4tz3.apviewer.ui.statusWithSlots
import io.github.d4rthr4tz3.apviewer.ui.theme.ApColors
import io.github.d4rthr4tz3.apviewer.ui.theme.ApViewerTheme

/** Tabs of a multiworld. */
enum class MultiworldTab { FEED, HINTS }

@Composable
fun FeedRoute(multiworldId: String, onBack: () -> Unit, onEdit: () -> Unit) {
    val viewModel = containerViewModel { FeedViewModel(it, multiworldId) }
    val hintsViewModel = containerViewModel { HintsViewModel(it, multiworldId) }
    val state by viewModel.state.collectAsStateWithLifecycle()
    val hints by hintsViewModel.state.collectAsStateWithLifecycle()
    FeedScreen(
        state = state,
        priorityHints = hints.priorityCount,
        onBack = onBack,
        onEdit = onEdit,
        onFilterChange = viewModel::setFilter,
        onItemClassesChange = viewModel::setItemClasses,
        onSendAsChange = viewModel::setSendAs,
        onSay = viewModel::say,
        onConnect = viewModel::connect,
        hintsList = { modifier ->
            HintsList(
                state = hints,
                onDirectionChange = hintsViewModel::setDirection,
                onSlotChange = hintsViewModel::setSlot,
                onHideFoundChange = hintsViewModel::setHideFound,
                onStatusChange = { hint, status -> hintsViewModel.updateStatus(hint, status) },
                modifier = modifier,
            )
        },
        hintsTable = {
            HintsTable(
                name = state.name,
                state = hints,
                onSortChange = hintsViewModel::setSortColumn,
                onBack = onBack,
            )
        },
    )
}

/**
 * Text client of a multiworld: the room's messages, filters and the chat input, and the Hints tab
 * with [hintsList]. In landscape the Hints tab fills the screen with [hintsTable].
 */
@Composable
fun FeedScreen(
    state: FeedState,
    priorityHints: Int,
    onBack: () -> Unit,
    onEdit: () -> Unit,
    onFilterChange: (FeedFilter) -> Unit,
    onItemClassesChange: (Set<ItemClass>) -> Unit,
    onSendAsChange: (String) -> Unit,
    onSay: (String) -> Boolean,
    onConnect: () -> Unit,
    hintsList: @Composable (Modifier) -> Unit,
    hintsTable: @Composable () -> Unit,
    modifier: Modifier = Modifier,
) {
    var tab by rememberSaveable { mutableStateOf(MultiworldTab.FEED) }
    var showItemFilter by rememberSaveable { mutableStateOf(false) }

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        if (tab == MultiworldTab.HINTS && maxWidth > maxHeight) {
            hintsTable()
        } else {
            FeedColumn(
                state = state,
                tab = tab,
                priorityHints = priorityHints,
                onTabChange = { tab = it },
                onBack = onBack,
                onEdit = onEdit,
                onFilterChange = onFilterChange,
                onOpenItemFilter = { showItemFilter = true },
                onSendAsChange = onSendAsChange,
                onSay = onSay,
                onConnect = onConnect,
                hintsList = hintsList,
            )
        }
    }
    if (showItemFilter) {
        ItemFilterSheet(
            selected = state.itemClasses,
            onChange = onItemClassesChange,
            onDismiss = { showItemFilter = false },
        )
    }
}

@Composable
private fun FeedColumn(
    state: FeedState,
    tab: MultiworldTab,
    priorityHints: Int,
    onTabChange: (MultiworldTab) -> Unit,
    onBack: () -> Unit,
    onEdit: () -> Unit,
    onFilterChange: (FeedFilter) -> Unit,
    onOpenItemFilter: () -> Unit,
    onSendAsChange: (String) -> Unit,
    onSay: (String) -> Boolean,
    onConnect: () -> Unit,
    hintsList: @Composable (Modifier) -> Unit,
) {
    Column(modifier = Modifier.fillMaxSize().imePadding()) {
        ScreenHeader(
            title = state.name,
            subtitle = statusWithSlots(state.status, state.slotNames.size),
            navigation = {
                ApIconButton(
                    icon = R.drawable.ic_arrow_back,
                    contentDescription = stringResource(R.string.action_back),
                    onClick = onBack,
                )
            },
            actions = {
                ApIconButton(
                    icon = R.drawable.ic_settings,
                    contentDescription = stringResource(R.string.feed_multiworld_settings),
                    onClick = onEdit,
                )
            },
        )
        Tabs(selected = tab, priorityHints = priorityHints, onSelect = onTabChange)
        when (tab) {
            MultiworldTab.FEED -> {
                FilterBar(
                    filter = state.filter,
                    itemFilterActive = state.itemClasses.size < ItemClass.entries.size,
                    onFilterChange = onFilterChange,
                    onOpenItemFilter = onOpenItemFilter,
                )
                FeedList(state = state, modifier = Modifier.weight(1f))
            }

            MultiworldTab.HINTS -> hintsList(Modifier.weight(1f))
        }
        HorizontalDivider(color = ApColors.Divider)
        if (state.status is MultiworldStatus.Connected) {
            SayBar(
                slotNames = state.slotNames,
                sendAs = state.sendAs,
                onSendAsChange = onSendAsChange,
                onSay = onSay,
            )
        } else {
            ConnectBar(status = state.status, onConnect = onConnect)
        }
    }
}

@Composable
private fun Tabs(selected: MultiworldTab, priorityHints: Int, onSelect: (MultiworldTab) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .drawBehind {
                drawLine(
                    ApColors.Divider,
                    Offset(0f, size.height - 0.5f),
                    Offset(size.width, size.height - 0.5f),
                    strokeWidth = 1.dp.toPx(),
                )
            },
    ) {
        listOf(
            MultiworldTab.FEED to stringResource(R.string.tab_feed),
            MultiworldTab.HINTS to stringResource(R.string.tab_hints),
        ).forEach { (tab, label) ->
            val isSelected = tab == selected
            Row(
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
                    .semantics { this.selected = isSelected }
                    .clickable(role = Role.Tab) { onSelect(tab) }
                    .drawBehind {
                        if (isSelected) {
                            drawRect(
                                ApColors.TextPrimary,
                                topLeft = Offset(0f, size.height - 2.dp.toPx()),
                            )
                        }
                    },
                horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                    color = if (isSelected) ApColors.TextPrimary else ApColors.TextSecondary,
                )
                if (tab == MultiworldTab.HINTS && priorityHints > 0) {
                    Text(
                        text = stringResource(R.string.hints_tab_count, priorityHints),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Medium,
                        color = ApColors.TextSecondary,
                    )
                }
            }
        }
    }
}

@Composable
private fun FilterBar(
    filter: FeedFilter,
    itemFilterActive: Boolean,
    onFilterChange: (FeedFilter) -> Unit,
    onOpenItemFilter: () -> Unit,
) {
    Row(
        modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SegmentedControl(
            options = listOf(
                FeedFilter.ALL to stringResource(R.string.filter_all),
                FeedFilter.FOR_ME to stringResource(R.string.filter_for_me),
                FeedFilter.FROM_ME to stringResource(R.string.filter_from_me),
                FeedFilter.CHAT to stringResource(R.string.filter_chat),
            ),
            selected = filter,
            onSelect = onFilterChange,
            modifier = Modifier.weight(1f),
        )
        IconButton(
            onClick = onOpenItemFilter,
            modifier = Modifier
                .size(40.dp)
                .background(
                    if (itemFilterActive) ApColors.SegmentActive else ApColors.Surface,
                    MaterialTheme.shapes.small,
                ),
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_filter_list),
                contentDescription = stringResource(R.string.filter_item_types),
                tint = ApColors.TextPrimary,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}

@Composable
private fun FeedList(state: FeedState, modifier: Modifier = Modifier) {
    val listState = rememberLazyListState()
    LaunchedEffect(state.entries.lastOrNull()?.id) {
        if (state.entries.isNotEmpty()) listState.scrollToItem(state.entries.lastIndex)
    }
    if (state.loaded && state.entries.isEmpty()) {
        Box(modifier = modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            Text(
                text = stringResource(R.string.feed_empty),
                style = MaterialTheme.typography.bodyMedium,
                color = ApColors.TextTertiary,
            )
        }
        return
    }
    LazyColumn(
        modifier = modifier.fillMaxWidth(),
        state = listState,
        contentPadding = PaddingValues(start = 10.dp, end = 10.dp, bottom = 8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp, Alignment.Bottom),
    ) {
        items(state.entries, key = { it.id }) { entry ->
            when (state.layout) {
                FeedLayout.CLASSIC -> ClassicFeedRow(entry)
                FeedLayout.STRUCTURED -> StructuredFeedRow(entry)
            }
        }
    }
}

@Composable
private fun SayBar(
    slotNames: List<String>,
    sendAs: String?,
    onSendAsChange: (String) -> Unit,
    onSay: (String) -> Boolean,
) {
    var text by rememberSaveable { mutableStateOf("") }
    val send = {
        if (onSay(text)) text = ""
    }
    Row(
        modifier = Modifier.padding(start = 12.dp, end = 12.dp, top = 10.dp, bottom = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (slotNames.size > 1 && sendAs != null) {
            SlotPicker(slotNames = slotNames, selected = sendAs, onSelect = onSendAsChange)
        }
        ApTextField(
            value = text,
            onValueChange = { text = it },
            modifier = Modifier.weight(1f),
            placeholder = stringResource(R.string.say_placeholder),
            height = 44.dp,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
            keyboardActions = KeyboardActions(onSend = { send() }),
        )
        IconButton(
            onClick = send,
            enabled = text.isNotBlank(),
            modifier = Modifier
                .size(44.dp)
                .background(
                    if (text.isNotBlank()) ApColors.TextPrimary else ApColors.SegmentActive,
                    MaterialTheme.shapes.small,
                ),
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_send),
                contentDescription = stringResource(R.string.say_send),
                tint = if (text.isNotBlank()) ApColors.Background else ApColors.TextTertiary,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}

@Composable
private fun SlotPicker(slotNames: List<String>, selected: String, onSelect: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    val label = stringResource(R.string.say_as, selected)
    Box {
        Row(
            modifier = Modifier
                .width(112.dp)
                .height(44.dp)
                .background(ApColors.Surface, MaterialTheme.shapes.small)
                .border(1.dp, ApColors.InputBorder, MaterialTheme.shapes.small)
                .clickable(role = Role.DropdownList, onClickLabel = label) { expanded = true }
                .padding(start = 8.dp, end = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = selected,
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.labelMedium,
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
            slotNames.forEach { name ->
                DropdownMenuItem(
                    text = { Text(name, color = ApColors.TextPrimary) },
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
private fun ConnectBar(status: MultiworldStatus, onConnect: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 12.dp, top = 10.dp, bottom = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = statusDetail(status) ?: stringResource(R.string.say_unavailable),
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodySmall,
            color = when (status) {
                is MultiworldStatus.Failed -> ApColors.Error
                is MultiworldStatus.Reconnecting -> ApColors.Warning
                else -> ApColors.TextSecondary
            },
        )
        if (status == MultiworldStatus.Disconnected || status is MultiworldStatus.Failed) {
            PrimaryButton(text = stringResource(R.string.action_connect), onClick = onConnect)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ItemFilterSheet(
    selected: Set<ItemClass>,
    onChange: (Set<ItemClass>) -> Unit,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = ApColors.Surface,
        contentColor = ApColors.TextPrimary,
    ) {
        Column(modifier = Modifier.padding(bottom = 24.dp)) {
            Text(
                text = stringResource(R.string.filter_item_types),
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                style = MaterialTheme.typography.titleMedium,
            )
            ItemClass.entries.forEach { itemClass ->
                SwitchRow(
                    label = itemClassLabel(itemClass),
                    checked = itemClass in selected,
                    onCheckedChange = { on ->
                        onChange(if (on) selected + itemClass else selected - itemClass)
                    },
                    leading = { ItemClassMark(itemClass) },
                )
            }
        }
    }
}

/** Short colored bar that marks an item class. */
@Composable
fun ItemClassMark(itemClass: ItemClass, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(width = 3.dp, height = 16.dp)
            .background(itemClassStyle(itemClass).color, MaterialTheme.shapes.extraSmall),
    )
}

/** Name of [itemClass]. */
@Composable
fun itemClassLabel(itemClass: ItemClass): String = stringResource(
    when (itemClass) {
        ItemClass.PROGRESSION -> R.string.item_class_progression
        ItemClass.USEFUL -> R.string.item_class_useful
        ItemClass.FILLER -> R.string.item_class_filler
        ItemClass.TRAP -> R.string.item_class_trap
    },
)

@Preview(backgroundColor = 0xFF141414, showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun FeedScreenPreview() {
    val entries = listOf(
        FeedEntry(
            id = 0,
            receivedAtMillis = 1_760_000_000_000,
            message = FeedMessage(
                type = "ItemSend",
                segments = listOf(
                    MessageSegment("ManneALTTP", SegmentKind.PLAYER),
                    MessageSegment(" sent "),
                    MessageSegment(
                        "Small Key (Skull Woods)",
                        SegmentKind.ITEM,
                        ItemFlags.PROGRESSION,
                    ),
                    MessageSegment(" to "),
                    MessageSegment("D4rth_Keymizer", SegmentKind.OWN_PLAYER),
                    MessageSegment(" ("),
                    MessageSegment("Sewers - Secret Room - Right", SegmentKind.LOCATION),
                    MessageSegment(")"),
                ),
                item = NetworkItem(
                    item = 1,
                    location = 2,
                    player = 3,
                    flags = ItemFlags.PROGRESSION,
                ),
                highlight = FeedHighlight.FOR_ME,
            ),
        ),
        FeedEntry(
            id = 1,
            receivedAtMillis = 1_760_000_040_000,
            message = FeedMessage(
                type = "Chat",
                segments = listOf(
                    MessageSegment("ManneALTTP", SegmentKind.PLAYER),
                    MessageSegment(": Hat jemand schon meinen Hookshot gesehen?"),
                ),
            ),
        ),
    )
    ApViewerTheme {
        FeedScreen(
            state = FeedState(
                loaded = true,
                name = "Zelda-Async Oktober",
                slotNames = listOf("D4rth_Keymizer", "D4rth_Doorsmizer"),
                status = MultiworldStatus.Connected(2),
                entries = entries,
                sendAs = "D4rth_Keymizer",
            ),
            onBack = {},
            onEdit = {},
            onFilterChange = {},
            onItemClassesChange = {},
            onSendAsChange = {},
            onSay = { true },
            onConnect = {},
            priorityHints = 2,
            hintsList = {},
            hintsTable = {},
        )
    }
}
