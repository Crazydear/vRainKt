package icu.hearme.vrain.bookcanvas

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import icu.hearme.vrain.configure.AncientBookState
import icu.hearme.vrain.configure.AncientCanvasState
import icu.hearme.vrain.configure.PageSplitConfig
import icu.hearme.vrain.engine.BookGrid
import icu.hearme.vrain.engine.BookPage
import icu.hearme.vrain.engine.CharTag

@Composable
fun BookPageCanvas(
    page: BookPage, grid: BookGrid,
    bookConfig: AncientBookState, canvasState: AncientCanvasState,
    psConfig: PageSplitConfig, modifier: Modifier = Modifier
) {
    val raisedHeadChars = remember(page) {
        page.chars.filter { it.tags.has(CharTag.RAISED_HEAD) }
    }
    val drawRaisedHeadOverlays: DrawScope.() -> Unit = {
        val mt = canvasState.marginsTop
        val mb = canvasState.marginsBottom
        val clw = (canvasState.canvasWidth - canvasState.marginsLeft - canvasState.marginsRight - canvasState.leafCenterWidth) / canvasState.leafCol.toFloat()
        val rh = (canvasState.canvasHeight - mt - mb) / bookConfig.rowNum.toFloat()

        val ilc = canvasState.inlineColor
        val olc = canvasState.outlineColor
        var ohm = canvasState.outlineHMargin
        if (ohm > 0.2f * clw) { ohm = 0.15f * clw }
        var ovm = canvasState.outlineVMargin
        if (ovm > 0.3f * rh) { ovm = 0.3f * rh }
        val ilw = canvasState.inlineWidth
        val olw = canvasState.outlineWidth

        for (i in raisedHeadChars.indices) {
            val renderChar = raisedHeadChars[i]
            val slot = renderChar.pcntIndex.toInt().coerceIn(0, grid.charsPerPage - 1)
            val basePos = grid.mainPositions[slot]
            if (canvasState.outlineVMargin < rh + 5) {
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

    Box(contentAlignment = Alignment.Center, modifier = modifier.background(Color.Cyan)){
        BackgroundCanvas(canvasState, psConfig, onDrawOverlays = drawRaisedHeadOverlays)
        TextLayerCanvas(page, grid, bookConfig, canvasState, psConfig)
    }
}