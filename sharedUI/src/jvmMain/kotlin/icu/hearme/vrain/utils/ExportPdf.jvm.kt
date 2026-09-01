package icu.hearme.vrain.utils

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

    val doc = PDDocument()
    val mainFonts = mutableListOf<PDType0Font>()

    bookConfig.getFontList("12345").forEach { font ->
        val fontFile = PlatformFontManager.getFileForBuiltInFont(font)
        val sysFont = PDFFontManager.loadSystemFont(font)
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
            val engine = PdfRenderEngine(bookConfig, canvasConfig, mainFonts)
            engine.renderToPdf(doc, pages)

            val info: PDDocumentInformation = doc.documentInformation
            info.title = bookConfig.title
            info.author = bookConfig.author
            info.subject = "中华古籍古版丛书"
            info.keywords = "${bookConfig.title}, ${bookConfig.author}, 古籍, 竖排"
            info.creator = "vRainKt for Desktop"
            info.producer = "vRainKt for Desktop，古籍刻本直排电子书制作工具\nhttps://github.com/Crazydear/vRainKt"
            info.creationDate = Calendar.getInstance()
            info.modificationDate = Calendar.getInstance()

            // info.setCustomMetadataValue("ProjectVersion", "1.0.0")
            // info.setCustomMetadataValue("LayoutEngine", "Vertical-RL")

            if (isSplite) {
                val pageTree = doc.pages
                val originalPages = pageTree.toList()

                for (originalPage in originalPages) {
                    val mediaBox = originalPage.mediaBox
                    val x = mediaBox.lowerLeftX
                    val y = mediaBox.lowerLeftY
                    val width = mediaBox.width
                    val height = mediaBox.height
                    val halfWidth = width / 2f
                    val middleX = x + halfWidth

                    val leftRect = PDRectangle(x, y, halfWidth, height)
                    val rightRect = PDRectangle(middleX, y, halfWidth, height)

                    val leftPage = PDPage(leftRect)

                    leftPage.cosObject.setItem(COSName.CONTENTS, originalPage.cosObject.getItem(COSName.CONTENTS))
                    leftPage.cosObject.setItem(COSName.RESOURCES, originalPage.cosObject.getItem(COSName.RESOURCES))
                    originalPage.mediaBox = rightRect
                    originalPage.cropBox = rightRect
                    pageTree.insertAfter(leftPage, originalPage)
                }
            }
            doc.save(targetFile)
        }
    }
}