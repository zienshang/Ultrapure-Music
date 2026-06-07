package com.ultrapuremusic.core.ui.components

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import kotlin.math.roundToInt

// ── State holder ─────────────────────────────────────────────────────────────

class DragDropState(
    val lazyListState: LazyListState,
    private val onSwap: (Int, Int) -> Unit,
) {
    var draggingIndex by mutableIntStateOf(-1)
        private set
    var dragOffsetY by mutableFloatStateOf(0f)
        private set
    private var accumulatedOffset by mutableFloatStateOf(0f)

    fun onDragStart(index: Int) {
        draggingIndex    = index
        dragOffsetY      = 0f
        accumulatedOffset = 0f
    }

    fun onDrag(deltaY: Float) {
        accumulatedOffset += deltaY
        dragOffsetY       = accumulatedOffset

        // Determine target index from the drag offset vs item height
        val visibleItems = lazyListState.layoutInfo.visibleItemsInfo
        val dragged      = visibleItems.firstOrNull { it.index == draggingIndex } ?: return
        val itemHeight   = dragged.size.toFloat()

        val targetIndex = (draggingIndex + (accumulatedOffset / itemHeight).roundToInt())
            .coerceIn(0, lazyListState.layoutInfo.totalItemsCount - 1)

        if (targetIndex != draggingIndex) {
            onSwap(draggingIndex, targetIndex)
            accumulatedOffset -= (targetIndex - draggingIndex) * itemHeight
            dragOffsetY       = accumulatedOffset
            draggingIndex     = targetIndex
        }
    }

    fun onDragEnd() {
        draggingIndex    = -1
        dragOffsetY      = 0f
        accumulatedOffset = 0f
    }
}

@Composable
fun rememberDragDropState(
    lazyListState: LazyListState,
    onSwap: (Int, Int) -> Unit,
): DragDropState = remember(lazyListState) {
    DragDropState(lazyListState = lazyListState, onSwap = onSwap)
}

// ── Modifier extension ────────────────────────────────────────────────────────

fun Modifier.dragHandle(
    index: Int,
    dragDropState: DragDropState,
): Modifier = this.pointerInput(index) {
    detectDragGesturesAfterLongPress(
        onDragStart = { dragDropState.onDragStart(index) },
        onDrag      = { _, offset -> dragDropState.onDrag(offset.y) },
        onDragEnd   = { dragDropState.onDragEnd() },
        onDragCancel = { dragDropState.onDragEnd() },
    )
}

// ── Wrapper composable ────────────────────────────────────────────────────────

/**
 * Wrap each LazyColumn item with this to get draggable elevation + translate.
 *
 * @param index          The item's current index in the list.
 * @param dragDropState  The shared [DragDropState].
 * @param content        The actual item UI.
 */
@Composable
fun DraggableItem(
    index: Int,
    dragDropState: DragDropState,
    modifier: Modifier = Modifier,
    content: @Composable (isDragging: Boolean) -> Unit,
) {
    val isDragging = index == dragDropState.draggingIndex
    val elevation  by animateDpAsState(
        targetValue = if (isDragging) 8.dp else 0.dp,
        label       = "drag_elevation",
    )

    Box(
        modifier = modifier
            .zIndex(if (isDragging) 1f else 0f)
            .shadow(elevation)
            .then(
                if (isDragging)
                    Modifier.offset { IntOffset(0, dragDropState.dragOffsetY.roundToInt()) }
                else Modifier
            ),
    ) {
        content(isDragging)
    }
}
