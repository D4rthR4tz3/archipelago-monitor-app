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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
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
import io.github.d4rthr4tz3.apviewer.ui.components.PlainButton
import io.github.d4rthr4tz3.apviewer.ui.components.PrimaryButton
import io.github.d4rthr4tz3.apviewer.ui.components.ScreenHeader
import io.github.d4rthr4tz3.apviewer.ui.components.SecondaryButton
import io.github.d4rthr4tz3.apviewer.ui.components.SegmentedControl
import io.github.d4rthr4tz3.apviewer.ui.components.SwitchRow
import io.github.d4rthr4tz3.apviewer.ui.containerViewModel
import io.github.d4rthr4tz3.apviewer.ui.failureAdvice
import io.github.d4rthr4tz3.apviewer.ui.failureHeadline
import io.github.d4rthr4tz3.apviewer.ui.failureTitle
import io.github.d4rthr4tz3.apviewer.ui.hints.HintsList
import io.github.d4rthr4tz3.apviewer.ui.hints.HintsTable
import io.github.d4rthr4tz3.apviewer.ui.hints.HintsViewModel
import io.github.d4rthr4tz3.apviewer.ui.items.ItemsList
import io.github.d4rthr4tz3.apviewer.ui.items.ItemsViewModel
import io.github.d4rthr4tz3.apviewer.ui.statusDetail
import io.github.d4rthr4tz3.apviewer.ui.statusWithSlots
import io.github.d4rthr4tz3.apviewer.ui.theme.ApColors
import io.github.d4rthr4tz3.apviewer.ui.theme.ApViewerTheme

/** Tabs of a multiworld. */
enum class MultiworldTab { FEED, HINTS, ITEMS }

@Composable
fun FeedRoute(multiworldId: String, onBack: () -> Unit, onEdit: () -> Unit) {
    val viewModel = containerViewModel { FeedViewModel(it, multiworldId) }
    val hintsViewModel = containerViewModel { HintsViewModel(it, multiworldId) }
    val itemsViewModel = containerViewModel { ItemsViewModel(it, multiworldId) }
    val state by viewModel.state.collectAsStateWithLifecycle()
    val hints by hintsViewModel.state.collectAsStateWithLifecycle()
    val items by itemsViewModel.state.collectAsStateWithLifecycle()
    FeedScreen(
        state = state,
        priorityHints = hints.priorityCount,
        unseenItems = items.unseenCount,
        onBack = onBack,
        onEdit = onEdit,
        onFilterChange = viewModel::setFilter,
        onItemClassesChange = viewModel::setItemClasses,
        onSendAsChange = viewModel::setSendAs,
        onSay = viewModel::say,
        onConnect = viewModel::connect,
        onDisconnect = viewModel::disconnect,
        hintsList = { modifier ->
            HintsList(
                state = hints,
                onDirectionChange = hintsViewModel::setDirection,
                onSlotChange = hintsViewModel::setSlot,
                onHideFoundChange = hintsViewModel::setHideFound,
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
        itemsList = { modifier, onOpenItemFilter ->
            ItemsList(
                state = items,
                itemClasses = state.itemClasses,
                onSlotChange = itemsViewModel::setSlot,
                onOpenItemFilter = onOpenItemFilter,
                onSeen = itemsViewModel::markSeen,
                onLeave = itemsViewModel::leave,
                modifier = modifier,
            )
        },
    )
}

/**
 * Text client of a multiworld: the room's messages, filters and the chat input, the Hints tab with
 * [hintsList] and the Items tab with [itemsList]. In landscape the Hints tab fills the screen with
 * [hintsTable].
 */
@Composable
fun FeedScreen(
    state: FeedState,
    priorityHints: Int,
    unseenItems: Int,
    onBack: () -> Unit,
    onEdit: () -> Unit,
    onFilterChange: (FeedFilter) -> Unit,
    onItemClassesChange: (Set<ItemClass>) -> Unit,
    onSendAsChange: (String) -> Unit,
    onSay: (String) -> Boolean,
    onConnect: () -> Unit,
    onDisconnect: () -> Unit,
    hintsList: @Composable (Modifier) -> Unit,
    hintsTable: @Composable () -> Unit,
    itemsList: @Composable (Modifier, onOpenItemFilter: () -> Unit) -> Unit,
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
                unseenItems = unseenItems,
                onTabChange = { tab = it },
                onBack = onBack,
                onEdit = onEdit,
                onFilterChange = onFilterChange,
                onOpenItemFilter = { showItemFilter = true },
                onSendAsChange = onSendAsChange,
                onSay = onSay,
                onConnect = onConnect,
                onDisconnect = onDisconnect,
                hintsList = hintsList,
                itemsList = itemsList,
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
    unseenItems: Int,
    onTabChange: (MultiworldTab) -> Unit,
    onBack: () -> Unit,
    onEdit: () -> Unit,
    onFilterChange: (FeedFilter) -> Unit,
    onOpenItemFilter: () -> Unit,
    onSendAsChange: (String) -> Unit,
    onSay: (String) -> Boolean,
    onConnect: () -> Unit,
    onDisconnect: () -> Unit,
    hintsList: @Composable (Modifier) -> Unit,
    itemsList: @Composable (Modifier, onOpenItemFilter: () -> Unit) -> Unit,
) {
    val status = state.status
    val failed = status as? MultiworldStatus.Failed
    Column(modifier = Modifier.fillMaxSize().imePadding()) {
        ScreenHeader(
            title = state.name,
            subtitle = if (failed != null) {
                failureTitle(failed.reason)
            } else {
                statusWithSlots(status, state.slotNames.size)
            },
            subtitleColor = if (failed != null) ApColors.Error else ApColors.TextSecondary,
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
        Tabs(
            selected = tab,
            counts = mapOf(
                MultiworldTab.HINTS to priorityHints,
                MultiworldTab.ITEMS to unseenItems,
            ),
            onSelect = onTabChange,
        )
        val connecting = status == MultiworldStatus.Connecting
        when (tab) {
            MultiworldTab.FEED -> when {
                failed != null -> {
                    FailureCard(failure = failed, onEdit = onEdit, onConnect = onConnect)
                    if (state.entries.isNotEmpty()) {
                        Text(
                            text = stringResource(R.string.feed_history),
                            modifier = Modifier.padding(
                                start = 16.dp,
                                end = 16.dp,
                                top = 8.dp,
                                bottom = 6.dp,
                            ),
                            style = MaterialTheme.typography.labelSmall,
                            color = ApColors.TextTertiary,
                        )
                    }
                    FeedList(state = state, modifier = Modifier.weight(1f).alpha(0.7f))
                }

                connecting && state.entries.isEmpty() -> ConnectingPanel(
                    address = state.address,
                    slotNames = state.slotNames,
                    modifier = Modifier.weight(1f),
                )

                else -> {
                    FilterBar(
                        filter = state.filter,
                        itemFilterActive = state.itemClasses.size < ItemClass.entries.size,
                        onFilterChange = onFilterChange,
                        onOpenItemFilter = onOpenItemFilter,
                    )
                    FeedList(state = state, modifier = Modifier.weight(1f))
                }
            }

            MultiworldTab.HINTS -> hintsList(Modifier.weight(1f))

            MultiworldTab.ITEMS -> itemsList(Modifier.weight(1f), onOpenItemFilter)
        }
        // The failure card of the feed has its own reconnect button.
        if (tab != MultiworldTab.FEED || failed == null) {
            HorizontalDivider(color = ApColors.Divider)
            when {
                status is MultiworldStatus.Connected -> SayBar(
                    slotNames = state.slotNames,
                    sendAs = state.sendAs,
                    onSendAsChange = onSendAsChange,
                    onSay = onSay,
                )

                connecting -> ConnectingBar(onCancel = onDisconnect)

                else -> ConnectBar(status = status, onConnect = onConnect)
            }
        }
    }
}

/** Spinner and the address while the first connection is made and no history is stored. */
@Composable
private fun ConnectingPanel(
    address: String,
    slotNames: List<String>,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth().padding(horizontal = 32.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        CircularProgressIndicator(
            modifier = Modifier.size(32.dp),
            color = ApColors.TextPrimary,
            trackColor = ApColors.InputBorder,
            strokeWidth = 3.dp,
        )
        Column(
            modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
            verticalArrangement = Arrangement.spacedBy(4.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = stringResource(R.string.feed_connecting_to, address),
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                color = ApColors.TextPrimary,
                textAlign = TextAlign.Center,
            )
            if (slotNames.isNotEmpty()) {
                Text(
                    text = joinWithAnd(slotNames),
                    style = MaterialTheme.typography.bodySmall,
                    color = ApColors.TextSecondary,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

/** "A, B and C" in the app's language. */
@Composable
private fun joinWithAnd(names: List<String>): String = if (names.size == 1) {
    names.single()
} else {
    stringResource(R.string.list_and, names.dropLast(1).joinToString(), names.last())
}

@Composable
private fun ConnectingBar(onCancel: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = stringResource(R.string.feed_connecting_hint),
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodySmall,
            color = ApColors.TextSecondary,
        )
        SecondaryButton(text = stringResource(R.string.action_cancel), onClick = onCancel)
    }
}

/** Why the connection failed, with the ways to fix it. */
@Composable
private fun FailureCard(
    failure: MultiworldStatus.Failed,
    onEdit: () -> Unit,
    onConnect: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 8.dp)
            .background(ApColors.Surface, MaterialTheme.shapes.medium)
            .border(1.dp, ApColors.ErrorBorder, MaterialTheme.shapes.medium)
            .semantics { liveRegion = LiveRegionMode.Assertive }
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Icon(
                painter = painterResource(R.drawable.ic_error),
                contentDescription = null,
                tint = ApColors.Error,
                modifier = Modifier.size(20.dp),
            )
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = failureHeadline(failure.reason),
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    color = ApColors.TextPrimary,
                )
                Text(
                    text = failureAdvice(failure),
                    style = MaterialTheme.typography.bodySmall,
                    color = ApColors.TextSecondary,
                )
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
        ) {
            SecondaryButton(
                text = stringResource(R.string.feed_multiworld_settings),
                onClick = onEdit,
                modifier = Modifier.height(40.dp),
            )
            PrimaryButton(
                text = stringResource(R.string.action_reconnect),
                onClick = onConnect,
            )
        }
    }
}

@Composable
private fun Tabs(
    selected: MultiworldTab,
    counts: Map<MultiworldTab, Int>,
    onSelect: (MultiworldTab) -> Unit,
) {
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
            MultiworldTab.ITEMS to stringResource(R.string.tab_items),
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
                val count = counts[tab] ?: 0
                if (count > 0) {
                    Text(
                        text = stringResource(R.string.tab_count, count),
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
        ItemFilterButton(active = itemFilterActive, onClick = onOpenItemFilter)
    }
}

/** Opens the item type filter; lighter while the filter hides a type. */
@Composable
fun ItemFilterButton(active: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    IconButton(
        onClick = onClick,
        modifier = modifier
            .size(40.dp)
            .background(
                if (active) ApColors.SegmentActive else ApColors.Surface,
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
            Row(
                modifier = Modifier.fillMaxWidth().padding(
                    start = 16.dp,
                    end = 8.dp,
                    bottom = 8.dp,
                ),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(R.string.item_filter_title),
                    modifier = Modifier.weight(1f).semantics { heading() },
                    style = MaterialTheme.typography.titleMedium,
                )
                PlainButton(
                    text = stringResource(R.string.item_filter_show_all),
                    onClick = { onChange(ItemClass.entries.toSet()) },
                )
            }
            ItemClass.entries.forEach { itemClass ->
                val style = itemClassStyle(itemClass)
                SwitchRow(
                    label = itemClassLabel(itemClass),
                    checked = itemClass in selected,
                    onCheckedChange = { on ->
                        onChange(if (on) selected + itemClass else selected - itemClass)
                    },
                    modifier = Modifier.heightIn(min = 56.dp),
                    leading = { ItemClassMark(itemClass) },
                    labelColor = style.color,
                    labelWeight = style.weight,
                )
            }
            Text(
                text = stringResource(R.string.item_filter_note),
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 8.dp),
                style = MaterialTheme.typography.bodySmall,
                color = ApColors.TextTertiary,
            )
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
                address = "archipelago.gg:38281",
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
            onDisconnect = {},
            priorityHints = 2,
            unseenItems = 3,
            hintsList = {},
            hintsTable = {},
            itemsList = { _, _ -> },
        )
    }
}
