package io.github.d4rthr4tz3.apviewer.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import io.github.d4rthr4tz3.apviewer.R
import io.github.d4rthr4tz3.apviewer.protocol.monitor.FailureReason
import io.github.d4rthr4tz3.apviewer.protocol.session.MultiworldStatus
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private val ClockFormat = DateTimeFormatter.ofPattern("HH:mm").withZone(ZoneId.systemDefault())
private val ClockSecondsFormat =
    DateTimeFormatter.ofPattern("HH:mm:ss").withZone(ZoneId.systemDefault())

/** Formats [millis] as `HH:mm`. */
fun formatClock(millis: Long): String = ClockFormat.format(Instant.ofEpochMilli(millis))

/** Formats [millis] as `HH:mm:ss`. */
fun formatClockSeconds(millis: Long): String =
    ClockSecondsFormat.format(Instant.ofEpochMilli(millis))

/** Short label of [status], such as "Verbunden". */
@Composable
fun statusLabel(status: MultiworldStatus): String = when (status) {
    MultiworldStatus.Disconnected -> stringResource(R.string.status_disconnected)
    MultiworldStatus.Connecting -> stringResource(R.string.status_connecting)
    is MultiworldStatus.Connected -> stringResource(R.string.status_connected)
    is MultiworldStatus.Reconnecting -> stringResource(R.string.status_reconnecting)
    is MultiworldStatus.Failed -> stringResource(R.string.status_failed)
}

/** Detail of [status] worth a line of its own, or `null`. */
@Composable
fun statusDetail(status: MultiworldStatus): String? = when (status) {
    is MultiworldStatus.Reconnecting -> stringResource(
        R.string.status_reconnecting_detail,
        formatClock(status.sinceMillis),
        status.attempt,
    )

    is MultiworldStatus.Failed -> stringResource(
        R.string.status_failed_detail,
        status.slotName,
        failureText(status.reason),
    )

    else -> null
}

/** Subtitle of a multiworld: its state and number of slots. */
@Composable
fun statusWithSlots(status: MultiworldStatus, slots: Int): String = stringResource(
    R.string.status_with_slots,
    statusLabel(status),
    pluralStringResource(R.plurals.slot_count, slots, slots),
)

/** Explains why a connection failed. */
@Composable
fun failureText(reason: FailureReason): String = when (reason) {
    is FailureReason.Unreachable -> reason.message?.let {
        stringResource(R.string.error_unreachable_detail, it)
    } ?: stringResource(R.string.error_unreachable)

    is FailureReason.ConnectionLost -> reason.message?.let {
        stringResource(R.string.error_connection_lost_detail, it)
    } ?: stringResource(R.string.error_connection_lost)

    is FailureReason.Refused -> stringResource(
        R.string.error_refused,
        reason.errors.map { refusalText(it) }.joinToString(),
    )
}

@Composable
private fun refusalText(error: String): String = when (error) {
    "InvalidSlot" -> stringResource(R.string.error_invalid_slot)
    "InvalidPassword" -> stringResource(R.string.error_invalid_password)
    "IncompatibleVersion" -> stringResource(R.string.error_incompatible_version)
    else -> error
}
