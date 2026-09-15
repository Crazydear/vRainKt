package icu.hearme.vrain.utils

import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

/**
 * 监听长按拖拽事件的 Modifier
 */
fun Modifier.dragContainer(dragDropState: DragDropState): Modifier {
    return this.pointerInput(dragDropState) {
        detectDragGesturesAfterLongPress(
            onDrag = { change, offset ->
                change.consume()
                dragDropState.onDrag(offset = offset)
            },
            onDragStart = { offset -> dragDropState.onDragStart(offset) },
            onDragEnd = { dragDropState.onDragInterrupted() },
            onDragCancel = { dragDropState.onDragInterrupted() }
        )
    }
}

@Composable
fun rememberDragDropState(lazyListState: LazyListState, onMove: (Int, Int) -> Boolean): DragDropState {
    val scope = rememberCoroutineScope()
    val currentOnMove by rememberUpdatedState(onMove)
    val state = remember(lazyListState) {
        DragDropState(lazyListState, { from, to -> currentOnMove(from, to) }, scope)
    }
    return state
}

class DragDropState(
    val state: LazyListState,
    private val onMove: (Int, Int) -> Boolean,
    private val scope: kotlinx.coroutines.CoroutineScope
) {
    var draggingItemIndex by mutableStateOf<Int?>(null)
        private set

    internal val draggingItemOffset: Float
        get() = draggingItemDraggedDelta + draggingItemInitialOffset

    private var draggingItemDraggedDelta by mutableStateOf(0f)
    private var draggingItemInitialOffset by mutableStateOf(0f)
    var isDragging by mutableStateOf(false)
        private set

    private var draggingJob: Job? = null

    internal fun onDragStart(offset: Offset) {
        state.layoutInfo.visibleItemsInfo
            .firstOrNull { item ->
                offset.y.toInt() in item.offset..(item.offset + item.size)
            }?.also {
                draggingItemIndex = it.index
                draggingItemInitialOffset = it.offset.toFloat()
                isDragging = true
            }
    }

    internal fun onDragInterrupted() {
        draggingItemIndex = null
        draggingItemDraggedDelta = 0f
        isDragging = false
        draggingJob?.cancel()
    }

    internal fun onDrag(offset: Offset) {
        draggingItemDraggedDelta += offset.y

        val draggingItem = draggingItemIndex ?: return
        val startOffset = draggingItemOffset
        val endOffset = startOffset + (state.layoutInfo.visibleItemsInfo.find { it.index == draggingItem }?.size ?: 0)

        val targetItem = state.layoutInfo.visibleItemsInfo.find { item ->
            val center = item.offset + item.size / 2f
            item.index != draggingItem && center in startOffset..endOffset
        }

        if (targetItem != null) {
            val targetIndex = targetItem.index
            if (onMove(draggingItem, targetIndex)) { draggingItemIndex = targetIndex }
        } else {
            val overscroll = when {
                draggingItemDraggedDelta > 0 -> (endOffset - state.layoutInfo.viewportEndOffset).coerceAtLeast(0f)
                draggingItemDraggedDelta < 0 -> (startOffset - state.layoutInfo.viewportStartOffset).coerceAtMost(0f)
                else -> 0f
            }
            if (overscroll != 0f) {
                draggingJob?.cancel()
                draggingJob = scope.launch { state.scrollBy(overscroll) }
            }
        }
    }
}