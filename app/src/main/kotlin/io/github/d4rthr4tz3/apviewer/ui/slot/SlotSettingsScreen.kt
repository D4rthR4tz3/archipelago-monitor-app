package io.github.d4rthr4tz3.apviewer.ui.slot

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.d4rthr4tz3.apviewer.R
import io.github.d4rthr4tz3.apviewer.protocol.session.SlotState
import io.github.d4rthr4tz3.apviewer.protocol.store.ItemClass
import io.github.d4rthr4tz3.apviewer.protocol.store.NotifyMode
import io.github.d4rthr4tz3.apviewer.protocol.store.SavedSlot
import io.github.d4rthr4tz3.apviewer.protocol.store.SlotNotifications
import io.github.d4rthr4tz3.apviewer.ui.components.ApIconButton
import io.github.d4rthr4tz3.apviewer.ui.components.LabeledTextField
import io.github.d4rthr4tz3.apviewer.ui.components.PlainButton
import io.github.d4rthr4tz3.apviewer.ui.components.RadioDot
import io.github.d4rthr4tz3.apviewer.ui.components.ScreenHeader
import io.github.d4rthr4tz3.apviewer.ui.components.SecondaryButton
import io.github.d4rthr4tz3.apviewer.ui.components.SectionTitle
import io.github.d4rthr4tz3.apviewer.ui.components.SwitchRow
import io.github.d4rthr4tz3.apviewer.ui.feed.ItemClassMark
import io.github.d4rthr4tz3.apviewer.ui.feed.itemClassLabel
import io.github.d4rthr4tz3.apviewer.ui.multiworld.ConfirmDialog
import io.github.d4rthr4tz3.apviewer.ui.multiworld.LiveSlot
import io.github.d4rthr4tz3.apviewer.ui.multiworld.MultiworldEditViewModel
import io.github.d4rthr4tz3.apviewer.ui.theme.ApColors
import io.github.d4rthr4tz3.apviewer.ui.theme.GeistMono

/** Settings of one of the user's slots, part of the draft of [viewModel]. */
@Composable
fun SlotSettingsScreen(viewModel: MultiworldEditViewModel, slotIndex: Int, onBack: () -> Unit) {
    val draft by viewModel.draft.collectAsStateWithLifecycle()
    val players by viewModel.roomPlayers.collectAsStateWithLifecycle()
    val slot = draft?.slots?.getOrNull(slotIndex) ?: return
    val liveFlow = remember(slot.name) { viewModel.liveSlot(slot.name) }
    val live by liveFlow.collectAsStateWithLifecycle(initialValue = null)
    val player = players.firstOrNull { it.name == slot.name }
    var confirmRemove by rememberSaveable { mutableStateOf(false) }
    val edit: ((SavedSlot) -> SavedSlot) -> Unit = { viewModel.editSlot(slotIndex, it) }

    Column(modifier = Modifier.fillMaxSize().imePadding()) {
        ScreenHeader(
            title = slot.name,
            subtitle = player?.let {
                listOfNotNull(it.game, stringResource(R.string.slot_number, it.slot)).joinToString()
            },
            navigation = {
                ApIconButton(
                    icon = R.drawable.ic_arrow_back,
                    contentDescription = stringResource(R.string.action_back),
                    onClick = onBack,
                )
            },
        )
        Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
            ConnectionCard(
                live = live,
                onConnect = viewModel::connect,
                onDisconnect = viewModel::disconnect,
            )
            AccessSection(slot = slot, onEdit = edit)
            HorizontalDivider(color = ApColors.Divider)
            NotificationSection(
                notifications = slot.notifications,
                onChange = { notifications -> edit { it.copy(notifications = notifications) } },
            )
            HorizontalDivider(color = ApColors.Divider)
            SyncSection(live = live, onCountFromNow = { viewModel.countFromNow(slot.name) })
            HorizontalDivider(color = ApColors.Divider)
            PlainButton(
                text = stringResource(R.string.slot_remove),
                onClick = { confirmRemove = true },
                color = ApColors.Error,
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 40.dp),
            )
        }
    }
    if (confirmRemove) {
        ConfirmDialog(
            title = stringResource(R.string.slot_remove_title, slot.name),
            text = stringResource(R.string.slot_remove_text),
            confirm = stringResource(R.string.action_remove),
            onConfirm = {
                confirmRemove = false
                viewModel.removeSlot(slotIndex)
                onBack()
            },
            onDismiss = { confirmRemove = false },
        )
    }
}

@Composable
private fun ConnectionCard(live: LiveSlot?, onConnect: () -> Unit, onDisconnect: () -> Unit) {
    val connected = live?.state is SlotState.Connected
    val failed = live?.state is SlotState.Failed
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, top = 8.dp)
            .background(ApColors.Surface, MaterialTheme.shapes.medium)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = stringResource(
                    when (live?.state) {
                        is SlotState.Connected -> R.string.slot_connected

                        is SlotState.Connecting,
                        is SlotState.Reconnecting,
                        -> R.string.status_connecting

                        is SlotState.Failed -> R.string.status_failed

                        else -> R.string.status_disconnected
                    },
                ),
                style = MaterialTheme.typography.titleSmall,
                color = if (failed) ApColors.Error else ApColors.TextPrimary,
            )
            Text(
                text = stringResource(R.string.slot_connected_hint),
                style = MaterialTheme.typography.bodySmall,
                color = ApColors.TextSecondary,
            )
        }
        if (connected) {
            SecondaryButton(
                text = stringResource(R.string.action_disconnect),
                onClick = onDisconnect,
            )
        } else {
            SecondaryButton(text = stringResource(R.string.action_connect), onClick = onConnect)
        }
    }
}

@Composable
private fun AccessSection(slot: SavedSlot, onEdit: ((SavedSlot) -> SavedSlot) -> Unit) {
    Column(
        modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 24.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        SectionTitle(stringResource(R.string.slot_access))
        LabeledTextField(
            value = slot.name,
            onValueChange = { name -> onEdit { it.copy(name = name) } },
            label = stringResource(R.string.slot_name),
            monospace = true,
        )
        SwitchRow(
            label = stringResource(R.string.slot_highlight),
            description = stringResource(R.string.slot_highlight_hint),
            checked = slot.highlight,
            onCheckedChange = { on -> onEdit { it.copy(highlight = on) } },
            inset = false,
        )
    }
}

@Composable
private fun NotificationSection(
    notifications: SlotNotifications,
    onChange: (SlotNotifications) -> Unit,
) {
    Column(
        modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 24.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        SectionTitle(stringResource(R.string.slot_notifications))
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(ApColors.Surface, MaterialTheme.shapes.medium)
                .padding(vertical = 4.dp),
        ) {
            val modes = listOf(
                NotifyMode.OFF to stringResource(R.string.notify_off),
                NotifyMode.SILENT to stringResource(R.string.notify_silent),
                NotifyMode.SOUND to stringResource(R.string.notify_sound),
            )
            Row(
                modifier = Modifier.padding(start = 16.dp, end = 8.dp, top = 8.dp, bottom = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(R.string.notify_item_type),
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.labelSmall,
                    color = ApColors.TextTertiary,
                )
                modes.forEach { (_, label) ->
                    Box(modifier = Modifier.width(56.dp), contentAlignment = Alignment.Center) {
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelSmall,
                            color = ApColors.TextTertiary,
                        )
                    }
                }
            }
            ItemClass.entries.forEach { itemClass ->
                HorizontalDivider(color = ApColors.Divider)
                val className = itemClassLabel(itemClass)
                Row(
                    modifier = Modifier
                        .padding(start = 16.dp, end = 8.dp)
                        .selectableGroup(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        ItemClassMark(itemClass)
                        Text(
                            text = className,
                            style = MaterialTheme.typography.labelLarge,
                            color = ApColors.TextPrimary,
                        )
                    }
                    modes.forEach { (mode, label) ->
                        val selected = notifications.mode(itemClass) == mode
                        Box(
                            modifier = Modifier
                                .width(56.dp)
                                .height(48.dp)
                                .semantics { contentDescription = "$className: $label" }
                                .selectable(
                                    selected = selected,
                                    role = Role.RadioButton,
                                    onClick = { onChange(notifications.withMode(itemClass, mode)) },
                                ),
                            contentAlignment = Alignment.Center,
                        ) {
                            RadioDot(selected = selected)
                        }
                    }
                }
            }
        }
        Column {
            SwitchRow(
                label = stringResource(R.string.notify_own_world),
                description = stringResource(R.string.notify_own_world_hint),
                checked = notifications.ownWorld,
                onCheckedChange = { onChange(notifications.copy(ownWorld = it)) },
                inset = false,
            )
            SwitchRow(
                label = stringResource(R.string.notify_hints),
                description = stringResource(R.string.notify_hints_hint),
                checked = notifications.hints,
                onCheckedChange = { onChange(notifications.copy(hints = it)) },
                inset = false,
            )
            SwitchRow(
                label = stringResource(R.string.notify_mentions),
                description = stringResource(R.string.notify_mentions_hint),
                checked = notifications.chatMentions,
                onCheckedChange = { onChange(notifications.copy(chatMentions = it)) },
                inset = false,
            )
        }
    }
}

@Composable
private fun SyncSection(live: LiveSlot?, onCountFromNow: () -> Unit) {
    val index = live?.lastReportedIndex
    Column(
        modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 24.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        SectionTitle(stringResource(R.string.slot_sync))
        val text = if (index == null || index < 0) {
            val message = stringResource(
                if (index == null) R.string.slot_sync_unknown else R.string.slot_sync_none,
            )
            buildAnnotatedString { append(message) }
        } else {
            val prefix = stringResource(R.string.slot_sync_last_prefix)
            val suffix = stringResource(R.string.slot_sync_last_suffix)
            buildAnnotatedString {
                append(prefix)
                withStyle(SpanStyle(fontFamily = GeistMono)) {
                    // Indices start at 0, the user counts items from 1.
                    append("#${index + 1}")
                }
                append(suffix)
            }
        }
        Text(text = text, style = MaterialTheme.typography.bodyMedium, color = ApColors.TextPrimary)
        SecondaryButton(
            text = stringResource(R.string.slot_count_from_now),
            onClick = onCountFromNow,
            enabled = live?.state is SlotState.Connected,
        )
    }
}
