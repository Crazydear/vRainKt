package icu.hearme.vrain.utils

import androidx.compose.ui.graphics.ImageBitmap
import icu.hearme.vrain.bookcanvas.FileInfo
import icu.hearme.vrain.configure.AncientBookState
import icu.hearme.vrain.configure.AncientCanvasState
import icu.hearme.vrain.engine.BookPage

actual suspend fun exportPdf(
    pages: List<BookPage>,
    bookConfig: AncientBookState,
    canvasConfig: AncientCanvasState,
    isSplite: Boolean,
    onProgress: (current: Int, total: Int) -> Unit
) {
}

actual suspend fun preViewPdfPage(
    page: BookPage,
    bookConfig: AncientBookState,
    canvasConfig: AncientCanvasState
): ImageBitmap? {
    return null
}

actual suspend fun exportPdf(
    textSources: List<FileInfo>,
    bookConfig: AncientBookState,
    canvasConfig: AncientCanvasState,
    isSplite: Boolean,
    onProgress: (file: String, current: Int, total: Int) -> Unit
) {
}