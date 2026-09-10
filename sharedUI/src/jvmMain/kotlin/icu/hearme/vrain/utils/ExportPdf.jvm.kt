package icu.hearme.vrain.utils

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import icu.hearme.vrain.configure.AncientBookState
import icu.hearme.vrain.configure.AncientCanvasState
import icu.hearme.vrain.engine.BookPage
import icu.hearme.vrain.engine.PdfRenderEngine
import icu.hearme.vrain.manager.PDFFontManager
import icu.hearme.vrain.manager.PlatformFontManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import org.apache.pdfbox.cos.COSName
import org.apache.pdfbox.pdmodel.PDDocument
import org.apache.pdfbox.pdmodel.PDDocumentInformation
import org.apache.pdfbox.pdmodel.PDPage
import org.apache.pdfbox.pdmodel.common.PDRectangle
import org.apache.pdfbox.pdmodel.font.PDType0Font
import org.apache.pdfbox.rendering.PDFRenderer
import vrain.sharedui.generated.resources.Res
import java.io.ByteArrayInputStream
import java.io.File
import java.util.Calendar
import javax.swing.JFileChooser
import javax.swing.UIManager
import javax.swing.filechooser.FileNameExtensionFilter
import kotlin.coroutines.resume

actual suspend fun exportPdf(
    pages: List<BookPage>,
    bookConfig: AncientBookState,
    canvasConfig: AncientCanvasState,
    isSplite: Boolean,
    onProgress: (current: Int, total: Int) -> Unit
) = withContext(Dispatchers.Default) {

    val mainFonts = mutableListOf<PDType0Font>()

    val targetFile = suspendCancellableCoroutine<File?> { continuation ->
        java.awt.EventQueue.invokeLater {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName())
            } catch (e: Exception) {
                e.printStackTrace()
            }

            val pdfFilter = FileNameExtensionFilter("PDF文档 (*.pdf)", "pdf")
            val chooser = JFileChooser().apply {
                dialogTitle = "另存为..."
                selectedFile = File(bookConfig.title)
                addChoosableFileFilter(pdfFilter)
                fileFilter = pdfFilter
            }

            val result = chooser.showSaveDialog(null)

            if (result == JFileChooser.APPROVE_OPTION) {
                var file = chooser.selectedFile
                val hasValidExtension = file.name.endsWith(".pdf", ignoreCase = true)
                if (!hasValidExtension) {
                    file = File(file.parent, "${file.name}.pdf")
                }
                continuation.resume(file)
            } else {
                continuation.resume(null)
            }
        }
    }

    if (targetFile != null) {
        PDDocument().use { doc ->
            bookConfig.getFontList("12345").forEach { font ->
                val fontFile = PlatformFontManager.getFileForBuiltInFont(font)
                val sysFont = PDFFontManager.loadSystemFont(font.substringBeforeLast("."))
                val pdfFont: PDType0Font
                if (fontFile != null) {
                    val fontRes = Res.readBytes(fontFile.path)
                    pdfFont = PDType0Font.load(doc, ByteArrayInputStream(fontRes), true)
                } else if (sysFont != null) {
                    pdfFont = PDType0Font.load(doc, sysFont, true)
                } else {
                    val fontRes = Res.readBytes("font/SourceHanSerif.ttf")
                    pdfFont = PDType0Font.load(doc, ByteArrayInputStream(fontRes), true)
                }
                mainFonts.add(pdfFont)
            }
            val engine = PdfRenderEngine(bookConfig, canvasConfig, mainFonts)
            pages.forEachIndexed { index, bookPage ->
                engine.renderToPage(doc, bookPage)
                onProgress(index + 1, pages.size)
            }

            engine.addFileInfo(doc)
            if (isSplite) { engine.splitPage(doc) }
            doc.save(targetFile)
        }
    }
}

actual suspend fun preViewPdfPage(
    page: BookPage, bookConfig: AncientBookState, canvasConfig: AncientCanvasState
): ImageBitmap? = withContext(Dispatchers.Default) {
    var finalBitmap: ImageBitmap? = null
    val mainFonts = mutableListOf<PDType0Font>()
    PDDocument().use { doc ->
        bookConfig.getFontList("12345").forEach { font ->
            val fontFile = PlatformFontManager.getFileForBuiltInFont(font)
            val sysFont = PDFFontManager.loadSystemFont(font.substringBeforeLast("."))
            val pdfFont: PDType0Font
            if (fontFile != null) {
                pdfFont = PDType0Font.load(doc, ByteArrayInputStream(fontFile.readBytes()), true)
            } else if (sysFont != null) {
                pdfFont = PDType0Font.load(doc, sysFont, true)
            } else {
                val fontRes = Res.readBytes("font/SourceHanSerif.ttf")
                pdfFont = PDType0Font.load(doc, ByteArrayInputStream(fontRes), true)
            }
            mainFonts.add(pdfFont)
        }
        val engine = PdfRenderEngine(bookConfig, canvasConfig, mainFonts).apply {
            if (!canvasConfig.bamboo) { isPdfPre = true }
        }
        engine.renderToPage(doc, page)
        val renderer = PDFRenderer(doc)
        val awtImage = renderer.renderImageWithDPI(0, 72f)
        finalBitmap = awtImage.toComposeImageBitmap()
    }
    return@withContext finalBitmap
}