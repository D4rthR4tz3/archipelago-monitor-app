package io.github.d4rthr4tz3.apviewer.protocol.session

import io.github.d4rthr4tz3.apviewer.protocol.message.FeedHighlight
import io.github.d4rthr4tz3.apviewer.protocol.model.JsonMessagePart
import io.github.d4rthr4tz3.apviewer.protocol.packet.ClientPacket
import io.github.d4rthr4tz3.apviewer.protocol.packet.ConnectionTags
import io.github.d4rthr4tz3.apviewer.protocol.packet.ServerPacket
import io.github.d4rthr4tz3.apviewer.protocol.store.InMemoryDataPackageCache
import io.github.d4rthr4tz3.apviewer.protocol.store.InMemoryItemIndexStore
import io.github.d4rthr4tz3.apviewer.protocol.testing.FakeSocket
import io.github.d4rthr4tz3.apviewer.protocol.testing.FakeSocketFactory
import io.github.d4rthr4tz3.apviewer.protocol.testing.TestRoom
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest

@OptIn(ExperimentalCoroutinesApi::class)
class MultiworldSessionTest {
    private val factory = FakeSocketFactory()

    private fun TestScope.session() = MultiworldSession(
        scope = backgroundScope,
        socketFactory = factory,
        config = MultiworldConfig(
            address = "localhost",
            roomPassword = "room",
            slots = listOf(SlotConfig("Link"), SlotConfig("Zelda", password = "own")),
        ),
        itemIndexStore = InMemoryItemIndexStore(),
        dataPackageCache = InMemoryDataPackageCache(),
        uuid = "uuid",
        clock = { 1_000L },
    )

    /** Starts [session] and completes the handshake of both slots. */
    private fun TestScope.connect(session: MultiworldSession): Pair<FakeSocket, FakeSocket> {
        session.start()
        runCurrent()
        val (link, zelda) = factory.sockets
        link.receive(TestRoom.roomInfo)
        zelda.receive(TestRoom.roomInfo)
        link.receive(TestRoom.connected(slot = 1))
        zelda.receive(TestRoom.connected(slot = 3))
        runCurrent()
        return link to zelda
    }

    private fun FakeSocket.connectPacket() = sentOf<ClientPacket.Connect>().single()

    @Test
    fun `connects every slot and only the first one with text`() = runTest {
        val (link, zelda) = connect(session())

        assertEquals("Link", link.connectPacket().name)
        assertEquals(listOf(ConnectionTags.TRACKER), link.connectPacket().tags)
        assertEquals("Zelda", zelda.connectPacket().name)
        assertTrue(ConnectionTags.NO_TEXT in zelda.connectPacket().tags)
    }

    @Test
    fun `uses the room password unless a slot has its own`() = runTest {
        val (link, zelda) = connect(session())

        assertEquals("room", link.connectPacket().password)
        assertEquals("own", zelda.connectPacket().password)
    }

    @Test
    fun `highlights items for and from all own slots in the feed`() = runTest {
        val session = session()
        val (link, _) = connect(session)

        link.receive(
            ServerPacket.PrintJson(
                type = "ItemSend",
                data = listOf(JsonMessagePart(type = "player_id", text = "3")),
                receiving = 3,
                item = TestRoom.item(1, from = 2),
            ),
            ServerPacket.PrintJson(
                type = "ItemSend",
                data = listOf(JsonMessagePart(type = "player_id", text = "3")),
                receiving = 2,
                item = TestRoom.item(2, from = 3),
            ),
            ServerPacket.PrintJson(data = listOf(JsonMessagePart(text = "hello"))),
        )
        runCurrent()

        assertEquals(
            listOf(FeedHighlight.FOR_ME, FeedHighlight.FROM_ME, FeedHighlight.NONE),
            session.feed.value.map { it.message.highlight },
        )
        assertEquals(listOf(0L, 1L, 2L), session.feed.value.map { it.id })
        assertEquals(listOf(1_000L), session.feed.value.map { it.receivedAtMillis }.distinct())
    }

    @Test
    fun `tags received items with the receiving slot`() = runTest {
        val session = session()
        val items = mutableListOf<SlotReceivedItem>()
        backgroundScope.launch { session.receivedItems.toList(items) }
        val (_, zelda) = connect(session)

        zelda.receive(
            ServerPacket.ReceivedItems(index = 0, items = listOf(TestRoom.item(7, from = 2))),
        )
        runCurrent()

        assertEquals("Zelda", items.single().slotName)
        assertEquals(7L, items.single().item.item.item)
    }

    @Test
    fun `sends Say from the chosen slot`() = runTest {
        val session = session()
        val (link, zelda) = connect(session)

        assertTrue(session.say("Zelda", "hi"))

        assertEquals(listOf(ClientPacket.Say("hi")), zelda.sentOf<ClientPacket.Say>())
        assertEquals(emptyList(), link.sentOf<ClientPacket.Say>())
    }
}
