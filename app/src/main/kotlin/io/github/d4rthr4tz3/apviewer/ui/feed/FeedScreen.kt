package io.github.d4rthr4tz3.apviewer.ui.feed

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import io.github.d4rthr4tz3.apviewer.R
import io.github.d4rthr4tz3.apviewer.protocol.message.FeedHighlight
import io.github.d4rthr4tz3.apviewer.protocol.message.FeedMessage
import io.github.d4rthr4tz3.apviewer.protocol.message.MessageSegment
import io.github.d4rthr4tz3.apviewer.protocol.message.SegmentKind
import io.github.d4rthr4tz3.apviewer.protocol.model.ItemFlags
import io.github.d4rthr4tz3.apviewer.protocol.monitor.FeedEntry
import io.github.d4rthr4tz3.apviewer.protocol.monitor.MonitorState
import io.github.d4rthr4tz3.apviewer.ui.theme.ApColors
import io.github.d4rthr4tz3.apviewer.ui.theme.ApViewerTheme
import io.github.d4rthr4tz3.apviewer.ui.theme.ContentColors
import io.github.d4rthr4tz3.apviewer.ui.theme.FeedMonoStyle
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private val TimeFormat = DateTimeFormatter.ofPattern("HH:mm:ss").withZone(ZoneId.systemDefault())

private val RowShape = RoundedCornerShape(6.dp)

/** Classic feed of a connected slot. Scrolls to new messages as they arrive. */
@Composable
fun FeedScreen(
    state: MonitorState.Connected,
    feed: List<FeedEntry>,
    onDisconnect: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val listState = rememberLazyListState()

    LaunchedEffect(feed.lastOrNull()?.id) {
        if (feed.isNotEmpty()) {
            listState.animateScrollToItem(feed.lastIndex)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(ApColors.Background)
            .safeDrawingPadding(),
    ) {
        FeedHeader(state = state, onDisconnect = onDisconnect)
        HorizontalDivider(color = ApColors.Divider)
        if (feed.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    text = stringResource(R.string.feed_empty),
                    color = ApColors.TextTertiary,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                state = listState,
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                items(feed, key = { it.id }) { entry -> FeedRow(entry) }
            }
        }
    }
}

@Composable
private fun FeedHeader(state: MonitorState.Connected, onDisconnect: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .padding(start = 16.dp, end = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = state.playerName,
                color = ApColors.TextPrimary,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = stringResource(R.string.feed_seed, state.seedName, state.serverVersion),
                color = ApColors.TextSecondary,
                style = MaterialTheme.typography.labelSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        TextButton(onClick = onDisconnect) {
            Text(
                text = stringResource(R.string.feed_disconnect),
                color = ApColors.TextSecondary,
            )
        }
    }
}

@Composable
private fun FeedRow(entry: FeedEntry) {
    val background = when (entry.message.highlight) {
        FeedHighlight.FOR_ME -> ContentColors.HighlightForMe
        FeedHighlight.FROM_ME -> ContentColors.HighlightFromMe
        FeedHighlight.NONE -> Color.Transparent
    }
    Text(
        text = entry.toAnnotatedString(),
        modifier = Modifier
            .fillMaxWidth()
            .background(background, RowShape)
            .padding(horizontal = 8.dp, vertical = 6.dp),
        style = FeedMonoStyle,
    )
}

private fun FeedEntry.toAnnotatedString(): AnnotatedString = buildAnnotatedString {
    if (receivedAtMillis > 0) {
        withStyle(SpanStyle(color = ApColors.TextTertiary)) {
            append(TimeFormat.format(Instant.ofEpochMilli(receivedAtMillis)))
            append(' ')
        }
    }
    val baseColor = when (message.type) {
        "Chat", "ServerChat" -> ApColors.TextPrimary
        else -> ApColors.TextSecondary
    }
    withStyle(SpanStyle(color = baseColor)) {
        message.segments.forEach { segment ->
            val style = segment.style()
            if (style == null) {
                append(segment.text)
            } else {
                withStyle(style) { append(segment.text) }
            }
        }
    }
}

private fun MessageSegment.style(): SpanStyle? = when (kind) {
    SegmentKind.TEXT, SegmentKind.HINT_STATUS -> null
    SegmentKind.OWN_PLAYER -> SpanStyle(color = ContentColors.OwnSlot)
    SegmentKind.PLAYER -> SpanStyle(color = ContentColors.OtherPlayer)
    SegmentKind.LOCATION -> SpanStyle(color = ContentColors.Location)
    SegmentKind.ENTRANCE -> SpanStyle(color = ContentColors.Entrance)
    SegmentKind.ITEM -> itemStyle()
}

private fun MessageSegment.itemStyle(): SpanStyle = when {
    itemFlags and ItemFlags.PROGRESSION != 0 -> SpanStyle(
        color = ContentColors.Progression,
        fontWeight = FontWeight.SemiBold,
    )

    itemFlags and ItemFlags.USEFUL != 0 -> SpanStyle(color = ContentColors.Useful)

    itemFlags and ItemFlags.TRAP != 0 -> SpanStyle(
        color = ContentColors.Trap,
        fontWeight = FontWeight.SemiBold,
    )

    else -> SpanStyle(color = ContentColors.Filler)
}

@Preview(backgroundColor = 0xFF141414, showBackground = true)
@Composable
private fun FeedScreenPreview() {
    ApViewerTheme {
        FeedScreen(
            state = MonitorState.Connected(
                url = "wss://archipelago.gg:38281",
                seedName = "12345",
                serverVersion = "0.6.1",
                team = 0,
                slot = 1,
                playerName = "Link",
            ),
            feed = listOf(
                FeedEntry(
                    id = 0,
                    receivedAtMillis = 1_760_000_000_000,
                    message = FeedMessage(
                        type = "ItemSend",
                        segments = listOf(
                            MessageSegment("Samus", SegmentKind.PLAYER),
                            MessageSegment(" sent "),
                            MessageSegment("Hookshot", SegmentKind.ITEM, ItemFlags.PROGRESSION),
                            MessageSegment(" to "),
                            MessageSegment("Link", SegmentKind.OWN_PLAYER),
                            MessageSegment(" ("),
                            MessageSegment("Morph Ball (Brinstar)", SegmentKind.LOCATION),
                            MessageSegment(")"),
                        ),
                        highlight = FeedHighlight.FOR_ME,
                    ),
                ),
                FeedEntry(
                    id = 1,
                    receivedAtMillis = 1_760_000_004_000,
                    message = FeedMessage(
                        type = "Chat",
                        segments = listOf(MessageSegment("Samus: gg")),
                    ),
                ),
            ),
            onDisconnect = {},
        )
    }
}
