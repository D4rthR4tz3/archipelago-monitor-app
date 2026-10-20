package io.github.d4rthr4tz3.apviewer.protocol.message

import io.github.d4rthr4tz3.apviewer.protocol.model.GameData
import io.github.d4rthr4tz3.apviewer.protocol.model.NetworkPlayer
import io.github.d4rthr4tz3.apviewer.protocol.model.NetworkSlot

/**
 * Resolves the numeric ids of the Archipelago protocol to display names. Instances are immutable;
 * the `with…` functions return updated copies.
 */
class NameResolver private constructor(
    private val team: Int,
    private val players: Map<Int, NetworkPlayer>,
    private val slots: Map<Int, NetworkSlot>,
    private val itemNames: Map<String, Map<Long, String>>,
    private val locationNames: Map<String, Map<Long, String>>,
) {
    constructor() : this(0, emptyMap(), emptyMap(), emptyMap(), emptyMap())

    /** Returns a copy that knows the [players] of [team] and the games of [slots]. */
    fun withPlayers(
        team: Int,
        players: List<NetworkPlayer>,
        slots: Map<Int, NetworkSlot> = this.slots,
    ): NameResolver = NameResolver(
        team = team,
        players = players.filter { it.team == team }.associateBy { it.slot },
        slots = slots,
        itemNames = itemNames,
        locationNames = locationNames,
    )

    /** Returns a copy that also knows the item and location names of [games]. */
    fun withGames(games: Map<String, GameData>): NameResolver = NameResolver(
        team = team,
        players = players,
        slots = slots,
        itemNames = itemNames + games.mapValues { (_, data) -> data.itemNameToId.inverted() },
        locationNames = locationNames +
            games.mapValues { (_, data) -> data.locationNameToId.inverted() },
    )

    /** Returns the alias of [slot], or a placeholder if the slot is unknown. */
    fun playerName(slot: Int): String = players[slot]?.alias ?: slots[slot]?.name ?: "Player $slot"

    /** Returns the name of item [id] from the game played in [slot]. */
    fun itemName(id: Long, slot: Int): String = itemNames[gameOf(slot)]?.get(id) ?: "Item $id"

    /** Returns the name of location [id] from the game played in [slot]. */
    fun locationName(id: Long, slot: Int): String =
        locationNames[gameOf(slot)]?.get(id) ?: "Location $id"

    private fun gameOf(slot: Int): String? = slots[slot]?.game

    private fun Map<String, Long>.inverted(): Map<Long, String> =
        entries.associate { (name, id) -> id to name }
}
