package io.github.d4rthr4tz3.apviewer.protocol.session

import io.github.d4rthr4tz3.apviewer.protocol.model.Hint
import io.github.d4rthr4tz3.apviewer.protocol.model.HintStatus
import io.github.d4rthr4tz3.apviewer.protocol.packet.ClientPacket
import io.github.d4rthr4tz3.apviewer.protocol.packet.PacketCodec
import io.github.d4rthr4tz3.apviewer.protocol.packet.ServerPacket
import io.github.d4rthr4tz3.apviewer.protocol.store.InMemoryDataPackageCache
import io.github.d4rthr4tz3.apviewer.protocol.store.InMemoryItemIndexStore
import io.github.d4rthr4tz3.apviewer.protocol.testing.FakeSocket
import io.github.d4rthr4tz3.apviewer.protocol.testing.FakeSocketFactory
import io.github.d4rthr4tz3.apviewer.protocol.testing.TestRoom
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

@OptIn(ExperimentalCoroutinesApi::class)
class HintsTest {
    private val factory = FakeSocketFactory()

    private fun hintJson(
        receiving: Int,
        finding: Int,
        location: Long,
        status: Int? = null,
        found: Boolean = false,
    ): JsonElement = buildJsonObject {
        put("class", "Hint")
        put("receiving_player", receiving)
        put("finding_player", finding)
        put("location", location)
        put("item", 7)
        put("found", found)
        put("entrance", "")
        put("item_flags", 1)
        if (status != null) put("status", status)
    }

    private fun TestScope.session() = MultiworldSession(
        scope = backgroundScope,
        socketFactory = factory,
        config = MultiworldConfig(
            address = "localhost",
            slots = listOf(SlotConfig("Link"), SlotConfig("Zelda")),
        ),
        itemIndexStore = InMemoryItemIndexStore(),
        dataPackageCache = InMemoryDataPackageCache(),
        uuid = "uuid",
    )

    private fun TestScope.connect(session: MultiworldSession): Pair<FakeSocket, FakeSocket> {
        session.start()
        runCurrent()
        val (link, zelda) = factory.sockets
        link.receive(TestRoom.roomInfo, TestRoom.connected(slot = 1))
        zelda.receive(TestRoom.roomInfo, TestRoom.connected(slot = 3))
        runCurrent()
        return link to zelda
    }

    @Test
    fun `decodes Retrieved and SetReply`() {
        val packets = PacketCodec().decode(
            """
            [{"cmd":"Retrieved","keys":{"_read_hints_0_1":[{"class":"Hint","receiving_player":1,
              "finding_player":2,"location":5,"item":7,"found":false,"entrance":"",
              "item_flags":1,"status":30}]}},
             {"cmd":"SetReply","key":"_read_hints_0_1","value":[],"original_value":null,"slot":1}]
            """.trimIndent(),
        )

        val retrieved = packets[0] as ServerPacket.Retrieved
        val hint = Hint.listFrom(retrieved.keys["_read_hints_0_1"]).single()
        assertEquals(HintStatus.PRIORITY, hint.hintStatus)
        assertEquals(5L, hint.location)
        assertEquals(ServerPacket.SetReply("_read_hints_0_1", JsonArray(emptyList())), packets[1])
    }

    @Test
    fun `treats found hints as found and ignores values that are not hint lists`() {
        val old = Hint.listFrom(JsonArray(listOf(hintJson(1, 2, 5, found = true))))
        assertEquals(HintStatus.FOUND, old.single().hintStatus)
        assertEquals(HintStatus.UNSPECIFIED, Hint.of(hintJson(1, 2, 5)).hintStatus)
        assertEquals(emptyList(), Hint.listFrom(JsonNull))
        assertEquals(emptyList(), Hint.listFrom(JsonPrimitive(3)))
    }

    @Test
    fun `reads and subscribes to the hints of each slot after connecting`() = runTest {
        val (link, zelda) = connect(session())

        assertEquals(listOf("_read_hints_0_1"), link.sentOf<ClientPacket.Get>().single().keys)
        assertEquals(listOf("_read_hints_0_1"), link.sentOf<ClientPacket.SetNotify>().single().keys)
        assertEquals(listOf("_read_hints_0_3"), zelda.sentOf<ClientPacket.Get>().single().keys)
    }

    @Test
    fun `merges the hints of all slots and marks own slots`() = runTest {
        val session = session()
        val (link, zelda) = connect(session)

        link.receive(
            ServerPacket.Retrieved(
                mapOf(
                    "_read_hints_0_1" to JsonArray(
                        listOf(hintJson(1, 2, 5, status = 30), hintJson(1, 3, 6)),
                    ),
                ),
            ),
        )
        zelda.receive(
            ServerPacket.SetReply(
                "_read_hints_0_3",
                JsonArray(listOf(hintJson(1, 3, 6), hintJson(2, 3, 8, status = 20))),
            ),
        )
        runCurrent()

        val hints = session.hints.first()
        assertEquals(listOf(5L, 6L, 8L), hints.map { it.hint.locationId })
        val fromSamus = hints[0]
        assertEquals("Link", fromSamus.hint.receiverName)
        assertEquals("Samus", fromSamus.hint.finderName)
        assertEquals("Item 7", fromSamus.hint.itemName)
        assertEquals(HintStatus.PRIORITY, fromSamus.hint.status)
        assertTrue(fromSamus.receiverIsOwn)
        assertFalse(fromSamus.finderIsOwn)
        assertTrue(fromSamus.editable)
        val forSamus = hints[2]
        assertFalse(forSamus.receiverIsOwn)
        assertTrue(forSamus.finderIsOwn)
        assertFalse(forSamus.editable)
    }

    @Test
    fun `ignores data storage values of other keys`() = runTest {
        val session = session()
        val (link, _) = connect(session)

        link.receive(ServerPacket.SetReply("other", JsonArray(listOf(hintJson(1, 2, 5)))))
        runCurrent()

        assertEquals(emptyList(), session.hints.first())
    }

    @Test
    fun `sends status updates from the receiving slot`() = runTest {
        val session = session()
        val (link, zelda) = connect(session)
        zelda.receive(
            ServerPacket.Retrieved(
                mapOf("_read_hints_0_3" to JsonArray(listOf(hintJson(3, 2, 9)))),
            ),
        )
        runCurrent()
        val hint = session.hints.first().single().hint

        assertTrue(session.updateHint(hint, HintStatus.AVOID))

        assertEquals(
            listOf(ClientPacket.UpdateHint(player = 2, location = 9, status = 20)),
            zelda.sentOf<ClientPacket.UpdateHint>(),
        )
        assertEquals(emptyList(), link.sentOf<ClientPacket.UpdateHint>())
    }

    @Test
    fun `clears the hints when stopped`() = runTest {
        val session = session()
        val (link, _) = connect(session)
        link.receive(
            ServerPacket.Retrieved(
                mapOf("_read_hints_0_1" to JsonArray(listOf(hintJson(1, 2, 5)))),
            ),
        )
        runCurrent()

        session.stop()

        assertEquals(emptyList(), session.hints.first())
        assertFalse(session.updateHint(sampleHint(), HintStatus.AVOID))
    }

    private fun sampleHint() = SlotHint(
        receivingSlot = 1,
        receiverName = "Link",
        findingSlot = 2,
        finderName = "Samus",
        itemId = 7,
        itemName = "Item 7",
        itemFlags = 1,
        locationId = 5,
        locationName = "Location 5",
        entrance = "",
        status = HintStatus.UNSPECIFIED,
    )

    private fun Hint.Companion.of(json: JsonElement): Hint =
        listFrom(JsonArray(listOf(json))).single()
}
