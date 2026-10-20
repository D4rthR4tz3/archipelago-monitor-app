package io.github.d4rthr4tz3.apviewer.protocol.session

import io.github.d4rthr4tz3.apviewer.protocol.model.JsonMessagePart
import io.github.d4rthr4tz3.apviewer.protocol.monitor.FeedEntry
import io.github.d4rthr4tz3.apviewer.protocol.packet.ServerPacket
import io.github.d4rthr4tz3.apviewer.protocol.store.AppSettings
import io.github.d4rthr4tz3.apviewer.protocol.store.InMemoryDataPackageCache
import io.github.d4rthr4tz3.apviewer.protocol.store.InMemoryItemIndexStore
import io.github.d4rthr4tz3.apviewer.protocol.store.Profiles
import io.github.d4rthr4tz3.apviewer.protocol.store.SavedMultiworld
import io.github.d4rthr4tz3.apviewer.protocol.store.SavedSlot
import io.github.d4rthr4tz3.apviewer.protocol.testing.FakeSocketFactory
import io.github.d4rthr4tz3.apviewer.protocol.testing.TestRoom
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotSame
import kotlin.test.assertSame
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest

@OptIn(ExperimentalCoroutinesApi::class)
class SessionManagerTest {
    private val factory = FakeSocketFactory()
    private val stored = mutableListOf<Pair<String, FeedEntry>>()

    private val zelda =
        SavedMultiworld(id = "zelda", address = "localhost", slots = listOf(SavedSlot("Link")))
    private val metroid = SavedMultiworld(
        id = "metroid",
        address = "other",
        slots = listOf(SavedSlot("Samus")),
        connectOnStart = false,
    )

    private fun TestScope.manager() = SessionManager(
        scope = backgroundScope,
        socketFactory = factory,
        itemIndexStore = InMemoryItemIndexStore(),
        dataPackageCache = InMemoryDataPackageCache(),
        feedSink = { id, entries -> entries.forEach { stored += id to it } },
        uuid = "uuid",
        clock = { 1_000L },
    )

    @Test
    fun `connects only multiworlds that connect on start`() = runTest {
        val manager = manager()

        manager.update(Profiles(multiworlds = listOf(zelda, metroid)))

        assertEquals(setOf("zelda"), manager.sessions.value.keys)
        assertEquals(setOf("zelda"), manager.wanted.value)
    }

    @Test
    fun `connect and disconnect change the running sessions`() = runTest {
        val manager = manager()
        manager.update(Profiles(multiworlds = listOf(zelda, metroid)))

        manager.connect("metroid")
        manager.disconnect("zelda")

        assertEquals(setOf("metroid"), manager.sessions.value.keys)
    }

    @Test
    fun `keeps the user's choice across profile updates`() = runTest {
        val manager = manager()
        manager.update(Profiles(multiworlds = listOf(zelda, metroid)))
        manager.disconnect("zelda")

        manager.update(Profiles(multiworlds = listOf(zelda.copy(label = "Renamed"), metroid)))

        assertEquals(emptySet(), manager.sessions.value.keys)
    }

    @Test
    fun `restarts a session only when its connection settings change`() = runTest {
        val manager = manager()
        manager.update(Profiles(multiworlds = listOf(zelda)))
        val first = manager.sessions.value.getValue("zelda")

        manager.update(Profiles(multiworlds = listOf(zelda.copy(label = "Renamed"))))
        assertSame(first, manager.sessions.value.getValue("zelda"))

        manager.update(Profiles(multiworlds = listOf(zelda.copy(roomPassword = "new"))))
        assertNotSame(first, manager.sessions.value.getValue("zelda"))
    }

    @Test
    fun `disabled connections stop every session`() = runTest {
        val manager = manager()
        manager.update(Profiles(multiworlds = listOf(zelda)))

        manager.update(
            Profiles(
                multiworlds = listOf(zelda),
                settings = AppSettings(connectionsEnabled = false),
            ),
        )

        assertEquals(emptySet(), manager.sessions.value.keys)
    }

    @Test
    fun `passes new feed messages to the sink once`() = runTest {
        val manager = manager()
        manager.update(Profiles(multiworlds = listOf(zelda)))
        runCurrent()
        factory.last.receive(TestRoom.roomInfo)
        factory.last.receive(TestRoom.connected(slot = 1))
        factory.last.receive(ServerPacket.PrintJson(data = listOf(JsonMessagePart(text = "one"))))
        runCurrent()
        factory.last.receive(ServerPacket.PrintJson(data = listOf(JsonMessagePart(text = "two"))))
        runCurrent()

        assertEquals(listOf("one", "two"), stored.map { it.second.message.plainText })
        assertEquals(setOf("zelda"), stored.map { it.first }.toSet())
    }

    @Test
    fun `reports the combined status`() = runTest {
        val manager = manager()
        manager.update(Profiles(multiworlds = listOf(zelda, metroid)))
        runCurrent()
        factory.last.receive(TestRoom.roomInfo)
        factory.last.receive(TestRoom.connected(slot = 1))
        runCurrent()

        assertEquals(MultiworldStatus.Connected(1), manager.status("zelda").first())
        assertEquals(MultiworldStatus.Disconnected, manager.status("metroid").first())
    }
}
