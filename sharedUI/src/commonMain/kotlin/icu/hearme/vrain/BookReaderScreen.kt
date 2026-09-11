package icu.hearme.vrain

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.Button
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.Text
import androidx.compose.material3.TooltipAnchorPosition
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import icu.hearme.vrain.bookcanvas.BookPageCanvas
import icu.hearme.vrain.bookcanvas.PdfPagePreviewer
import icu.hearme.vrain.configure.AncientBookState
import icu.hearme.vrain.configure.AncientCanvasState
import icu.hearme.vrain.configure.PageSplitConfig
import icu.hearme.vrain.engine.BookGrid
import icu.hearme.vrain.engine.BookPage
import kotlinx.coroutines.launch

@Composable
fun BookReaderScreen(pages: List<BookPage>, grid: BookGrid, bookConfig: AncientBookState, canvasConfig: AncientCanvasState) {
    val pagerState = rememberPagerState(pageCount = { pages.size })
    var isPdfPre by remember { mutableStateOf(false) }
    val preTip by produceState("", isPdfPre) {
        value = if (isPdfPre) "切换原生预览" else "切换PDF预览"
    }
    var isCover by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()
    val prePage = {
        coroutineScope.launch {
            if (pagerState.currentPage > 0) { pagerState.animateScrollToPage(pagerState.currentPage - 1) }
        }
    }
    val nextPage = {
        coroutineScope.launch {
            if (pagerState.currentPage < pages.size - 1) { pagerState.animateScrollToPage(pagerState.currentPage + 1) }
        }
    }
    Column(modifier = Modifier.fillMaxSize()
        .pointerInput(Unit) {
            awaitPointerEventScope {
                while (true) {
                    val event = awaitPointerEvent()
                    if (event.type == PointerEventType.Scroll) {
                        val scrollDelta = event.changes.first().scrollDelta
                        event.changes.first().consume()
                        if (scrollDelta.y > 0) { nextPage() } else if (scrollDelta.y < 0) { prePage() }
                    }
                }
            }
        }
    ) {
        if (pagerState.pageCount != 0){
            HorizontalPager(pagerState, Modifier.weight(1f), reverseLayout = true) { pageIndex ->
                if (isPdfPre) {
                    PdfPagePreviewer(pages[pageIndex].let { if (isCover) it.copy(pageIndex = 0) else it }, bookConfig, canvasConfig)
                } else {
                    val psConfig by remember { mutableStateOf(PageSplitConfig(pageIndex)) }
                    BookPageCanvas(pages[pageIndex], grid, bookConfig, canvasConfig, psConfig)
                }
            }
        } else {
            BookPageCanvas(BookPage(0, emptyList()), grid, bookConfig, canvasConfig, PageSplitConfig(0))
        }

        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Button(onClick = { prePage() }, enabled = pagerState.currentPage > 0) {
                Text("上一页")
            }
            TooltipBox(
                positionProvider = TooltipDefaults.rememberTooltipPositionProvider(TooltipAnchorPosition.Above),
                tooltip = { PlainTooltip { Text(preTip) } },
                state = rememberTooltipState()
            ) {
                Text("第 ${pagerState.currentPage + 1} / ${pages.size} 页", modifier = Modifier.clickable { isPdfPre = !isPdfPre })
            }
            Text("预览封面", modifier = Modifier.clickable { isCover = !isCover })
            Button(onClick = { nextPage() }, enabled = pagerState.currentPage < pages.size - 1) {
                Text("下一页")
            }
        }
    }
}
