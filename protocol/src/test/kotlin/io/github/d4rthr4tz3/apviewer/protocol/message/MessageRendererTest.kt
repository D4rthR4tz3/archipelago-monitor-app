package io.github.d4rthr4tz3.apviewer.protocol.message

import io.github.d4rthr4tz3.apviewer.protocol.model.GameData
import io.github.d4rthr4tz3.apviewer.protocol.model.ItemFlags
import io.github.d4rthr4tz3.apviewer.protocol.model.JsonMessagePart
import io.github.d4rthr4tz3.apviewer.protocol.model.NetworkPlayer
import io.github.d4rthr4tz3.apviewer.protocol.model.NetworkSlot
import io.github.d4rthr4tz3.apviewer.protocol.packet.ServerPacket
import kotlin.test.Test
import kotlin.test.assertEquals

class MessageRendererTest {
    private val names = NameResolver()
        .withPlayers(
            team = 0,
            players = listOf(
                NetworkPlayer(team = 0, slot = 1, alias = "Link", name = "Link"),
                NetworkPlayer(team = 0, slot = 2, alias = "Samus", name = "Samus"),
            ),
            slots = mapOf(
                1 to NetworkSlot(name = "Link", game = "A Link to the Past"),
                2 to NetworkSlot(name = "Samus", game = "Super Metroid"),
            ),
        )
        .withGames(
            mapOf(
                "A Link to the Past" to GameData(
                    itemNameToId = mapOf("Hookshot" to 10L),
                    locationNameToId = mapOf("Link's House" to 100L),
                ),
                "Super Metroid" to GameData(
                    itemNameToId = mapOf("Morph Ball" to 20L),
                    locationNameToId = mapOf("Morph Ball (Brinstar)" to 200L),
                ),
            ),
        )

    @Test
    fun `resolves ids in an ItemSend message with the games of the owning slots`() {
        val packet = ServerPacket.PrintJson(
            type = "ItemSend",
            data = listOf(
                JsonMessagePart(type = "player_id", text = "2"),
                JsonMessagePart(text = " sent "),
                JsonMessagePart(
                    type = "item_id",
                    text = "10",
                    player = 1,
                    flags = ItemFlags.PROGRESSION,
                ),
                JsonMessagePart(text = " to "),
                JsonMessagePart(type = "player_id", text = "1"),
                JsonMessagePart(text = " ("),
                JsonMessagePart(type = "location_id", text = "200", player = 2),
                JsonMessagePart(text = ")"),
            ),
        )

        val message = MessageRenderer(ownSlots = setOf(1)).render(packet, names)

        assertEquals("Samus sent Hookshot to Link (Morph Ball (Brinstar))", message.plainText)
        assertEquals(SegmentKind.PLAYER, message.segments[0].kind)
        assertEquals(SegmentKind.ITEM, message.segments[2].kind)
        assertEquals(ItemFlags.PROGRESSION, message.segments[2].itemFlags)
        assertEquals(SegmentKind.OWN_PLAYER, message.segments[4].kind)
        assertEquals(SegmentKind.LOCATION, message.segments[6].kind)
    }

    @Test
    fun `falls back to placeholders for unknown ids`() {
        val packet = ServerPacket.PrintJson(
            data = listOf(
                JsonMessagePart(type = "player_id", text = "9"),
                JsonMessagePart(text = ": "),
                JsonMessagePart(type = "item_id", text = "99", player = 1),
            ),
        )

        val message = MessageRenderer(ownSlots = emptySet()).render(packet, names)

        assertEquals("Player 9: Item 99", message.plainText)
    }
}
