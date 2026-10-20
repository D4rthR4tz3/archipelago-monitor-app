package io.github.d4rthr4tz3.apviewer.protocol.message

import io.github.d4rthr4tz3.apviewer.protocol.model.ItemFlags
import io.github.d4rthr4tz3.apviewer.protocol.model.NetworkItem
import io.github.d4rthr4tz3.apviewer.protocol.store.ItemClass
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class FeedFilterTest {
    private val all = ItemClass.entries.toSet()

    private val sent = FeedMessage(
        type = "ItemSend",
        segments = listOf(
            MessageSegment("Samus", SegmentKind.PLAYER),
            MessageSegment(" sent "),
            MessageSegment("Hookshot", SegmentKind.ITEM, ItemFlags.PROGRESSION),
            MessageSegment(" to "),
            MessageSegment("Link", SegmentKind.OWN_PLAYER),
            MessageSegment(" ("),
            MessageSegment("Link's House", SegmentKind.LOCATION),
            MessageSegment(")"),
        ),
        receivingSlot = 1,
        item = NetworkItem(item = 1, location = 2, player = 2, flags = ItemFlags.PROGRESSION),
        highlight = FeedHighlight.FOR_ME,
    )

    private val chat = FeedMessage(type = "Chat", segments = listOf(MessageSegment("Samus: hi")))

    @Test
    fun `quick filters select by highlight or by being chat`() {
        assertTrue(sent.matches(FeedFilter.ALL, all))
        assertTrue(sent.matches(FeedFilter.FOR_ME, all))
        assertFalse(sent.matches(FeedFilter.FROM_ME, all))
        assertFalse(sent.matches(FeedFilter.CHAT, all))
        assertTrue(chat.matches(FeedFilter.CHAT, all))
        assertFalse(chat.matches(FeedFilter.FOR_ME, all))
    }

    @Test
    fun `item classes filter only item messages`() {
        val noProgression = all - ItemClass.PROGRESSION

        assertFalse(sent.matches(FeedFilter.ALL, noProgression))
        assertTrue(chat.matches(FeedFilter.ALL, emptySet()))
    }

    @Test
    fun `splits item messages into their parts`() {
        val parts = sent.itemParts()!!

        assertEquals("Hookshot", parts.item.text)
        assertEquals("Samus", parts.sender.text)
        assertEquals("Link", parts.receiver?.text)
        assertEquals("Link's House", parts.location?.text)
        assertNull(chat.itemParts())
    }

    @Test
    fun `a found item has no receiver`() {
        val found = sent.copy(
            type = "ItemSend",
            segments = listOf(
                MessageSegment("Link", SegmentKind.OWN_PLAYER),
                MessageSegment(" found their "),
                MessageSegment("Bow", SegmentKind.ITEM, ItemFlags.PROGRESSION),
            ),
        )

        assertEquals("Link", found.itemParts()?.sender?.text)
        assertNull(found.itemParts()?.receiver)
    }

    @Test
    fun `splits hint messages into their parts`() {
        val hint = sent.copy(
            type = "Hint",
            segments = listOf(
                MessageSegment("Link", SegmentKind.OWN_PLAYER),
                MessageSegment("'s "),
                MessageSegment("Glove", SegmentKind.ITEM, ItemFlags.PROGRESSION),
                MessageSegment(" is at "),
                MessageSegment("Kakariko Well", SegmentKind.LOCATION),
                MessageSegment(" in "),
                MessageSegment("Samus", SegmentKind.PLAYER),
                MessageSegment("'s World. "),
                MessageSegment("(priority)", SegmentKind.HINT_STATUS),
            ),
        )

        val parts = hint.hintParts()!!
        assertEquals("Glove", parts.item.text)
        assertEquals("Link", parts.receiver.text)
        assertEquals("Samus", parts.finder?.text)
        assertEquals("Kakariko Well", parts.location?.text)
        assertEquals("(priority)", parts.status?.text)
        assertNull(hint.itemParts())
        assertNull(sent.hintParts())
    }
}
