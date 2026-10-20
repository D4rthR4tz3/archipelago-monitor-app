package io.github.d4rthr4tz3.apviewer.protocol.store

import io.github.d4rthr4tz3.apviewer.protocol.message.FeedHighlight
import io.github.d4rthr4tz3.apviewer.protocol.message.FeedMessage
import io.github.d4rthr4tz3.apviewer.protocol.message.MessageSegment
import io.github.d4rthr4tz3.apviewer.protocol.message.SegmentKind
import io.github.d4rthr4tz3.apviewer.protocol.model.GameData
import io.github.d4rthr4tz3.apviewer.protocol.model.ItemFlags
import io.github.d4rthr4tz3.apviewer.protocol.model.NetworkItem
import io.github.d4rthr4tz3.apviewer.protocol.session.MultiworldConfig
import io.github.d4rthr4tz3.apviewer.protocol.session.SlotConfig
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlinx.serialization.SerializationException

class StoreCodecTest {
    private val multiworld = SavedMultiworld(
        id = "mw-1",
        label = "Friday seed",
        address = "archipelago.gg:38281",
        roomPassword = "room",
        slots = listOf(SavedSlot("Link"), SavedSlot("Samus", password = "slot")),
    )

    @Test
    fun `profiles survive a round trip`() {
        val profiles = Profiles(
            multiworlds = listOf(multiworld),
            settings = AppSettings(feedRetentionDays = 3),
        )

        assertEquals(profiles, StoreCodec.decodeProfiles(StoreCodec.encodeProfiles(profiles)))
    }

    @Test
    fun `profiles tolerate unknown and missing keys`() {
        val text = """
            {"multiworlds":[{"id":"mw-1","address":"localhost","future":true}],"other":1}
        """.trimIndent()

        val profiles = StoreCodec.decodeProfiles(text)

        assertEquals(
            listOf(SavedMultiworld(id = "mw-1", address = "localhost")),
            profiles.multiworlds,
        )
        assertEquals(AppSettings.DEFAULT_FEED_RETENTION_DAYS, profiles.settings.feedRetentionDays)
    }

    @Test
    fun `invalid profiles throw`() {
        assertFailsWith<SerializationException> { StoreCodec.decodeProfiles("not json") }
    }

    @Test
    fun `feed messages survive a round trip`() {
        val message = FeedMessage(
            type = "ItemSend",
            segments = listOf(
                MessageSegment("Samus", SegmentKind.PLAYER),
                MessageSegment(" sent "),
                MessageSegment("Hookshot", SegmentKind.ITEM, ItemFlags.PROGRESSION),
            ),
            receivingSlot = 1,
            item = NetworkItem(
                item = 10,
                location = 100,
                player = 2,
                flags = ItemFlags.PROGRESSION,
            ),
            highlight = FeedHighlight.FOR_ME,
        )

        assertEquals(
            message,
            StoreCodec.decodeFeedMessage(StoreCodec.encodeFeedMessage(message)),
        )
    }

    @Test
    fun `game data survives a round trip`() {
        val data = GameData(
            itemNameToId = mapOf("Hookshot" to 10L),
            locationNameToId = mapOf("Link's House" to 100L),
            checksum = "abc",
        )

        assertEquals(data, StoreCodec.decodeGameData(StoreCodec.encodeGameData(data)))
    }

    @Test
    fun `withMultiworld adds new and replaces existing multiworlds`() {
        val other = SavedMultiworld(id = "mw-2", address = "localhost")
        val renamed = multiworld.copy(label = "Renamed")

        val profiles = Profiles()
            .withMultiworld(multiworld)
            .withMultiworld(other)
            .withMultiworld(renamed)

        assertEquals(listOf(renamed, other), profiles.multiworlds)
        assertEquals(listOf(other), profiles.withoutMultiworld("mw-1").multiworlds)
    }

    @Test
    fun `saved multiworld maps to a session config`() {
        assertEquals(
            MultiworldConfig(
                address = "archipelago.gg:38281",
                roomPassword = "room",
                slots = listOf(SlotConfig("Link"), SlotConfig("Samus", password = "slot")),
            ),
            multiworld.toConfig(),
        )
    }
}
