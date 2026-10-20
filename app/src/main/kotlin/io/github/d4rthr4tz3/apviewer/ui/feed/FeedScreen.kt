package io.github.d4rthr4tz3.apviewer.ui.feed

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import io.github.d4rthr4tz3.apviewer.R
import io.github.d4rthr4tz3.apviewer.protocol.message.FeedMessage
import io.github.d4rthr4tz3.apviewer.protocol.message.MessageSegment
import io.github.d4rthr4tz3.apviewer.protocol.message.SegmentKind
import io.github.d4rthr4tz3.apviewer.protocol.model.ItemFlags
import io.github.d4rthr4tz3.apviewer.protocol.monitor.FeedEntry
import io.github.d4rthr4tz3.apviewer.protocol.monitor.MonitorState
import io.github.d4rthr4tz3.apviewer.ui.theme.ApViewerTheme
import io.github.d4rthr4tz3.apviewer.ui.theme.FeedColors
import io.github.d4rthr4tz3.apviewer.ui.theme.feedColors

/** Live message feed of a connected slot. Scrolls to new messages as they arrive. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FeedScreen(
    state: MonitorState.Connected,
    feed: List<FeedEntry>,
    onDisconnect: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val listState = rememberLazyListState()
    val colors = feedColors()

    LaunchedEffect(feed.lastOrNull()?.id) {
        if (feed.isNotEmpty()) {
            listState.animateScrollToItem(feed.lastIndex)
        }
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(state.playerName)
                        Text(
                            text = stringResource(
                                R.string.feed_seed,
                                state.seedName,
                                state.serverVersion,
                            ),
                            style = MaterialTheme.typography.labelMedium,
                        )
                    }
                },
                actions = {
                    TextButton(onClick = onDisconnect) {
                        Text(stringResource(R.string.feed_disconnect))
                    }
                },
            )
        },
    ) { innerPadding ->
        if (feed.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center,
            ) {
                Text(stringResource(R.string.feed_empty))
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                state = listState,
                contentPadding = innerPadding,
            ) {
                items(feed, key = { it.id }) { entry ->
                    FeedRow(
                        message = entry.message,
                        colors = colors,
                        modifier = Modifier.padding(horizontal = 16.dp),
                    )
                    HorizontalDivider()
                }
            }
        }
    }
}

@Composable
private fun FeedRow(message: FeedMessage, colors: FeedColors, modifier: Modifier = Modifier) {
    Text(
        text = message.toAnnotatedString(colors),
        modifier = modifier.padding(vertical = 8.dp),
        style = MaterialTheme.typography.bodyMedium,
    )
}

private fun FeedMessage.toAnnotatedString(colors: FeedColors): AnnotatedString =
    buildAnnotatedString {
        segments.forEach { segment ->
            val style = segment.style(colors)
            if (style == null) {
                append(segment.text)
            } else {
                withStyle(style) { append(segment.text) }
            }
        }
    }

private fun MessageSegment.style(colors: FeedColors): SpanStyle? = when (kind) {
    SegmentKind.TEXT, SegmentKind.HINT_STATUS -> null
    SegmentKind.OWN_PLAYER -> SpanStyle(color = colors.ownPlayer, fontWeight = FontWeight.Bold)
    SegmentKind.PLAYER -> SpanStyle(color = colors.player, fontWeight = FontWeight.Bold)
    SegmentKind.LOCATION -> SpanStyle(color = colors.location)
    SegmentKind.ENTRANCE -> SpanStyle(color = colors.entrance)
    SegmentKind.ITEM -> SpanStyle(color = itemColor(colors))
}

private fun MessageSegment.itemColor(colors: FeedColors) = when {
    itemFlags and ItemFlags.PROGRESSION != 0 -> colors.progressionItem
    itemFlags and ItemFlags.USEFUL != 0 -> colors.usefulItem
    itemFlags and ItemFlags.TRAP != 0 -> colors.trapItem
    else -> colors.item
}

@Preview(showBackground = true)
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
                    ),
                ),
            ),
            onDisconnect = {},
        )
    }
}
