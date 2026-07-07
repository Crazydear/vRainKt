package icu.hearme.vrain.bookcanvas

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.unit.IntSize
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.roundToInt
import kotlin.math.sin

object DrawModel {
    fun DrawScope.drawFishTop(
        centerX: Float, lcw: Float, fy: Float, dy1: Float, dy2: Float,
        flm: Float, flc: Color, flw: Float, ftc: Color, iff: Boolean
    ) {
        val leftX = centerX - lcw / 2f
        val rightX = centerX + lcw / 2f

        // 鱼尾上细线
        drawLine(color = flc, start = Offset(leftX, fy - flm), end = Offset(rightX, fy - flm), strokeWidth = flw)

        // 鱼尾五边形 (原脚本中上鱼尾只填充不描边，原 stroke 被注释)
        val path = Path().apply {
            moveTo(leftX, fy)
            lineTo(rightX, fy)
            lineTo(rightX, fy + dy1 + dy2)
            lineTo(centerX, fy + dy1)
            lineTo(leftX, fy + dy1 + dy2)
            close()
        }
        drawPath(path = path, color = ftc, style = Fill)

        // 鱼尾下方两根细斜线 (八字须)
        if (!iff) {
            drawLine(color = flc, start = Offset(leftX, fy + dy1 + dy2 + flm), end = Offset(centerX, fy + dy1 + flm), strokeWidth = flw)
            drawLine(color = flc, start = Offset(centerX, fy + dy1 + flm), end = Offset(rightX, fy + dy1 + dy2 + flm), strokeWidth = flw)
        }
    }

    fun DrawScope.drawFishBtmDown(
        centerX: Float, lcw: Float, fy: Float, dy1: Float, dy2: Float,
        flm: Float, flc: Color, flw: Float, fbc: Color, iff: Boolean
    ) {
        val leftX = centerX - lcw / 2f
        val rightX = centerX + lcw / 2f

        // 鱼尾上细线
        drawLine(color = flc, start = Offset(leftX, fy - flm), end = Offset(rightX, fy - flm), strokeWidth = flw)

        if (dy1 > 0f || dy2 > 0f) { // 设置为0时，下鱼尾萎缩为双横线
            val path = Path().apply {
                moveTo(leftX, fy)
                lineTo(rightX, fy)
                lineTo(rightX, fy + dy1 + dy2)
                lineTo(centerX, fy + dy1)
                lineTo(leftX, fy + dy1 + dy2)
                close()
            }
            // 原脚本中下鱼尾既有填充又有描边
            drawPath(path = path, color = fbc, style = Fill)
            drawPath(path = path, color = flc, style = Stroke(width = flw))
        }

        if (!iff || (dy1 == 0f && dy2 == 0f)) { // 非花鱼尾或下鱼尾萎缩时
            drawLine(color = flc, start = Offset(leftX, fy + dy1 + dy2 + flm), end = Offset(centerX, fy + dy1 + flm), strokeWidth = flw)
            drawLine(color = flc, start = Offset(centerX, fy + dy1 + flm), end = Offset(rightX, fy + dy1 + dy2 + flm), strokeWidth = flw)
        }
    }

    fun DrawScope.drawFishBtmUp(
        centerX: Float, lcw: Float, fy: Float, dy1: Float, dy2: Float,
        flm: Float, flc: Color, flw: Float, fbc: Color, iff: Boolean
    ) {
        val leftX = centerX - lcw / 2f
        val rightX = centerX + lcw / 2f

        // 注意坐标系，对鱼尾向上，原脚本此处为 +flm
        drawLine(color = flc, start = Offset(leftX, fy + flm), end = Offset(rightX, fy + flm), strokeWidth = flw)

        if (dy1 > 0f || dy2 > 0f) {
            val path = Path().apply {
                moveTo(leftX, fy)
                lineTo(rightX, fy)
                lineTo(rightX, fy - dy1 - dy2) // 向上指，减去高度
                lineTo(centerX, fy - dy1)
                lineTo(leftX, fy - dy1 - dy2)
                close()
            }
            drawPath(path = path, color = fbc, style = Fill)
            drawPath(path = path, color = flc, style = Stroke(width = flw))
        }

        if (!iff || (dy1 == 0f && dy2 == 0f)) {
            drawLine(color = flc, start = Offset(leftX, fy - dy1 - dy2 - flm), end = Offset(centerX, fy - dy1 - flm), strokeWidth = flw)
            drawLine(color = flc, start = Offset(centerX, fy - dy1 - flm), end = Offset(rightX, fy - dy1 - dy2 - flm), strokeWidth = flw)
        }
    }

    fun DrawScope.drawFishFlowerDecorations(
        flowerBitmap: ImageBitmap?,
        centerX: Float, lcw: Float, delta: Float, gr: Float,
        fty: Float, ftrh: Float,
        fby: Float, fbrh: Float, fbth: Float, fbd: Int
    ) {
        if (flowerBitmap == null) return
        val baseSize = ftrh * gr

        val angleRad = 30.0 * Math.PI / 180.0
        val rotBoxSize = baseSize * (cos(angleRad) + sin(angleRad)).toFloat()
        // 三叶草图层
        // 将装饰图缩小为鱼尾尾部高度的黄金分割比例，距版心左、右侧线距离为delta并与鱼身高度对齐
        fun drawSingleFlower(x: Float, y: Float, degrees: Float) {
            withTransform({
                translate(left = x, top = y)
                rotate(degrees = degrees, pivot = Offset(rotBoxSize / 2f, rotBoxSize / 2f))
                translate(left = (rotBoxSize - baseSize) / 2f, top = (rotBoxSize - baseSize) / 2f)
            }) {
                drawImage(
                    image = flowerBitmap,
                    dstSize = IntSize(baseSize.roundToInt(), baseSize.roundToInt()),
                    blendMode = BlendMode.DstOut
                )
            }
        }

        drawSingleFlower(centerX - lcw / 2f + delta, fty + ftrh - rotBoxSize, -30f)
        drawSingleFlower(centerX + lcw / 2f - rotBoxSize - delta, fty + ftrh - rotBoxSize, 30f)

        if (fbrh > 0f && fbth > 0f) {
            if (fbd == 0) { // 顺鱼尾
                drawSingleFlower(centerX - lcw / 2f + delta, fby + fbrh - rotBoxSize, -150f)
                drawSingleFlower(centerX + lcw / 2f - rotBoxSize - delta, fby + fbrh - rotBoxSize, 150f)
            } else if (fbd == 1) {  // 对鱼尾
                drawSingleFlower(centerX - lcw / 2f + delta, fby - fbrh, -150f)
                drawSingleFlower(centerX + lcw / 2f - rotBoxSize - delta, fby - fbrh, 150f)
            }
        }
    }

    fun DrawScope.drawArcFishTail(
        centerX: Float, lcw: Float, yBase: Float, ftth: Float, ftc: Color,
        fbd: Int = 0, isBottom: Boolean = false
    ) {
        val halfW = lcw / 2f
        val dd = hypot(halfW, ftth) // 矩阵对边边长
        val dsin = ftth / dd        // 矩阵左上三角形右上锐角的正弦
        val dcos = halfW / dd       // 矩阵左上三角形右上锐角的余弦
        val ddr = 0.4f              // 第一段弧线对应的边长长度占比

        // 坐标映射辅助函数：将原 $eimg (lcw/2 x ftth) 的内部局部坐标，映射到主画布的绝对坐标
        fun eX(x: Float) = centerX - halfW + x
        fun eY(y: Float) = yBase + y

        // 完全等价于get_2points_ellipse，并直接在 Canvas 上绘制
        fun drawArc(
            cd: Float,
            x1: Float, y1: Float,
            x2: Float, y2: Float,
            isFill: Boolean
        ) {
            val cx = (x1 + x2) / 2f
            val cy = (y1 + y2) / 2f
            val d21 = hypot(x1 - x2, y1 - y2)
            if (d21 == 0f) return

            val sin21 = abs(x2 - x1) / d21
            val cos21 = abs(y2 - y1) / d21
            val ncx = cx - cd * cos21
            val ncy = cy - cd * sin21
            val cr = hypot(ncx - x1, ncy - y1)

            val startAngle = Math.toDegrees(atan2(y1 - ncy, x1 - ncx).toDouble()).toFloat()
            val endAngle = Math.toDegrees(atan2(y2 - ncy, x2 - ncx).toDouble()).toFloat()

            // 确保绘制的是两点间的最短弧（劣弧）
            var sweepAngle = endAngle - startAngle
            if (sweepAngle > 180) sweepAngle -= 360f
            if (sweepAngle < -180) sweepAngle += 360f

            val path = Path().apply {
                if (isFill) {
                    moveTo(eX(ncx), eY(ncy))
                }
                arcTo(
                    rect = Rect(eX(ncx - cr), eY(ncy - cr), eX(ncx + cr), eY(ncy + cr)),
                    startAngleDegrees = startAngle,
                    sweepAngleDegrees = sweepAngle,
                    forceMoveTo = !isFill
                )
                if (isFill) {
                    close()
                }
            }

            drawPath(
                path = path,
                color = ftc,
                style = if (isFill) Fill else Stroke(width = 1f)
            )
        }

        // 绘制左半侧花鱼尾
        fun drawLeftHalf() {
            // 第一段填充弧形 (两端内收 2 像素)
            drawArc(
                cd = 14f,
                x1 = halfW - 2 * dcos, y1 = 2 * dsin,
                x2 = halfW - (dd * ddr - 2) * dcos, y2 = (dd * ddr - 2) * dsin,
                isFill = true
            )
            // 第一段弧线描边 (全尺寸贯穿)
            drawArc(
                cd = 10f,
                x1 = halfW, y1 = 0f,
                x2 = halfW - dd * ddr * dcos, y2 = dd * ddr * dsin,
                isFill = false
            )
            // 第二段带填充弧形
            drawArc(
                cd = 14f,
                x1 = halfW - (dd * ddr + 2) * dcos, y1 = (dd * ddr + 2) * dsin,
                x2 = halfW - (dd - 2) * dcos, y2 = (dd - 2) * dsin,
                isFill = true
            )
            // 第二段弧线描边
            drawArc(
                cd = 10f,
                x1 = halfW - dd * ddr * dcos, y1 = dd * ddr * dsin,
                x2 = 0f, y2 = ftth,
                isFill = false
            )
        }

        // 判断是否是对鱼尾，决定是否上下翻转 (Flip)
        val flipY = if (isBottom && fbd == 1) -1f else 1f

        // 顶级 Transform 容器，处理上下翻转
        withTransform({
            scale(scaleX = 1f, scaleY = flipY, pivot = Offset(centerX, yBase))
        }) {
            // 1. 绘制左侧
            drawLeftHalf()

            // 2. 左右翻转 (Flop) 并绘制右侧
            withTransform({
                scale(scaleX = -1f, scaleY = 1f, pivot = Offset(centerX, yBase))
            }) {
                drawLeftHalf()
            }
        }
    }
}