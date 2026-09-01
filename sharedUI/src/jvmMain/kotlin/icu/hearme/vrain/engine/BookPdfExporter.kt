package icu.hearme.vrain.engine

import androidx.compose.ui.graphics.Color as ComposeColor
import icu.hearme.vrain.configure.AncientBookState
import icu.hearme.vrain.configure.AncientCanvasState
import icu.hearme.vrain.pdfcanvas.renderPageBackgroundToBytes
import org.apache.pdfbox.pdmodel.PDDocument
import org.apache.pdfbox.pdmodel.PDPage
import org.apache.pdfbox.pdmodel.PDPageContentStream
import org.apache.pdfbox.pdmodel.common.PDRectangle
import org.apache.pdfbox.pdmodel.font.PDType0Font
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject
import org.apache.pdfbox.util.Matrix
import java.awt.Color
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin

data class CharMetrics(
    val x: Float, val y: Float, val fsize: Float, val fdgrees: Double?,
    val fontIndex: Int, val bestFont: PDType0Font, val targetFontList: List<PDType0Font>,
    val matched: Boolean
)

class PdfRenderEngine(
    val bookConfig: AncientBookState,
    val canvasConfig: AncientCanvasState,
    val fonts: List<PDType0Font>
) {
    val mainFonts: List<PDType0Font>
    val subFonts: List<PDType0Font>
    private val mainFontScales: Map<PDType0Font, Float>
    private val subFontScales: Map<PDType0Font, Float>
    val grid: BookGrid
    val cw: Float   // 列宽
    val rh: Float   // 行高
    private var ly: Float

    private var blCount = 0

    init {
        mainFonts = bookConfig.textFontsArray.mapNotNull { char -> fonts[char - '1'] }
        subFonts = bookConfig.commentFontsArray.mapNotNull { char -> fonts[char - '1'] }

        if (bookConfig.ifFontMetricAdjust) {
            mainFontScales = computeFontScales(mainFonts)
            subFontScales = computeFontScales(subFonts)
        } else {
            mainFontScales = mainFonts.associateWith { 1.0f }
            subFontScales = subFonts.associateWith { 1.0f }
        }
        cw = (canvasConfig.canvasWidth - canvasConfig.marginsLeft - canvasConfig.marginsRight - canvasConfig.leafCenterWidth) / canvasConfig.leafCol.toFloat()
        rh = (canvasConfig.canvasHeight - canvasConfig.marginsTop - canvasConfig.marginsBottom) / bookConfig.rowNum.toFloat()
        grid = BookGridEngine.calculateGrid(canvasConfig, bookConfig, true)
        ly = canvasConfig.marginsBottom + 100f
    }

    suspend fun renderToPdf(doc: PDDocument, pages: List<BookPage>) {
        val bgBytes = renderPageBackgroundToBytes(pages.first(), bookConfig, canvasConfig, true)
        val commonBgImage = PDImageXObject.createFromByteArray(doc, bgBytes, "bg_common")
        for (bookPage in pages) {
            val hasRaisedHead = bookPage.chars.any { CharTag.RAISED_HEAD in it.tags }
            val page = PDPage(PDRectangle(canvasConfig.canvasWidth, canvasConfig.canvasHeight))
            doc.addPage(page)
            PDPageContentStream(doc, page).use { cs ->
                if (hasRaisedHead) {
                    val bgBytes = renderPageBackgroundToBytes(bookPage, bookConfig, canvasConfig)
                    val pdImage = PDImageXObject.createFromByteArray(doc, bgBytes, "bg_page_${bookPage.pageIndex}")
                    cs.drawImage(pdImage, 0f, 0f, canvasConfig.canvasWidth, canvasConfig.canvasHeight)
                } else {
                    cs.drawImage(commonBgImage, 0f, 0f, canvasConfig.canvasWidth, canvasConfig.canvasHeight)
                }

                bookPage.chars.forEachIndexed { index, rc ->
                    val lr = if (index != 0 && CharTag.RECT_FRAME in rc.tags) { bookPage.chars[index-1] } else null
                    renderTags(cs, rc, lr)
                }

                for (rc in bookPage.chars) {
                    renderSingleChar(cs, rc)
                }
            }
            ly = canvasConfig.marginsBottom + 100f
        }
    }

    private fun renderSingleChar(cs: PDPageContentStream, rc: RenderChar) {
        if (rc.char == " ") return
        val metrics = calculateRenderMetrics(rc)

        var textColor = (if (rc.isComment) bookConfig.commentFontColor else bookConfig.textFontColor).toAwtColor()
        var x = metrics.x
        var y = metrics.y
        var fsize = metrics.fsize

        if (CharTag.RECT_FRAME in rc.tags){
            textColor = bookConfig.rectFcolor.toAwtColor()
            val trf = if (rc.isComment) bookConfig.commRectF else bookConfig.textRectF
            fsize *= trf
        }
        if (CharTag.CIRCLE_FRAME in rc.tags){
            textColor = bookConfig.circleFcolor.toAwtColor()
            val tcf = if (rc.isComment) bookConfig.commCircleF else bookConfig.textCircleF
            x += fsize * (1 - tcf) / 2
            y += fsize * (1 - tcf) / 2
            fsize *= tcf
        }
        cs.beginText()
        if (rc.isRotated) {
            val matrix = Matrix.getRotateInstance(Math.toRadians(-90.0), x, y)
            cs.setTextMatrix(matrix)
        } else {
            cs.newLineAtOffset(x, y)
        }

        if (bookConfig.ifFallbackBold && metrics.fontIndex != 0){
            cs.setLineWidth(bookConfig.fallbackBoldStrokeWidth)
        }

        if (metrics.fdgrees != null && metrics.fdgrees != 0.0){
            val matrix = Matrix.getRotateInstance(Math.toRadians(metrics.fdgrees), x, y)
            cs.setTextMatrix(matrix)
        }

        cs.setNonStrokingColor(textColor)
        cs.setFont(metrics.bestFont, fsize)
        cs.showText(if (metrics.matched) rc.char else "□")
        cs.endText()
        ly = y
    }

    private fun renderTags(cs: PDPageContentStream, rc: RenderChar, lr: RenderChar? = null){
        if (CharTag.BOOK_LINE in rc.tags) { blCount++ } else { blCount = 0 }
        if (rc.char == " "){ return }
        val metrics = calculateRenderMetrics(rc)

        if (rc.tags.isNotEmpty()) {
            drawCharTags(cs, metrics.x, metrics.y, metrics.fsize, rc, lr)
        }
    }

    private fun drawCharTags(cs: PDPageContentStream, x: Float, y: Float, fsize: Float, rc: RenderChar, lr: RenderChar? = null) {
        val tags: Set<CharTag> = rc.tags
        var strokeColor: Color
        // 正文文字右侧圈注
        if (CharTag.CIRCLE_NOTE in tags) {
            val ox = x + cw / 2 + fsize * bookConfig.textNoteOx
            val oy = y + fsize * bookConfig.textNoteOy
            val or = fsize * bookConfig.textNoteOr
            val ow = bookConfig.textNoteOw
            val oc = bookConfig.textNoteOc.toAwtColor()

            drawCircle(cs, ox, oy, or, oc, ow)
        }
        // 正文文字右侧点注
        if (CharTag.POINT_NOTE in tags) {
            val fchar = "、"
            val (_, ffn, matched) = selectFontForChar(fchar, mainFonts)
            val px = x + cw / 2 + fsize * bookConfig.textNotePx
            val py = y + fsize * bookConfig.textNotePy
            val ps = fsize * bookConfig.textNotePs
            val pc = bookConfig.textNotePc.toAwtColor()
            cs.beginText()
            cs.setNonStrokingColor(pc)
            cs.setFont(ffn, ps)
            cs.newLineAtOffset(px, py)
            cs.showText(if (matched) fchar else "□")
            cs.endText()
        }
        // 正文文字右侧线注
        if (CharTag.LINE_NOTE in tags) {
            var ty = y + rh * (1 + bookConfig.textNoteLy)
            var by = y + rh * bookConfig.textNoteLy
            val lx = x + cw / 2 + fsize * bookConfig.textNoteLx

            val lw = bookConfig.textNoteLw
            val lc = bookConfig.textNoteLc.toAwtColor()
            if ((rc.pcntIndex % bookConfig.rowNum).toInt() == 1){ ty = canvasConfig.canvasHeight - canvasConfig.marginsTop - 5 }
            if ((rc.pcntIndex % bookConfig.rowNum).toInt() == 0){ by = canvasConfig.marginsBottom + 4 }

            cs.setStrokingColor(lc)
            cs.setLineWidth(lw)
            cs.moveTo(lx, by)
            cs.lineTo(lx, ty)
            cs.stroke()
        }

        // 书名左侧边线
        if (CharTag.BOOK_LINE in tags) {
            var ty = y + rh * 0.8f
            var by = y - rh * 0.2f
            if (ty >= canvasConfig.canvasHeight - canvasConfig.marginsTop) {
                ty = canvasConfig.canvasHeight - canvasConfig.marginsTop - 5
            }
            if (blCount == 1) { ty -= 0.25f * rh }
            if (by <= canvasConfig.marginsBottom) { by = canvasConfig.marginsBottom + 3 }
            strokeColor = bookConfig.bookLineColor.toAwtColor()
            val bl = bookConfig.bookLineWidth + if (rc.isComment) 0 else 1
            drawWavyLine(cs, x-2, by, x-2, ty, strokeColor, bl)
        }
        // 圆角方框
        if (CharTag.RECT_FRAME in tags) {
            val r = if (rc.isComment) bookConfig.commRectR else bookConfig.textRectR
            val rty = if (rc.isComment) bookConfig.commRectY else bookConfig.textRectY
            val rth = if (rc.isComment) bookConfig.commRectH else bookConfig.textRectH
            val cx = x + r
            var cy = y - rty * if (rc.isComment) fsize else rh
            var ch = fsize * (1 + rth)
            if (rc.isComment){
                if (y <= canvasConfig.marginsBottom + 10) { cy = y - 6; ch -= 6 }
                if (y + rh >= canvasConfig.canvasHeight - canvasConfig.marginsTop - 5) { ch -= 8 }
            } else {
                if ((rc.pcntIndex % bookConfig.rowNum).toInt() == 0) { cy += 2; ch -= 4 }
                if ((rc.pcntIndex % bookConfig.rowNum).toInt() == 1) { ch -= 4 }
            }

            val rtype = bookConfig.rectType
            if (rtype == 0){
                drawRect(cs, cx-2, cy-2, fsize - 2 * r + 4,ch + 4, r, bookConfig.rectBcolor.toAwtColor())
                drawRect(cs, cx-1, cy-1, fsize - 2 * r + 2,ch + 2, r, Color.WHITE)
                drawRect(cs, cx+1, cy+1, fsize - 2 * r - 2,ch - 2, r, bookConfig.rectBcolor.toAwtColor())
            }
            if (rtype == 1){
                val tlr = if (lr != null && CharTag.RECT_FRAME in lr.tags) { lr.char } else null
                drawRect(cs, cx, cy, fsize - 2 * r, ch, r, bookConfig.rectBcolor.toAwtColor(), rc.isComment, tlr)
            }
        }
        // 圆形框
        if (CharTag.CIRCLE_FRAME in tags) {
            val tcy = if (rc.isComment) bookConfig.commCircleY else bookConfig.textCircleY
            val tcr = if (rc.isComment) bookConfig.commCircleR else bookConfig.textCircleR
            val cx = x + fsize / 2
            val cy = y + fsize / 2 + fsize * tcy
            val cr = 1 + fsize / 2 * tcr
            val oc = bookConfig.circleBcolor.toAwtColor()
            val ctype = bookConfig.circleType
            if (ctype == 0){
                val offr = if (rc.isComment) -1 else 0
                drawCircle(cs, cx, cy, cr + 4 + offr, oc)
                drawCircle(cs, cx, cy, cr + 2 + offr, Color.WHITE)
                drawCircle(cs, cx, cy, cr, oc)
            }
            if (ctype == 1){
                drawCircle(cs, cx, cy, cr, oc)
            }
        }
    }

    private fun calculateRenderMetrics(rc: RenderChar): CharMetrics {
        val slot = rc.pcntIndex.toInt().coerceIn(0, grid.charsPerPage - 1)
        val offset = rc.pcntIndex - slot
        val subIndex = (offset * 4 + 0.1f).toInt()
        val isRightHalf = (subIndex == 0 || subIndex == 1)

        val pos = if (rc.isComment) {
            if (isRightHalf) grid.subPositions[slot] else grid.mainPositions[slot]
        } else {
            grid.mainPositions[slot]
        }

        val targetFontList = if (rc.isComment) subFonts else mainFonts
        val (fontIndex, bestFont, matched) = selectFontForChar(rc.char, targetFontList)

        val baseFontSize = if (rc.isComment) {
            bookConfig.getFonts()[fontIndex].second ?: bookConfig.commentFont1Size
        } else {
            bookConfig.getFonts()[fontIndex].first ?: bookConfig.textFont1Size
        }
        var fdgrees = bookConfig.getFonts()[fontIndex].third

        var fsize = baseFontSize
        if (bookConfig.ifFontMetricAdjust){
            fsize *= (if (rc.isComment) subFontScales[bestFont] else mainFontScales[bestFont]) ?: 1f
        }

        var x = pos.x
        var y = pos.y

        if (CharTag.RAISED_HEAD in rc.tags) { y += rh }

        if (rc.isNop) {
            fsize *= if (rc.isComment) bookConfig.commentCommaNopSize else bookConfig.textCommaNopSize
            x += (cw * if (rc.isComment) bookConfig.commentCommaNopX / 2 else bookConfig.textCommaNopX)
            y -= (rh * if (rc.isComment) bookConfig.commentCommaNopY else bookConfig.textCommaNopY)
            if (y - canvasConfig.marginsBottom < 10){
                if (rc.isComment){
                    y = canvasConfig.marginsBottom + 2
                } else {
                    y = canvasConfig.marginsBottom + 5
                    if (rc.char == "…" || rc.char == "—") { y += fsize / 2 }
                }
            }
            if (!rc.isComment && rc.char in bookConfig.textComma90) { fdgrees = -90.0 }
        } else {
            if (rc.isComment){
                if (bookConfig.commentGridType == 4) {
                    val isTop = (subIndex % 2 == 0)
                    if (isTop) { y += rh / 2f }
                    if (isRightHalf) { x += (cw - fsize * 2) / 4 } else { x += cw / 4 }
                    y += (rh / 2f - fsize) / 4f
                    fsize /= 2
                } else {
                    x += (cw / 2f - fsize) / 2f
                    y += (rh - fsize) / 2f
                }

                if (rc.char in bookConfig.commentComma90) {
                    fsize *= bookConfig.commentComma90Size
                    x += cw / 2 * bookConfig.commentComma90X
                    y += rh * bookConfig.commentComma90Y
                    fdgrees = -90.0
                }
            } else {
                if (rc.char in bookConfig.textComma90){
                    fsize *= bookConfig.textComma90Size
                    x += cw * bookConfig.textComma90X
                    y += rh * bookConfig.textComma90Y
                    fdgrees = -90.0
                } else {
                    x += (cw - fsize) / 2
                }
            }
        }

        if (CharTag.ZOOM_IN in rc.tags) {
            x += fsize * (1 - bookConfig.textZoom) / 2
            fsize = baseFontSize * bookConfig.textZoom
        }

        return CharMetrics(x, y, fsize, fdgrees, fontIndex, bestFont, targetFontList, matched)
    }

    private fun drawCircle(cs: PDPageContentStream, cx: Float, cy: Float, radius: Float, color: Color, lw: Float?=null) {
        appendCirclePath(cs, cx, cy, radius)
        if (lw != null) {
            cs.setStrokingColor(color)
            cs.setLineWidth(lw)
            cs.stroke()
        } else {
            cs.setNonStrokingColor(color)
            cs.fill()
        }
    }

    private fun appendCirclePath(cs: PDPageContentStream, cx: Float, cy: Float, radius: Float) {
        val magic = 0.55228475f * radius
        cs.moveTo(cx, cy + radius)
        cs.curveTo(cx - magic, cy + radius, cx - radius, cy + magic, cx - radius, cy)
        cs.curveTo(cx - radius, cy - magic, cx - magic, cy - radius, cx, cy - radius)
        cs.curveTo(cx + magic, cy - radius, cx + radius, cy - magic, cx + radius, cy)
        cs.curveTo(cx + radius, cy + magic, cx + magic, cy + radius, cx, cy + radius)
    }

    private fun drawRect(cs: PDPageContentStream, x: Float, y: Float, w: Float, h: Float, r: Float, c: Color, isComment: Boolean? = null, lr: String? = null) {
        cs.setNonStrokingColor(c)

        appendCirclePath(cs, x, y + r / 2, r)
        appendCirclePath(cs, x + w, y + r / 2, r)
        appendCirclePath(cs, x, y + h, r)
        appendCirclePath(cs, x + w, y + h, r)

        cs.addRect(x - r, y + r / 2, w + 2 * r, h - r / 2)
        cs.addRect(x, y - r / 2, w, h + 3 * r / 2)
        if (isComment == null){
            cs.fill()
            return
        }

        if (y < canvasConfig.canvasHeight - canvasConfig.marginsTop - rh) {
            if (lr != null) {
                if (!isComment || (isComment && y < ly - bookConfig.rowDeltaY)) {
                    cs.addRect(x - r, y + h, w + 2 * r, 3 * r)
                }
            }
        }
        cs.fill()
    }

    private fun drawWavyLine(cs: PDPageContentStream, x1: Float, y1: Float, x2: Float, y2: Float, color: Color= Color.BLACK, width: Float = 1f) {
        val amplitude = 1.25f // 波浪振幅
        val wavelength = 10f  // 波长

        val dx = x2 - x1
        val dy = y2 - y1
        val length = hypot(dx.toDouble(), dy.toDouble()).toFloat()
        val angle = atan2(dy.toDouble(), dx.toDouble()).toFloat()

        val segments = (length / (wavelength / 5)).toInt().coerceAtLeast(1)
        cs.setStrokingColor(color)
        cs.setLineWidth(width)
        cs.moveTo(x1, y1)

        for (i in 1..segments) {
            val t = i.toFloat() / segments
            val distance = t * length
            // 使用正弦计算偏移量
            val waveOffset = (amplitude * sin((2 * Math.PI * distance / wavelength))).toFloat()

            // 法线方向偏移
            val perpX = (-sin(angle.toDouble()) * waveOffset).toFloat()
            val perpY = (cos(angle.toDouble()) * waveOffset).toFloat()

            val curX = x1 + (cos(angle.toDouble()) * distance).toFloat() + perpX
            val curY = y1 + (sin(angle.toDouble()) * distance).toFloat() + perpY

            cs.lineTo(curX, curY)
        }
        cs.stroke()
    }

    private fun selectFontForChar(char: String, fonts: List<PDType0Font>): Triple<Int, PDType0Font, Boolean> {
        for ((index, font) in fonts.withIndex()) {
            try {
                font.encode(char)
                return Triple(index, font, true)
            } catch (e: Exception) { }
        }
        return Triple(0, fonts.first(), false)
    }

    private fun getGlyphHeight(font: PDType0Font, char: String): Float {
        return try {
            if (!font.hasGlyph(char.codePointAt(0))) return 0f

            val path = font.getPath(char.codePointAt(0))
            val bounds = path.bounds2D
            val height = bounds.height.toFloat()

            if (height > 0f) height else 0f
        } catch (e: Exception) {
            0f
        }
    }

    private fun getFaceHeight(font: PDType0Font): Float {
        val fd = font.fontDescriptor ?: return 1000f
        val ascender = fd.ascent
        val descender = fd.descent
        val h = ascender - descender
        return h
    }

    private fun computeFontScales(fonts: List<PDType0Font>): Map<PDType0Font, Float> {
        val scaleMap = mutableMapOf<PDType0Font, Float>()
        if (fonts.isEmpty()) return scaleMap

        val primaryFont = fonts.first()
        scaleMap[primaryFont] = 1.0f
        val refChar = "国"

        val heights = mutableMapOf<PDType0Font, Float>()
        val needsFallback = mutableListOf<PDType0Font>()

        val primaryHeight = getGlyphHeight(primaryFont, refChar)
        if (primaryHeight <= 0f) return scaleMap
        heights[primaryFont] = primaryHeight

        for (font in fonts) {
            if (font == primaryFont) continue

            val h = getGlyphHeight(font, refChar)
            if (h > 0f) {
                heights[font] = h
            } else {
                needsFallback.add(font)
            }
        }

        if (needsFallback.isNotEmpty()) {
            val primaryFaceH = getFaceHeight(primaryFont)
            if (primaryFaceH > 0f) {
                val calibration = primaryHeight / primaryFaceH

                for (font in needsFallback) {
                    val faceH = getFaceHeight(font)
                    if (faceH > 0f) {
                        heights[font] = faceH * calibration
                    }
                }
            }
        }

        for (font in fonts) {
            if (font == primaryFont) continue
            val h = heights[font]
            if (h != null && h > 0f) {
                val scale = primaryHeight / h
                scaleMap[font] = if (scale in 0.85f..1.15f) scale else 1f
            } else {
                scaleMap[font] = 1.0f
            }
        }
        return scaleMap
    }

    private fun ComposeColor.toAwtColor(): Color {
        return Color(this.red, this.green, this.blue, this.alpha)
    }
}