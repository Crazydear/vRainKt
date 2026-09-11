package icu.hearme.vrain.engine

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.ImageComposeScene
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.awt.toAwtColor
import androidx.compose.ui.graphics.drawscope.DrawScope
import icu.hearme.vrain.bookcanvas.BackgroundCanvas
import icu.hearme.vrain.configure.AncientBookState
import icu.hearme.vrain.configure.AncientCanvasState
import icu.hearme.vrain.configure.PageSplitConfig
import icu.hearme.vrain.pdfbox.drawLine
import icu.hearme.vrain.pdfbox.drawPath
import icu.hearme.vrain.pdfbox.drawRoundRect
import org.apache.pdfbox.pdmodel.PDDocument
import org.apache.pdfbox.pdmodel.PDFormContentStream
import org.apache.pdfbox.pdmodel.common.PDRectangle
import org.apache.pdfbox.pdmodel.graphics.form.PDFormXObject
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject
import org.jetbrains.skia.EncodedImageFormat

@OptIn(ExperimentalComposeUiApi::class)
class CanvasEngine(val canvasConfig: AncientCanvasState) {
    private val cw = canvasConfig.canvasWidth
    private val ch = canvasConfig.canvasHeight
    private val lcw = canvasConfig.leafCenterWidth
    private val flm = canvasConfig.fishLineMargin
    private val flc = canvasConfig.fishLineColor.toAwtColor()

    private var commonBgImage: PDImageXObject? = null
    private var commonBgForm: PDFormXObject? = null

    fun createCanvasForm(doc: PDDocument): PDFormXObject {
        commonBgForm?.let { return it }
        val newForm = canvasForm(doc)
        return newForm.also { commonBgForm = it }
    }

    suspend fun createCanvasImg(doc: PDDocument, bookConfig: AncientBookState, page: BookPage? = null): PDImageXObject {
        val hasRaisedHead = page?.chars?.any { it.tags.has(CharTag.RAISED_HEAD) } == true
        if (!hasRaisedHead) { commonBgImage?.let { return it } }
        val bgBytes = renderPageBackgroundToBytes(bookConfig, canvasConfig, page)
        val imageName = if (hasRaisedHead) "bg_page_${page.pageIndex}" else "bg_page_common"
        val newImage = PDImageXObject.createFromByteArray(doc, bgBytes, imageName)
        return if (hasRaisedHead) { newImage } else { newImage.also { commonBgImage = it } }
    }

    private fun canvasForm(doc: PDDocument): PDFormXObject {
        val form = PDFormXObject(doc)
        val ml = canvasConfig.marginsLeft
        val mr = canvasConfig.marginsRight
        val mt = canvasConfig.marginsTop
        val mb = canvasConfig.marginsBottom
        val delta = 5f

        val ilc = canvasConfig.inlineColor.toAwtColor()
        val ilw = canvasConfig.inlineWidth

        val olc = canvasConfig.outlineColor.toAwtColor()
        val olw = canvasConfig.outlineWidth
        val moh = canvasConfig.outlineHMargin
        val mov = canvasConfig.outlineVMargin

        val cln = canvasConfig.leafCol

        val clw = canvasConfig.colW

        val ifmr = canvasConfig.ifMultirows
        val mrn = canvasConfig.multirowsNum
        val mrcc = canvasConfig.multirowsColcolor.toAwtColor()
        val mrlw = canvasConfig.multirowsLinewidth
        val fbd = canvasConfig.fishBtmDirection

        form.bBox = PDRectangle(cw, ch)

        PDFormContentStream(form).use { cs ->
            val innerX = ml
            val innerY = mb - delta
            val innerW = cw - mr - ml
            val innerH = ch - mb - mt + 2 * delta
            cs.drawRoundRect(innerX, innerY, innerW, innerH, 0f, strokeColor = ilc, lineWidth = ilw)

            val outerX = ml - olw / 2f - moh
            val outerY = mb - olw / 2f - mov - delta
            val outerW = cw - mr + olw / 2f + moh - outerX
            val outerH = ch - mt + olw / 2f + mov + delta - outerY
            cs.drawRoundRect(outerX, outerY, outerW, outerH, 0f, strokeColor = olc, lineWidth = olw)

            for (cid in 1..cln) {
                var tilc = if (ifmr && mrn > 1) mrcc else ilc
                if (cid == cln / 2 || cid == cln / 2 + 1) { tilc = ilc }
                val wd = if (cid > cln / 2) lcw - clw else 0f
                val lineX = ml + wd + clw * cid
                cs.drawLine(lineX, ch - mt, lineX, mb, ilw, tilc)
            }

            if (ifmr && mrn > 1) {
                val mrh = (ch - mt - mb) / mrn
                for (rid in 1..<mrn) {
                    val lineY = ch - (mt + rid * mrh)
                    cs.drawLine(ml, lineY, cw / 2f - lcw / 2f, lineY, mrlw, ilc)
                    cs.drawLine(cw - mr, lineY, cw / 2f + lcw / 2f, lineY, mrlw, ilc)
                }
            }
            cs.drawFish(true, fbd)
            cs.drawFish(false, fbd)
            cs.drawLine(cw / 2, ch - mt + mov + delta, cw / 2 , ch - canvasConfig.fishTopY + flm, canvasConfig.fishTopLinewidth,flc)
            cs.drawLine(cw / 2, ch - canvasConfig.fishBtmY-flm, cw / 2 , mb - mov - delta, canvasConfig.fishBtmLinewidth, flc)
        }
        return form
    }

    private fun PDFormContentStream.drawFish(isTop: Boolean, fbd: Int) {
        val flw = canvasConfig.fishLineWidth
        val iff = canvasConfig.ifFishflower
        val fy = ch - if (isTop) canvasConfig.fishTopY else canvasConfig.fishBtmY
        val dy1 = if (isTop) canvasConfig.fishTopRectHeight else canvasConfig.fishBtmRectHeight
        val dy2 = if (isTop) canvasConfig.fishTopTriaHeight else canvasConfig.fishBtmTriaHeight
        val fc = (if (isTop) canvasConfig.fishTopColor else canvasConfig.fishBtmColor).toAwtColor()

        val xCenter = cw / 2f
        val xLeft = xCenter - lcw / 2f
        val xRight = xCenter + lcw / 2f
        this.drawLine(xLeft, fy+flm, xRight, fy+flm, flm, fc)

        if (isTop || fbd == 0) {
            this.fishPath(xLeft, fy, xRight, fy, xRight, fy - dy1 - dy2, xCenter, fy - dy1, xLeft, fy - dy1 - dy2)
        } else {
            this.fishPath(xLeft, fy, xRight, fy, xRight, fy + dy1 + dy2, xCenter, fy + dy1, xLeft, fy + dy1 + dy2)
        }
        this.drawPath(fc, if (isTop) null else flc, flw)

        if (!iff && (isTop || fbd == 0)) {
            this.drawLine(xLeft, fy - dy1 - dy2 - flm, xCenter, fy - dy1 - flm, flw, flc)
            this.drawLine(xCenter, fy - dy1 - flm,xRight, fy - dy1 - dy2 - flm, flw, flc)
        } else if (!iff && fbd == 1){
            this.drawLine(xLeft, fy + dy1 + dy2 + flm, xCenter, fy + dy1 + flm, flw, flc)
            this.drawLine(xCenter, fy + dy1 + flm,xRight, fy + dy1 + dy2 + flm, flw, flc)
        }
    }

    private fun PDFormContentStream.fishPath(x1: Float, y1: Float, x2: Float, y2: Float, x3: Float, y3: Float,x4: Float, y4: Float,x5: Float, y5: Float) {
        this.moveTo(x1, y1)
        this.lineTo(x2, y2)
        this.lineTo(x3, y3)
        this.lineTo(x4, y4)
        this.lineTo(x5, y5)
        this.closePath()
    }
}

suspend fun renderPageBackgroundToBytes(
    bookConfig: AncientBookState,
    canvasConfig: AncientCanvasState,
    page: BookPage? = null
): ByteArray {

    val width = canvasConfig.canvasWidth.toInt()
    val height = canvasConfig.canvasHeight.toInt()
    val psConfig = PageSplitConfig()
    val scene = ImageComposeScene(width = width, height = height)
    val grid = BookGridEngine.calculateGrid(canvasConfig, bookConfig)

    var drawRaisedHeadOverlays: DrawScope.() -> Unit = {  }
    if (page != null && page.chars.any { it.tags.has(CharTag.RAISED_HEAD) }) {
        drawRaisedHeadOverlays = {
            val mt = canvasConfig.marginsTop
            val mb = canvasConfig.marginsBottom
            val clw = (canvasConfig.canvasWidth - canvasConfig.marginsLeft - canvasConfig.marginsRight - canvasConfig.leafCenterWidth) / canvasConfig.leafCol.toFloat()
            val rh = (canvasConfig.canvasHeight - mt - mb) / bookConfig.rowNum.toFloat()

            val ilc = canvasConfig.inlineColor
            val olc = canvasConfig.outlineColor
            var ohm = canvasConfig.outlineHMargin
            if (ohm > 0.2f * clw) { ohm = 0.15f * clw }
            var ovm = canvasConfig.outlineVMargin
            if (ovm > 0.3f * rh) { ovm = 0.3f * rh }
            val ilw = canvasConfig.inlineWidth
            val olw = canvasConfig.outlineWidth
            val raisedHeadChars = page.chars.filter { it.tags.has(CharTag.RAISED_HEAD) }
            for (i in raisedHeadChars.indices) {
                val renderChar = raisedHeadChars[i]
                val slot = renderChar.pcntIndex.toInt().coerceIn(0, grid.charsPerPage - 1)
                val basePos = grid.mainPositions[slot]
                if (canvasConfig.outlineVMargin < rh + 5) {
                    // 1. 外粗线框延伸 (底层黑块)
                    drawRect(
                        color = olc,
                        topLeft = Offset(basePos.x - ohm - olw, mt - rh - ovm - olw / 2 - 5),
                        size = Size(clw + ohm * 2 + olw * 2, rh + olw / 2)
                    )

                    // 2. 外粗线框覆盖 (上层画布底色块，凿空内部并覆盖下沿)
                    drawRect(
                        color = Color.Transparent,
                        topLeft = Offset(basePos.x - ohm, mt - rh - ovm - 5 + olw / 2),
                        size = Size(clw + ohm * 2, rh + ovm),
                        blendMode = BlendMode.Clear
                    )
                }

                // 3. 内细线框延伸
                drawRect(
                    color = ilc,
                    topLeft = Offset(basePos.x, mt - rh - ilw / 2 - 5),
                    size = Size(clw, rh + ilw / 2)
                )
                // 4. 内细线框覆盖 (上层画布底色块，凿空内部并覆盖下沿)
                drawRect(
                    color = Color.Transparent,
                    topLeft = Offset(basePos.x + ilw, mt - rh + ilw / 2 - 5),
                    size = Size(clw - ilw * 2, rh + ilw * 4),
                    blendMode = BlendMode.Clear
                )
            }
        }
    }

    scene.setContent {
        BackgroundCanvas(canvasConfig, psConfig, onDrawOverlays = drawRaisedHeadOverlays)
    }

    val skiaImage = scene.render()
    scene.close()

    val data = skiaImage.encodeToData(EncodedImageFormat.PNG)
        ?: throw IllegalStateException("Failed to encode Skia Image to PNG")

    return data.bytes
}