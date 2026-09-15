package icu.hearme.vrain.utils

import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.unit.dp
import icu.hearme.vrain.bookcanvas.BookPageCanvas
import icu.hearme.vrain.bookcanvas.FileInfo
import icu.hearme.vrain.configure.AncientBookState
import icu.hearme.vrain.configure.AncientCanvasState
import icu.hearme.vrain.configure.PageSplitConfig
import icu.hearme.vrain.configure.isDesktopPlatform
import icu.hearme.vrain.engine.BookGridEngine
import icu.hearme.vrain.engine.BookPage
import io.github.bigboyapps.kmpdf.PageSize
import io.github.bigboyapps.kmpdf.PdfConfig
import io.github.bigboyapps.kmpdf.PdfResult
import io.github.bigboyapps.kmpdf.createKmPdfGenerator
import io.github.bigboyapps.kmpdf.sharePdf

object ExportPdf {
    val generator = createKmPdfGenerator()

    suspend fun createPdf(
        pages: List<BookPage>, bookConfig: AncientBookState, canvasConfig: AncientCanvasState,
        isSplite: Boolean = false, onProgress: (current: Int, total: Int) -> Unit
    ) {
        if (isDesktopPlatform()) {
            exportPdf(pages, bookConfig, canvasConfig, isSplite, onProgress)
        } else {
            val totalPages = pages.size
            val result = generator.generatePdf(
                config = PdfConfig(
                    pageSize = PageSize(canvasConfig.canvasWidth.dp, canvasConfig.canvasHeight.dp),
                    fileName = bookConfig.title + ".pdf"
                )
            ) {
                val grid = BookGridEngine.calculateGrid(canvasConfig, bookConfig)
                pages.forEachIndexed { index, bookPage ->
                    page {
                        val psConfig by remember { mutableStateOf(PageSplitConfig(index)) }
                        BookPageCanvas(bookPage, grid, bookConfig, canvasConfig, psConfig)
                        SideEffect {
                            onProgress(index + 1, totalPages)
                        }
                    }
                }
            }
            when (result) {
                is PdfResult.Success -> {
                    println("PDF: ${result.filePath}")
                    println("${result.pageCount} pages, ${result.fileSize} bytes")
                    sharePdf(result.uri)
                }
                is PdfResult.Error -> {
                    println("Error: ${result.message}")
                }
            }
        }
    }

    suspend fun exportAllPdf(
        textSources: List<FileInfo>, bookConfig: AncientBookState, canvasConfig: AncientCanvasState,
        isSplite: Boolean = false, onProgress: (file: String, current: Int, total: Int) -> Unit
    ) {
        exportPdf( textSources, bookConfig, canvasConfig, isSplite, onProgress)
    }
}

expect suspend fun exportPdf(
    pages: List<BookPage>, bookConfig: AncientBookState, canvasConfig: AncientCanvasState,
    isSplite: Boolean = false, onProgress: (current: Int, total: Int) -> Unit
)

/** 将文件列表导出一个PDF文件 */
expect suspend fun exportPdf(
    textSources: List<FileInfo>, bookConfig: AncientBookState, canvasConfig: AncientCanvasState,
    isSplite: Boolean = false, onProgress: (file: String, current: Int, total: Int) -> Unit
)

/** 将PDF页面转换成预览图片 */
expect suspend fun preViewPdfPage(page: BookPage, bookConfig: AncientBookState, canvasConfig: AncientCanvasState): ImageBitmap?