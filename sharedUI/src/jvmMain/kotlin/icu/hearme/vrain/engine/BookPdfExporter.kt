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
    val matched: Boolean, val isFirst: Boolean = false, val isLast: Boolean = false
){
    var offsetX: Float = 0f
    var OffserY: Float = 0f
}

class PdfRenderEngine(
    val bookConfig: AncientBookState,
    val canvasConfig: AncientCanvasState,
    val fonts: List<PDType0Font>
) {
    val textFonts: List<PDType0Font>
    val commentFonts: List<PDType0Font>
    private val textFontScales: Map<PDType0Font, Float>
    private val commentFontScales: Map<PDType0Font, Float>
    val grid: BookGrid
    val cw: Float   // 列宽
    val rh: Float   // 行高

    val textDrawCommands = mutableListOf<() -> Unit>()

    init {
        textFonts = bookConfig.textFontsArray.mapNotNull { char -> fonts[char - '1'] }
        commentFonts = bookConfig.commentFontsArray.mapNotNull { char -> fonts[char - '1'] }

        if (bookConfig.ifFontMetricAdjust) {
            textFontScales = computeFontScales(textFonts)
            commentFontScales = computeFontScales(commentFonts)
        } else {
            textFontScales = textFonts.associateWith { 1.0f }
            commentFontScales = commentFonts.associateWith { 1.0f }
        }
        cw = canvasConfig.colW
        rh = canvasConfig.contentHeight / bookConfig.rowNum.toFloat()
        grid = BookGridEngine.calculateGrid(canvasConfig, bookConfig, true)
    }

    suspend fun renderToPdf(doc: PDDocument, pages: List<BookPage>) {
        val bgBytes = renderPageBackgroundToBytes(pages.first(), bookConfig, canvasConfig, true)
        val commonBgImage = PDImageXObject.createFromByteArray(doc, bgBytes, "bg_common")
        pages.forEachIndexed { index, bookPage ->
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
                textDrawCommands.clear()
                var blStartY: Float? = null     // 书名号波浪线起点
                var rfStartY: Float? = null     // 圆角方框起点
                bookPage.chars.forEachIndexed { index, rc ->
                    if (!rc.char.isNotBlank()) return@forEachIndexed
                    val metrics = calculateRenderMetrics(rc)
                    var fsize = metrics.fsize
                    var fcolor: Color = (if (rc.isComment) bookConfig.commentFontColor else bookConfig.textFontColor).toAwtColor()

                    if (CharTag.CIRCLE_NOTE in rc.tags) {       // 正文文字右侧圈注
                        val ox = metrics.x + cw / 2 + metrics.fsize * bookConfig.textNoteOx
                        val oy = metrics.y + metrics.fsize * bookConfig.textNoteOy
                        val or = metrics.fsize * bookConfig.textNoteOr
                        val ow = bookConfig.textNoteOw
                        val oc = bookConfig.textNoteOc.toAwtColor()
                        cs.drawCircle(ox, oy, or, null, oc, ow)
                    }
                    if (CharTag.POINT_NOTE in rc.tags) {        // 正文文字右侧点注
                        val fchar = "、"
                        val (_, ffn, matched) = selectFontForChar(fchar, textFonts)
                        val px = metrics.x + cw / 2 + metrics.fsize * bookConfig.textNotePx
                        val py = metrics.y + metrics.fsize * bookConfig.textNotePy
                        val ps = metrics.fsize * bookConfig.textNotePs
                        val pc = bookConfig.textNotePc.toAwtColor()
                        val text = if (matched) fchar else "□"
                        textDrawCommands.add { cs.textlable(px, py, ffn, ps, text, pc) }
                    }
                    if (CharTag.LINE_NOTE in rc.tags) {         // 正文文字右侧线注
                        var ty = metrics.y + rh * (1 + bookConfig.textNoteLy)
                        var by = metrics.y + rh * bookConfig.textNoteLy
                        val lx = metrics.x + cw / 2 + metrics.fsize * bookConfig.textNoteLx
                        val lw = bookConfig.textNoteLw
                        val lc = bookConfig.textNoteLc.toAwtColor()
                        if (metrics.isLast) { ty = canvasConfig.canvasHeight - canvasConfig.marginsTop - 5 }
                        if (metrics.isLast) { by = canvasConfig.marginsBottom + 4 }
                        cs.drawLine(lx, ty, lx, by, lw, lc)
                    }
                    if (CharTag.BOOK_LINE in rc.tags) {         // 书名左侧边线
                        if (blStartY == null) {
                            blStartY = metrics.y + rh * 0.8f
                            if (blStartY >= canvasConfig.canvasHeight - canvasConfig.marginsTop) {
                                blStartY = canvasConfig.canvasHeight - canvasConfig.marginsTop - 5
                            }

                            blStartY += if (rc.isComment) 0.25f * rh else 5f
                            if (rc.isComment && bookConfig.commentGridType == 4) { blStartY -= rh }
                        }
                        var by = metrics.y - rh * 0.2f
                        if (by <= canvasConfig.marginsBottom) { by = canvasConfig.marginsBottom + 3 }
                        val lc = bookConfig.bookLineColor.toAwtColor()
                        val bl = bookConfig.bookLineWidth + if (rc.isComment) 0 else 1

                        val nextChar = bookPage.chars.getOrNull(index + 1)
                        val nextHasBookLineTag = nextChar?.tags?.contains(CharTag.BOOK_LINE) == true
                        val isNextBlank = nextChar?.char?.isBlank() == true
                        val isNextDiffColumn = nextChar != null && (nextChar.isComment != rc.isComment || nextChar.isRight != rc.isRight)
                        if (metrics.isLast || !nextHasBookLineTag || isNextBlank || isNextDiffColumn) {
                            val x = metrics.x - if (rc.isComment) 0f else 2f
                            cs.drawWavyLine(x, blStartY, x, by, lc, bl)
                            blStartY = null
                        }
                    }
                    if (CharTag.RECT_FRAME in rc.tags) {       // 圆角方框
                        val r = if (rc.isComment) bookConfig.commRectR else bookConfig.textRectR
                        val rty = if (rc.isComment) bookConfig.commRectY else bookConfig.textRectY
                        val rth = if (rc.isComment) bookConfig.commRectH else bookConfig.textRectH
                        val x = metrics.x
                        var y = metrics.y - rty * if (rc.isComment) metrics.fsize else rh
                        var h = metrics.fsize * (1 + rth)
                        if (rc.isComment){
                            if (y <= canvasConfig.marginsBottom + 10) { y = metrics.y - 6; h -= 6 }
                            if (y + h >= canvasConfig.canvasHeight - canvasConfig.marginsTop - 5) { h -= 8 }
                        } else {
                            if (metrics.isFirst) { y = metrics.y + 2; h -= 4 }
                            if (metrics.isLast) { h -= 4 }
                        }

                        val rtype = bookConfig.rectType
                        if (rtype == 0) {
                            cs.drawRoundRect(x-2, y-2, metrics.fsize + 4,h + 4, r, bookConfig.rectBcolor.toAwtColor(), null, 2f)
                            cs.drawRoundRect(x, y, metrics.fsize, h, r, bookConfig.rectBcolor.toAwtColor(), Color.WHITE, 2f)
                        }
                        if (rtype == 1) {
                            if (rfStartY == null) { rfStartY = y + h }
                            val nextChar = bookPage.chars.getOrNull(index + 1)
                            val nextHasRectTag = nextChar?.tags?.contains(CharTag.RECT_FRAME) == true
                            val isNextBlank = nextChar?.char?.isBlank() == true
                            val isNextDiffColumn = nextChar != null && (nextChar.isComment != rc.isComment || nextChar.isRight != rc.isRight)

                            if (metrics.isLast || !nextHasRectTag || isNextBlank || isNextDiffColumn) {
                                val totalHeight = rfStartY - y
                                cs.drawRoundRect(x, y, metrics.fsize, totalHeight, r, bookConfig.rectBcolor.toAwtColor(), null, 1f)
                                rfStartY = null
                            }
                        }
                        val trf = if (rc.isComment) bookConfig.commRectF else bookConfig.textRectF
                        fcolor = bookConfig.rectFcolor.toAwtColor()
                        fsize *= trf
                    }
                    if (CharTag.CIRCLE_FRAME in rc.tags) {     // 圆形框
                        val tcy = if (rc.isComment) bookConfig.commCircleY else bookConfig.textCircleY
                        val tcr = if (rc.isComment) bookConfig.commCircleR else bookConfig.textCircleR
                        val cx = metrics.x + metrics.fsize / 2
                        val cy = metrics.y + metrics.fsize / 2 + metrics.fsize * tcy
                        val cr = metrics.fsize / 2 * tcr + 1
                        val oc = bookConfig.circleBcolor.toAwtColor()
                        val ctype = bookConfig.circleType
                        if (ctype == 0) {
                            val offr = if (rc.isComment) -1 else 0
                            cs.drawCircle(cx, cy, cr + 4 + offr, oc)
                            cs.drawCircle(cx, cy, cr, oc, Color.WHITE, 2f)
                        }
                        if (ctype == 1){
                            cs.drawCircle(cx, cy, cr, oc)
                        }
                        val tcf = if (rc.isComment) bookConfig.commCircleF else bookConfig.textCircleF
                        fcolor = bookConfig.circleFcolor.toAwtColor()
                        metrics.offsetX += fsize * (1 - tcf) / 2f
                        metrics.OffserY += fsize * (1 - tcf) / 2f
                        fsize *= tcf
                    }
                    if (rc.isRotateLetter) {
                        metrics.offsetX += fsize / 4
                        metrics.OffserY += fsize / 2
                    }
                    val text = if (metrics.matched) rc.char else "□"
                    val fdgrees = if (rc.isRotated || rc.isRotateLetter) -90.0 else metrics.fdgrees ?: 0.0
                    textDrawCommands.add {
                        cs.textlable(metrics.x + metrics.offsetX, metrics.y + metrics.OffserY, metrics.bestFont, fsize, text, fcolor, fdgrees)
                    }
                }
                textDrawCommands.forEach { it.invoke() }
            }
        }
    }

    private fun calculateRenderMetrics(rc: RenderChar): CharMetrics {
        val slot = rc.pcntIndex.toInt().coerceIn(0, grid.charsPerPage - 1)
        val pos = if (rc.isRightComment) { grid.subPositions[slot] } else { grid.mainPositions[slot] }
        val charInRowIndex = slot % bookConfig.rowNum
        val isFirstInColumn = charInRowIndex == 0 && rc.char.isNotBlank()
        val isLastInColumn = charInRowIndex == bookConfig.rowNum - 1  && rc.char.isNotBlank()
                && (!rc.isComment || (bookConfig.commentGridType == 4 && !rc.isTop))

        val activeFont = if (rc.isComment) commentFonts else textFonts
        val (fontIndex, bestFont, matched) = selectFontForChar(rc.char, activeFont)

        val baseFontSize = if (rc.isComment) {
            val scale = if (bookConfig.commentGridType == 4) bookConfig.commentFontZoom else 1f
            val commentFs = bookConfig.getFonts()[fontIndex].second ?: bookConfig.commentFont1Size
            scale * commentFs
        } else {
            bookConfig.getFonts()[fontIndex].first ?: bookConfig.textFont1Size
        }
        var fdgrees = bookConfig.getFonts()[fontIndex].third

        var fsize = baseFontSize
        if (bookConfig.ifFontMetricAdjust){
            fsize *= (if (rc.isComment) commentFontScales[bestFont] else textFontScales[bestFont]) ?: 1f
        }

        var x = pos.x
        var y = pos.y

        if (CharTag.RAISED_HEAD in rc.tags) { y += rh }

        if (rc.isNop) {
            val nopSize = if (rc.isComment) bookConfig.commentCommaNopSize else bookConfig.textCommaNopSize
            val nopX = if (rc.isComment) bookConfig.commentCommaNopX / 2 else bookConfig.textCommaNopX
            val nopY = if (rc.isComment) bookConfig.commentCommaNopY else bookConfig.textCommaNopY
            fsize *= nopSize
            x += cw * nopX
            y -= rh * nopY
            if (y - canvasConfig.marginsBottom < 10){
                y = canvasConfig.marginsBottom + if (rc.isComment) 2 else 5
                if (!rc.isComment && "…—".contains(rc.char)) { y += fsize / 2 }
            }
            if (rc.isRotated) { fdgrees = -90.0 }
        } else {
            if (rc.isComment) {
                if (bookConfig.commentGridType == 4) {
                    if (rc.isTop) { y += rh / 2f }
                    x += (cw - fsize * 2) / 4f
                    y += (rh - fsize * 2) / 4f
                } else {
                    x += (cw - fsize * 2) / 4f
                    y += (rh - fsize) / 2f
                }

                if (rc.isRotated) {
                    fsize *= bookConfig.commentComma90Size
                    x += cw / 2 * bookConfig.commentComma90X
                    y += rh * bookConfig.commentComma90Y
                    fdgrees = -90.0
                }
            } else {
                if (rc.isRotated) {
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

        return CharMetrics(x, y, fsize, fdgrees, fontIndex, bestFont, activeFont, matched, isFirstInColumn, isLastInColumn)
    }

    private fun PDPageContentStream.textlable(x: Float, y: Float, font: PDType0Font, fsize: Float, text: String, color: Color, angle: Double = 0.0) {
        this.beginText()
        this.setNonStrokingColor(color)
        this.setFont(font, fsize)
        if (angle == 0.0) {
            this.newLineAtOffset(x, y)
        } else {
            val rad = Math.toRadians(angle)
            val matrix = Matrix.getRotateInstance(rad, x, y)
            this.setTextMatrix(matrix)
        }
        this.showText(text)
        this.endText()
    }

    private fun PDPageContentStream.drawLine(startX: Float, startY: Float, endX: Float, endY: Float, width: Float, color: Color){
        this.setStrokingColor(color)
        this.setLineWidth(width)
        this.moveTo(startX, startY)
        this.lineTo(endX, endY)
        this.stroke()
    }

    private fun PDPageContentStream.drawCircle(cx: Float, cy: Float, radius: Float, fillColor: Color? = null, strokeColor: Color? = null, lineWidth: Float = 1f) {
        this.appendCirclePath(cx, cy, radius)
        drawPath(fillColor, strokeColor, lineWidth)
    }

    private fun PDPageContentStream.appendCirclePath(cx: Float, cy: Float, radius: Float) {
        val magic = 0.55228475f * radius
        this.moveTo(cx, cy + radius)
        this.curveTo(cx - magic, cy + radius, cx - radius, cy + magic, cx - radius, cy)
        this.curveTo(cx - radius, cy - magic, cx - magic, cy - radius, cx, cy - radius)
        this.curveTo(cx + magic, cy - radius, cx + radius, cy - magic, cx + radius, cy)
        this.curveTo(cx + radius, cy + magic, cx + magic, cy + radius, cx, cy + radius)
    }

    fun PDPageContentStream.drawRoundRect(
        x: Float, y: Float, w: Float, h: Float, r: Float,
        fillColor: Color? = null, strokeColor: Color? = null, lineWidth: Float = 1f
    ) {
        if (fillColor == null && strokeColor == null) return

        val maxR = (w / 2f).coerceAtMost(h / 2f)
        val clampedR = r.coerceIn(0f, maxR)

        if (clampedR <= 0f) {
            addRect(x, y, w, h)
            drawPath(fillColor, strokeColor, lineWidth)
            return
        }

        val magic = 0.55228475f * clampedR
        val xRight = x + w - clampedR
        val yTop = y + h
        val xRightOuter = x + w
        val yBottomOuter = y - clampedR
        val yTopOuter = yTop + clampedR

        moveTo(x + clampedR, yBottomOuter)
        lineTo(xRight, yBottomOuter)
        curveTo(xRight + magic, yBottomOuter, xRightOuter, y - magic, xRightOuter, y)
        lineTo(xRightOuter, yTop)
        curveTo(xRightOuter, yTop + magic, xRight + magic, yTopOuter, xRight, yTopOuter)
        lineTo(x + clampedR, yTopOuter)
        curveTo(x + clampedR - magic, yTopOuter, x, yTop + magic, x, yTop)
        lineTo(x, y)
        curveTo(x, y - magic, x + clampedR - magic, yBottomOuter, x + clampedR, yBottomOuter)
        closePath()

        drawPath(fillColor, strokeColor, lineWidth)
    }

    private fun PDPageContentStream.drawPath(fillColor: Color?, strokeColor: Color?, lineWidth: Float) {
        fillColor?.let { setNonStrokingColor(it) }
        strokeColor?.let {
            setStrokingColor(it)
            setLineWidth(lineWidth)
        }

        when {
            fillColor != null && strokeColor != null -> fillAndStroke()
            fillColor != null -> fill()
            strokeColor != null -> stroke()
        }
    }

    private fun PDPageContentStream.drawWavyLine(x1: Float, y1: Float, x2: Float, y2: Float, color: Color= Color.BLACK, width: Float = 1f) {
        val amplitude = 1.25f // 波浪振幅
        val wavelength = 10f  // 波长

        val dx = x2 - x1
        val dy = y2 - y1
        val length = hypot(dx.toDouble(), dy.toDouble()).toFloat()
        val angle = atan2(dy.toDouble(), dx.toDouble()).toFloat()

        val segments = (length / (wavelength / 5)).toInt().coerceAtLeast(1)
        this.setStrokingColor(color)
        this.setLineWidth(width)
        this.moveTo(x1, y1)

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

            this.lineTo(curX, curY)
        }
        this.stroke()
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