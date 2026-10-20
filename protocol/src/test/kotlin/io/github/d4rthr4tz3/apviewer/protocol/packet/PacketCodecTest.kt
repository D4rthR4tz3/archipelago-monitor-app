package io.github.d4rthr4tz3.apviewer.protocol.packet

import io.github.d4rthr4tz3.apviewer.protocol.model.NetworkVersion
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

class PacketCodecTest {
    private val codec = PacketCodec()

    @Test
    fun `decodes RoomInfo`() {
        val frame = """
            [{"cmd":"RoomInfo","password":false,"games":["A Link to the Past","Archipelago"],
              "tags":["AP"],"version":{"major":0,"minor":6,"build":1,"class":"Version"},
              "generator_version":{"major":0,"minor":6,"build":1,"class":"Version"},
              "permissions":{"release":2,"collect":2,"remaining":2},"hint_cost":10,
              "location_check_points":1,"datapackage_checksums":{"Archipelago":"ac9141"},
              "seed_name":"12345","time":1760900000.5}]
        """.trimIndent()

        val packet = assertIs<ServerPacket.RoomInfo>(codec.decode(frame).single())

        assertEquals("0.6.1", packet.version.toString())
        assertEquals(listOf("A Link to the Past", "Archipelago"), packet.games)
        assertEquals("12345", packet.seedName)
        assertEquals(mapOf("Archipelago" to "ac9141"), packet.dataPackageChecksums)
    }

    @Test
    fun `decodes Connected with numeric slot_info keys`() {
        val frame = """
            [{"cmd":"Connected","team":0,"slot":2,
              "players":[{"team":0,"slot":1,"alias":"Link","name":"Link","class":"NetworkPlayer"},
                         {"team":0,"slot":2,"alias":"Zelda","name":"Zelda","class":"NetworkPlayer"}],
              "missing_locations":[1,2],"checked_locations":[3],
              "slot_info":{"1":{"name":"Link","game":"A Link to the Past","type":1,
                                "group_members":[],"class":"NetworkSlot"}},
              "slot_data":{"goal":1},"hint_points":4}]
        """.trimIndent()

        val packet = assertIs<ServerPacket.Connected>(codec.decode(frame).single())

        assertEquals(2, packet.slot)
        assertEquals(listOf("Link", "Zelda"), packet.players.map { it.alias })
        assertEquals("A Link to the Past", packet.slotInfo.getValue(1).game)
    }

    @Test
    fun `decodes several packets in one frame and keeps unknown ones`() {
        val frame = """
            [{"cmd":"PrintJSON","data":[{"text":"Hello"}],"type":"Chat"},
             {"cmd":"Bounced","data":{}}]
        """.trimIndent()

        val packets = codec.decode(frame)

        assertIs<ServerPacket.PrintJson>(packets[0])
        assertEquals("Bounced", assertIs<ServerPacket.Unknown>(packets[1]).cmd)
    }

    @Test
    fun `encodes Connect with cmd and version class`() {
        val frame = codec.encode(
            listOf(
                ClientPacket.Connect(
                    name = "Zelda",
                    uuid = "abc",
                    version = NetworkVersion(major = 0, minor = 6, build = 0),
                    tags = listOf(ConnectionTags.TRACKER),
                ),
            ),
        )

        val packet = Json.parseToJsonElement(frame).jsonArray.single().jsonObject
        assertEquals("Connect", packet.getValue("cmd").jsonPrimitive.content)
        assertEquals("Zelda", packet.getValue("name").jsonPrimitive.content)
        assertEquals("", packet.getValue("game").jsonPrimitive.content)
        assertEquals(ItemsHandling.ALL, packet.getValue("items_handling").jsonPrimitive.int)
        val version = packet.getValue("version").jsonObject
        assertEquals("Version", version.getValue("class").jsonPrimitive.content)
    }

    @Test
    fun `encodes data object packets`() {
        assertEquals("""[{"cmd":"Sync"}]""", codec.encode(listOf(ClientPacket.Sync)))
    }
}
