package io.github.d4rthr4tz3.apviewer.protocol.monitor

import io.github.d4rthr4tz3.apviewer.protocol.client.PacketSocket
import io.github.d4rthr4tz3.apviewer.protocol.client.PacketSocketFactory
import io.github.d4rthr4tz3.apviewer.protocol.model.JsonMessagePart
import io.github.d4rthr4tz3.apviewer.protocol.model.NetworkPlayer
import io.github.d4rthr4tz3.apviewer.protocol.model.NetworkVersion
import io.github.d4rthr4tz3.apviewer.protocol.packet.ClientPacket
import io.github.d4rthr4tz3.apviewer.protocol.packet.ConnectionTags
import io.github.d4rthr4tz3.apviewer.protocol.packet.ServerPacket
import java.io.IOException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest

@OptIn(ExperimentalCoroutinesApi::class)
class ArchipelagoMonitorTest {
    private class FakeSocket : PacketSocket {
        val server = Channel<ServerPacket>(Channel.UNLIMITED)
        val sent = mutableListOf<ClientPacket>()
        var closed = false

        override val incoming: Flow<ServerPacket> = server.receiveAsFlow()

        override fun send(packets: List<ClientPacket>): Boolean {
            sent += packets
            return true
        }

        override fun close() {
            closed = true
            server.close()
        }
    }

    private val roomInfo = ServerPacket.RoomInfo(
        version = NetworkVersion(major = 0, minor = 6, build = 1),
        games = listOf("Archipelago"),
        seedName = "seed",
    )

    private fun TestScope.monitor(factory: PacketSocketFactory) =
        ArchipelagoMonitor(scope = backgroundScope, socketFactory = factory, uuid = "uuid")

    @Test
    fun `connects as tracker and collects messages`() = runTest {
        val socket = FakeSocket()
        val monitor = monitor { socket }

        monitor.connect(ConnectionConfig(address = "localhost", slotName = "Link"))
        socket.server.send(roomInfo)
        runCurrent()

        val connect = socket.sent.filterIsInstance<ClientPacket.Connect>().single()
        assertEquals("Link", connect.name)
        assertEquals(listOf(ConnectionTags.TRACKER), connect.tags)
        assertEquals(
            listOf("Archipelago"),
            socket.sent.filterIsInstance<ClientPacket.GetDataPackage>().single().games,
        )

        socket.server.send(
            ServerPacket.Connected(
                team = 0,
                slot = 1,
                players = listOf(NetworkPlayer(team = 0, slot = 1, alias = "Hero", name = "Link")),
            ),
        )
        socket.server.send(
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
        assertEquals("Hero", state.playerName)
        assertEquals("0.6.1", state.serverVersion)
        assertEquals(listOf("Hero joined"), monitor.feed.value.map { it.message.plainText })
    }

    @Test
    fun `falls back to ws when wss cannot be opened`() = runTest {
        val socket = FakeSocket()
        val opened = mutableListOf<String>()
        val monitor = monitor { url ->
            opened += url
            if (url.startsWith("wss://")) throw IOException("handshake failed") else socket
        }

        monitor.connect(ConnectionConfig(address = "example.org:1234", slotName = "Link"))
        runCurrent()

        assertEquals(listOf("wss://example.org:1234", "ws://example.org:1234"), opened)
        assertEquals(MonitorState.Connecting("ws://example.org:1234"), monitor.state.value)
    }

    @Test
    fun `reports unreachable server`() = runTest {
        val monitor = monitor { throw IOException("refused") }

        monitor.connect(ConnectionConfig(address = "example.org", slotName = "Link"))
        runCurrent()

        val state = assertIs<MonitorState.Failed>(monitor.state.value)
        assertEquals(FailureReason.Unreachable("refused"), state.reason)
    }

    @Test
    fun `reports refused connection and closes the socket`() = runTest {
        val socket = FakeSocket()
        val monitor = monitor { socket }

        monitor.connect(ConnectionConfig(address = "localhost", slotName = "Nobody"))
        socket.server.send(roomInfo)
        socket.server.send(ServerPacket.ConnectionRefused(errors = listOf("InvalidSlot")))
        runCurrent()

        val state = assertIs<MonitorState.Failed>(monitor.state.value)
        assertEquals(FailureReason.Refused(listOf("InvalidSlot")), state.reason)
        assertEquals(true, socket.closed)
    }

    @Test
    fun `reports lost connection when the server closes it`() = runTest {
        val socket = FakeSocket()
        val monitor = monitor { socket }

        monitor.connect(ConnectionConfig(address = "localhost", slotName = "Link"))
        socket.server.send(roomInfo)
        socket.server.close()
        runCurrent()

        val state = assertIs<MonitorState.Failed>(monitor.state.value)
        assertEquals(FailureReason.ConnectionLost(null), state.reason)
    }

    @Test
    fun `builds candidate urls from user input`() {
        assertEquals(
            listOf("wss://archipelago.gg:38281", "ws://archipelago.gg:38281"),
            ArchipelagoMonitor.candidateUrls(" archipelago.gg "),
        )
        assertEquals(
            listOf("wss://archipelago.gg:52000", "ws://archipelago.gg:52000"),
            ArchipelagoMonitor.candidateUrls("archipelago.gg:52000"),
        )
        assertEquals(
            listOf("ws://localhost:1"),
            ArchipelagoMonitor.candidateUrls("ws://localhost:1"),
        )
        assertEquals(
            listOf("wss://[::1]:38281", "ws://[::1]:38281"),
            ArchipelagoMonitor.candidateUrls("[::1]"),
        )
    }
}
