package icu.hearme.vrain.pdfcanvas

import androidx.compose.ui.ImageComposeScene
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import icu.hearme.vrain.bookcanvas.BackgroundCanvas
import icu.hearme.vrain.configure.AncientBookState
import icu.hearme.vrain.configure.AncientCanvasState
import icu.hearme.vrain.configure.PageSplitConfig
import icu.hearme.vrain.engine.BookGridEngine
import icu.hearme.vrain.engine.BookPage
import icu.hearme.vrain.engine.CharTag
import org.jetbrains.skia.EncodedImageFormat

suspend fun renderPageBackgroundToBytes(
    page: BookPage,
    bookConfig: AncientBookState,
    canvasConfig: AncientCanvasState,
    isCommon: Boolean = false
): ByteArray {

    val width = canvasConfig.canvasWidth.toInt()
    val height = canvasConfig.canvasHeight.toInt()
    val psConfig = PageSplitConfig()
    val scene = ImageComposeScene(width = width, height = height)
    val grid = BookGridEngine.calculateGrid(canvasConfig, bookConfig)

    var drawRaisedHeadOverlays: (DrawScope.() -> Unit)? = null
    if (!isCommon) {
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

            page.chars.forEach { renderChar ->
                if (!isCommon && CharTag.RAISED_HEAD in renderChar.tags) {
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