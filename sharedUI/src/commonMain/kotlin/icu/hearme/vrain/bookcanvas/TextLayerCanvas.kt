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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import icu.hearme.vrain.configure.AncientBookSplitType
import icu.hearme.vrain.configure.AncientBookState
import icu.hearme.vrain.configure.AncientCanvasState
import icu.hearme.vrain.configure.FontManager
import icu.hearme.vrain.configure.PageSplitConfig
import icu.hearme.vrain.configure.getZhPageNum
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

    var textFontSize = bookConfig.textFont1Size
    var commentFontSize = bookConfig.commentFont1Size
    val maxTextSize = minOf(rh, colW) * 0.95f
    if (textFontSize > maxTextSize) {
        val scaleRatio = maxTextSize / textFontSize
        textFontSize = maxTextSize
        commentFontSize *= scaleRatio
    }

    val maxCommentSize = minOf(rh, colW / 2f) * 0.95f
    if (commentFontSize > maxCommentSize) {
        commentFontSize = maxCommentSize
    }
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
            val textDrawCommands = mutableListOf<() -> Unit>()
            page.chars.forEachIndexed { index, renderChar ->
                
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
                    fontFamily = activeFontFamily,
                    fontWeight = if (renderChar.isComment) FontWeight.Normal else FontWeight.SemiBold
                )

                var layoutResult = textMeasurer.measure(renderChar.char.toString(), fontStyle)
                val baseline = layoutResult.firstBaseline.takeIf { !it.isNaN() } ?: fSize
                val ascent = -baseline
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
                    val offsetY = (rh - fSize) / 2f - ascent - textH
                    finalX += offsetX
                    finalY += offsetY
                }

                basePos = basePos.copy(x = finalX, y = finalY)

                val cellLeft = grid.mainPositions[slot].x
                val cellTop = grid.mainPositions[slot].y
                val visualLeftX = cellLeft + (colW - fSize) / 2f
                val visualTopY = cellTop + (rh - fSize) / 2f

                if (CharTag.RAISED_HEAD in renderChar.tags) {
                    basePos = basePos.copy(y = basePos.y - rh)
                }

                if (CharTag.ZOOM_IN in renderChar.tags) {
                    basePos = basePos.copy(x = basePos.x + fSize * (1 - bookConfig.textZoom) / 2)
                    fSize *= bookConfig.textZoom
                    fontStyle = fontStyle.copy(fontSize = with(density) { fSize.toSp() })
                }

                if (CharTag.CIRCLE_NOTE in renderChar.tags && renderChar.char != ' ') {
                    val ox = visualLeftX + colW / 2f + fSize * bookConfig.textNoteOx
                    val oy = visualTopY + fSize * bookConfig.textNoteOy
                    val or = fSize * bookConfig.textNoteOr

                    drawCircle(
                        color = bookConfig.textNoteOc,
                        radius = or,
                        center = Offset(ox, oy),
                        style = Stroke(width = bookConfig.textNoteOw)
                    )
                }

                if (CharTag.POINT_NOTE in renderChar.tags && renderChar.char != ' ') {
                    val pointChar = "、"

                    val px = visualLeftX + colW / 2f + fSize * bookConfig.textNotePx
                    val py = visualTopY + fSize * bookConfig.textNotePy
                    val ps = fSize * bookConfig.textNotePs

                    val pointStyle = fontStyle.copy(
                        color = bookConfig.textNotePc,
                        fontSize = with(density) { ps.toSp() }
                    )
                    val pointLayout = textMeasurer.measure(pointChar, pointStyle)
                    val pointBaseline = pointLayout.firstBaseline.takeIf { !it.isNaN() } ?: ps

                    textDrawCommands.add {
                        drawText(
                            textLayoutResult = pointLayout,
                            color = bookConfig.textNotePc,
                            topLeft = Offset(px, py - pointBaseline / 4f)
                        )
                    }
                }

                if (CharTag.LINE_NOTE in renderChar.tags && renderChar.char != ' ') {
                    val lx = visualLeftX + colW / 2f + fSize * bookConfig.textNoteLx

                    var startY = visualTopY + rh * bookConfig.textNoteLy
                    var endY = visualTopY + rh * (1 + bookConfig.textNoteLy)

                    val rowIdx = slot % bookConfig.rowNum

                    if (rowIdx == 0) { startY -= 5f }
                    if (rowIdx == bookConfig.rowNum - 1) { endY += 4f }

                    drawLine(
                        color = bookConfig.textNoteLc,
                        start = Offset(lx, startY),
                        end = Offset(lx, endY),
                        strokeWidth = bookConfig.textNoteLw
                    )
                }

                if (CharTag.BOOK_LINE in renderChar.tags && renderChar.char != ' ') {
                    var waveTop = basePos.y + rh * 0.2f
                    var waveBottom = basePos.y + rh * 1.2f
                    var waveX = basePos.x
                    if (!renderChar.isComment) { waveX -= 2f }
                    val isFirstBookLineChar = index == 0 || CharTag.BOOK_LINE !in page.chars[index - 1].tags

                    if (isFirstBookLineChar) {
                        waveTop += (if (renderChar.isComment) rh * 0.25f else 5f)
                    }

                    val contentTop = canvasConfig.marginsTop
                    val contentBottom = canvasConfig.canvasHeight - canvasConfig.marginsBottom

                    if (waveTop < contentTop) { waveTop = contentTop + 5f }

                    if (waveBottom > contentBottom) {
                        waveBottom = contentBottom - if (!renderChar.isComment) 4f else 2f
                    }

                    val wavePath = createWavyLinePath(
                        start = Offset(waveX, waveTop),
                        end = Offset(waveX, waveBottom),
                        amplitude = 1.5f,
                        wavelength = 8f
                    )

                    drawPath(
                        path = wavePath,
                        color = bookConfig.bookLineColor,
                        style = Stroke(width = bookConfig.bookLineWidth + if (!renderChar.isComment) 1f else 0f)
                    )
                }

                if (CharTag.RECT_FRAME in renderChar.tags && renderChar.char != ' ') {
                    val r = if (renderChar.isComment) 5f else 10f
                    val rectYOffset = if (renderChar.isComment) bookConfig.commRectY else bookConfig.textRectY
                    val rectHExtra = if (renderChar.isComment) bookConfig.commRectH else bookConfig.textRectH
                    var hCore = fSize * (1 + rectHExtra)
                    val rectYOffsetValue = (if (renderChar.isComment) fSize else rh) * rectYOffset
                    var baseYTop = basePos.y - (hCore - fSize) / 2f + rectYOffsetValue

                    val rowIdx = slot % bookConfig.rowNum
                    if (renderChar.isComment) {
                        if (rowIdx == bookConfig.rowNum - 1) {
                            hCore -= 6f
                            baseYTop -= 6f
                        }
                        if (rowIdx == 0) {
                            hCore -= 8f
                            baseYTop += 4f
                        }
                    } else {
                        if (rowIdx == bookConfig.rowNum - 1) {
                            baseYTop -= 2f
                            hCore -= 4f
                        }
                        if (rowIdx == 0) {
                            hCore -= 4f
                            baseYTop += 2f
                        }
                    }

                    val baseLeft = basePos.x - (if (!renderChar.isComment) 1f else 0f)
                    val baseTop = baseYTop - r

                    val baseWidth = fSize + (if (!renderChar.isComment) 2f else 0f)
                    val baseHeight = hCore + 2 * r

                    if (bookConfig.rectType == 0) {     // 单字符，带外边框
                        drawRoundRect(
                            color = bookConfig.rectBcolor,
                            topLeft = Offset(baseLeft - 2f, baseTop - 2f),
                            size = Size(baseWidth + 4f, baseHeight + 4f),
                            cornerRadius = CornerRadius(r, r),
                            style = Fill
                        )
                        drawRoundRect(
                            color = Color.White,
                            topLeft = Offset(baseLeft - 1f, baseTop - 1f),
                            size = Size(baseWidth + 2f, baseHeight + 2f),
                            cornerRadius = CornerRadius(r, r),
                            style = Fill
                        )
                        drawRoundRect(
                            color = bookConfig.rectBcolor,
                            topLeft = Offset(baseLeft + 1f, baseTop + 1f),
                            size = Size(baseWidth - 2f, baseHeight - 2f),
                            cornerRadius = CornerRadius(r, r),
                            style = Fill
                        )
                    } else {    // 不带外边框，支持多字连续
                        drawRoundRect(
                            color = bookConfig.rectBcolor,
                            topLeft = Offset(baseLeft, baseTop),
                            size = Size(baseWidth, baseHeight),
                            cornerRadius = CornerRadius(r, r),
                            style = Fill
                        )

                        val isFirstRectChar = index == 0 || CharTag.RECT_FRAME !in page.chars[index - 1].tags
                        val prevChar = if (index > 0) page.chars[index - 1] else null

                        val isSameColumn = prevChar != null && run {
                            val currCol = slot / bookConfig.rowNum
                            val prevSlot = prevChar.pcntIndex.toInt().coerceIn(0, grid.charsPerPage - 1)
                            val prevCol = prevSlot / bookConfig.rowNum
                            val currIsRightHalf = (renderChar.pcntIndex - slot) == 0f
                            val prevIsRightHalf = (prevChar.pcntIndex - prevSlot) == 0f
                            currCol == prevCol && renderChar.isComment == prevChar.isComment && currIsRightHalf == prevIsRightHalf
                        }
                        val isContiguous = !isFirstRectChar && prevChar?.char != ' ' && isSameColumn

                        if (isContiguous) {
                            val bridgeReach = (if (renderChar.isComment) fSize else rh) * 0.8f
                            drawRect(
                                color = bookConfig.rectBcolor,
                                topLeft = Offset(baseLeft, baseTop - bridgeReach),
                                size = Size(baseWidth, bridgeReach + r)
                            )
                        }
                    }
                    fontStyle = fontStyle.copy(color = bookConfig.rectFcolor)
                }

                if (CharTag.CIRCLE_FRAME in renderChar.tags && renderChar.char != ' ') {
                    val isComm = renderChar.isComment

                    val cyOffset = if (isComm) bookConfig.commCircleY else bookConfig.textCircleY
                    val crRatio = if (isComm) bookConfig.commCircleR else bookConfig.textCircleR
                    val cfRatio = if (isComm) bookConfig.commCircleF else bookConfig.textCircleF

                    val cellLeft = if (isComm) {
                        val isRightHalf = (renderChar.pcntIndex - slot) == 0f
                        if (isRightHalf) grid.subPositions[slot].x else grid.mainPositions[slot].x
                    } else {
                        grid.mainPositions[slot].x
                    }
                    val cellTop = grid.mainPositions[slot].y
                    val cellW = if (isComm) colW / 2f else colW
                    val cellH = rh

                    val cx = cellLeft + cellW / 2f
                    val cy = cellTop + cellH / 2f + fSize * cyOffset
                    val cr = fSize / 2f * crRatio + 1f

                    if (bookConfig.circleType == 0) {
                        val outerOffset = if (isComm) 3f else 4f
                        val innerOffset = if (isComm) 1f else 2f
                        drawCircle(bookConfig.circleBcolor, cr + outerOffset, Offset(cx, cy))
                        drawCircle(Color.White, cr + innerOffset, Offset(cx, cy))
                        drawCircle(bookConfig.circleBcolor, cr, Offset(cx, cy))
                    } else {
                        drawCircle(bookConfig.circleBcolor, cr, Offset(cx, cy))
                    }

                    fSize *= cfRatio
                    fontStyle = fontStyle.copy(bookConfig.circleFcolor, with(density) { fSize.toSp() })
                    layoutResult = textMeasurer.measure(renderChar.char.toString(), fontStyle)
                    basePos = Offset(cx - fSize / 2f, cy - layoutResult.size.height / 2f)
                }
                val finalPos = basePos
                val finalFSize = fSize
                val finalStyle = fontStyle
                val finalLayout = layoutResult
                val finalIsRotated = renderChar.isRotated

                textDrawCommands.add {
                    withTransform({
                        translate(finalPos.x, finalPos.y)
                        if (finalIsRotated) {
                            translate(finalFSize / 4f, finalFSize / 2f)
                            rotate(-90f, Offset.Zero)
                        }
                    }) {
                        drawText(finalLayout, color = finalStyle.color)
                    }
                }
            }

            if (canvasConfig.leafCenterWidth > 0f) {
                val centerTitle = bookConfig.title.takeIf { it.isNotBlank() } ?: ""
                val centerX = cw / 2f

                var centerStyle = TextStyle(
                    color = bookConfig.titleFontColor,
                    fontSize = with(density) { bookConfig.titleFontSize.toSp() },
                    fontFamily = textFont,
                    fontWeight = FontWeight.Normal
                )

                // 版心标题
                val titleYStart = ch - bookConfig.titleY
                centerTitle.forEachIndexed { index, char ->
                    val layout = textMeasurer.measure(char.toString(), centerStyle)
                    val charW = layout.size.width.toFloat()

                    val fx = centerX - charW / 2f
                    val fy = titleYStart + index * (centerStyle.fontSize.value * bookConfig.titleYdis)

                    textDrawCommands.add {
                        drawText(layout, bookConfig.titleFontColor, Offset(fx, fy))
                    }
                }

                // 版心页码
                val pcharsZh = getZhPageNum(pageNum)

                centerStyle = centerStyle.copy(
                    color = bookConfig.pagerFontColor,
                    fontSize = with(density) { bookConfig.pagerFontSize.toSp() }
                )
                val pagerYStart = ch - bookConfig.pagerY
                pcharsZh.forEachIndexed { index, char ->
                    val layout = textMeasurer.measure(char.toString(), centerStyle)
                    val charW = layout.size.width.toFloat()

                    val px = centerX - charW / 2f
                    val py = pagerYStart + index * (centerStyle.fontSize.value * 1.1f)

                    textDrawCommands.add {
                        drawText(layout, topLeft = Offset(px, py), color = bookConfig.pagerFontColor)
                    }
                }
            }

            textDrawCommands.forEach { it.invoke() }
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
