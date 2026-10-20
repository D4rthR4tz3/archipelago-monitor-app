package io.github.d4rthr4tz3.apviewer.protocol.alert

import io.github.d4rthr4tz3.apviewer.protocol.model.ItemFlags
import io.github.d4rthr4tz3.apviewer.protocol.model.JsonMessagePart
import io.github.d4rthr4tz3.apviewer.protocol.packet.ServerPacket
import io.github.d4rthr4tz3.apviewer.protocol.session.SessionManager
import io.github.d4rthr4tz3.apviewer.protocol.store.InMemoryDataPackageCache
import io.github.d4rthr4tz3.apviewer.protocol.store.InMemoryItemIndexStore
import io.github.d4rthr4tz3.apviewer.protocol.store.NotifyMode
import io.github.d4rthr4tz3.apviewer.protocol.store.Profiles
import io.github.d4rthr4tz3.apviewer.protocol.store.SavedMultiworld
import io.github.d4rthr4tz3.apviewer.protocol.store.SavedSlot
import io.github.d4rthr4tz3.apviewer.protocol.store.SlotNotifications
import io.github.d4rthr4tz3.apviewer.protocol.testing.FakeSocketFactory
import io.github.d4rthr4tz3.apviewer.protocol.testing.TestRoom
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

@OptIn(ExperimentalCoroutinesApi::class)
class AlertSourceTest {
    private val factory = FakeSocketFactory()

    private val zelda = SavedMultiworld(
        id = "zelda",
        address = "localhost",
        slots = listOf(
            SavedSlot("Link"),
            SavedSlot("Zelda", notifications = SlotNotifications(hints = false)),
        ),
    )

    private fun TestScope.start(profiles: Profiles = Profiles(listOf(zelda))): List<Alert> {
        val manager = SessionManager(
            scope = backgroundScope,
            socketFactory = factory,
            itemIndexStore = InMemoryItemIndexStore(),
            dataPackageCache = InMemoryDataPackageCache(),
            feedSink = { _, _ -> },
            uuid = "uuid",
        )
        manager.update(profiles)
        val source = AlertSource(manager.sessions, MutableStateFlow(profiles))
        val alerts = mutableListOf<Alert>()
        backgroundScope.launch { source.alerts.collect { alerts += it } }
        runCurrent()
        val (link, zeldaSocket) = factory.sockets
        link.receive(TestRoom.roomInfo, TestRoom.connected(slot = 1))
        zeldaSocket.receive(TestRoom.roomInfo, TestRoom.connected(slot = 3))
        runCurrent()
        return alerts
    }

    private fun hintJson(receiving: Int, finding: Int, location: Long) = buildJsonObject {
        put("class", "Hint")
        put("receiving_player", receiving)
        put("finding_player", finding)
        put("location", location)
        put("item", 7)
        put("found", false)
        put("entrance", "")
        put("item_flags", 1)
    }

    private fun chat(slot: Int, text: String) = ServerPacket.PrintJson(
        type = "Chat",
        data = listOf(JsonMessagePart(text = text)),
        slot = slot,
    )

    @Test
    fun `reports new items as the slot settings say`() = runTest {
        val alerts = start()

        factory.sockets[0].receive(
            ServerPacket.ReceivedItems(
                index = 0,
                items = listOf(
                    TestRoom.item(1, from = 2, flags = ItemFlags.PROGRESSION),
                    TestRoom.item(2, from = 2),
                    TestRoom.item(3, from = 2, flags = ItemFlags.USEFUL),
                    TestRoom.item(4, from = 1, flags = ItemFlags.PROGRESSION),
                ),
            ),
        )
        runCurrent()

        val items = alerts.filterIsInstance<Alert.Item>()
        assertEquals(listOf(0, 2), items.map { it.item.index })
        assertEquals(listOf(NotifyMode.SOUND, NotifyMode.SILENT), items.map { it.mode })
        assertTrue(items.all { it.slotName == "Link" && it.multiworldId == "zelda" })
    }

    @Test
    fun `reports hints made while connected for slots that want them`() = runTest {
        val alerts = start()
        val (link, zeldaSocket) = factory.sockets
        link.receive(
            ServerPacket.Retrieved(
                mapOf("_read_hints_0_1" to JsonArray(listOf(hintJson(1, 2, 5)))),
            ),
        )
        runCurrent()

        link.receive(
            ServerPacket.SetReply(
                "_read_hints_0_1",
                JsonArray(listOf(hintJson(1, 2, 5), hintJson(1, 3, 6), hintJson(2, 1, 9))),
            ),
        )
        zeldaSocket.receive(
            ServerPacket.SetReply("_read_hints_0_3", JsonArray(listOf(hintJson(3, 2, 8)))),
        )
        runCurrent()

        val hints = alerts.filterIsInstance<Alert.NewHint>()
        assertEquals(listOf(6L), hints.map { it.hint.locationId })
        assertEquals("Link", hints.single().slotName)
    }

    @Test
    fun `reports chat messages of others that name a slot`() = runTest {
        val alerts = start()
        val link = factory.sockets[0]

        link.receive(
            chat(2, "Samus: link, do you have the hookshot?"),
            chat(2, "Samus: Linkle is not here"),
            chat(1, "Link: Zelda, I have it"),
            chat(2, "Samus: Zelda and Link, thanks"),
        )
        runCurrent()

        val mentions = alerts.filterIsInstance<Alert.Mention>()
        assertEquals(listOf(listOf("Link"), listOf("Link", "Zelda")), mentions.map { it.slotNames })
    }

    @Test
    fun `matches whole names only`() {
        assertTrue(mentions("hi D4rth_Keymizer!", "d4rth_keymizer"))
        assertFalse(mentions("D4rth_Keymizer2 is done", "D4rth_Keymizer"))
        assertFalse(mentions("Linkle", "Link"))
    }
}
