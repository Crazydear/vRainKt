package icu.hearme.vrain.utils

import icu.hearme.vrain.configure.AncientBookState
import icu.hearme.vrain.configure.AncientCanvasState
import icu.hearme.vrain.engine.BookPage
import icu.hearme.vrain.engine.PdfRenderEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import org.apache.pdfbox.pdmodel.PDDocument
import org.apache.pdfbox.pdmodel.font.PDType0Font
import vrain.sharedui.generated.resources.Res
import java.io.ByteArrayInputStream
import java.io.File
import javax.swing.JFileChooser
import javax.swing.UIManager
import javax.swing.filechooser.FileNameExtensionFilter
import kotlin.coroutines.resume

actual suspend fun exportPdf(
    pages: List<BookPage>,
    bookConfig: AncientBookState,
    canvasConfig: AncientCanvasState,
    onProgress: (current: Int, total: Int) -> Unit
) = withContext(Dispatchers.Default) {

    val doc = PDDocument()
    val mainFonts = mutableListOf<PDType0Font>()

    bookConfig.getFontList("12345").forEach { font ->
        val fontRes = Res.readBytes("font/$font")
        val pdfFont = PDType0Font.load(doc, ByteArrayInputStream(fontRes))
        mainFonts.add(pdfFont)
    }

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
        try {
            val engine = PdfRenderEngine(bookConfig, canvasConfig, mainFonts)
            engine.renderToPdf(doc, pages, targetFile)
        } finally {
            doc.close()
        }
    } else {
        doc.close()
    }
}