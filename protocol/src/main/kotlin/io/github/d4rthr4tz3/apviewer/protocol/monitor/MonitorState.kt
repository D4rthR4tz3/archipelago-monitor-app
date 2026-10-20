package io.github.d4rthr4tz3.apviewer.protocol.monitor

import io.github.d4rthr4tz3.apviewer.protocol.message.FeedMessage

/** Server address and slot credentials entered by the user. */
data class ConnectionConfig(val address: String, val slotName: String, val password: String = "")

/** Connection state of an [ArchipelagoMonitor]. */
sealed interface MonitorState {
    data object Disconnected : MonitorState

    data class Connecting(val url: String) : MonitorState

    data class Connected(
        val url: String,
        val seedName: String,
        val serverVersion: String,
        val team: Int,
        val slot: Int,
        val playerName: String,
    ) : MonitorState

    data class Failed(val reason: FailureReason) : MonitorState
}

/** Why an [ArchipelagoMonitor] is not connected. The UI turns these into localized text. */
sealed interface FailureReason {
    /** No candidate URL could be opened. [message] is the last error, if any. */
    data class Unreachable(val message: String?) : FailureReason

    /** The server rejected the slot, for example `InvalidSlot` or `InvalidPassword`. */
    data class Refused(val errors: List<String>) : FailureReason

    /** The connection broke or was closed by the server after it was established. */
    data class ConnectionLost(val message: String?) : FailureReason
}

/** A [FeedMessage] with an id that is unique within one connection. */
data class FeedEntry(val id: Long, val message: FeedMessage)
