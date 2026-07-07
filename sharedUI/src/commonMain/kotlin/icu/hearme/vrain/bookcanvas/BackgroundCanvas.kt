package icu.hearme.vrain.bookcanvas

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import icu.hearme.vrain.bookcanvas.DrawModel.drawArcFishTail
import icu.hearme.vrain.bookcanvas.DrawModel.drawFishBtmDown
import icu.hearme.vrain.bookcanvas.DrawModel.drawFishBtmUp
import icu.hearme.vrain.bookcanvas.DrawModel.drawFishFlowerDecorations
import icu.hearme.vrain.bookcanvas.DrawModel.drawFishTop
import icu.hearme.vrain.configure.AncientBookSplitType
import icu.hearme.vrain.configure.AncientCanvasState
import icu.hearme.vrain.configure.PageSplitConfig
import icu.hearme.vrain.configure.rememberImageBitmapFromString
import org.jetbrains.compose.resources.Font
import vrain.sharedui.generated.resources.Res
import vrain.sharedui.generated.resources.qiji_combo
import kotlin.math.roundToInt

@Composable
fun BackgroundCanvas(config: AncientCanvasState, psConfig: PageSplitConfig, modifier: Modifier = Modifier) {
    val bgBitmap = rememberImageBitmapFromString(config.canvasBackgroundImage)
    val fishFlowerBitmap = rememberImageBitmapFromString(config.fishFlowerImage)
    val logoBitmap = rememberImageBitmapFromString(config.logoImage)
    val customFontFamily = FontFamily(Font(Res.font.qiji_combo))
    val textMeasurer = rememberTextMeasurer()

    Canvas(modifier = modifier.fillMaxSize().background(config.canvasColor).graphicsLayer {
        compositingStrategy = CompositingStrategy.Offscreen
    }) {
        val cw = config.canvasWidth
        val ch = config.canvasHeight
        val pageNum = psConfig.pageNumber.value
        var resolvedSplitMode = when (psConfig.splitType.value) {
            AncientBookSplitType.SPLIT_BY_PAGE -> true
            AncientBookSplitType.FULL_PAGE -> false
            AncientBookSplitType.AUTO -> {
                val containerRatio = size.width / size.height
                containerRatio < 1.1f
            }
        }
        if (config.isFullpage) { resolvedSplitMode = false }

        val targetW = if (resolvedSplitMode) cw / 2f else cw
        val targetH = ch

        val scale = minOf(size.width / targetW, size.height / targetH)
        val offsetX = (size.width - targetW * scale) / 2f
        val offsetY = (size.height - targetH * scale) / 2f

        withTransform({
            translate(offsetX, offsetY)
            scale(scale, scale, Offset.Zero)

            if (resolvedSplitMode) {
                if (pageNum % 2 != 0) {
                    translate(-cw / 2f, 0f)
                }
                clipRect(
                    left = if (pageNum % 2 != 0) cw / 2f else 0f,
                    top = 0f,
                    right = if (pageNum % 2 != 0) cw else cw / 2f,
                    bottom = ch
                )
            }
        }) {
            // 使用背景图
            if (bgBitmap != null) {
                drawImage(image = bgBitmap, dstSize = IntSize(cw.roundToInt(), ch.roundToInt()))
            } else {
                drawRect(color = config.canvasColor, size = Size(cw, ch))
            }

            val mt = config.marginsTop
            val mb = config.marginsBottom
            val ml = config.marginsLeft
            val mr = config.marginsRight

            val ilw = config.inlineWidth
            val ilc = config.inlineColor
            val olw = config.outlineWidth
            val olc = config.outlineColor
            val moh = config.outlineHMargin
            val mov = config.outlineVMargin

            val cln = config.leafCol
            val lcw = config.leafCenterWidth
            val ifmr = config.ifMultirows
            val mrn = config.multirowsNum
            val mrcc = config.multirowsColcolor

            val delta = 5f  // 标准间距
            val gr = 0.618f // 黄金分割率
            // 细内框
            val innerX1 = ml
            val innerY1 = mt - delta
            val innerX2 = cw - mr
            val innerY2 = ch - mb + delta

            drawRect(
                color = ilc,
                topLeft = Offset(innerX1, innerY1),
                size = Size(innerX2 - innerX1, innerY2 - innerY1),
                style = Stroke(width = ilw)
            )

            // 粗外框
            val outerX1 = ml - olw / 2f - moh
            val outerY1 = mt - olw / 2f - mov - delta
            val outerX2 = cw - mr + olw / 2f + moh
            val outerY2 = ch - mb + olw / 2f + mov + delta

            drawRect(
                color = olc,
                topLeft = Offset(outerX1, outerY1),
                size = Size(outerX2 - outerX1, outerY2 - outerY1),
                style = Stroke(width = olw)
            )

            // 列细线
            val clw = (cw - ml - mr - lcw) / cln

            for (cid in 1..cln) {
                var tilc = if (ifmr && mrn > 1) mrcc else ilc

                if (cid == cln / 2 || cid == cln / 2 + 1) {
                    tilc = ilc
                }
                val wd = if (cid > cln / 2) (lcw - clw) else 0f

                val lineX = ml + wd + clw * cid

                drawLine(
                    color = tilc,
                    start = Offset(x = lineX, y = mt),
                    end = Offset(x = lineX, y = ch - mb),
                    strokeWidth = ilw
                )
            }
            val centerX = cw / 2f
            // 多栏模式时打印分栏横线
            if (ifmr && mrn > 1) {
                val mrlw = config.multirowsLinewidth

                val mrh = (ch - mt - mb) / mrn
                for (rid in 1 until mrn) {
                    val y = mt + rid * mrh
                    drawLine(ilc,Offset(ml, y), Offset(centerX - lcw / 2f, y), mrlw)
                    drawLine(ilc,Offset(cw - mr, y),Offset(centerX + lcw / 2f, y), mrlw)
                }
            }
            val iff = config.ifFishflower
            val flm = config.fishLineMargin
            val flw = config.fishLineWidth
            val flc = config.fishLineColor
            val ftc = config.fishTopColor
            val fbc = config.fishBtmColor

            // 上鱼尾
            val fty = config.fishTopY
            val ftrh = config.fishTopRectHeight
            val ftth = config.fishTopTriaHeight

            drawFishTop(
                centerX = centerX, lcw = lcw, fy = fty, dy1 = ftrh, dy2 = ftth,
                flm = flm, flc = flc, flw = flw, ftc = ftc, iff = iff
            )

            // 下鱼尾参数
            val fby = config.fishBtmY
            val fbrh = config.fishBtmRectHeight
            val fbth = config.fishBtmTriaHeight
            val fbd = config.fishBtmDirection

            // 下鱼尾
            if (fbd == 0) { // 顺鱼尾
                drawFishBtmDown(
                    centerX = centerX, lcw = lcw, fy = fby, dy1 = fbrh, dy2 = fbth,
                    flm = flm, flc = flc, flw = flw, fbc = fbc, iff = iff
                )
            } else if (fbd == 1) { // 对鱼尾
                drawFishBtmUp(
                    centerX = centerX, lcw = lcw, fy = fby, dy1 = fbrh, dy2 = fbth,
                    flm = flm, flc = flc, flw = flw, fbc = fbc, iff = iff
                )
            }
            // 鱼尾装饰图，要求：正方形，透明底色，主体图案为白色
            drawFishFlowerDecorations(
                flowerBitmap = fishFlowerBitmap,
                centerX = centerX, lcw = lcw, delta = delta, gr = gr,
                fty = fty, ftrh = ftrh,
                fby = fby, fbrh = fbrh, fbth = fbth, fbd = fbd
            )

            // 花鱼尾，弧形花鱼尾
            if (iff) { // 弧形花鱼尾图层
                drawArcFishTail(centerX, lcw, fty + ftrh, ftth, ftc)

                // 下花鱼尾
                if (fbrh > 0f && fbth > 0f) {
                    drawArcFishTail(centerX, lcw,
                        yBase = if (fbd == 0) fby + fbrh else fby - fbrh,
                        ftth = fbth,
                        fbc,
                        fbd = fbd,
                        isBottom = true
                    )
                }
            }

            val ftlw = config.fishTopLinewidth
            val fblw = config.fishBtmLinewidth

            // 版心鱼尾到上下边框的粗线
            if (ftlw > 0f) {
                drawLine(flc, Offset(centerX, mt - mov - delta), Offset(centerX, fty - flm), ftlw)
            }

            if (fblw > 0f) {
                drawLine(flc, Offset(centerX, fby + flm), Offset(centerX, ch - mb + mov + delta), fblw)
            }
            // 版心底部的logo
            val lgy = config.logoY ?: (ch - mb - 300f)
            val lgs = config.logoFontSize ?: 40f
            val lgc = config.logoColor
            val lgt = config.logoText

            if (logoBitmap != null) {
                val scale = 1f / 3f
                val scaledW = logoBitmap.width * scale
                val scaledH = logoBitmap.height * scale
                val posX = cw / 2f + lcw / 4f - scaledW / 2f
                val posY = ch - mb - scaledH

                drawImage(
                    image = logoBitmap,
                    dstSize = IntSize(scaledW.roundToInt(), scaledH.roundToInt()),
                    dstOffset = IntOffset(posX.roundToInt(), posY.roundToInt())
                )
            } else if (!lgt.isNullOrEmpty()) {
                val chars = lgt.toCharArray()

                val textStyle = TextStyle(
                    color = lgc,
                    fontSize = with(this) { lgs.toSp() },
                    fontFamily = customFontFamily
                )

                chars.forEachIndexed { index, char ->
                    val textLayoutResult = textMeasurer.measure(
                        text = char.toString(),
                        style = textStyle
                    )

                    val posX = cw / 2f
                    val posY = lgy + textLayoutResult.size.height * index
                    drawText(
                        textLayoutResult = textLayoutResult,
                        topLeft = Offset(posX, posY),
                    )

                    drawText(
                        textLayoutResult = textMeasurer.measure(
                            text = char.toString(),
                            style = textStyle.copy(drawStyle = Stroke(width = 1f))
                        ),
                        topLeft = Offset(posX, posY)
                    )
                }
            }
        }
    }
}