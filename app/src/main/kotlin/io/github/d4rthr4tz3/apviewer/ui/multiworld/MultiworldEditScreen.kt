package io.github.d4rthr4tz3.apviewer.ui.multiworld

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.d4rthr4tz3.apviewer.R
import io.github.d4rthr4tz3.apviewer.protocol.session.MultiworldStatus
import io.github.d4rthr4tz3.apviewer.protocol.session.ProbeResult
import io.github.d4rthr4tz3.apviewer.protocol.session.RoomPlayer
import io.github.d4rthr4tz3.apviewer.protocol.store.SavedMultiworld
import io.github.d4rthr4tz3.apviewer.ui.components.ApIconButton
import io.github.d4rthr4tz3.apviewer.ui.components.ApTextField
import io.github.d4rthr4tz3.apviewer.ui.components.LabeledTextField
import io.github.d4rthr4tz3.apviewer.ui.components.PlainButton
import io.github.d4rthr4tz3.apviewer.ui.components.PrimaryButton
import io.github.d4rthr4tz3.apviewer.ui.components.ScreenHeader
import io.github.d4rthr4tz3.apviewer.ui.components.SecondaryButton
import io.github.d4rthr4tz3.apviewer.ui.components.SectionTitle
import io.github.d4rthr4tz3.apviewer.ui.components.SwitchRow
import io.github.d4rthr4tz3.apviewer.ui.statusLabel
import io.github.d4rthr4tz3.apviewer.ui.theme.ApColors

/** Settings of a multiworld: connection, the user's slots, other slots and behavior. */
@Composable
fun MultiworldEditScreen(
    viewModel: MultiworldEditViewModel,
    onClose: () -> Unit,
    onSaved: () -> Unit,
    onRemoved: () -> Unit,
    onOpenSlot: (Int) -> Unit,
) {
    val draft by viewModel.draft.collectAsStateWithLifecycle()
    val probe by viewModel.probe.collectAsStateWithLifecycle()
    val status by viewModel.status.collectAsStateWithLifecycle()
    val players by viewModel.roomPlayers.collectAsStateWithLifecycle()
    val current = draft ?: return
    var confirmRemove by rememberSaveable { mutableStateOf(false) }
    var addSlotDialog by rememberSaveable { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize().imePadding()) {
        ScreenHeader(
            title = stringResource(
                if (viewModel.isNew) R.string.edit_title_new else R.string.edit_title,
            ),
            navigation = {
                ApIconButton(
                    icon = R.drawable.ic_close,
                    contentDescription = stringResource(R.string.action_close),
                    onClick = onClose,
                )
            },
            actions = {
                PrimaryButton(
                    text = stringResource(R.string.action_save),
                    onClick = { if (viewModel.save()) onSaved() },
                    enabled = current.canSave,
                    modifier = Modifier.padding(end = 4.dp),
                )
            },
        )
        Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
            ConnectionSection(
                draft = current,
                probe = probe,
                onEdit = viewModel::edit,
                onProbe = viewModel::runProbe,
            )
            HorizontalDivider(color = ApColors.Divider)
            OwnSlotsSection(
                draft = current,
                status = status,
                players = players,
                onOpenSlot = onOpenSlot,
                onAddManually = { addSlotDialog = true },
            )
            val ownNames = current.slots.map { it.name }.toSet()
            val others = players.filter { it.name !in ownNames }
            if (others.isNotEmpty()) {
                HorizontalDivider(color = ApColors.Divider)
                OtherSlotsSection(players = others, onAdd = viewModel::addSlot)
            }
            HorizontalDivider(color = ApColors.Divider)
            BehaviorSection(draft = current, onEdit = viewModel::edit)
            if (!viewModel.isNew) {
                PlainButton(
                    text = stringResource(R.string.edit_remove),
                    onClick = { confirmRemove = true },
                    color = ApColors.Error,
                    modifier = Modifier.padding(
                        start = 16.dp,
                        end = 16.dp,
                        top = 16.dp,
                        bottom = 40.dp,
                    ),
                )
            }
        }
    }

    if (confirmRemove) {
        ConfirmDialog(
            title = stringResource(
                R.string.edit_remove_title,
                current.name.ifBlank {
                    current.host
                },
            ),
            text = stringResource(R.string.edit_remove_text),
            confirm = stringResource(R.string.action_remove),
            onConfirm = {
                confirmRemove = false
                viewModel.remove()
                onRemoved()
            },
            onDismiss = { confirmRemove = false },
        )
    }
    if (addSlotDialog) {
        AddSlotDialog(
            onAdd = {
                viewModel.addSlot(it)
                addSlotDialog = false
            },
            onDismiss = { addSlotDialog = false },
        )
    }
}

@Composable
private fun ConnectionSection(
    draft: MultiworldDraft,
    probe: ProbeState,
    onEdit: ((MultiworldDraft) -> MultiworldDraft) -> Unit,
    onProbe: () -> Unit,
) {
    var showPassword by rememberSaveable { mutableStateOf(false) }
    Column(
        modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        SectionTitle(stringResource(R.string.edit_connection))
        LabeledTextField(
            value = draft.name,
            onValueChange = { name -> onEdit { it.copy(name = name) } },
            label = stringResource(R.string.edit_name),
            placeholder = draft.host.ifBlank { null },
        )
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            LabeledTextField(
                value = draft.host,
                onValueChange = { host -> onEdit { it.copy(host = host) } },
                label = stringResource(R.string.edit_host),
                placeholder = stringResource(R.string.edit_host_hint),
                monospace = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                modifier = Modifier.weight(1f),
            )
            LabeledTextField(
                value = draft.port,
                onValueChange = { port -> onEdit { it.copy(port = port.filter(Char::isDigit)) } },
                label = stringResource(R.string.edit_port),
                monospace = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.width(100.dp),
            )
        }
        LabeledTextField(
            value = draft.roomPassword,
            onValueChange = { password -> onEdit { it.copy(roomPassword = password) } },
            label = stringResource(R.string.edit_room_password),
            placeholder = stringResource(R.string.password_none),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            visualTransformation = if (showPassword) {
                VisualTransformation.None
            } else {
                PasswordVisualTransformation()
            },
            trailingIcon = {
                ApIconButton(
                    icon = if (showPassword) {
                        R.drawable.ic_visibility_off
                    } else {
                        R.drawable.ic_visibility
                    },
                    contentDescription = stringResource(
                        if (showPassword) R.string.password_hide else R.string.password_show,
                    ),
                    onClick = { showPassword = !showPassword },
                    tint = ApColors.TextSecondary,
                )
            },
        )
        ProbeCard(probe = probe, enabled = draft.canSave, onProbe = onProbe)
    }
}

@Composable
private fun ProbeCard(probe: ProbeState, enabled: Boolean, onProbe: () -> Unit) {
    val result = (probe as? ProbeState.Done)?.result
    val title = stringResource(
        when {
            probe == ProbeState.Checking -> R.string.probe_checking
            result is ProbeResult.Reachable -> R.string.probe_reachable
            result is ProbeResult.Unreachable -> R.string.probe_unreachable
            else -> R.string.probe_idle
        },
    )
    val detail = when (result) {
        is ProbeResult.Reachable -> stringResource(
            if (result.secure) R.string.probe_secure else R.string.probe_insecure,
            result.serverVersion,
        )

        is ProbeResult.Unreachable -> result.message

        null -> null
    }
    val failed = result is ProbeResult.Unreachable
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(ApColors.Surface, MaterialTheme.shapes.medium)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                color = if (failed) ApColors.Error else ApColors.TextPrimary,
            )
            if (detail != null) {
                Text(
                    text = detail,
                    style = MaterialTheme.typography.bodySmall,
                    color = ApColors.TextSecondary,
                )
            }
        }
        SecondaryButton(
            text = stringResource(
                if (probe == ProbeState.Idle) R.string.probe_test else R.string.probe_retest,
            ),
            onClick = onProbe,
            enabled = enabled && probe != ProbeState.Checking,
        )
    }
}

@Composable
private fun OwnSlotsSection(
    draft: MultiworldDraft,
    status: MultiworldStatus,
    players: List<RoomPlayer>,
    onOpenSlot: (Int) -> Unit,
    onAddManually: () -> Unit,
) {
    Column(modifier = Modifier.padding(top = 8.dp, bottom = 24.dp)) {
        SectionTitle(
            text = stringResource(R.string.edit_own_slots),
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 8.dp),
        )
        draft.slots.forEachIndexed { index, slot ->
            if (index > 0) HorizontalDivider(color = ApColors.Divider)
            val game = players.firstOrNull { it.name == slot.name }?.game
            val state = if (status is MultiworldStatus.Connected) statusLabel(status) else null
            ListRow(
                title = slot.name,
                subtitle = listOfNotNull(game, state).joinToString().ifEmpty { null },
                onClick = { onOpenSlot(index) },
            )
        }
        if (draft.slots.isNotEmpty()) HorizontalDivider(color = ApColors.Divider)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .clickable(role = Role.Button, onClick = onAddManually)
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_add),
                contentDescription = null,
                tint = ApColors.TextPrimary,
            )
            Text(
                text = stringResource(R.string.edit_add_slot),
                style = MaterialTheme.typography.labelLarge,
                color = ApColors.TextPrimary,
            )
        }
    }
}

@Composable
private fun ListRow(title: String, subtitle: String?, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(start = 16.dp, end = 8.dp, top = 12.dp, bottom = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelLarge,
                color = ApColors.TextPrimary,
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = ApColors.TextSecondary,
                )
            }
        }
        Icon(
            painter = painterResource(R.drawable.ic_chevron_right),
            contentDescription = null,
            tint = ApColors.TextTertiary,
        )
    }
}

@Composable
private fun OtherSlotsSection(players: List<RoomPlayer>, onAdd: (String) -> Unit) {
    var query by rememberSaveable { mutableStateOf("") }
    var showAll by rememberSaveable { mutableStateOf(false) }
    val matches = players.filter {
        query.isBlank() ||
            it.name.contains(query, ignoreCase = true) ||
            it.game.orEmpty().contains(query, ignoreCase = true)
    }
    val shown = if (showAll || query.isNotBlank()) matches else matches.take(COLLAPSED_SLOTS)
    Column(
        modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            SectionTitle(stringResource(R.string.edit_other_slots))
            Text(
                text = stringResource(R.string.edit_other_slots_hint),
                style = MaterialTheme.typography.bodySmall,
                color = ApColors.TextTertiary,
            )
        }
        ApTextField(
            value = query,
            onValueChange = { query = it },
            modifier = Modifier.fillMaxWidth(),
            placeholder = stringResource(R.string.edit_search_slots),
            height = 44.dp,
        )
        Column {
            shown.forEachIndexed { index, player ->
                if (index > 0) HorizontalDivider(color = ApColors.Divider)
                Row(
                    modifier = Modifier.padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(2.dp),
                    ) {
                        Text(
                            text = player.name,
                            style = MaterialTheme.typography.bodyMedium,
                            color = ApColors.TextPrimary,
                        )
                        player.game?.let {
                            Text(
                                text = it,
                                style = MaterialTheme.typography.bodySmall,
                                color = ApColors.TextTertiary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                    ApIconButton(
                        icon = R.drawable.ic_add,
                        contentDescription = stringResource(
                            R.string.edit_add_named_slot,
                            player.name,
                        ),
                        onClick = { onAdd(player.name) },
                    )
                }
            }
        }
        if (!showAll && query.isBlank() && matches.size > COLLAPSED_SLOTS) {
            PlainButton(
                text = pluralStringResource(R.plurals.edit_show_all, matches.size, matches.size),
                onClick = { showAll = true },
            )
        }
    }
}

@Composable
private fun BehaviorSection(
    draft: MultiworldDraft,
    onEdit: ((MultiworldDraft) -> MultiworldDraft) -> Unit,
) {
    Column(modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)) {
        SectionTitle(
            text = stringResource(R.string.edit_behavior),
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 4.dp),
        )
        SwitchRow(
            label = stringResource(R.string.edit_connect_on_start),
            checked = draft.connectOnStart,
            onCheckedChange = { on -> onEdit { it.copy(connectOnStart = on) } },
        )
        SwitchRow(
            label = stringResource(R.string.edit_keep_in_background),
            description = stringResource(R.string.edit_keep_in_background_hint),
            checked = draft.keepInBackground,
            onCheckedChange = { on -> onEdit { it.copy(keepInBackground = on) } },
        )
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(R.string.edit_retention),
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.labelLarge,
                color = ApColors.TextPrimary,
            )
            RetentionPicker(
                days = draft.feedRetentionDays,
                onSelect = { days -> onEdit { it.copy(feedRetentionDays = days) } },
            )
        }
    }
}

@Composable
private fun RetentionPicker(days: Int, onSelect: (Int) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        Row(
            modifier = Modifier
                .width(132.dp)
                .height(40.dp)
                .background(ApColors.Surface, MaterialTheme.shapes.small)
                .border(1.dp, ApColors.InputBorder, MaterialTheme.shapes.small)
                .clickable(role = Role.DropdownList) { expanded = true }
                .padding(start = 10.dp, end = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = retentionLabel(days),
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.bodyMedium,
                color = ApColors.TextPrimary,
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
            RETENTION_OPTIONS.forEach { option ->
                DropdownMenuItem(
                    text = { Text(retentionLabel(option), color = ApColors.TextPrimary) },
                    onClick = {
                        onSelect(option)
                        expanded = false
                    },
                )
            }
        }
    }
}

@Composable
private fun retentionLabel(days: Int): String = if (days == SavedMultiworld.RETENTION_UNLIMITED) {
    stringResource(R.string.retention_unlimited)
} else {
    pluralStringResource(R.plurals.retention_days, days, days)
}

@Composable
private fun AddSlotDialog(onAdd: (String) -> Unit, onDismiss: () -> Unit) {
    var name by rememberSaveable { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = ApColors.Surface,
        title = { Text(stringResource(R.string.edit_add_slot)) },
        text = {
            LabeledTextField(
                value = name,
                onValueChange = { name = it },
                label = stringResource(R.string.slot_name),
                monospace = true,
            )
        },
        confirmButton = {
            PrimaryButton(
                text = stringResource(R.string.action_add),
                onClick = { onAdd(name) },
                enabled = name.isNotBlank(),
            )
        },
        dismissButton = {
            PlainButton(text = stringResource(R.string.action_cancel), onClick = onDismiss)
        },
    )
}

/** Asks before a step that cannot be undone. */
@Composable
fun ConfirmDialog(
    title: String,
    text: String,
    confirm: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = ApColors.Surface,
        title = { Text(title) },
        text = { Text(text, color = ApColors.TextSecondary) },
        confirmButton = {
            PlainButton(text = confirm, onClick = onConfirm, color = ApColors.Error)
        },
        dismissButton = {
            PlainButton(text = stringResource(R.string.action_cancel), onClick = onDismiss)
        },
    )
}

private const val COLLAPSED_SLOTS = 3

private val RETENTION_OPTIONS = listOf(1, 7, 30, SavedMultiworld.RETENTION_UNLIMITED)
