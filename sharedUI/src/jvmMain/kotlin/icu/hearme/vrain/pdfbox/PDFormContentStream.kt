package icu.hearme.vrain.pdfbox

import org.apache.pdfbox.pdmodel.PDFormContentStream
import java.awt.Color

fun PDFormContentStream.drawLine(startX: Float, startY: Float, endX: Float, endY: Float, width: Float, color: Color) {
    this.setStrokingColor(color)
    this.setLineWidth(width)
    this.moveTo(startX, startY)
    this.lineTo(endX, endY)
    this.stroke()
}

fun PDFormContentStream.drawRoundRect(
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

fun PDFormContentStream.drawPath(fillColor: Color?, strokeColor: Color?, lineWidth: Float) {
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