package icu.hearme.vrain.utils

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import icu.hearme.vrain.bookcanvas.FileInfo
import icu.hearme.vrain.configure.AncientBookState
import icu.hearme.vrain.configure.AncientCanvasState
import icu.hearme.vrain.configure.getZhPageNum
import icu.hearme.vrain.engine.BookPage
import icu.hearme.vrain.engine.BookTextEngine.parseFileToPages
import icu.hearme.vrain.engine.PdfRenderEngine
import icu.hearme.vrain.manager.PDFFontManager
import icu.hearme.vrain.manager.PlatformFontManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import org.apache.pdfbox.pdmodel.PDDocument
import org.apache.pdfbox.pdmodel.font.PDType0Font
import org.apache.pdfbox.pdmodel.interactive.documentnavigation.destination.PDPageXYZDestination
import org.apache.pdfbox.pdmodel.interactive.documentnavigation.outline.PDDocumentOutline
import org.apache.pdfbox.pdmodel.interactive.documentnavigation.outline.PDOutlineItem
import org.apache.pdfbox.pdmodel.interactive.documentnavigation.outline.PDOutlineNode
import org.apache.pdfbox.rendering.ImageType
import org.apache.pdfbox.rendering.PDFRenderer
import java.io.ByteArrayInputStream
import java.io.File
import javax.swing.JFileChooser
import javax.swing.UIManager
import javax.swing.filechooser.FileNameExtensionFilter
import kotlin.coroutines.resume

actual suspend fun exportPdf(
    pages: List<BookPage>,
    bookConfig: AncientBookState, canvasConfig: AncientCanvasState,
    isSplite: Boolean, onProgress: (current: Int, total: Int) -> Unit
) = withContext(Dispatchers.Default) {
    PDDocument().use { doc ->
        val mainFonts = mutableListOf<PDType0Font>()
        bookConfig.getFontList("12345").forEach { font ->
            val fontFile = PlatformFontManager.getFileForBuiltInFont(font)
            val sysFont = PDFFontManager.loadSystemFont(font.substringBeforeLast("."))
            val pdfFont: PDType0Font
            if (fontFile != null && fontFile.exists()) {
                pdfFont = PDType0Font.load(doc, ByteArrayInputStream(fontFile.readBytes()), true)
            } else if (sysFont != null) {
                pdfFont = PDType0Font.load(doc, sysFont, true)
            } else {
                val fontRes = Thread.currentThread().contextClassLoader.getResourceAsStream("font/SourceHanSerif.ttf")
                pdfFont = PDType0Font.load(doc, fontRes, true)
            }
            mainFonts.add(pdfFont)
        }
        val engine = PdfRenderEngine(bookConfig, canvasConfig, mainFonts)
        engine.renderToCover(doc)   // 封面
        val pageOffset = 1
        val tpchars = bookConfig.title

        pages.forEachIndexed { index, bookPage ->
            val pchars = getZhPageNum(index + pageOffset)
            engine.renderToPage(doc, bookPage) { cs ->
                if (canvasConfig.bamboo) return@renderToPage
                engine.renderTypePage(cs, tpchars, pchars)
            }
            onProgress(index + 1, pages.size)
        }
        engine.addFileInfo(doc)     // PDF元信息
        if (isSplite) { engine.splitPage(doc) }
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
        if (targetFile != null) { doc.save(targetFile) }
    }
}

actual suspend fun preViewPdfPage(
    page: BookPage, bookConfig: AncientBookState, canvasConfig: AncientCanvasState
): ImageBitmap? = withContext(Dispatchers.Default) {
    var finalBitmap: ImageBitmap? = null
    PDDocument().use { doc ->
        val mainFonts = mutableListOf<PDType0Font>()
        bookConfig.getFontList("12345").forEach { font ->
            val fontFile = PlatformFontManager.getFileForBuiltInFont(font)
            val sysFont = PDFFontManager.loadSystemFont(font.substringBeforeLast("."))
            val pdfFont: PDType0Font
            if (fontFile != null && fontFile.exists()) {
                pdfFont = PDType0Font.load(doc, ByteArrayInputStream(fontFile.readBytes()), true)
            } else if (sysFont != null) {
                pdfFont = PDType0Font.load(doc, sysFont, true)
            } else {
                val fontRes = Thread.currentThread().contextClassLoader.getResourceAsStream("font/SourceHanSerif.ttf")
                pdfFont = PDType0Font.load(doc, fontRes, true)
            }
            mainFonts.add(pdfFont)
        }
        val engine = PdfRenderEngine(bookConfig, canvasConfig, mainFonts).apply { isPdfPre = true }
        engine.renderToCover(doc)
        engine.renderToPage(doc, page) { cs ->
            if (canvasConfig.bamboo) return@renderToPage
            val pchars = getZhPageNum(page.pageIndex)
            engine.renderTypePage(cs, bookConfig.title, pchars)
        }
        val renderer = PDFRenderer(doc)
        val awtImage = renderer.renderImageWithDPI(if (page.pageIndex == 0) 0 else 1, 72f, ImageType.RGB)
        finalBitmap = awtImage.toComposeImageBitmap()
    }
    return@withContext finalBitmap
}

actual suspend fun exportPdf(
    textSources: List<FileInfo>,
    bookConfig: AncientBookState, canvasConfig: AncientCanvasState,
    isSplite: Boolean, onProgress: (file: String, current: Int, total: Int) -> Unit
) = withContext(Dispatchers.Default) {
    PDDocument().use { doc ->
        val mainFonts = mutableListOf<PDType0Font>()
        bookConfig.getFontList("12345").forEach { font ->
            val fontFile = PlatformFontManager.getFileForBuiltInFont(font)
            val sysFont = PDFFontManager.loadSystemFont(font.substringBeforeLast("."))
            val pdfFont: PDType0Font = if (fontFile != null && fontFile.exists()) {
                PDType0Font.load(doc, ByteArrayInputStream(fontFile.readBytes()), true)
            } else if (sysFont != null) {
                PDType0Font.load(doc, sysFont, true)
            } else {
                val fontRes = Thread.currentThread().contextClassLoader.getResourceAsStream("font/SourceHanSerif.ttf")
                PDType0Font.load(doc, fontRes, true)
            }
            mainFonts.add(pdfFont)
        }
        val outline = doc.documentCatalog.documentOutline ?: PDDocumentOutline().also {
            doc.documentCatalog.documentOutline = it
        }
        val rootBookmark = PDOutlineItem().apply { title = bookConfig.title }
        outline.addLast(rootBookmark)
        val outlineParents = arrayOfNulls<PDOutlineNode>(9).apply {
            this[0] = outline
            this[1] = rootBookmark
        }
        val engine = PdfRenderEngine(bookConfig, canvasConfig, mainFonts)
        engine.renderToCover(doc)
        var pageOffset = 1
        textSources.forEachIndexed { tid, source ->
            val pages: List<BookPage> = parseFileToPages(source.file, bookConfig, engine.grid)
            val tpchars = if (bookConfig.titlePostfix?.isNotBlank() == true) {
                val cid = if (textSources.first().id == 0) tid else tid + 1
                var tpost = bookConfig.titlePostfix!!.replace("X", getZhPageNum(cid))
                tpost = if (tid == 0 && source.id == 0) "序" else if (source.id == 999 && source == textSources.last()) "附" else tpost
                "${bookConfig.title}${tpost}"
            } else {
                bookConfig.title
            }
            val tpostBookmark = PDOutlineItem().apply {
                title = tpchars
                openNode()
            }
            rootBookmark.addLast(tpostBookmark)
            outlineParents[2] = tpostBookmark
            for (l in 3..8) { outlineParents[l] = null }
            var isFirstPageOfSource = true
            pages.forEachIndexed { index, bookPage ->
                val pchars = getZhPageNum(index + pageOffset)
                engine.renderToPage(doc, bookPage,
                    onOutLine = { item, level ->
                        val stackIndex = level + 2
                        val parent = (stackIndex - 1 downTo 2).firstNotNullOfOrNull { outlineParents[it] } ?: tpostBookmark
                        parent.addLast(item)
                        outlineParents[stackIndex] = item
                        for (l in (stackIndex + 1)..8) { outlineParents[l] = null }
                    }
                ) { cs ->
                    if (isFirstPageOfSource) {
                        val currentPage = doc.getPage(doc.numberOfPages - 1)
                        tpostBookmark.destination = PDPageXYZDestination().apply { page = currentPage }
                        isFirstPageOfSource = false
                    }
                    if (!canvasConfig.bamboo) {
                        engine.renderTypePage(cs, tpchars, pchars)
                    }
                }
                onProgress(source.file.name, index + 1, pages.size)
            }
            pageOffset += if (source.id == 0) 0 else pages.size
        }
        engine.addFileInfo(doc)
        if (isSplite) { engine.splitPage(doc) }
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
        if (targetFile != null) { doc.save(targetFile) }
    }
}