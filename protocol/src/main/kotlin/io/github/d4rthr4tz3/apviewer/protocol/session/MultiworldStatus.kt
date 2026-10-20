package io.github.d4rthr4tz3.apviewer.protocol.session

import io.github.d4rthr4tz3.apviewer.protocol.monitor.FailureReason

/** Combined connection state of all slots of a multiworld. */
sealed interface MultiworldStatus {
    /** No slot is connected or trying to connect. */
    data object Disconnected : MultiworldStatus

    data object Connecting : MultiworldStatus

    /** All slots are connected. */
    data class Connected(val slots: Int) : MultiworldStatus

    /**
     * At least one slot lost its connection and tries again. [attempt] is the highest attempt
     * among the slots, [sinceMillis] when the first slot lost its connection.
     */
    data class Reconnecting(val attempt: Int, val sinceMillis: Long) : MultiworldStatus

    /** The device has no network; the slots connect again once it is back. */
    data object Offline : MultiworldStatus

    /** At least one slot was refused or the server could not be reached. */
    data class Failed(val slotName: String, val reason: FailureReason) : MultiworldStatus

    companion object {
        /**
         * Combines the [states] of the slots named [slotNames]. A failure outranks a missing
         * network, which outranks a reconnect, which outranks connecting. [previous] keeps the start time of an ongoing reconnect.
         */
        fun of(
            slotNames: List<String>,
            states: List<SlotState>,
            previous: MultiworldStatus,
            nowMillis: Long,
        ): MultiworldStatus {
            val failed = states.indexOfFirst { it is SlotState.Failed }
            if (failed >= 0) {
                return Failed(slotNames[failed], (states[failed] as SlotState.Failed).reason)
            }
            if (states.any { it is SlotState.Offline }) return Offline
            val reconnecting = states.filterIsInstance<SlotState.Reconnecting>()
            if (reconnecting.isNotEmpty()) {
                val since = (previous as? Reconnecting)?.sinceMillis ?: nowMillis
                return Reconnecting(reconnecting.maxOf { it.attempt }, since)
            }
            return when {
                states.isEmpty() || states.all { it is SlotState.Idle } -> Disconnected
                states.all { it is SlotState.Connected } -> Connected(states.size)
                else -> Connecting
            }
        }
    }
}
