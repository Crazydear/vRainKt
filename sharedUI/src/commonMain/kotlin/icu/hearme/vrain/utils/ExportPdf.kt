package icu.hearme.vrain.utils

import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import icu.hearme.vrain.bookcanvas.BookPageCanvas
import icu.hearme.vrain.configure.AncientBookState
import icu.hearme.vrain.configure.AncientCanvasState
import icu.hearme.vrain.configure.PageSplitConfig
import icu.hearme.vrain.engine.BookGrid
import icu.hearme.vrain.engine.BookPage
import io.github.bigboyapps.kmpdf.PageSize
import io.github.bigboyapps.kmpdf.PdfConfig
import io.github.bigboyapps.kmpdf.PdfResult
import io.github.bigboyapps.kmpdf.createKmPdfGenerator
import io.github.bigboyapps.kmpdf.sharePdf

object ExportPdf {
    val generator = createKmPdfGenerator()

    suspend fun createPdf(
        pages: List<BookPage>,
        grid: BookGrid,
        bookConfig: AncientBookState,
        canvasConfig: AncientCanvasState,
        onProgress: (current: Int, total: Int) -> Unit
    ){
        exportPdf(pages, bookConfig, canvasConfig, onProgress)
//        val totalPages = pages.size
//        val result = generator.generatePdf(
//            config = PdfConfig(
//                pageSize = PageSize(width = canvasConfig.widthDp, height = canvasConfig.heightDp),
//                fileName = bookConfig.title + ".pdf"
//            )
//        ) {
//            pages.forEachIndexed { index, bookPage ->
//                page {
//                    val psConfig by remember { mutableStateOf(PageSplitConfig(index)) }
//                    BookPageCanvas(bookPage, grid, bookConfig, canvasConfig, psConfig)
//                    SideEffect {
//                        onProgress(index + 1, totalPages)
//                    }
//                }
//            }
//        }
//
//        when (result) {
//            is PdfResult.Success -> {
//                println("PDF: ${result.filePath}")
//                println("${result.pageCount} pages, ${result.fileSize} bytes")
//                sharePdf(result.uri)
//            }
//            is PdfResult.Error -> {
//                println("Error: ${result.message}")
//            }
//        }
    }


}
expect suspend fun exportPdf(pages: List<BookPage>,
                             bookConfig: AncientBookState,
                             canvasConfig: AncientCanvasState,
                             onProgress: (current: Int, total: Int) -> Unit)