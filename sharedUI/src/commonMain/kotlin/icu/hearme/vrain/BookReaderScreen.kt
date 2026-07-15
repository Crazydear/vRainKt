package icu.hearme.vrain

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import icu.hearme.vrain.bookcanvas.BookPageCanvas
import icu.hearme.vrain.configure.AncientBookState
import icu.hearme.vrain.configure.AncientCanvasState
import icu.hearme.vrain.configure.PageSplitConfig
import icu.hearme.vrain.engine.BookGrid
import icu.hearme.vrain.engine.BookPage

@Composable
fun BookReaderScreen(
    pages: List<BookPage>,
    grid: BookGrid,
    bookConfig: AncientBookState,
    canvasConfig: AncientCanvasState,
    psConfig: PageSplitConfig
) {
    // 创建 Pager 状态
    val pagerState = rememberPagerState(pageCount = { pages.size })

    Column(modifier = Modifier.fillMaxSize()) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.weight(1f)
        ) { pageIndex ->
            // 这里渲染每一页
            BookPageCanvas(pages[pageIndex], grid, bookConfig, canvasConfig, psConfig)
        }

        // 可选：添加分页指示器
        Text(
            text = "第 ${pagerState.currentPage + 1} / ${pages.size} 页",
            modifier = Modifier.padding(8.dp)
        )
    }
}