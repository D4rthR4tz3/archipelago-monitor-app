package io.github.d4rthr4tz3.apviewer.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import io.github.d4rthr4tz3.apviewer.R
import io.github.d4rthr4tz3.apviewer.ui.theme.ApColors
import kotlin.math.roundToInt
import kotlinx.coroutines.launch

/**
 * Swiping [content] from right to left reveals a delete action. Released beyond
 * [DELETE_THRESHOLD] of the width, it calls [onDelete], which should ask before deleting; the row
 * always slides back. Screen readers offer the same action as [deleteLabel].
 */
@Composable
fun SwipeToDelete(
    deleteLabel: String,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val scope = rememberCoroutineScope()
    val offset = remember { Animatable(0f) }
    var width by remember { mutableIntStateOf(0) }
    val armed = width > 0 && -offset.value >= width * DELETE_THRESHOLD
    Box(
        modifier = modifier
            .fillMaxWidth()
            .onSizeChanged { width = it.width }
            .semantics {
                customActions = listOf(
                    CustomAccessibilityAction(deleteLabel) {
                        onDelete()
                        true
                    },
                )
            },
    ) {
        if (offset.value < 0f) {
            Row(
                modifier = Modifier
                    .matchParentSize()
                    .background(if (armed) ApColors.Error else ApColors.ErrorBorder)
                    .padding(end = 24.dp),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_delete),
                    contentDescription = null,
                    tint = if (armed) ApColors.Background else ApColors.Error,
                )
            }
        }
        Box(
            modifier = Modifier
                .offset { IntOffset(offset.value.roundToInt(), 0) }
                .background(ApColors.Background)
                .draggable(
                    orientation = Orientation.Horizontal,
                    state = rememberDraggableState { delta ->
                        scope.launch {
                            offset.snapTo((offset.value + delta).coerceIn(-width.toFloat(), 0f))
                        }
                    },
                    onDragStopped = {
                        if (width > 0 && -offset.value >= width * DELETE_THRESHOLD) onDelete()
                        offset.animateTo(0f)
                    },
                ),
        ) {
            content()
        }
    }
}

/** Share of the width a row must be swiped to delete it. */
private const val DELETE_THRESHOLD = 0.4f
