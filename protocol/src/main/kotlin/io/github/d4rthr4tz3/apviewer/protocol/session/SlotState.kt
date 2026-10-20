package io.github.d4rthr4tz3.apviewer.protocol.session

import io.github.d4rthr4tz3.apviewer.protocol.model.NetworkItem
import io.github.d4rthr4tz3.apviewer.protocol.monitor.FailureReason

/** Server address and credentials of one slot. */
data class SlotConnectionConfig(
    val address: String,
    val slotName: String,
    val password: String = "",
    /** Whether this connection receives the room's text; all others connect with `NoText`. */
    val receivesText: Boolean = true,
    /** Names of all of the user's slots in this room, used to highlight the feed. */
    val ownSlotNames: Set<String> = setOf(slotName),
)

/** A slot of the room, as reported by the server. */
data class RoomPlayer(val slot: Int, val name: String, val game: String?)

/** Information about a connected slot. [players] are all slots of the slot's team. */
data class SlotInfo(
    val url: String,
    val seedName: String,
    val serverVersion: String,
    val team: Int,
    val slot: Int,
    val playerName: String,
    val game: String?,
    val players: List<RoomPlayer> = emptyList(),
)

/** Connection state of a [SlotConnection]. */
sealed interface SlotState {
    /** Not started, or stopped. */
    data object Idle : SlotState

    data class Connecting(val url: String) : SlotState

    data class Connected(val info: SlotInfo) : SlotState

    /** The connection broke after it was established; the next attempt starts after a delay. */
    data class Reconnecting(
        val attempt: Int,
        val delayMillis: Long,
        val reason: FailureReason,
        val lastUrl: String,
    ) : SlotState

    /** The slot was refused, or the server could not be reached on the first attempt. */
    data class Failed(val reason: FailureReason) : SlotState
}

/**
 * An item the slot received. [index] is its position in the slot's item list. [isNew] is `true` if
 * the item was not reported before, which is when a notification is due.
 */
data class ReceivedItem(
    val index: Int,
    val item: NetworkItem,
    val itemName: String,
    val senderName: String,
    val locationName: String,
    val isNew: Boolean,
)
