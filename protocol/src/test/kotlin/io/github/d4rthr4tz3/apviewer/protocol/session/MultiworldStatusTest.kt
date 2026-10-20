package io.github.d4rthr4tz3.apviewer.protocol.session

import io.github.d4rthr4tz3.apviewer.protocol.monitor.FailureReason
import kotlin.test.Test
import kotlin.test.assertEquals

class MultiworldStatusTest {
    private val names = listOf("Link", "Zelda")
    private val info = SlotInfo("wss://x", "seed", "0.6.1", 0, 1, "Link", null)
    private val reconnecting = SlotState.Reconnecting(
        attempt = 2,
        delayMillis = 2_000,
        reason = FailureReason.ConnectionLost(null),
        lastUrl = "wss://x",
    )

    private fun of(
        vararg states: SlotState,
        previous: MultiworldStatus = MultiworldStatus.Disconnected,
    ) = MultiworldStatus.of(names, states.toList(), previous, nowMillis = 5_000)

    @Test
    fun `all idle is disconnected`() {
        assertEquals(MultiworldStatus.Disconnected, of(SlotState.Idle, SlotState.Idle))
    }

    @Test
    fun `all connected is connected`() {
        assertEquals(
            MultiworldStatus.Connected(2),
            of(SlotState.Connected(info), SlotState.Connected(info)),
        )
    }

    @Test
    fun `a slot still connecting makes the multiworld connecting`() {
        assertEquals(
            MultiworldStatus.Connecting,
            of(SlotState.Connected(info), SlotState.Connecting("wss://x")),
        )
    }

    @Test
    fun `a failure outranks everything and names the slot`() {
        val refused = FailureReason.Refused(listOf("InvalidSlot"))

        assertEquals(
            MultiworldStatus.Failed("Zelda", refused),
            of(reconnecting, SlotState.Failed(refused)),
        )
    }

    @Test
    fun `a reconnect keeps the time it started`() {
        val first = of(SlotState.Connected(info), reconnecting)
        val later = MultiworldStatus.of(
            names,
            listOf(SlotState.Connected(info), reconnecting.copy(attempt = 3)),
            previous = first,
            nowMillis = 9_000,
        )

        assertEquals(MultiworldStatus.Reconnecting(attempt = 2, sinceMillis = 5_000), first)
        assertEquals(MultiworldStatus.Reconnecting(attempt = 3, sinceMillis = 5_000), later)
    }
}
