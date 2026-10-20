package io.github.d4rthr4tz3.apviewer.ui.feed

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.d4rthr4tz3.apviewer.R
import io.github.d4rthr4tz3.apviewer.protocol.message.FeedHighlight
import io.github.d4rthr4tz3.apviewer.protocol.message.FeedMessage
import io.github.d4rthr4tz3.apviewer.protocol.message.MessageSegment
import io.github.d4rthr4tz3.apviewer.protocol.message.SegmentKind
import io.github.d4rthr4tz3.apviewer.protocol.message.hintParts
import io.github.d4rthr4tz3.apviewer.protocol.message.itemParts
import io.github.d4rthr4tz3.apviewer.protocol.monitor.FeedEntry
import io.github.d4rthr4tz3.apviewer.protocol.store.ItemClass
import io.github.d4rthr4tz3.apviewer.ui.formatClock
import io.github.d4rthr4tz3.apviewer.ui.formatClockSeconds
import io.github.d4rthr4tz3.apviewer.ui.theme.ApColors
import io.github.d4rthr4tz3.apviewer.ui.theme.ContentColors
import io.github.d4rthr4tz3.apviewer.ui.theme.FeedMonoStyle
import io.github.d4rthr4tz3.apviewer.ui.theme.GeistMono

private val RowShape = RoundedCornerShape(6.dp)

/** Color and weight of an item name of [itemClass]. */
data class ItemClassStyle(val color: Color, val weight: FontWeight)

fun itemClassStyle(itemClass: ItemClass): ItemClassStyle = when (itemClass) {
    ItemClass.PROGRESSION -> ItemClassStyle(ContentColors.Progression, FontWeight.SemiBold)
    ItemClass.USEFUL -> ItemClassStyle(ContentColors.Useful, FontWeight.Normal)
    ItemClass.TRAP -> ItemClassStyle(ContentColors.Trap, FontWeight.SemiBold)
    ItemClass.FILLER -> ItemClassStyle(ContentColors.Filler, FontWeight.Normal)
}

/** Only items for the user's slots are highlighted; hints carry an icon instead. */
private fun FeedMessage.background(): Color =
    if (highlight == FeedHighlight.FOR_ME && hintParts() == null) {
        ContentColors.HighlightForMe
    } else {
        Color.Transparent
    }

private fun FeedMessage.isChat(): Boolean = type == "Chat" || type == "ServerChat"

/** One monospaced line per message, like the Archipelago text client. */
@Composable
fun ClassicFeedRow(entry: FeedEntry, modifier: Modifier = Modifier) {
    Text(
        text = entry.toAnnotatedString(),
        modifier = modifier
            .fillMaxWidth()
            .background(entry.message.background(), RowShape)
            .padding(horizontal = 8.dp, vertical = 6.dp),
        style = FeedMonoStyle,
    )
}

/**
 * Item first, then sender and receiver, then the location; hints alike; other messages as text.
 */
@Composable
fun StructuredFeedRow(entry: FeedEntry, modifier: Modifier = Modifier) {
    val message = entry.message
    val hint = message.hintParts()
    val parts = if (hint == null) message.itemParts() else null
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(message.background(), RowShape)
            .padding(8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        // On the first line of the message, whatever its text size.
        Column(modifier = Modifier.width(42.dp).alignByBaseline()) {
            Text(
                text = if (entry.receivedAtMillis > 0) formatClock(entry.receivedAtMillis) else "",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontFamily = GeistMono,
                    fontSize = 11.sp,
                    lineHeight = 18.sp,
                ),
                color = ApColors.TextTertiary,
            )
            if (hint != null) {
                // Centered on the lines below the item: "Hint for ..." and the location.
                Icon(
                    painter = painterResource(R.drawable.ic_info),
                    contentDescription = stringResource(R.string.feed_hint),
                    modifier = Modifier
                        .padding(top = if (hint.location != null) 12.dp else 3.dp)
                        .size(14.dp),
                    tint = ApColors.TextTertiary,
                )
            }
        }
        Column(
            modifier = Modifier.alignByBaseline(),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            when {
                hint != null -> {
                    ItemHeadline(hint.item)
                    val hintFor = stringResource(R.string.feed_hint_for)
                    val at = stringResource(R.string.feed_hint_at)
                    DetailLine(
                        buildAnnotatedString {
                            append(hintFor)
                            appendSegment(hint.receiver)
                            hint.finder?.let {
                                append(at)
                                appendSegment(it)
                            }
                            hint.status?.let {
                                append(' ')
                                appendSegment(it)
                            }
                        },
                    )
                    hint.location?.let { LocationLine(it) }
                }

                parts != null -> {
                    ItemHeadline(parts.item)
                    val receiver = parts.receiver
                    val to = stringResource(R.string.feed_to)
                    val themselves = stringResource(R.string.feed_themselves)
                    DetailLine(
                        buildAnnotatedString {
                            appendSegment(parts.sender)
                            append(to)
                            if (receiver != null) appendSegment(receiver) else append(themselves)
                        },
                    )
                    parts.location?.let { LocationLine(it) }
                }

                else -> Text(
                    text = message.segments.toAnnotatedString(baseColor(message)),
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
    }
}

@Composable
private fun ItemHeadline(item: MessageSegment) {
    val style = itemClassStyle(ItemClass.of(item.itemFlags))
    Text(
        text = item.text,
        style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 18.sp),
        color = style.color,
        fontWeight = style.weight,
    )
}

/** The location on a line of its own, as location names are often long. */
@Composable
private fun LocationLine(location: MessageSegment) {
    DetailLine(buildAnnotatedString { appendSegment(location) })
}

@Composable
private fun DetailLine(text: AnnotatedString) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelSmall.copy(lineHeight = 16.sp),
        color = ApColors.TextTertiary,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
    )
}

private fun FeedEntry.toAnnotatedString(): AnnotatedString = buildAnnotatedString {
    if (receivedAtMillis > 0) {
        withStyle(SpanStyle(color = ApColors.TextTertiary)) {
            append(formatClockSeconds(receivedAtMillis))
            append(' ')
        }
    }
    append(message.segments.toAnnotatedString(baseColor(message)))
}

private fun baseColor(message: FeedMessage): Color =
    if (message.isChat()) ApColors.TextPrimary else ApColors.TextSecondary

private fun List<MessageSegment>.toAnnotatedString(baseColor: Color): AnnotatedString =
    buildAnnotatedString {
        withStyle(SpanStyle(color = baseColor)) {
            forEach { appendSegment(it) }
        }
    }

private fun AnnotatedString.Builder.appendSegment(segment: MessageSegment) {
    val style = segment.style()
    if (style == null) {
        append(segment.text)
    } else {
        withStyle(style) { append(segment.text) }
    }
}

private fun MessageSegment.style(): SpanStyle? = when (kind) {
    SegmentKind.TEXT, SegmentKind.HINT_STATUS -> null

    SegmentKind.OWN_PLAYER -> SpanStyle(color = ContentColors.OwnSlot)

    SegmentKind.PLAYER -> SpanStyle(color = ContentColors.OtherPlayer)

    SegmentKind.LOCATION -> SpanStyle(color = ContentColors.Location)

    SegmentKind.ENTRANCE -> SpanStyle(color = ContentColors.Entrance)

    SegmentKind.ITEM -> itemClassStyle(ItemClass.of(itemFlags)).let {
        SpanStyle(color = it.color, fontWeight = it.weight)
    }
}
