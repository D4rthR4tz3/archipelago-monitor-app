package io.github.d4rthr4tz3.apviewer.protocol.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Archipelago version triple, serialized with the `class` marker the server expects. */
@Serializable
data class NetworkVersion(
    val major: Int,
    val minor: Int,
    val build: Int,
    @SerialName("class") val type: String = "Version",
) {
    override fun toString(): String = "$major.$minor.$build"
}

/** A player in the multiworld, identified by team and slot. */
@Serializable
data class NetworkPlayer(val team: Int, val slot: Int, val alias: String, val name: String)

/** An item placed at a location. [player] is the sending or receiving slot, by context. */
@Serializable
data class NetworkItem(val item: Long, val location: Long, val player: Int, val flags: Int = 0) {
    val isProgression: Boolean get() = flags and ItemFlags.PROGRESSION != 0
    val isUseful: Boolean get() = flags and ItemFlags.USEFUL != 0
    val isTrap: Boolean get() = flags and ItemFlags.TRAP != 0
}

/** Bit flags of [NetworkItem.flags]. */
object ItemFlags {
    const val PROGRESSION = 0b001
    const val USEFUL = 0b010
    const val TRAP = 0b100
}

/** Static information about a slot: its name, game and whether it is a player or a group. */
@Serializable
data class NetworkSlot(
    val name: String,
    val game: String,
    val type: Int = 0,
    @SerialName("group_members") val groupMembers: List<Int> = emptyList(),
)

/** One part of a `PrintJSON` message. Its [type] decides how [text] is interpreted. */
@Serializable
data class JsonMessagePart(
    val type: String? = null,
    val text: String? = null,
    val color: String? = null,
    val flags: Int? = null,
    val player: Int? = null,
)

/** Item and location names of one game, as sent in a `DataPackage` packet. */
@Serializable
data class GameData(
    @SerialName("item_name_to_id") val itemNameToId: Map<String, Long> = emptyMap(),
    @SerialName("location_name_to_id") val locationNameToId: Map<String, Long> = emptyMap(),
    val checksum: String = "",
)

/** Container of all [GameData] in a `DataPackage` packet, keyed by game name. */
@Serializable
data class DataPackageObject(val games: Map<String, GameData> = emptyMap())
