package io.github.d4rthr4tz3.apviewer.protocol.message

import io.github.d4rthr4tz3.apviewer.protocol.model.JsonMessagePart
import io.github.d4rthr4tz3.apviewer.protocol.packet.ServerPacket

/** Kinds of [MessageSegment], used by the UI to pick a color. */
enum class SegmentKind {
    TEXT,
    PLAYER,
    OWN_PLAYER,
    ITEM,
    LOCATION,
    ENTRANCE,
    HINT_STATUS,
}

/** A piece of a rendered message. [itemFlags] is set for [SegmentKind.ITEM] only. */
data class MessageSegment(
    val text: String,
    val kind: SegmentKind = SegmentKind.TEXT,
    val itemFlags: Int = 0,
)

/** A rendered `PrintJSON` message. [type] is the server's message type, such as `ItemSend`. */
data class FeedMessage(val type: String?, val segments: List<MessageSegment>) {
    val plainText: String get() = segments.joinToString(separator = "") { it.text }
}

/** Turns `PrintJSON` packets into [FeedMessage]s with resolved names. */
class MessageRenderer(private val ownSlot: Int?) {
    /** Returns [packet] rendered with the names known to [names]. */
    fun render(packet: ServerPacket.PrintJson, names: NameResolver): FeedMessage = FeedMessage(
        type = packet.type,
        segments = packet.data.map { part -> renderPart(part, names) },
    )

    private fun renderPart(part: JsonMessagePart, names: NameResolver): MessageSegment {
        val text = part.text.orEmpty()
        return when (part.type) {
            "player_id" -> {
                val slot = text.toIntOrNull()
                if (slot == null) {
                    MessageSegment(text, SegmentKind.PLAYER)
                } else {
                    MessageSegment(names.playerName(slot), playerKind(slot))
                }
            }

            "player_name" -> MessageSegment(text, SegmentKind.PLAYER)

            "item_id" -> MessageSegment(
                text = resolveId(text, part.player) { id, slot -> names.itemName(id, slot) },
                kind = SegmentKind.ITEM,
                itemFlags = part.flags ?: 0,
            )

            "item_name" -> MessageSegment(text, SegmentKind.ITEM, part.flags ?: 0)

            "location_id" -> MessageSegment(
                text = resolveId(text, part.player) { id, slot -> names.locationName(id, slot) },
                kind = SegmentKind.LOCATION,
            )

            "location_name" -> MessageSegment(text, SegmentKind.LOCATION)

            "entrance_name" -> MessageSegment(text, SegmentKind.ENTRANCE)

            "hint_status" -> MessageSegment(text, SegmentKind.HINT_STATUS)

            else -> MessageSegment(text)
        }
    }

    private fun playerKind(slot: Int): SegmentKind =
        if (slot == ownSlot) SegmentKind.OWN_PLAYER else SegmentKind.PLAYER

    private inline fun resolveId(
        text: String,
        slot: Int?,
        resolve: (id: Long, slot: Int) -> String,
    ): String {
        val id = text.toLongOrNull() ?: return text
        return if (slot == null) text else resolve(id, slot)
    }
}
