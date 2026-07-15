package icu.hearme.vrain.bookcanvas

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import icu.hearme.vrain.configure.AncientBookSplitType
import icu.hearme.vrain.configure.AncientBookState
import icu.hearme.vrain.configure.AncientCanvasState
import icu.hearme.vrain.configure.FontManager
import icu.hearme.vrain.configure.PageSplitConfig
import icu.hearme.vrain.engine.BookGrid
import icu.hearme.vrain.engine.BookPage
import icu.hearme.vrain.engine.CharTag
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

@Composable
fun TextLayerCanvas(
    page: BookPage, grid: BookGrid,
    bookConfig: AncientBookState, canvasConfig: AncientCanvasState,
    psConfig: PageSplitConfig, modifier: Modifier = Modifier
) {
    val textFont = FontManager.getFontFamily(bookConfig.getFontList(bookConfig.textFontsArray))
    val commentFont = FontManager.getFontFamily(bookConfig.getFontList(bookConfig.commentFontsArray))

    val textMeasurer = rememberTextMeasurer()
    val density = LocalDensity.current
    val cw = canvasConfig.canvasWidth
    val ch = canvasConfig.canvasHeight
    val colW = (cw - canvasConfig.marginsLeft - canvasConfig.marginsRight - canvasConfig.leafCenterWidth) / canvasConfig.leafCol.toFloat()
    val rh = (ch - canvasConfig.marginsTop - canvasConfig.marginsBottom) / bookConfig.rowNum.toFloat()

    val textFontSize = bookConfig.textFont1Size
    val commentFontSize = bookConfig.commentFont1Size

    val multiplyPaint = remember { Paint().apply { blendMode = BlendMode.Multiply } }
    val charLayerRect = remember(cw, ch) { Rect(0f, 0f, cw, ch) }

    Canvas(modifier = modifier.fillMaxSize().graphicsLayer {
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
        if (canvasConfig.isFullpage) { resolvedSplitMode = false }

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
            drawContext.canvas.saveLayer(charLayerRect, multiplyPaint)

            page.chars.forEach { renderChar ->
                val slot = renderChar.pcntIndex.toInt().coerceIn(0, grid.charsPerPage - 1)

                var basePos = if (renderChar.isComment) {
                    val isRightHalf = (renderChar.pcntIndex - slot) == 0f
                    if (isRightHalf) grid.subPositions[slot] else grid.mainPositions[slot]
                } else {
                    grid.mainPositions[slot]
                }

                val activeFontFamily = if (renderChar.isComment) commentFont else textFont
                var fSize = if (renderChar.isComment) commentFontSize else textFontSize
                if (renderChar.isNop) {
                    val nopSize = if (renderChar.isComment) bookConfig.commentCommaNopSize else bookConfig.textCommaNopSize
                    fSize *= nopSize
                }
                var fontStyle = TextStyle(
                    color = if (renderChar.isComment) bookConfig.commentFontColor else bookConfig.textFontColor,
                    fontSize = with(density) { fSize.toSp() },
                    fontFamily = activeFontFamily
                )
                val layoutResult = textMeasurer.measure(
                    text = renderChar.char.toString(),
                    style = fontStyle
                )
                val textH = layoutResult.size.height.toFloat()
                var finalX = basePos.x
                var finalY = basePos.y

                if (renderChar.isNop) {
                    val nopX = if (renderChar.isComment) bookConfig.commentCommaNopX else bookConfig.textCommaNopX
                    val nopY = if (renderChar.isComment) bookConfig.commentCommaNopY else bookConfig.textCommaNopY

                    finalX += (colW / if (renderChar.isComment) 2f else 1f) * nopX

                    finalY += rh * (if (renderChar.isComment) 0.85f else 0.5f)
                    finalY -= rh * nopY
                    fontStyle = fontStyle.copy(color = bookConfig.commaColor)
                } else {
                    val offsetX = if (renderChar.isComment) {
                        (colW / 2f - fSize) / 2f
                    } else {
                        (colW - fSize) / 2f
                    }

                    val offsetY =  if (renderChar.isComment) {
                        (rh - textH) / 1f
                    } else {
                        (rh - textH) / 0.5f
                    }

                    finalX += offsetX
                    finalY += offsetY
                }

                basePos = basePos.copy(x = finalX, y = finalY)

                if (CharTag.RAISED_HEAD in renderChar.tags) {
                    basePos = basePos.copy(y = basePos.y - rh)
                }

                if (CharTag.ZOOM_IN in renderChar.tags) {
                    basePos = basePos.copy(x = basePos.x + fSize * (1 - bookConfig.textZoom) / 2)
                    fSize *= bookConfig.textZoom
                    fontStyle = fontStyle.copy(fontSize = with(density) { fSize.toSp() })
                }

                if (CharTag.RECT_FRAME in renderChar.tags) {
                    val r = 10f
                    val rx = basePos.x + r
                    val ry = basePos.y - rh * bookConfig.textRectY
                    val rhg = fSize * (1 + bookConfig.textRectH)

                    drawRoundRect(
                        color = bookConfig.rectBcolor,
                        topLeft = Offset(rx - 2f, ry - 2f),
                        size = Size(fSize - 2 * r + 4f, rhg + 4f),
                        cornerRadius = CornerRadius(r, r),
                        style = if (bookConfig.rectType == 0) Stroke(width = 2f) else androidx.compose.ui.graphics.drawscope.Fill
                    )
                    fontStyle = fontStyle.copy(color = bookConfig.rectFcolor)
                }

                if (CharTag.CIRCLE_NOTE in renderChar.tags) {
                    drawCircle(
                        color = bookConfig.textNoteOc,
                        radius = fSize * bookConfig.textNoteOr,
                        center = Offset(basePos.x + colW / 2f + fSize * bookConfig.textNoteOx, basePos.y + fSize * bookConfig.textNoteOy),
                        style = Stroke(width = bookConfig.textNoteOw)
                    )
                }

                if (CharTag.BOOK_LINE in renderChar.tags) {
                    val waveTop = basePos.y - rh * 0.2f
                    val waveBottom = basePos.y + rh * 0.8f
                    val waveX = basePos.x - 2f

                    val wavePath = createWavyLinePath(
                        start = Offset(waveX, waveTop),
                        end = Offset(waveX, waveBottom),
                        amplitude = 1.5f,
                        wavelength = 8f
                    )
                    drawPath(
                        path = wavePath,
                        color = bookConfig.bookLineColor,
                        style = Stroke(width = bookConfig.bookLineWidth + 1f)
                    )
                }

                // 终极印字 (支持英文字母及拼音的 90 度自动翻转直排)
                withTransform({
                    translate(basePos.x, basePos.y)
                    if (renderChar.isRotated) {
                        translate(fSize / 4f, fSize / 2f)
                        rotate(-90f, Offset.Zero)
                    }
                }) {
                    drawText(layoutResult, color = fontStyle.color)

                    // 粗体描边效果（由配置控制）
                    if (bookConfig.ifFallbackBold) {
                        drawText(
                            textLayoutResult = layoutResult,
                            drawStyle = Stroke(width = bookConfig.fallbackBoldStrokeWidth)
                        )
                    }
                }
            }
            drawContext.canvas.restore()
        }
    }
}

private fun createWavyLinePath(start: Offset, end: Offset, amplitude: Float, wavelength: Float): Path {
    val path = Path()
    val dx = end.x - start.x
    val dy = end.y - start.y
    val length = sqrt(dx * dx + dy * dy)
    val angle = atan2(dy, dx)

    val segments = (length / (wavelength / 5)).toInt().coerceAtLeast(1)

    path.moveTo(start.x, start.y)

    for (i in 1..segments) {
        val t = i.toFloat() / segments
        val distance = t * length
        val waveOffset = amplitude * sin(2 * Math.PI * distance / wavelength).toFloat()

        val perpX = -sin(angle) * waveOffset
        val perpY = cos(angle) * waveOffset

        val x = start.x + cos(angle) * distance + perpX
        val y = start.y + sin(angle) * distance + perpY

        path.lineTo(x, y)
    }
    return path
}
