package io.github.d4rthr4tz3.apviewer.protocol.packet

import io.github.d4rthr4tz3.apviewer.protocol.model.NetworkVersion
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Packets sent from the client to the server. The `cmd` field is added by [PacketCodec]. */
@Serializable
sealed interface ClientPacket {
    /** Authenticates against a slot. */
    @Serializable
    @SerialName("Connect")
    data class Connect(
        val name: String,
        val password: String = "",
        val game: String = "",
        val uuid: String,
        val version: NetworkVersion,
        @SerialName("items_handling") val itemsHandling: Int = ItemsHandling.ALL,
        val tags: List<String> = emptyList(),
        @SerialName("slot_data") val slotData: Boolean = false,
    ) : ClientPacket

    /** Requests the item and location names of [games]. */
    @Serializable
    @SerialName("GetDataPackage")
    data class GetDataPackage(val games: List<String>) : ClientPacket

    /** Sends a chat message or a `!command` to the room. */
    @Serializable
    @SerialName("Say")
    data class Say(val text: String) : ClientPacket

    /** Reads the data storage values of [keys]; the server answers with `Retrieved`. */
    @Serializable
    @SerialName("Get")
    data class Get(val keys: List<String>) : ClientPacket

    /** Subscribes to changes of the data storage values of [keys], sent as `SetReply`. */
    @Serializable
    @SerialName("SetNotify")
    data class SetNotify(val keys: List<String>) : ClientPacket

    /** Asks the server to resend all received items. */
    @Serializable
    @SerialName("Sync")
    data object Sync : ClientPacket
}

/** Bit flags for [ClientPacket.Connect.itemsHandling]. */
object ItemsHandling {
    const val OTHER_WORLDS = 0b001
    const val OWN_WORLD = 0b010
    const val STARTING_INVENTORY = 0b100
    const val ALL = OTHER_WORLDS or OWN_WORLD or STARTING_INVENTORY
}

/** Values for [ClientPacket.Connect.tags]. */
object ConnectionTags {
    /** Marks a client that never sends checks; allows connecting without a game. */
    const val TRACKER = "Tracker"

    /** Marks a client that only reads and sends text. */
    const val TEXT_ONLY = "TextOnly"

    /** Asks the server not to send `PrintJSON` text to this connection. */
    const val NO_TEXT = "NoText"
}
