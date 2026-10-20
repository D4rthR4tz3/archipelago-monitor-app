package io.github.d4rthr4tz3.apviewer.protocol.monitor

import io.github.d4rthr4tz3.apviewer.protocol.model.JsonMessagePart
import io.github.d4rthr4tz3.apviewer.protocol.packet.ClientPacket
import io.github.d4rthr4tz3.apviewer.protocol.packet.ConnectionTags
import io.github.d4rthr4tz3.apviewer.protocol.packet.ServerPacket
import io.github.d4rthr4tz3.apviewer.protocol.testing.FakeSocketFactory
import io.github.d4rthr4tz3.apviewer.protocol.testing.TestRoom
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest

@OptIn(ExperimentalCoroutinesApi::class)
class ArchipelagoMonitorTest {
    private fun TestScope.monitor(factory: FakeSocketFactory) =
        ArchipelagoMonitor(scope = backgroundScope, socketFactory = factory, uuid = "uuid")

    @Test
    fun `connects as tracker and collects messages`() = runTest {
        val factory = FakeSocketFactory()
        val monitor = monitor(factory)

        monitor.connect(ConnectionConfig(address = "localhost", slotName = "Link"))
        runCurrent()
        val socket = factory.last
        socket.receive(TestRoom.roomInfo)
        runCurrent()

        val connect = socket.sentOf<ClientPacket.Connect>().single()
        assertEquals("Link", connect.name)
        assertEquals(listOf(ConnectionTags.TRACKER), connect.tags)

        socket.receive(
            TestRoom.connected(slot = 1),
            ServerPacket.PrintJson(
                data = listOf(
                    JsonMessagePart(type = "player_id", text = "1"),
                    JsonMessagePart(text = " joined"),
                ),
            ),
        )
        runCurrent()

        val state = assertIs<MonitorState.Connected>(monitor.state.value)
        assertEquals("wss://localhost:38281", state.url)
        assertEquals("Link", state.playerName)
        assertEquals("0.6.1", state.serverVersion)
        assertEquals(listOf("Link joined"), monitor.feed.value.map { it.message.plainText })
    }

    @Test
    fun `reports connecting synchronously when connect is called`() = runTest {
        val monitor = monitor(FakeSocketFactory())

        monitor.connect(ConnectionConfig(address = "example.org", slotName = "Link"))

        assertEquals(MonitorState.Connecting("wss://example.org:38281"), monitor.state.value)
    }

    @Test
    fun `reports unreachable server`() = runTest {
        val monitor = monitor(FakeSocketFactory(reachable = { false }))

        monitor.connect(ConnectionConfig(address = "example.org", slotName = "Link"))
        runCurrent()

        val state = assertIs<MonitorState.Failed>(monitor.state.value)
        assertIs<FailureReason.Unreachable>(state.reason)
    }

    @Test
    fun `reports refused connection`() = runTest {
        val factory = FakeSocketFactory()
        val monitor = monitor(factory)

        monitor.connect(ConnectionConfig(address = "localhost", slotName = "Nobody"))
        runCurrent()
        factory.last.receive(TestRoom.roomInfo)
        factory.last.receive(ServerPacket.ConnectionRefused(errors = listOf("InvalidSlot")))
        runCurrent()

        val state = assertIs<MonitorState.Failed>(monitor.state.value)
        assertEquals(FailureReason.Refused(listOf("InvalidSlot")), state.reason)
    }

    @Test
    fun `shows connecting while reconnecting after a lost connection`() = runTest {
        val factory = FakeSocketFactory()
        val monitor = monitor(factory)

        monitor.connect(ConnectionConfig(address = "localhost", slotName = "Link"))
        runCurrent()
        factory.last.receive(TestRoom.roomInfo, TestRoom.connected(slot = 1))
        runCurrent()
        factory.last.drop()
        runCurrent()

        assertEquals(MonitorState.Connecting("wss://localhost:38281"), monitor.state.value)
    }

    @Test
    fun `disconnect stops the connection`() = runTest {
        val factory = FakeSocketFactory()
        val monitor = monitor(factory)

        monitor.connect(ConnectionConfig(address = "localhost", slotName = "Link"))
        runCurrent()
        monitor.disconnect()
        runCurrent()

        assertEquals(MonitorState.Disconnected, monitor.state.value)
        assertEquals(true, factory.last.closed)
    }
}
