package io.github.d4rthr4tz3.apviewer.protocol.store

import io.github.d4rthr4tz3.apviewer.protocol.model.ItemFlags
import io.github.d4rthr4tz3.apviewer.protocol.session.MultiworldConfig
import io.github.d4rthr4tz3.apviewer.protocol.session.SlotConfig
import kotlinx.serialization.Serializable

/** Classification of an item, from its flags. The order is the order of precedence. */
enum class ItemClass {
    PROGRESSION,
    USEFUL,
    TRAP,
    FILLER,
    ;

    companion object {
        /** Returns the class of an item with [flags]; progression wins over useful over trap. */
        fun of(flags: Int): ItemClass = when {
            flags and ItemFlags.PROGRESSION != 0 -> PROGRESSION
            flags and ItemFlags.USEFUL != 0 -> USEFUL
            flags and ItemFlags.TRAP != 0 -> TRAP
            else -> FILLER
        }
    }
}

/** How a notification is shown. */
enum class NotifyMode { OFF, SILENT, SOUND }

/** Which events of a slot notify the user. */
@Serializable
data class SlotNotifications(
    val progression: NotifyMode = NotifyMode.SOUND,
    val useful: NotifyMode = NotifyMode.SILENT,
    val filler: NotifyMode = NotifyMode.OFF,
    val trap: NotifyMode = NotifyMode.SOUND,
    /** Also notify for items the slot found in its own world. */
    val ownWorld: Boolean = false,
    /** Notify when someone hints an item for this slot. */
    val hints: Boolean = true,
    /** Notify when the slot name is mentioned in the chat. */
    val chatMentions: Boolean = true,
) {
    fun mode(itemClass: ItemClass): NotifyMode = when (itemClass) {
        ItemClass.PROGRESSION -> progression
        ItemClass.USEFUL -> useful
        ItemClass.TRAP -> trap
        ItemClass.FILLER -> filler
    }

    fun withMode(itemClass: ItemClass, mode: NotifyMode): SlotNotifications = when (itemClass) {
        ItemClass.PROGRESSION -> copy(progression = mode)
        ItemClass.USEFUL -> copy(useful = mode)
        ItemClass.TRAP -> copy(trap = mode)
        ItemClass.FILLER -> copy(filler = mode)
    }
}

/** One of the user's slots in a [SavedMultiworld]. */
@Serializable
data class SavedSlot(
    val name: String,
    /** Whether items for and from this slot are highlighted in the feed. */
    val highlight: Boolean = true,
    val notifications: SlotNotifications = SlotNotifications(),
)

/**
 * A multiworld the user saved. [id] is stable across edits and links the multiworld to its stored
 * feed. The first slot receives the room's text.
 */
@Serializable
data class SavedMultiworld(
    val id: String,
    val label: String = "",
    val address: String,
    val roomPassword: String = "",
    val slots: List<SavedSlot> = emptyList(),
    /** Connect when the app starts. */
    val connectOnStart: Boolean = true,
    /** Keep the connection while the app is in the background. */
    val keepInBackground: Boolean = true,
    /** Days to keep the feed, [RETENTION_UNLIMITED] to keep it forever. */
    val feedRetentionDays: Int = DEFAULT_FEED_RETENTION_DAYS,
    /** Id of the newest stored feed message the user has seen. */
    val lastSeenFeedId: Long = 0,
) {
    /** The label, or the address if the multiworld has no label. */
    val displayName: String get() = label.ifBlank { address }

    /** Returns the connection settings of this multiworld for a session. */
    fun toConfig(): MultiworldConfig = MultiworldConfig(
        address = address,
        roomPassword = roomPassword,
        slots = slots.map {
            SlotConfig(name = it.name, highlight = it.highlight)
        },
    )

    companion object {
        const val DEFAULT_FEED_RETENTION_DAYS = 7
        const val RETENTION_UNLIMITED = 0
    }
}

/** Layout of feed rows. */
enum class FeedLayout {
    /** One line of monospaced text per message, like the Archipelago text client. */
    CLASSIC,

    /** The item first, sender, receiver and location below. */
    STRUCTURED,
}

/** App-wide settings. */
@Serializable
data class AppSettings(
    /** Master switch: when off, no multiworld is connected. */
    val connectionsEnabled: Boolean = true,
    val feedLayout: FeedLayout = FeedLayout.CLASSIC,
)

/** Everything the user configured: saved multiworlds and settings. */
@Serializable
data class Profiles(
    val multiworlds: List<SavedMultiworld> = emptyList(),
    val settings: AppSettings = AppSettings(),
) {
    fun multiworld(id: String): SavedMultiworld? = multiworlds.firstOrNull { it.id == id }

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
