package icu.hearme.vrain.engine

import androidx.compose.ui.geometry.Offset
import icu.hearme.vrain.configure.AncientBookState
import icu.hearme.vrain.configure.AncientCanvasState

data class BookGrid(
    val mainPositions: List<Offset>, // 正文单列文字的坐标集合 (TopLeft)
    val subPositions: List<Offset>,  // 夹批双排小字的坐标集合 (TopLeft，右移半个字宽)
    val charsPerPage: Int            // 每页的标准字位总数
)

object BookGridEngine {

    fun calculateGrid(canvasState: AncientCanvasState, bookState: AncientBookState): BookGrid {
        // 1. 获取画布度量参数
        val canvasWidth = canvasState.canvasWidth
        val canvasHeight = canvasState.canvasHeight
        val marginsTop = canvasState.marginsTop
        val marginsBottom = canvasState.marginsBottom
        val marginsLeft = canvasState.marginsLeft
        val marginsRight = canvasState.marginsRight
        val colNum = canvasState.leafCol
        val lcWidth = canvasState.leafCenterWidth

        // 2. 获取排版控制参数
        val rowNum = bookState.rowNum
        val rowDeltaY = bookState.rowDeltaY
        val isMultirows = canvasState.ifMultirows
        val multirowsNum = canvasState.multirowsNum
        val multirowsHLayout = bookState.multirowsHorizontalLayout

        // 3. 计算单列字宽与行高
        val cw = (canvasWidth - marginsLeft - marginsRight - lcWidth) / colNum
        val rh = (canvasHeight - marginsTop - marginsBottom) / rowNum

        val mainPos = mutableListOf<Offset>()
        val subPos = mutableListOf<Offset>()

        // 4. 核心排版逻辑
        if (isMultirows && multirowsNum > 1) {
            // 模式 A：多栏横向布局
            require(rowNum % multirowsNum == 0) { "多栏模式下，每列字数应是栏数的倍数！" }
            val rrowNum = rowNum / multirowsNum

            // 分栏横向整叶换行
            if (multirowsHLayout == 1) {
                for (rid in 0 until multirowsNum) {
                    for (i in 0 until colNum) {
                        for (j in 0 until rrowNum) {
                            val x = canvasWidth - marginsRight - cw * (i + 1) - if (i >= colNum / 2) lcWidth else 0f
                            val y = marginsTop + (rrowNum * rid * rh) + (rh * j) + rowDeltaY

                            mainPos.add(Offset(x, y))
                            subPos.add(Offset(x + cw / 2f, y))
                        }
                    }
                }
            }
            // 分栏横向半叶换行
            if (multirowsHLayout == 2) {
                for (rid in 0 until multirowsNum) {
                    for (i in 0 until colNum / 2) {
                        for (j in 0 until rrowNum) {
                            val x = canvasWidth - marginsRight - cw * (i + 1)
                            val y = marginsTop + (rrowNum * rid * rh) + (rh * j) + rowDeltaY
                            mainPos.add(Offset(x, y))
                            subPos.add(Offset(x + cw / 2f, y))
                        }
                    }
                }

                for (rid in 0 until multirowsNum) {
                    for (i in colNum / 2 until colNum) {
                        for (j in 0 until rrowNum) {
                            val x = canvasWidth - marginsRight - cw * (i + 1) - lcWidth
                            val y = marginsTop + (rrowNum * rid * rh) + (rh * j) + rowDeltaY
                            mainPos.add(Offset(x, y))
                            subPos.add(Offset(x + cw / 2f, y))
                        }
                    }
                }
            }
        } else {
            for (i in 0 until colNum) {
                for (j in 0 until rowNum) {
                    val x = canvasWidth - marginsRight - cw * (i + 1) - if (i >= colNum / 2) lcWidth else 0f
                    val y = marginsTop + (rh * j) + rowDeltaY
                    mainPos.add(Offset(x, y))
                    subPos.add(Offset(x + cw / 2f, y))
                }
            }
        }

        val pageCharsNum = colNum * rowNum

        return BookGrid(mainPos, subPos, pageCharsNum)
    }
}