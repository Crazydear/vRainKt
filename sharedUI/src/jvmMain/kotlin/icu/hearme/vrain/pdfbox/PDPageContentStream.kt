package icu.hearme.vrain.pdfbox

import org.apache.pdfbox.pdmodel.PDPageContentStream
import org.apache.pdfbox.pdmodel.font.PDType0Font
import org.apache.pdfbox.util.Matrix
import java.awt.Color
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin

fun PDPageContentStream.textlable(x: Float, y: Float, font: PDType0Font, fsize: Float, text: String, color: Color, angle: Double = 0.0) {
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

fun PDPageContentStream.drawLine(startX: Float, startY: Float, endX: Float, endY: Float, width: Float, color: Color){
    this.setStrokingColor(color)
    this.setLineWidth(width)
    this.moveTo(startX, startY)
    this.lineTo(endX, endY)
    this.stroke()
}

fun PDPageContentStream.drawCircle(cx: Float, cy: Float, radius: Float, fillColor: Color? = null, strokeColor: Color? = null, lineWidth: Float = 1f) {
    this.appendCirclePath(cx, cy, radius)
    drawPath(fillColor, strokeColor, lineWidth)
}

fun PDPageContentStream.appendCirclePath(cx: Float, cy: Float, radius: Float) {
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

fun PDPageContentStream.drawPath(fillColor: Color?, strokeColor: Color?, lineWidth: Float) {
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

fun PDPageContentStream.drawWavyLine(x1: Float, y1: Float, x2: Float, y2: Float, color: Color= Color.BLACK, width: Float = 1f) {
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
