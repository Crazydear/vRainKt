package icu.hearme.vrain.bookcanvas

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.ImageBitmapConfig
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import icu.hearme.vrain.bookcanvas.DrawModel.drawArcFishTail
import icu.hearme.vrain.bookcanvas.DrawModel.drawFishBtmDown
import icu.hearme.vrain.bookcanvas.DrawModel.drawFishBtmUp
import icu.hearme.vrain.bookcanvas.DrawModel.drawFishFlowerDecorations
import icu.hearme.vrain.bookcanvas.DrawModel.drawFishTop
import icu.hearme.vrain.configure.AncientBookSplitType
import icu.hearme.vrain.configure.AncientCanvasState
import icu.hearme.vrain.configure.FontManager
import icu.hearme.vrain.configure.PageSplitConfig
import icu.hearme.vrain.configure.rememberImageBitmapFromString
import kotlin.math.roundToInt

@Composable
fun BackgroundCanvas(config: AncientCanvasState, psConfig: PageSplitConfig, modifier: Modifier = Modifier) {
    val bgBitmap = rememberImageBitmapFromString(config.canvasBackgroundImage)
    val fishFlowerBitmap = rememberImageBitmapFromString(config.fishFlowerImage)
    val logoBitmap = rememberImageBitmapFromString(config.logoImage)
    val customFontFamily = FontManager.getFontFamily(config.logoFont)
    val textMeasurer = rememberTextMeasurer()
    val isVintage = config.isVintage
    val cw = config.canvasWidth
    val ch = config.canvasHeight
    val density = LocalDensity.current
    val multiplyPaint = remember { Paint().apply { blendMode = BlendMode.Multiply } }
    val inkLayerRect = remember(cw, ch) { Rect(0f, 0f, cw, ch) }
    val ilw = config.inlineWidth
    val olw = config.outlineWidth
    val inlineStroke = remember(ilw) { Stroke(width = ilw) }
    val outlineStroke = remember(olw) { Stroke(width = olw) }

    val inkSpotsBitmap = remember(cw, ch, isVintage) {
        val widthInt = cw.roundToInt()
        val heightInt = ch.roundToInt()

        if (widthInt <= 0 || heightInt <= 0 || !isVintage) {
            ImageBitmap(1, 1)
        } else {
            val bitmap = ImageBitmap(widthInt, heightInt, ImageBitmapConfig.Argb8888)
            val canvas = Canvas(bitmap)
            val drawScope = CanvasDrawScope()

            drawScope.draw(density, LayoutDirection.Ltr, canvas, Size(cw, ch)) {
                val random = kotlin.random.Random(42)
                repeat(2000) {
                    val px = random.nextFloat() * cw
                    val py = random.nextFloat() * ch
                    val size = 10f + random.nextInt(40)

                    val brush = Brush.radialGradient(
                        0.0f to Color.White.copy(alpha = 0.8f),
                        0.5f to Color.White.copy(alpha = 0.3f),
                        1.0f to Color.Transparent,
                        center = Offset(size / 2f, size / 2f),
                        radius = size * 0.6f
                    )
                    val ovalWidth = size * 0.6f
                    val ovalHeight = size * 0.4f
                    val topLeftX = size * 0.2f
                    val topLeftY = size * 0.3f
                    withTransform({
                        translate(px - size * 0.4f, py)
                        rotate(random.nextFloat() * 45f - 22.5f, Offset(size / 2f, size / 2f))
                    }) {
                        drawOval(brush = brush, topLeft = Offset(topLeftX, topLeftY), size = Size(ovalWidth, ovalHeight))
                    }
                }
            }
            bitmap
        }
    }

    val bambooBitmap = remember(cw, ch, config.leafCol, config.marginsLeft, config.marginsTop, config.marginsBottom) {
        val widthInt = cw.roundToInt()
        val heightInt = ch.roundToInt()

        if (widthInt <= 0 || heightInt <= 0) {
            ImageBitmap(1, 1)
        } else {
            val bitmap = ImageBitmap(widthInt, heightInt, ImageBitmapConfig.Argb8888)
            val canvas = Canvas(bitmap)
            val drawScope = CanvasDrawScope()

            drawScope.draw(density, LayoutDirection.Ltr, canvas, Size(cw, ch)) {
                val random = kotlin.random.Random(42)

                val hm = config.marginsLeft
                val itm = config.marginsTop
                val ibm = config.marginsBottom
                val cln = config.leafCol

                val bc1 = Color(233, 189, 96)         // 竹简色
                val shadowColor = Color(0xFFCCCCCC)   // 阴影色
                val bc3 = Color(148, 112, 55)         // 韦编色

                val slipColW = (cw - hm * 2) / cln                      // 列宽

                for (i in 0 until cln) {
                    val tm = itm - 50f - random.nextFloat() * 6f    // 竹简上边高度增加些微随机
                    val bm = ibm - 50f - random.nextFloat() * 6f    // 竹简下边高度增加些微随机

                    val left = hm + slipColW * i + slipColW * 0.05f
                    val right = hm + slipColW * (i + 1) - slipColW * 0.05f
                    val top = tm
                    val bottom = ch - bm

                    // 竹简
                    drawRect(
                        color = bc1,
                        topLeft = Offset(left, top),
                        size = Size(right - left, bottom - top)
                    )

                    drawLine(shadowColor, Offset(right, bottom), Offset(right, top), strokeWidth = 2f)  // 右阴影线
                    drawLine(shadowColor, Offset(right, bottom), Offset(left, bottom), strokeWidth = 2f)// 下阴影线

                    // 韦编
                    val l1x = hm + slipColW * i - slipColW * 0.025f
                    val l1y = itm - 15f
                    val l2x = hm + slipColW * i
                    val l2y = itm - 10f
                    val l3x = hm + slipColW * (i + 1)
                    val l3y = itm - 10f
                    val l4x = hm + slipColW * (i + 1) + slipColW * 0.025f
                    val l4y = itm - 15f
                    val ld = slipColW / 10f

                    for (j in 0..9) {
                        if (j == 5) continue
                        val t1x = l1x + ld * j
                        val t2x = l1x + ld * (j + 1)
                        val t3x = hm + slipColW * i + ld * j
                        // 交叉绳纹
                        drawLine(bc3, Offset(t1x, l1y), Offset(t3x, l2y), strokeWidth = 2f)
                        drawLine(bc3, Offset(t2x, l1y), Offset(t3x, l2y), strokeWidth = 2f)
                    }
                    // 横向主绳
                    drawLine(bc3, Offset(l2x, l2y), Offset(l3x, l3y), strokeWidth = 1f)
                    drawLine(bc3, Offset(l1x, l1y), Offset(l4x, l4y), strokeWidth = 2f)

                    // 下方韦编
                    val b1y = ch - ibm + 15f
                    val b2y = ch - ibm + 10f
                    val b3y = ch - ibm + 10f
                    val b4y = ch - ibm + 15f

                    for (j in 0..9) {
                        if (j == 5) continue
                        val t1x = l1x + ld * j
                        val t2x = l1x + ld * (j + 1)
                        val t3x = hm + slipColW * i + ld * j
                        // 交叉绳纹
                        drawLine(bc3, Offset(t1x, b1y), Offset(t3x, b2y), strokeWidth = 2f)
                        drawLine(bc3, Offset(t2x, b1y), Offset(t3x, b2y), strokeWidth = 2f)
                    }
                    // 横向主绳
                    drawLine(bc3, Offset(l2x, b2y), Offset(l3x, b3y), strokeWidth = 1f)
                    drawLine(bc3, Offset(l1x, b1y), Offset(l4x, b4y), strokeWidth = 2f)

                    // 竹简纹理
                    val rlc = 30
                    for (k in 0..rlc) {
                        val ci = 220 + random.nextInt(35)
                        val rc = Color(ci, ci, ci, 100)

                        val rx = left + slipColW * 0.9f * (random.nextInt(10) / 10f)
                        val r1y = itm + 100f + (ch - itm - ibm - 100f) * random.nextFloat()
                        val r2y = itm + 100f + (ch - itm - ibm - 100f) * random.nextFloat()

                        drawLine(
                            color = rc,
                            start = Offset(rx, r1y),
                            end = Offset(rx, r2y),
                            strokeWidth = random.nextFloat() * 3f
                        )
                    }
                }
            }
            bitmap
        }
    }

    val lgt = config.logoText
    val lgc = config.logoColor
    val lgs = config.logoFontSize
    val logoTextLayoutResults = remember(lgt, lgc, lgs, customFontFamily, density, textMeasurer) {
        if (lgt.isNullOrEmpty()) {
            emptyList()
        } else {
            val baseStyle = TextStyle(
                color = lgc,
                fontSize = with(density) { lgs.toSp() },
                fontFamily = customFontFamily
            )
            val strokeStyle = baseStyle.copy(drawStyle = Stroke(width = 1f))

            lgt.map { char ->
                val charStr = char.toString()
                val normalLayout = textMeasurer.measure(text = charStr, style = baseStyle)
                val strokeLayout = textMeasurer.measure(text = charStr, style = strokeStyle)
                normalLayout to strokeLayout
            }
        }
    }

    Canvas(modifier = modifier.fillMaxSize().background(config.canvasColor).graphicsLayer {
        compositingStrategy = CompositingStrategy.Offscreen
    }) {

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
            drawContext.canvas.saveLayer(inkLayerRect, multiplyPaint)
            // 竹简
            if (config.bamboo) {
                drawImage(bambooBitmap, blendMode = BlendMode.SrcOver)
                return@withTransform
            }
            val mt = config.marginsTop
            val mb = config.marginsBottom
            val ml = config.marginsLeft
            val mr = config.marginsRight

            val ilc = config.inlineColor
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
                style = inlineStroke
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
                style = outlineStroke
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
            drawImage(inkSpotsBitmap, blendMode = BlendMode.SrcOver) // 做旧
            drawContext.canvas.restore()
            // 版心底部的logo
            val lgy = config.logoY

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
            } else if (logoTextLayoutResults.isNotEmpty()) {
                logoTextLayoutResults.forEachIndexed { index, (normalLayout, strokeLayout) ->
                    val posX = cw / 2f - normalLayout.size.width / 2f
                    val posY = lgy + normalLayout.size.height * (index-1)

                    // 渲染标准字体
                    drawText(normalLayout, topLeft = Offset(posX, posY))

                    // 渲染描边加粗
                    drawText(strokeLayout, topLeft = Offset(posX, posY))
                }
            }
        }
    }
}