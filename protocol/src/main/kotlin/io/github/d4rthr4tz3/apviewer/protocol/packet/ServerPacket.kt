package io.github.d4rthr4tz3.apviewer.protocol.packet

import io.github.d4rthr4tz3.apviewer.protocol.model.DataPackageObject
import io.github.d4rthr4tz3.apviewer.protocol.model.JsonMessagePart
import io.github.d4rthr4tz3.apviewer.protocol.model.NetworkItem
import io.github.d4rthr4tz3.apviewer.protocol.model.NetworkPlayer
import io.github.d4rthr4tz3.apviewer.protocol.model.NetworkSlot
import io.github.d4rthr4tz3.apviewer.protocol.model.NetworkVersion
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject

/** Packets sent from the server to the client. */
sealed interface ServerPacket {
    /** First packet after the WebSocket handshake. */
    @Serializable
    data class RoomInfo(
        val version: NetworkVersion,
        @SerialName("generator_version") val generatorVersion: NetworkVersion? = null,
        val tags: List<String> = emptyList(),
        val password: Boolean = false,
        @SerialName("hint_cost") val hintCost: Int = 0,
        @SerialName("location_check_points") val locationCheckPoints: Int = 0,
        val games: List<String> = emptyList(),
        @SerialName("datapackage_checksums") val dataPackageChecksums: Map<String, String> =
            emptyMap(),
        @SerialName("seed_name") val seedName: String = "",
        val time: Double = 0.0,
    ) : ServerPacket

    /** Answer to [ClientPacket.Connect] when authentication failed. */
    @Serializable
    data class ConnectionRefused(val errors: List<String> = emptyList()) : ServerPacket

    /** Answer to [ClientPacket.Connect] when authentication succeeded. */
    @Serializable
    data class Connected(
        val team: Int,
        val slot: Int,
        val players: List<NetworkPlayer> = emptyList(),
        @SerialName("missing_locations") val missingLocations: List<Long> = emptyList(),
        @SerialName("checked_locations") val checkedLocations: List<Long> = emptyList(),
        @SerialName("slot_info") val slotInfo: Map<Int, NetworkSlot> = emptyMap(),
        @SerialName("hint_points") val hintPoints: Int = 0,
    ) : ServerPacket

    /** Items the connected slot received, starting at [index] of its item list. */
    @Serializable
    data class ReceivedItems(val index: Int, val items: List<NetworkItem> = emptyList()) :
        ServerPacket

    /** Changes to the room state since [Connected] or [RoomInfo]. */
    @Serializable
    data class RoomUpdate(
        val players: List<NetworkPlayer>? = null,
        @SerialName("checked_locations") val checkedLocations: List<Long>? = null,
        @SerialName("hint_points") val hintPoints: Int? = null,
    ) : ServerPacket

    /** Item and location names requested with [ClientPacket.GetDataPackage]. */
    @Serializable
    data class DataPackage(val data: DataPackageObject) : ServerPacket

    /** A chat or event message made of typed parts. */
    @Serializable
    data class PrintJson(
        val data: List<JsonMessagePart> = emptyList(),
        val type: String? = null,
        val receiving: Int? = null,
        val item: NetworkItem? = null,
        val found: Boolean? = null,
        val slot: Int? = null,
        val message: String? = null,
    ) : ServerPacket

    /** Answer to [ClientPacket.Get] with the values of the requested keys. */
    @Serializable
    data class Retrieved(val keys: Map<String, JsonElement> = emptyMap()) : ServerPacket

    /** A changed data storage value the client subscribed to with [ClientPacket.SetNotify]. */
    @Serializable
    data class SetReply(val key: String, val value: JsonElement = JsonNull) : ServerPacket

    /** A packet whose `cmd` this client does not handle. */
    data class Unknown(val cmd: String, val raw: JsonObject) : ServerPacket
}
