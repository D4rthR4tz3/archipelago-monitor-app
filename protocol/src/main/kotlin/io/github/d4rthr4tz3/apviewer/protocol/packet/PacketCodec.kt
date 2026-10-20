package io.github.d4rthr4tz3.apviewer.protocol.packet

import kotlinx.serialization.KSerializer
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/**
 * Encodes and decodes Archipelago WebSocket frames. Every frame is a JSON array of packets, each
 * identified by its `cmd` field.
 */
class PacketCodec {
    private val json = Json {
        classDiscriminator = "cmd"
        encodeDefaults = true
        explicitNulls = false
        ignoreUnknownKeys = true
        coerceInputValues = true
    }

    private val clientPacketsSerializer = ListSerializer(ClientPacket.serializer())

    /** Returns the frame text for [packets]. */
    fun encode(packets: List<ClientPacket>): String =
        json.encodeToString(clientPacketsSerializer, packets)

    /**
     * Returns the packets contained in [frame]. Packets with an unknown `cmd` are returned as
     * [ServerPacket.Unknown].
     *
     * @throws kotlinx.serialization.SerializationException if [frame] is not valid packet JSON.
     */
    fun decode(frame: String): List<ServerPacket> =
        json.parseToJsonElement(frame).jsonArray.map { element -> decodePacket(element.jsonObject) }

    private fun decodePacket(packet: JsonObject): ServerPacket {
        val cmd = packet["cmd"]?.jsonPrimitive?.content.orEmpty()
        val serializer = serializerFor(cmd) ?: return ServerPacket.Unknown(cmd, packet)
        return json.decodeFromJsonElement(serializer, packet)
    }

    private fun serializerFor(cmd: String): KSerializer<out ServerPacket>? = when (cmd) {
        "RoomInfo" -> ServerPacket.RoomInfo.serializer()
        "ConnectionRefused" -> ServerPacket.ConnectionRefused.serializer()
        "Connected" -> ServerPacket.Connected.serializer()
        "ReceivedItems" -> ServerPacket.ReceivedItems.serializer()
        "RoomUpdate" -> ServerPacket.RoomUpdate.serializer()
        "DataPackage" -> ServerPacket.DataPackage.serializer()
        "PrintJSON" -> ServerPacket.PrintJson.serializer()
        "Retrieved" -> ServerPacket.Retrieved.serializer()
        "SetReply" -> ServerPacket.SetReply.serializer()
        else -> null
    }
}
