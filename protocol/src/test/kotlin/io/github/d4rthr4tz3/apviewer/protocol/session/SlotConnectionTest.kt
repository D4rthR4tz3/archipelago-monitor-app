package io.github.d4rthr4tz3.apviewer.protocol.session

import io.github.d4rthr4tz3.apviewer.protocol.model.DataPackageObject
import io.github.d4rthr4tz3.apviewer.protocol.model.GameData
import io.github.d4rthr4tz3.apviewer.protocol.model.ItemFlags
import io.github.d4rthr4tz3.apviewer.protocol.model.NetworkSlot
import io.github.d4rthr4tz3.apviewer.protocol.monitor.FailureReason
import io.github.d4rthr4tz3.apviewer.protocol.packet.ClientPacket
import io.github.d4rthr4tz3.apviewer.protocol.packet.ConnectionTags
import io.github.d4rthr4tz3.apviewer.protocol.packet.ServerPacket
import io.github.d4rthr4tz3.apviewer.protocol.store.InMemoryDataPackageCache
import io.github.d4rthr4tz3.apviewer.protocol.store.InMemoryItemIndexStore
import io.github.d4rthr4tz3.apviewer.protocol.store.SlotKey
import io.github.d4rthr4tz3.apviewer.protocol.testing.FakeSocketFactory
import io.github.d4rthr4tz3.apviewer.protocol.testing.TestRoom
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest

@OptIn(ExperimentalCoroutinesApi::class)
class SlotConnectionTest {
    private val factory = FakeSocketFactory()
    private val indexStore = InMemoryItemIndexStore()
    private val cache = InMemoryDataPackageCache()
    private val key = SlotKey(server = "localhost:38281", seedName = "seed", team = 0, slot = 1)

    private fun TestScope.connection(
        receivesText: Boolean = true,
        socketFactory: FakeSocketFactory = factory,
    ) = SlotConnection(
        scope = backgroundScope,
        socketFactory = socketFactory,
        config = SlotConnectionConfig(
            address = "localhost",
            slotName = "Link",
            receivesText = receivesText,
        ),
        itemIndexStore = indexStore,
        dataPackageCache = cache,
        reconnectPolicy = ReconnectPolicy(initialDelayMillis = 1_000, maxDelayMillis = 8_000),
        uuid = "uuid",
    )

    private fun TestScope.collectItems(connection: SlotConnection): List<ReceivedItem> {
        val items = mutableListOf<ReceivedItem>()
        backgroundScope.launch { connection.receivedItems.toList(items) }
        runCurrent()
        return items
    }

    @Test
    fun `adds NoText tag to connections without text`() = runTest {
        connection(receivesText = false).start()
        runCurrent()
        factory.last.receive(TestRoom.roomInfo)
        runCurrent()

        val connect = factory.last.sentOf<ClientPacket.Connect>().single()
        assertEquals(listOf(ConnectionTags.TRACKER, ConnectionTags.NO_TEXT), connect.tags)
    }

    @Test
    fun `requests the data package only for games without a matching cache entry`() = runTest {
        cache.put("A Link to the Past", GameData(checksum = "abc"))
        connection().start()
        runCurrent()
        factory.last.receive(TestRoom.roomInfo.copy(games = listOf("A Link to the Past", "Other")))
        runCurrent()

        val request = factory.last.sentOf<ClientPacket.GetDataPackage>().single()
        assertEquals(listOf("Other"), request.games)
    }

    @Test
    fun `caches received data packages`() = runTest {
        connection().start()
        runCurrent()
        val data = GameData(itemNameToId = mapOf("Hookshot" to 10L), checksum = "abc")
        factory.last.receive(TestRoom.roomInfo)
        factory.last.receive(
            ServerPacket.DataPackage(DataPackageObject(mapOf("A Link to the Past" to data))),
        )
        runCurrent()

        assertEquals(data, cache.get("A Link to the Past", "abc"))
    }

    @Test
    fun `treats items received with the first connection as history`() = runTest {
        val connection = connection()
        val items = collectItems(connection)
        connection.start()
        runCurrent()
        factory.last.receive(TestRoom.roomInfo)
        factory.last.receive(
            TestRoom.connected(slot = 1),
            ServerPacket.ReceivedItems(
                index = 0,
                items = listOf(TestRoom.item(1, from = 2), TestRoom.item(2, from = 3)),
            ),
        )
        factory.last.receive(
            ServerPacket.ReceivedItems(index = 2, items = listOf(TestRoom.item(3, from = 2))),
        )
        runCurrent()

        assertEquals(listOf(false, false, true), items.map { it.isNew })
        assertEquals(2, indexStore.lastReportedIndex(key))
    }

    @Test
    fun `reports items that arrive after a first connection without items`() = runTest {
        val connection = connection()
        val items = collectItems(connection)
        connection.start()
        runCurrent()
        factory.last.receive(TestRoom.roomInfo)
        factory.last.receive(TestRoom.connected(slot = 1))
        factory.last.receive(
            ServerPacket.ReceivedItems(index = 0, items = listOf(TestRoom.item(1, from = 2))),
        )
        runCurrent()

        assertEquals(listOf(true), items.map { it.isNew })
    }

    @Test
    fun `reports only items above the stored index after a reconnect`() = runTest {
        indexStore.setLastReportedIndex(key, 0)
        val connection = connection()
        val items = collectItems(connection)
        connection.start()
        runCurrent()
        factory.last.receive(TestRoom.roomInfo)
        factory.last.receive(
            TestRoom.connected(slot = 1),
            ServerPacket.ReceivedItems(
                index = 0,
                items = listOf(
                    TestRoom.item(1, from = 2),
                    TestRoom.item(2, from = 3, flags = ItemFlags.PROGRESSION),
                ),
            ),
        )
        runCurrent()

        assertEquals(listOf(false, true), items.map { it.isNew })
        assertTrue(items[1].item.isProgression)
        assertEquals(1, indexStore.lastReportedIndex(key))
    }

    @Test
    fun `resolves names of received items`() = runTest {
        val connection = connection()
        val items = collectItems(connection)
        connection.start()
        runCurrent()
        factory.last.receive(TestRoom.roomInfo)
        factory.last.receive(
            ServerPacket.DataPackage(
                DataPackageObject(
                    mapOf(
                        "A Link to the Past" to GameData(itemNameToId = mapOf("Hookshot" to 1L)),
                        "Super Metroid" to GameData(locationNameToId = mapOf("Brinstar" to 101L)),
                    ),
                ),
            ),
        )
        factory.last.receive(
            TestRoom.connected(slot = 1).copy(
                slotInfo = mapOf(
                    1 to NetworkSlot(name = "Link", game = "A Link to the Past"),
                    2 to NetworkSlot(name = "Samus", game = "Super Metroid"),
                ),
            ),
        )
        factory.last.receive(
            ServerPacket.ReceivedItems(index = 0, items = listOf(TestRoom.item(1, from = 2))),
        )
        runCurrent()

        val item = items.single()
        assertEquals("Hookshot", item.itemName)
        assertEquals("Samus", item.senderName)
        assertEquals("Brinstar", item.locationName)
    }

    @Test
    fun `asks for a sync when items were missed`() = runTest {
        connection().start()
        runCurrent()
        factory.last.receive(TestRoom.roomInfo)
        factory.last.receive(TestRoom.connected(slot = 1))
        factory.last.receive(
            ServerPacket.ReceivedItems(index = 3, items = listOf(TestRoom.item(1, from = 2))),
        )
        runCurrent()

        assertEquals(listOf(ClientPacket.Sync), factory.last.sentOf<ClientPacket.Sync>())
    }

    @Test
    fun `reconnects with backoff after an established connection breaks`() = runTest {
        val connection = connection()
        connection.start()
        runCurrent()
        factory.last.receive(TestRoom.roomInfo, TestRoom.connected(slot = 1))
        runCurrent()
        factory.last.drop()
        runCurrent()

        val reconnecting = assertIs<SlotState.Reconnecting>(connection.state.value)
        assertEquals(1, reconnecting.attempt)
        assertEquals(1_000, reconnecting.delayMillis)
        assertEquals(1, factory.sockets.size)

        advanceTimeBy(1_001)
        runCurrent()
        assertEquals(2, factory.sockets.size)
        assertEquals("wss://localhost:38281", factory.last.url)

        factory.last.drop()
        runCurrent()
        assertEquals(2_000, assertIs<SlotState.Reconnecting>(connection.state.value).delayMillis)
    }

    @Test
    fun `fails without retry when the slot is refused`() = runTest {
        val connection = connection()
        connection.start()
        runCurrent()
        factory.last.receive(TestRoom.roomInfo)
        factory.last.receive(ServerPacket.ConnectionRefused(errors = listOf("InvalidSlot")))
        runCurrent()
        advanceTimeBy(60_000)

        val failed = assertIs<SlotState.Failed>(connection.state.value)
        assertEquals(FailureReason.Refused(listOf("InvalidSlot")), failed.reason)
        assertEquals(1, factory.sockets.size)
    }

    @Test
    fun `fails without retry when the server is unreachable on the first attempt`() = runTest {
        val unreachable = FakeSocketFactory(reachable = { it.startsWith("ws://") })
        val connection = connection(socketFactory = unreachable)
        connection.start()
        runCurrent()
        unreachable.last.drop()
        runCurrent()

        assertEquals(listOf("wss://localhost:38281", "ws://localhost:38281"), unreachable.opened)
        assertIs<SlotState.Failed>(connection.state.value)
    }

    @Test
    fun `sends Say only while connected`() = runTest {
        val connection = connection()
        connection.start()
        runCurrent()
        assertFalse(connection.say("!hint Hookshot"))

        factory.last.receive(TestRoom.roomInfo, TestRoom.connected(slot = 1))
        runCurrent()

        assertTrue(connection.say("!hint Hookshot"))
        assertEquals(
            listOf(ClientPacket.Say("!hint Hookshot")),
            factory.last.sentOf<ClientPacket.Say>(),
        )
    }

    @Test
    fun `reports the players of the team with their games`() = runTest {
        val connection = connection()
        connection.start()
        runCurrent()
        factory.last.receive(TestRoom.roomInfo)
        factory.last.receive(
            TestRoom.connected(slot = 1).copy(
                slotInfo = mapOf(2 to NetworkSlot(name = "Samus", game = "Super Metroid")),
            ),
        )
        runCurrent()

        val info = assertIs<SlotState.Connected>(connection.state.value).info
        assertEquals(
            listOf(
                RoomPlayer(1, "Link", null),
                RoomPlayer(2, "Samus", "Super Metroid"),
                RoomPlayer(3, "Zelda", null),
            ),
            info.players,
        )
    }

    @Test
    fun `counts from now after countFromNow`() = runTest {
        val connection = connection()
        connection.start()
        runCurrent()
        factory.last.receive(TestRoom.roomInfo)
        factory.last.receive(TestRoom.connected(slot = 1))
        runCurrent()
        assertEquals(-1, connection.lastReportedIndex.value)
        factory.last.receive(
            ServerPacket.ReceivedItems(
                index = 0,
                items = listOf(TestRoom.item(1, from = 2), TestRoom.item(2, from = 2)),
            ),
        )
        runCurrent()

        assertTrue(connection.countFromNow())
        runCurrent()

        assertEquals(1, connection.lastReportedIndex.value)
        assertEquals(1, indexStore.lastReportedIndex(key))
    }

    @Test
    fun `countFromNow does nothing while not connected`() = runTest {
        assertFalse(connection().countFromNow())
    }

    @Test
    fun `builds candidate urls from user input`() {
        assertEquals(
            listOf("wss://archipelago.gg:38281", "ws://archipelago.gg:38281"),
            SlotConnection.candidateUrls(" archipelago.gg "),
        )
        assertEquals(
            listOf("wss://archipelago.gg:52000", "ws://archipelago.gg:52000"),
            SlotConnection.candidateUrls("archipelago.gg:52000"),
        )
        assertEquals(listOf("ws://localhost:1"), SlotConnection.candidateUrls("ws://localhost:1"))
        assertEquals(
            listOf("wss://[::1]:38281", "ws://[::1]:38281"),
            SlotConnection.candidateUrls("[::1]"),
        )
        assertEquals("archipelago.gg:38281", SlotConnection.serverKey("Archipelago.gg"))
    }

    @Test
    fun `backoff doubles up to the maximum`() {
        val policy = ReconnectPolicy(initialDelayMillis = 1_000, maxDelayMillis = 5_000)

        assertEquals(
            listOf(1_000L, 2_000L, 4_000L, 5_000L, 5_000L),
            (1..5).map(policy::delayMillis),
        )
    }
}
