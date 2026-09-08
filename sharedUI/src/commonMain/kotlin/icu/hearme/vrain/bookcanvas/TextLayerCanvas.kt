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
import icu.hearme.vrain.manager.FontManager
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
    val colW = canvasConfig.colW
    val rh = canvasConfig.contentHeight / bookConfig.rowNum.toFloat()

    var textFontSize = bookConfig.textFont1Size
    var commentFontSize = bookConfig.commentFont1Size

    val maxTextSize = minOf(rh, colW) * 0.98f
    if (textFontSize > maxTextSize) { textFontSize = maxTextSize }

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
            var blStartY: Float? = null     // 书名号波浪线起点
            var rfStartY: Float? = null     // 圆角方框起点
            page.chars.forEachIndexed { index, renderChar ->
                val slot = renderChar.pcntIndex.toInt().coerceIn(0, grid.charsPerPage - 1)
                val charInRowIndex = slot % bookConfig.rowNum
                val isRightHalf = renderChar.isRight
                val isFirstInColumn = charInRowIndex == 0
                val isLastInColumn = charInRowIndex == bookConfig.rowNum - 1 && (!renderChar.isComment || (bookConfig.commentGridType == 4 && !renderChar.isTop))

                val basePos = if (renderChar.isRightComment) { grid.subPositions[slot] } else { grid.mainPositions[slot] }
                val activeFontFamily = if (renderChar.isComment) commentFont else textFont
                var fSize = if (renderChar.isComment) {
                    val scale = if (bookConfig.commentGridType == 4) bookConfig.commentFontZoom else 1f
                    commentFontSize * scale
                } else textFontSize

                var fontStyle = TextStyle(
                    color = if (renderChar.isComment) bookConfig.commentFontColor else bookConfig.textFontColor,
                    fontFamily = activeFontFamily,
                    fontWeight = if (renderChar.isComment) FontWeight.Normal else FontWeight.SemiBold
                )
                var tlOffset: Offset = Offset.Zero

                if (renderChar.isNop) {
                    val nopSize = if (renderChar.isComment) bookConfig.commentCommaNopSize else bookConfig.textCommaNopSize
                    val nopX = if (renderChar.isComment) bookConfig.commentCommaNopX else bookConfig.textCommaNopX
                    val nopY = if (renderChar.isComment) bookConfig.commentCommaNopY else bookConfig.textCommaNopY
                    fSize *= nopSize
                    val ox = (colW / if (renderChar.isComment) 2f else 1f) * nopX
                    val oy = rh * nopY * if (renderChar.isComment && bookConfig.commentGridType == 4) 0.5f else 1f
                    tlOffset = tlOffset.plus(Offset(ox, oy - rh))
                    fontStyle = fontStyle.merge(color = bookConfig.commaColor)
                } else {
                    val ox = if (renderChar.isComment) {
                        if (bookConfig.commentGridType == 4) {
                            if (isRightHalf) { textFontSize / 2f - fSize } else { (colW - textFontSize) / 2f }
                        } else {
                            (colW - fSize * 2f) / 4f
                        }
                    } else {
                        (colW - fSize) / 2f
                    }
                    val oy = if (renderChar.isComment && bookConfig.commentGridType == 4) {
                        if (renderChar.isTop) { rh  } else { rh / 2f }
                    } else {
                        (fSize + rh) / 2f
                    }
                    tlOffset = tlOffset.plus(Offset(ox, -oy))
                    if (renderChar.isRotated) {
                        val c90s = if (renderChar.isComment) bookConfig.commentComma90Size else bookConfig.textComma90Size
                        val c90x = if (renderChar.isComment) bookConfig.commentComma90X / 2 else bookConfig.textComma90X
                        val c90y = if (renderChar.isComment) bookConfig.commentComma90Y else bookConfig.textComma90Y
                        fSize *= c90s
                        tlOffset = tlOffset.plus(Offset(colW * (c90x - 1), -rh * c90y))
                    }
                }

                if (CharTag.RAISED_HEAD in renderChar.tags) {
                    tlOffset = tlOffset.minus(Offset(0f, rh))
                }

                if (CharTag.ZOOM_IN in renderChar.tags) {
                    tlOffset = tlOffset.plus(Offset(fSize * (1 - bookConfig.textZoom) / 2,0f))
                    fSize *= bookConfig.textZoom
                }

                if (CharTag.CIRCLE_NOTE in renderChar.tags && renderChar.char != " ") {
                    val ox = colW / 2f + fSize * bookConfig.textNoteOx
                    val oy = fSize * bookConfig.textNoteOy
                    val or = fSize * bookConfig.textNoteOr
                    withTransform({ translate(basePos.x, basePos.y) }) {
                        drawCircle(bookConfig.textNoteOc, or, tlOffset.plus(Offset(ox, rh-oy)), style = Stroke(width = bookConfig.textNoteOw))
                    }
                }

                if (CharTag.POINT_NOTE in renderChar.tags && renderChar.char != " ") {
                    val fChar = "、"
                    val ox = colW / 2f + fSize * bookConfig.textNotePx
                    val oy = fSize * bookConfig.textNotePy

                    val pointStyle = fontStyle.merge(fontSize = with(density){ fSize.times(bookConfig.textNotePs).toSp() })
                    val pointLayout = textMeasurer.measure(fChar, pointStyle)

                    textDrawCommands.add {
                        withTransform({ translate(basePos.x, basePos.y) }) {
                            drawText(pointLayout, color = bookConfig.textNotePc, topLeft = tlOffset.plus(Offset(ox, -oy)))
                        }
                    }
                }

                if (CharTag.LINE_NOTE in renderChar.tags && renderChar.char != " ") {
                    var ty = basePos.y - rh * bookConfig.textNoteLy + tlOffset.y
                    var by = basePos.y - rh * (bookConfig.textNoteLy - 1) + tlOffset.y
                    val lx = tlOffset.x + colW / 2f + fSize * bookConfig.textNoteLx

                    if (isFirstInColumn) { ty = canvasConfig.marginsTop + 5 }
                    if (isLastInColumn) { by = ch - canvasConfig.marginsBottom - 4f }
                    withTransform({ translate(basePos.x, 0f) }) {
                        drawLine(bookConfig.textNoteLc, Offset(lx, ty), Offset(lx, by), bookConfig.textNoteLw)
                    }
                }

                if (CharTag.BOOK_LINE in renderChar.tags && renderChar.char != " ") {
                    val isMinGrid = renderChar.isComment && bookConfig.commentGridType == 4
                    if (blStartY == null) {
                        blStartY = basePos.y + tlOffset.y + 0.2f * rh
                        if (isFirstInColumn) { blStartY = canvasConfig.marginsTop + 5 }
                        blStartY += if (renderChar.isComment) 0.25f * rh else 5f
                    }
                    var blEndY = basePos.y + tlOffset.y + 1.2f * rh
                    if (isLastInColumn) { blEndY = ch - canvasConfig.marginsBottom - if (renderChar.isComment) 2f else 4f }

                    val nextChar = page.chars.getOrNull(index + 1)
                    val nextHasBookLineTag = nextChar?.tags?.contains(CharTag.BOOK_LINE) == true
                    val isNextBlank = nextChar?.char?.isBlank() == true
                    val isNextDiffColumn = nextChar != null && (nextChar.isComment != renderChar.isComment || nextChar.isRight != renderChar.isRight)
                    if (isLastInColumn || !nextHasBookLineTag || isNextBlank || isNextDiffColumn) {
                        if (isMinGrid) { blEndY -= rh / 2f }
                        val waveX = tlOffset.x - if (renderChar.isComment) 0f else 2f
                        val wavePath = createWavyLinePath(Offset(waveX, blStartY), Offset(waveX, blEndY))

                        withTransform({ translate(basePos.x, 0f) }) {
                            drawPath(
                                wavePath, bookConfig.bookLineColor,
                                style = Stroke(width = bookConfig.bookLineWidth + if (!renderChar.isComment) 1f else 0f)
                            )
                        }
                        blStartY = null
                    }
                }

                if (CharTag.RECT_FRAME in renderChar.tags && renderChar.char != " ") {
                    val r = if (renderChar.isComment) bookConfig.commRectR else bookConfig.textRectR
                    val rty = if (renderChar.isComment) bookConfig.commRectY else bookConfig.textRectY
                    val rth = if (renderChar.isComment) bookConfig.commRectH else bookConfig.textRectH
                    val rtf = if (renderChar.isComment) bookConfig.commRectF else bookConfig.textRectF
                    var tlo: Offset = tlOffset.plus(Offset(-2f,fSize * rty + 2f))
                    var rectH = fSize * (1 + rth)

                    if (isLastInColumn){
                        tlo = tlo.plus(Offset(0f, -2f))
                        rectH -= if (renderChar.isComment) 6 else 4
                    }
                    if (isFirstInColumn) { rectH -= if (renderChar.isComment) 8 else 4 }

                    if (bookConfig.rectType == 0){      // 单字符，带外边框
                        withTransform({ translate(basePos.x, basePos.y) }) {
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
                        }
                    } else {
                        if (rfStartY == null) { rfStartY = basePos.y + tlo.y }

                        val nextChar = page.chars.getOrNull(index + 1)
                        val nextHasRectTag = nextChar?.tags?.contains(CharTag.RECT_FRAME) == true
                        val isNextBlank = nextChar?.char?.isBlank() == true
                        val isNextDiffColumn = nextChar != null && (nextChar.isComment != renderChar.isComment || nextChar.isRight != renderChar.isRight)

                        if (isLastInColumn || !nextHasRectTag || isNextBlank || isNextDiffColumn) {
                            val totalHeight = basePos.y + tlo.y + rectH - rfStartY
                            withTransform({ translate(basePos.x, 0f) }) {
                                drawRoundRect(
                                    bookConfig.rectBcolor, Offset(tlOffset.x, rfStartY!!),
                                    Size(fSize, totalHeight), CornerRadius(r, r), Fill
                                )
                            }
                            rfStartY = null
                        }
                    }
                    fSize *= rtf
                    fontStyle = fontStyle.copy(color = bookConfig.rectFcolor)
                }

                if (CharTag.CIRCLE_FRAME in renderChar.tags && renderChar.char != " ") {
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
                            drawCircle(bookConfig.circleBcolor, cr + outerOffset, cfOffset, style = Fill)
                            drawCircle(Color.White, cr + innerOffset, cfOffset, style = Stroke(2f))
                        } else {
                            drawCircle(bookConfig.circleBcolor, cr, cfOffset)
                        }
                    }

                    fSize *= cfRatio
                    tlOffset = tlOffset.plus(Offset(fSize * (1 - cfRatio) / 2, fSize * (1 - cfRatio) / 2))
                    fontStyle = fontStyle.merge(bookConfig.circleFcolor)
                }
                if (renderChar.isRotateLetter) { tlOffset = tlOffset.plus(Offset(-fSize * 10/9, 0f)) }

                val finalFSize = fSize

                val finalStyle = fontStyle.merge(fontSize = with(density){ fSize.toSp() })
                val finalLayout = textMeasurer.measure(renderChar.char, finalStyle)
                val finalIsRotated = renderChar.isRotated || renderChar.isRotateLetter
                if (renderChar.char.isNotBlank())
                textDrawCommands.add {
                    withTransform({
                        translate(basePos.x, basePos.y)
                        if (finalIsRotated) {
                            val pivotX = tlOffset.x + finalFSize * 2 / 3
                            val pivotY = tlOffset.y + finalFSize * 14 / 9
                            rotate(90f, Offset(pivotX, pivotY))
                        } else {
                            rotate(-bookConfig.font1Rotate.toFloat(), Offset(finalFSize / 4f, finalFSize / 2f))
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