package io.github.d4rthr4tz3.apviewer.protocol.store

import io.github.d4rthr4tz3.apviewer.protocol.session.MultiworldConfig
import io.github.d4rthr4tz3.apviewer.protocol.session.SlotConfig
import kotlinx.serialization.Serializable

/** One of the user's slots in a [SavedMultiworld]. */
@Serializable
data class SavedSlot(
    val name: String,
    /** Overrides the room password for this slot if not empty. */
    val password: String = "",
)

/**
 * A multiworld the user saved. [id] is stable across edits and links the multiworld to its stored
 * feed. Inactive multiworlds are kept but not connected.
 */
@Serializable
data class SavedMultiworld(
    val id: String,
    val label: String = "",
    val address: String,
    val roomPassword: String = "",
    val slots: List<SavedSlot> = emptyList(),
    val active: Boolean = true,
) {
    /** Returns the connection settings of this multiworld for a session. */
    fun toConfig(): MultiworldConfig = MultiworldConfig(
        address = address,
        roomPassword = roomPassword,
        slots = slots.map { SlotConfig(name = it.name, password = it.password) },
    )
}

/** App-wide settings. */
@Serializable
data class AppSettings(
    /** Feed messages older than this are deleted. */
    val feedRetentionDays: Int = DEFAULT_FEED_RETENTION_DAYS,
) {
    companion object {
        const val DEFAULT_FEED_RETENTION_DAYS = 14
    }
}

/** Everything the user configured: saved multiworlds and settings. */
@Serializable
data class Profiles(
    val multiworlds: List<SavedMultiworld> = emptyList(),
    val settings: AppSettings = AppSettings(),
) {
    /** Returns a copy with [multiworld] added, or replacing the one with the same id. */
    fun withMultiworld(multiworld: SavedMultiworld): Profiles {
        val index = multiworlds.indexOfFirst { it.id == multiworld.id }
        val updated = if (index < 0) {
            multiworlds + multiworld
        } else {
            multiworlds.toMutableList().also { it[index] = multiworld }
        }
        return copy(multiworlds = updated)
    }

    /** Returns a copy without the multiworld with [id]. */
    fun withoutMultiworld(id: String): Profiles =
        copy(multiworlds = multiworlds.filterNot { it.id == id })
}
