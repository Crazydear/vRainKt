package icu.hearme.vrain.bookcanvas

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
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
    val maxTextSize = minOf(rh, colW) * 0.98f
    if (textFontSize > maxTextSize) {
        val scaleRatio = maxTextSize / textFontSize
        textFontSize = maxTextSize
        commentFontSize *= scaleRatio
    }

    val maxCommentSize = minOf(rh, colW / 2f) * 0.98f
    if (commentFontSize > maxCommentSize) { commentFontSize = maxCommentSize }

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
            val textDrawCommands = mutableListOf<() -> Unit>()
            println(grid.toString())
            page.chars.forEachIndexed { index, renderChar ->
                val slot = renderChar.pcntIndex.toInt().coerceIn(0, grid.charsPerPage - 1)
                val offset = renderChar.pcntIndex - slot
                val subIndex = (offset * 4 + 0.1f).toInt()
                val isRightHalf = (subIndex == 0 || subIndex == 1)

                var basePos = if (renderChar.isComment) {
                    if (isRightHalf) grid.subPositions[slot] else grid.mainPositions[slot]
                } else {
                    grid.mainPositions[slot]
                }
                val activeFontFamily = if (renderChar.isComment) commentFont else textFont
                var fSize = if (renderChar.isComment) {
                    val scale = if (bookConfig.commentGridType == 4) bookConfig.commentFontZoom else 1f
                    commentFontSize * scale
                } else textFontSize
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
                var tlOffset: Offset = Offset.Zero

                if (renderChar.isNop) {
                    val nopX = if (renderChar.isComment) bookConfig.commentCommaNopX else bookConfig.textCommaNopX
                    val nopY = if (renderChar.isComment) bookConfig.commentCommaNopY else bookConfig.textCommaNopY

                    val ox = (colW / if (renderChar.isComment) 2f else 1f) * nopX
                    val oy = rh * nopY * if (renderChar.isComment && bookConfig.commentGridType == 4) 0.5f else 1f
                    tlOffset = tlOffset.plus(Offset(ox, oy - rh))
                    fontStyle = fontStyle.merge(color = bookConfig.commaColor)
                } else {
                    val ox = if (renderChar.isComment) {
                        if (bookConfig.commentGridType == 4) {
                            if (isRightHalf) { textFontSize / 2f - fSize} else { (colW - textFontSize) / 2f }
                        } else {
                            if (isRightHalf) { (colW - fSize * 2f) / 4 } else { (colW - fSize * 2f) / 4f }
                        }
                    } else {
                        (colW - fSize) / 2f
                    }
                    val oy = if (renderChar.isComment && bookConfig.commentGridType == 4){
                        val isTop = (subIndex % 2 == 0)
                        if (isTop) { rh  } else { rh / 2f }
                    } else {
                        (fSize + rh) / 2f
                    }
                    tlOffset = tlOffset.plus(Offset(ox, -oy))
                }

                if (CharTag.RAISED_HEAD in renderChar.tags) {
                    tlOffset = tlOffset.minus(Offset(0f, rh))
                }

                if (CharTag.ZOOM_IN in renderChar.tags) {
                    tlOffset = tlOffset.plus(Offset(fSize * (1 - bookConfig.textZoom) / 2,0f))
                    fSize *= bookConfig.textZoom
                    fontStyle = fontStyle.merge(fontSize = fontStyle.fontSize.times(bookConfig.textZoom))
                }

                if (CharTag.CIRCLE_NOTE in renderChar.tags && renderChar.char != ' ') {
                    val ox = colW * 0.7f + fSize * bookConfig.textNoteOx
                    val oy = fSize * bookConfig.textNoteOy
                    val or = fSize * bookConfig.textNoteOr
                    withTransform({ translate(basePos.x, basePos.y) }) {
                        drawCircle(bookConfig.textNoteOc, or, Offset(ox, -oy), style = Stroke(width = bookConfig.textNoteOw))
                    }
                }

                if (CharTag.POINT_NOTE in renderChar.tags && renderChar.char != ' ') {
                    val pointChar = "、"

                    val ox = colW * 0.7f + fSize * bookConfig.textNotePx
                    val oy = fSize * bookConfig.textNotePy

                    val pointStyle = fontStyle.merge(fontSize = fontStyle.fontSize.times(bookConfig.textNotePs))
                    val pointLayout = textMeasurer.measure(pointChar, pointStyle)

                    textDrawCommands.add {
                        withTransform({ translate(basePos.x, basePos.y - rh * 1.25f) }) {
                            drawText(pointLayout, color = bookConfig.textNotePc, topLeft = Offset(ox, -oy))
                        }
                    }
                }

                if (CharTag.LINE_NOTE in renderChar.tags && renderChar.char != ' ') {
                    var ty = rh * (bookConfig.textNoteLy + 1)
                    var by = rh * bookConfig.textNoteLy
                    val lx = colW * 0.75f + fSize * bookConfig.textNoteLx
                    val rowIdx = slot % bookConfig.rowNum

                    if (rowIdx == 0) { ty -= 5f }
                    if (rowIdx == bookConfig.rowNum - 1) { by += 4f }
                    withTransform({ translate(basePos.x, basePos.y) }) {
                        drawLine(bookConfig.textNoteLc, Offset(lx, -ty), Offset(lx, -by), bookConfig.textNoteLw)
                    }

                }

                if (CharTag.BOOK_LINE in renderChar.tags && renderChar.char != ' ') {
                    var waveTop = rh * 0.8f
                    val waveBottom = rh * 0.2f
                    var waveX = tlOffset.x
                    waveX -=  if (!renderChar.isComment) { 2f } else 1f
                    val isFirstBookLineChar = index == 0 || CharTag.BOOK_LINE !in page.chars[index - 1].tags

                    if (isFirstBookLineChar) {
                        waveTop -= (if (renderChar.isComment) rh * 0.25f else 5f)
                    }

                    val wavePath = createWavyLinePath(Offset(waveX, -waveTop), Offset(waveX, waveBottom))

                    withTransform({ translate(basePos.x, basePos.y) }) {
                        drawPath(
                            wavePath, bookConfig.bookLineColor,
                            style = Stroke(width = bookConfig.bookLineWidth + if (!renderChar.isComment) 1f else 0f)
                        )
                    }
                }

                if (CharTag.RECT_FRAME in renderChar.tags && renderChar.char != ' ') {
                    val r = if (renderChar.isComment) bookConfig.commRectR else bookConfig.textRectR
                    val rty = if (renderChar.isComment) bookConfig.commRectY else bookConfig.textRectY
                    val rth = if (renderChar.isComment) bookConfig.commRectH else bookConfig.textRectH
                    val tfs = if (renderChar.isComment) fSize else textFontSize
                    var tlo: Offset = tlOffset.plus(Offset(-2f,tfs * rty + 2f))
                    var rectH = tfs * (1 + rth)
                    if (!renderChar.isComment) {
                        val rowIdx = slot % bookConfig.rowNum
                        if (rowIdx == bookConfig.rowNum - 1){
                            tlo = tlo.plus(Offset(0f, -2f))
                            rectH -= 4
                        }
                        if (rowIdx == 0) { rectH -= 4 }
                    }

                    withTransform({ translate(basePos.x, basePos.y) }) {
                        if (bookConfig.rectType == 0) {     // 单字符，带外边框
                            drawRoundRect(
                                bookConfig.rectBcolor, tlo,
                                Size(fSize + 4f, rectH + 4f),
                                CornerRadius(r, r), Fill
                            )
                            drawRoundRect(
                                Color.White, tlo.plus(Offset(2f, 2f)),
                                Size(fSize + 2f, rectH + 2f),
                                CornerRadius(r, r), Stroke(1f)
                            )
                        } else {    // 不带外边框，支持多字连续
                            drawRoundRect(
                                bookConfig.rectBcolor, tlo,
                                Size(fSize, rectH),
                                CornerRadius(r, r), Fill
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
                                    bookConfig.rectBcolor, tlOffset.minus(Offset(0f, bridgeReach)),
                                    Size(fSize, bridgeReach + r), style = Fill
                                )
                            }
                        }
                    }
                    fontStyle = fontStyle.copy(color = bookConfig.rectFcolor)
                }

                if (CharTag.CIRCLE_FRAME in renderChar.tags && renderChar.char != ' ') {
                    val isComm = renderChar.isComment
                    val cyOffset = if (isComm) bookConfig.commCircleY else bookConfig.textCircleY
                    val crRatio = if (isComm) bookConfig.commCircleR else bookConfig.textCircleR
                    val cfRatio = if (isComm) bookConfig.commCircleF else bookConfig.textCircleF
                    val cr = fSize / 2f * crRatio + 1
                    val cfOffset = tlOffset.plus(Offset(fSize / 2f, fSize / 2f + fSize * cyOffset))

                    withTransform({ translate(basePos.x, basePos.y) }) {
                        if (bookConfig.circleType == 0) {
                            val outerOffset = if (isComm) 3f else 4f
                            val innerOffset = if (isComm) 1f else 2f
                            drawCircle(bookConfig.circleBcolor, cr + outerOffset, cfOffset)
                            drawCircle(Color.White, cr + innerOffset, cfOffset)
                            drawCircle(bookConfig.circleBcolor, cr, cfOffset)
                        } else {
                            drawCircle(bookConfig.circleBcolor, cr, cfOffset)
                        }
                    }

                    fSize *= cfRatio
                    tlOffset = tlOffset.plus(Offset(fSize * (1 - cfRatio) / 2, fSize * (1 - cfRatio) / 2))
                    fontStyle = fontStyle.merge(bookConfig.circleFcolor, fontStyle.fontSize * cfRatio)
                }

                val finalFSize = fSize
                val finalStyle = fontStyle
                val finalLayout = textMeasurer.measure(renderChar.char.toString(), fontStyle)
                val finalIsRotated = renderChar.isRotated

                textDrawCommands.add {
                    withTransform({
                        translate(basePos.x, basePos.y)
                        if (finalIsRotated) {
                            translate(finalFSize / 4f, finalFSize / 2f)
                            rotate(-90f, Offset.Zero)
                        }
                    }) {
                        drawText(finalLayout, color = finalStyle.color, topLeft = tlOffset)
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
        }
    }
}

private fun createWavyLinePath(start: Offset, end: Offset, amplitude: Float = 1.25f, wavelength: Float = 10f): Path {
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
