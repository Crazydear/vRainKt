package icu.hearme.vrain.utils

import icu.hearme.vrain.configure.AncientBookState
import icu.hearme.vrain.configure.AncientCanvasState
import icu.hearme.vrain.engine.BookPage
import io.github.bigboyapps.kmpdf.createKmPdfGenerator

object ExportPdf {
    val generator = createKmPdfGenerator()

    suspend fun createPdf(
        pages: List<BookPage>,
        bookConfig: AncientBookState,
        canvasConfig: AncientCanvasState,
        isSplite: Boolean = false,
        onProgress: (current: Int, total: Int) -> Unit
    ){
        exportPdf(pages, bookConfig, canvasConfig, isSplite , onProgress)
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
                             isSplite: Boolean = false,
                             onProgress: (current: Int, total: Int) -> Unit)