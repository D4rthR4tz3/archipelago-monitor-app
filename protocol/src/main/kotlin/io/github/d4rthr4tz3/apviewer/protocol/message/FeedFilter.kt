package io.github.d4rthr4tz3.apviewer.protocol.message

import io.github.d4rthr4tz3.apviewer.protocol.store.ItemClass

/** Quick filters of the feed. */
enum class FeedFilter {
    ALL,

    /** Items for one of the user's highlighted slots. */
    FOR_ME,

    /** Items one of the user's highlighted slots sent to someone else. */
    FROM_ME,

    /** Everything that is not about an item: chat, joins, goals and server messages. */
    CHAT,
}

/**
 * Returns whether this message passes [filter] and, if it is about an item, whether the item's
 * class is one of [itemClasses].
 */
fun FeedMessage.matches(filter: FeedFilter, itemClasses: Set<ItemClass>): Boolean {
    val item = item
    if (item != null && ItemClass.of(item.flags) !in itemClasses) return false
    return when (filter) {
        FeedFilter.ALL -> true
        FeedFilter.FOR_ME -> highlight == FeedHighlight.FOR_ME
        FeedFilter.FROM_ME -> highlight == FeedHighlight.FROM_ME
        FeedFilter.CHAT -> item == null
    }
}

/** The parts of an item message, for layouts that show the item first. */
data class ItemParts(
    val item: MessageSegment,
    val sender: MessageSegment,
    /** `null` if the sender found the item in its own world. */
    val receiver: MessageSegment?,
    val location: MessageSegment?,
)

/** Splits an item message into its parts, or returns `null` if it is not about an item. */
fun FeedMessage.itemParts(): ItemParts? {
    if (item == null) return null
    val itemSegment = segments.firstOrNull { it.kind == SegmentKind.ITEM } ?: return null
    val players = segments.filter {
        it.kind == SegmentKind.PLAYER ||
            it.kind == SegmentKind.OWN_PLAYER
    }
    val sender = players.firstOrNull() ?: return null
    return ItemParts(
        item = itemSegment,
        sender = sender,
        receiver = players.getOrNull(1),
        location = segments.firstOrNull { it.kind == SegmentKind.LOCATION },
    )
}
