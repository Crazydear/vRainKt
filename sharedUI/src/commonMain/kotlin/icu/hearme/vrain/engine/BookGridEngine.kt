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

            // 模式 A-1：分栏横向整叶换行 (先扫满一整横栏，再换下一横栏)
            if (multirowsHLayout == 1) {
                for (rid in 0 until multirowsNum) {
                    for (col in 0 until colNum) {
                        for (charIdx in 0 until rrowNum) {
                            // X 轴：右往左排。如果跨越中缝，需额外减去版心宽度 lcWidth
                            val x = canvasWidth - marginsRight - cw * (col + 1) - if (col >= colNum / 2) lcWidth else 0f
                            // Y 轴：上往下排。加上当前栏的 Y 轴偏置
                            val y = marginsTop + (rrowNum * rid * rh) + (rh * charIdx) + rowDeltaY

                            mainPos.add(Offset(x, y))
                            subPos.add(Offset(x + cw / 2f, y)) // 夹批小字右移半个字宽
                        }
                    }
                }
            }
            // 模式 A-2：分栏横向半叶换行 (先排右半页，再排左半页)
            if (multirowsHLayout == 2) {
                // 第一步：排右半页（col: 0 ~ colNum/2-1）
                for (rid in 0 until multirowsNum) {
                    for (col in 0 until colNum / 2) {
                        for (charIdx in 0 until rrowNum) {
                            val x = canvasWidth - marginsRight - cw * (col + 1)
                            val y = marginsTop + (rrowNum * rid * rh) + (rh * charIdx) + rowDeltaY
                            mainPos.add(Offset(x, y))
                            subPos.add(Offset(x + cw / 2f, y))
                        }
                    }
                }

                // 第二步：排左半页（col: colNum/2 ~ colNum-1）
                for (rid in 0 until multirowsNum) {
                    for (col in colNum / 2 until colNum) {
                        for (charIdx in 0 until rrowNum) {
                            val x = canvasWidth - marginsRight - cw * (col + 1) - lcWidth
                            val y = marginsTop + (rrowNum * rid * rh) + (rh * charIdx) + rowDeltaY
                            mainPos.add(Offset(x, y))
                            subPos.add(Offset(x + cw / 2f, y))
                        }
                    }
                }
            }
        } else {
            // 模式 B：单栏常规直排
            for (col in 0 until colNum) {
                for (charIdx in 0 until rowNum) {
                    val x = canvasWidth - marginsRight - cw * (col + 1) - if (col >= colNum / 2) lcWidth else 0f
                    val y = marginsTop + (rh * charIdx) + rowDeltaY

                    mainPos.add(Offset(x, y))
                    subPos.add(Offset(x + cw / 2f, y))
                }
            }
        }

        // 5. 计算每页字容量
        val pageCharsNum = colNum * rowNum

        return BookGrid(mainPos, subPos, pageCharsNum)
    }
}