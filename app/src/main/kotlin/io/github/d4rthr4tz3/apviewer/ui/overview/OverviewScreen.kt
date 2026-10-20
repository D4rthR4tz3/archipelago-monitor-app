package io.github.d4rthr4tz3.apviewer.ui.overview

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.d4rthr4tz3.apviewer.R
import io.github.d4rthr4tz3.apviewer.protocol.message.FeedHighlight
import io.github.d4rthr4tz3.apviewer.protocol.message.FeedMessage
import io.github.d4rthr4tz3.apviewer.protocol.message.MessageSegment
import io.github.d4rthr4tz3.apviewer.protocol.message.SegmentKind
import io.github.d4rthr4tz3.apviewer.protocol.message.itemParts
import io.github.d4rthr4tz3.apviewer.protocol.model.NetworkItem
import io.github.d4rthr4tz3.apviewer.protocol.monitor.FailureReason
import io.github.d4rthr4tz3.apviewer.protocol.monitor.FeedEntry
import io.github.d4rthr4tz3.apviewer.protocol.session.MultiworldStatus
import io.github.d4rthr4tz3.apviewer.ui.components.ApIconButton
import io.github.d4rthr4tz3.apviewer.ui.components.ApSwitch
import io.github.d4rthr4tz3.apviewer.ui.components.PrimaryButton
import io.github.d4rthr4tz3.apviewer.ui.components.ScreenHeader
import io.github.d4rthr4tz3.apviewer.ui.components.SectionTitle
import io.github.d4rthr4tz3.apviewer.ui.containerViewModel
import io.github.d4rthr4tz3.apviewer.ui.formatClock
import io.github.d4rthr4tz3.apviewer.ui.multiworld.ConfirmDialog
import io.github.d4rthr4tz3.apviewer.ui.statusDetail
import io.github.d4rthr4tz3.apviewer.ui.statusLabel
import io.github.d4rthr4tz3.apviewer.ui.theme.ApColors
import io.github.d4rthr4tz3.apviewer.ui.theme.ApViewerTheme

@Composable
fun OverviewRoute(
    onOpenFeed: (String) -> Unit,
    onAddMultiworld: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    val viewModel = containerViewModel { OverviewViewModel(it) }
    val state by viewModel.state.collectAsStateWithLifecycle()
    OverviewScreen(
        state = state,
        onConnectionsEnabledChange = viewModel::setConnectionsEnabled,
        onOpenFeed = onOpenFeed,
        onAddMultiworld = onAddMultiworld,
        onOpenSettings = onOpenSettings,
    )
}

/** List of saved multiworlds with their connection state. */
@Composable
fun OverviewScreen(
    state: OverviewState,
    onConnectionsEnabledChange: (Boolean) -> Unit,
    onOpenFeed: (String) -> Unit,
    onAddMultiworld: () -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val empty = state.loaded && state.rows.isEmpty()
    Box(modifier = modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            ScreenHeader(
                title = stringResource(R.string.app_name),
                actions = {
                    ApIconButton(
                        icon = R.drawable.ic_settings,
                        contentDescription = stringResource(R.string.overview_settings),
                        onClick = onOpenSettings,
                    )
                },
            )
            if (empty) {
                EmptyOverview(onAddMultiworld = onAddMultiworld, modifier = Modifier.weight(1f))
            } else if (state.rows.isNotEmpty()) {
                ConnectionsCard(state = state, onCheckedChange = onConnectionsEnabledChange)
                SectionTitle(
                    text = stringResource(R.string.overview_multiworlds),
                    modifier = Modifier.padding(start = 20.dp, end = 20.dp, bottom = 8.dp),
                )
                LazyColumn(contentPadding = PaddingValues(bottom = 104.dp)) {
                    items(state.rows, key = { it.id }) { row ->
                        HorizontalDivider(color = ApColors.Divider)
                        MultiworldListItem(row = row, onClick = { onOpenFeed(row.id) })
                    }
                    item { HorizontalDivider(color = ApColors.Divider) }
                }
            }
        }
        if (state.rows.isNotEmpty()) {
            PrimaryButton(
                text = stringResource(R.string.overview_add),
                onClick = onAddMultiworld,
                icon = R.drawable.ic_add,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 16.dp, bottom = 24.dp)
                    .height(56.dp),
            )
        }
    }
}

/** First start: what the app does and the button to add a multiworld. */
@Composable
private fun EmptyOverview(onAddMultiworld: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = 32.dp, end = 32.dp, bottom = 120.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically),
    ) {
        Text(
            text = stringResource(R.string.overview_empty),
            modifier = Modifier.semantics { heading() },
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold,
            color = ApColors.TextPrimary,
        )
        Text(
            text = stringResource(R.string.overview_empty_text),
            style = MaterialTheme.typography.bodyLarge,
            color = ApColors.TextSecondary,
        )
        PrimaryButton(
            text = stringResource(R.string.overview_empty_add),
            onClick = onAddMultiworld,
            icon = R.drawable.ic_add,
            modifier = Modifier.padding(top = 12.dp).height(48.dp),
        )
    }
}

@Composable
private fun ConnectionsCard(state: OverviewState, onCheckedChange: (Boolean) -> Unit) {
    // The card sits at the top of the screen, so turning everything off asks first.
    var confirmOff by rememberSaveable { mutableStateOf(false) }
    val onChange = { enabled: Boolean ->
        if (enabled) onCheckedChange(true) else confirmOff = true
    }
    if (confirmOff) {
        ConfirmDialog(
            title = stringResource(R.string.overview_disable_title),
            text = stringResource(R.string.overview_disable_text),
            confirm = stringResource(R.string.overview_disable_confirm),
            onConfirm = {
                confirmOff = false
                onCheckedChange(false)
            },
            onDismiss = { confirmOff = false },
        )
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 20.dp)
            .background(ApColors.Surface, MaterialTheme.shapes.medium)
            .toggleable(
                value = state.connectionsEnabled,
                // Without a network the switch changes nothing anyone could notice.
                enabled = state.online,
                role = Role.Switch,
                onValueChange = onChange,
            )
            .padding(start = 16.dp, end = 8.dp, top = 12.dp, bottom = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = stringResource(R.string.overview_connections),
                style = MaterialTheme.typography.labelLarge,
                color = if (state.online) ApColors.TextPrimary else ApColors.TextTertiary,
            )
            Text(
                text = if (state.online) {
                    stringResource(
                        R.string.overview_active_slots,
                        state.connectedSlots,
                        state.totalSlots,
                    )
                } else {
                    stringResource(R.string.overview_offline)
                },
                style = MaterialTheme.typography.bodySmall,
                color = if (state.online) ApColors.TextSecondary else ApColors.Warning,
            )
        }
        ApSwitch(
            checked = state.connectionsEnabled,
            onCheckedChange = onChange,
            enabled = state.online,
        )
    }
}

@Composable
private fun MultiworldListItem(row: MultiworldRow, onClick: () -> Unit) {
    val disconnected = row.status == MultiworldStatus.Disconnected
    val statusColor = when (row.status) {
        is MultiworldStatus.Reconnecting, MultiworldStatus.Offline -> ApColors.Warning
        is MultiworldStatus.Failed -> ApColors.Error
        MultiworldStatus.Disconnected -> ApColors.TextTertiary
        else -> ApColors.TextSecondary
    }
    val detail = statusDetail(row.status) ?: row.latestForMe
        ?.takeUnless { disconnected }
        ?.let { latestItemText(it) }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(start = 20.dp, end = 12.dp, top = 16.dp, bottom = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        MultiworldSummary(
            row = row,
            disconnected = disconnected,
            statusColor = statusColor,
            detail = detail,
            modifier = Modifier.weight(1f),
        )
        Icon(
            painter = painterResource(R.drawable.ic_chevron_right),
            contentDescription = null,
            tint = ApColors.TextTertiary,
        )
    }
}

@Composable
private fun MultiworldSummary(
    row: MultiworldRow,
    disconnected: Boolean,
    statusColor: Color,
    detail: String?,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                text = row.name,
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
                color = if (disconnected) ApColors.TextSecondary else ApColors.TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (row.unread > 0) {
                Text(
                    text = pluralStringResource(R.plurals.overview_unread, row.unread, row.unread),
                    style = MaterialTheme.typography.labelMedium,
                    color = ApColors.TextPrimary,
                )
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                text = row.slotNames.joinToString(),
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.bodySmall,
                color = if (disconnected) ApColors.TextTertiary else ApColors.TextSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = statusLabel(row.status),
                style = MaterialTheme.typography.bodySmall,
                color = statusColor,
            )
        }
        if (detail != null) {
            Text(
                text = detail,
                modifier = Modifier.padding(top = 2.dp),
                style = MaterialTheme.typography.bodySmall,
                color = ApColors.TextTertiary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun latestItemText(entry: FeedEntry): String? {
    val parts = entry.message.itemParts() ?: return null
    val receiver = (parts.receiver ?: parts.sender).text
    return stringResource(
        R.string.overview_latest_item,
        formatClock(entry.receivedAtMillis),
        parts.item.text,
        receiver,
    )
}

@Preview(backgroundColor = 0xFF141414, showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun OverviewScreenPreview() {
    val latest = FeedEntry(
        id = 1,
        receivedAtMillis = 1_760_000_000_000,
        message = FeedMessage(
            type = "ItemSend",
            segments = listOf(
                MessageSegment("hellow-oos", SegmentKind.PLAYER),
                MessageSegment(" sent "),
                MessageSegment("Small Heart", SegmentKind.ITEM),
                MessageSegment(" to "),
                MessageSegment("D4rth_Keymizer", SegmentKind.OWN_PLAYER),
            ),
            item = NetworkItem(item = 1, location = 2, player = 3),
            highlight = FeedHighlight.FOR_ME,
        ),
    )
    ApViewerTheme {
        OverviewScreen(
            state = OverviewState(
                loaded = true,
                rows = listOf(
                    MultiworldRow(
                        "1",
                        "Zelda-Async Oktober",
                        listOf("D4rth_Keymizer", "D4rth_Doorsmizer"),
                        MultiworldStatus.Connected(2),
                        5,
                        latest,
                    ),
                    MultiworldRow(
                        "2",
                        "Freitagsrunde",
                        listOf("Sven-HK"),
                        MultiworldStatus.Reconnecting(3, 1_760_000_000_000),
                        0,
                        null,
                    ),
                    MultiworldRow(
                        "3",
                        "Metroid-Weekly",
                        listOf("Sven_SM"),
                        MultiworldStatus.Failed(
                            "Sven_SM",
                            FailureReason.Refused(listOf("InvalidSlot")),
                        ),
                        0,
                        null,
                    ),
                ),
            ),
            onConnectionsEnabledChange = {},
            onOpenFeed = {},
            onAddMultiworld = {},
            onOpenSettings = {},
        )
    }
}
