package icu.hearme.vrain.engine

import icu.hearme.vrain.configure.AncientBookState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.stream.Collectors
import kotlin.math.ceil
import kotlin.math.min

data class BookPage(
    val pageIndex: Int,
    val chars: List<RenderChar>
)

/** 单个渲染字符的详尽指令集 */
data class RenderChar(
    val char: String,           // 字符本身
    val isComment: Boolean,     // 是否是夹批双排小字
    val pcntIndex: Float,       // 网格指针位 (0.0, 0.5, 1.0, 1.5...)
    val isRotated: Boolean,     // 是否需要逆时针旋转 90 度 (如英文字母、拼音)
    val isNop: Boolean,         // 是否是不占字位的标点 (如破折号延伸)
    val tags: Set<CharTag>      // 该字符挂载的特殊视觉标记
)

/** 特殊排版标记枚举 */
enum class CharTag {
    BOOK_LINE,      // 书名号左侧波浪线 《》
    RECT_FRAME,     // 圆角方框 〔〕
    CIRCLE_FRAME,   // 圆形框 〈〉
    ZOOM_IN,        // 字体缩放 （）
    CIRCLE_NOTE,    // 右侧圈注 ｛｝
    POINT_NOTE,     // 右侧点注 ＜＞
    LINE_NOTE,       // 右侧线注 ［］
    RAISED_HEAD    // 抬头/顶格/进一字 (对应标识符 T)
}

object BookTextEngine {

    suspend fun parseTextToPages(rawText: String, bookState: AncientBookState, grid: BookGrid): List<BookPage> =
        withContext(Dispatchers.Default) {
        val config = bookState
        val rowNum = bookState.rowNum / (grid.mrowNum ?: 1)
        val charsPerPage = grid.charsPerPage

        // 1. 全局预处理：标点替换、清洗、对齐补空
        val processedText = preprocessText(rawText, config, rowNum)

        val pages = mutableListOf<BookPage>()
        var currentPageChars = mutableListOf<RenderChar>()
        var pageIndex = 1
        var pcnt = 0f // 核心：标准字位指针

        // 状态机标签
        var tagBookline = false
        var tagRectFrame = false
        var tagCircleFrame = false
        var tagZoom = false
        var tagCircleNote = false
        var tagPointNote = false
        var tagLineNote = false

        fun nextPage() {
            if (currentPageChars.isNotEmpty()) {
                pages.add(BookPage(pageIndex, currentPageChars))
                pageIndex++
                currentPageChars = mutableListOf()
            }
            pcnt = 0f
        }

        var i = 0
        val charList: List<String> = processedText.codePoints()
            .mapToObj { String(Character.toChars(it)) }
            .collect(Collectors.toList())
        val textLength = charList.size

        // 2. 逐字扫描状态机
        while (i < textLength) {
            // 如果指针达到页尾，且当前字符不是不占位的附庸标点，则翻页
            if (pcnt >= charsPerPage && !isNopComma(charList[i], config.textCommaNop)) {
                nextPage()
                continue
            }

            val c = charList[i]

            // A. 控制符处理：换段与排版跳跃 (对应 $, %, &)
            when (c) {
                AncientBookState.tagHalfpage -> { // 前进半页或整页
                    pcnt = if (pcnt < charsPerPage / 2f) charsPerPage / 2f else charsPerPage.toFloat()
                    i++
                    continue
                }
                AncientBookState.tagNewpage -> { // 强制分页符
                    pcnt = if (pcnt > 1) charsPerPage.toFloat() else 0f
                    i++
                    continue
                }
                AncientBookState.tagLastcol -> { // 跳至本页最后一列
                    if (pcnt <= charsPerPage - rowNum) {
                        pcnt = (charsPerPage - rowNum).toFloat()
                    }
                    i++
                    continue
                }
                AncientBookState.tagNewraw -> { // 多栏模式下跳转到下一栏
                    val charsPerSection = charsPerPage / (grid.mrowNum ?: 1)
                    i += rowNum
                    if (pcnt % charsPerSection == 0f) { continue }
                    pcnt = ((pcnt / charsPerSection).toInt() + 1) * charsPerSection.toFloat()
                    continue
                }
            }

            // B. 特殊标记符开关处理
            if (config.ifTagBookline && c == "《") { tagBookline = true; i++; continue }
            if (config.ifTagBookline && c == "》") { tagBookline = false; i++; continue }

            if (config.ifTagRectframe && c == "〔") { tagRectFrame = true; i++; continue }
            if (config.ifTagRectframe && c == "〕") { tagRectFrame = false; i++; continue }

            if (config.ifTagTextzoom && c == "（") { tagZoom = true; i++; continue }
            if (config.ifTagTextzoom && c == "）") { tagZoom = false; i++; continue }

            if (config.ifTagCircleframe && c == "〈") { tagCircleFrame = true; i++; continue }
            if (config.ifTagCircleframe && c == "〉") { tagCircleFrame = false; i++; continue }

            if (config.ifTagCirclenote && c == "｛") { tagCircleNote = true; i++; continue }
            if (config.ifTagCirclenote && c == "｝") { tagCircleNote = false; i++; continue }

            if (config.ifTagPointnote && c == "＜") { tagPointNote = true; i++; continue }
            if (config.ifTagPointnote && c == "＞") { tagPointNote = false; i++; continue }

            if (config.ifTagLinenote && c == "［") { tagLineNote = true; i++; continue }
            if (config.ifTagLinenote && c == "］") { tagLineNote = false; i++; continue }

            // C. 夹批（双排小字）解析逻辑
            if (c == "【") {
                i++

                // 1. 提取夹批全部文本
                val commentContent = StringBuilder()
                while (i < textLength && charList[i] != "】") {
                    commentContent.append(charList[i])
                    i++
                }
                if (i < textLength && charList[i] == "】") i++

                class CommentUnit(val mainChar: String, val nops: List<String>, val tags: Set<CharTag>)
                val units = mutableListOf<CommentUnit>()
                var j = 0
                val commentCharList: List<String> = commentContent.codePoints()
                    .mapToObj { String(Character.toChars(it)) }
                    .collect(Collectors.toList())
                val cLen = commentCharList.size
                var innerTagBookline = tagBookline
                var innerTagRectFrame = tagRectFrame
                var innerTagCircleFrame = tagCircleFrame
                while (j < cLen) {
                    val cc = commentCharList[j]
                    if (config.ifTagBookline && cc == "《") { innerTagBookline = true; j++; continue }
                    if (config.ifTagBookline && cc == "》") { innerTagBookline = false; j++; continue }
                    if (config.ifTagRectframe && cc == "〔") { innerTagRectFrame = true; j++; continue }
                    if (config.ifTagRectframe && cc == "〕") { innerTagRectFrame = false; j++; continue }
                    if (config.ifTagCircleframe && cc == "〈") { innerTagCircleFrame = true; j++; continue }
                    if (config.ifTagCircleframe && cc == "〉") { innerTagCircleFrame = false; j++; continue }

                    val activeTags = buildTags(innerTagBookline, innerTagRectFrame, innerTagCircleFrame, false, false, false, false)
                    if (!isNopComma(cc, config.commentCommaNop)) {
                        val nops = mutableListOf<String>()
                        j++
                        while (j < cLen) {
                            val nopChar = commentCharList[j]
                            if (config.ifTagBookline && nopChar == "《") { innerTagBookline = true; j++; continue }
                            if (config.ifTagBookline && nopChar == "》") { innerTagBookline = false; j++; continue }
                            if (config.ifTagRectframe && nopChar == "〔") { innerTagRectFrame = true; j++; continue }
                            if (config.ifTagRectframe && nopChar == "〕") { innerTagRectFrame = false; j++; continue }
                            if (config.ifTagCircleframe && nopChar == "〈") { innerTagCircleFrame = true; j++; continue }
                            if (config.ifTagCircleframe && nopChar == "〉") { innerTagCircleFrame = false; j++; continue }

                            if (isNopComma(nopChar, config.commentCommaNop)) {
                                nops.add(nopChar)
                                j++
                            } else {
                                break
                            }
                        }
                        units.add(CommentUnit(cc, nops, activeTags))
                    } else {
                        units.add(CommentUnit(cc, emptyList(), activeTags))
                        j++
                    }
                }

                // 3. 按照“当前列剩余格子”精确切片投放
                var commentUnits = units.toList()
                while (commentUnits.isNotEmpty()) {
                    // 页满换页
                    if (pcnt >= charsPerPage) { nextPage() }

                    val currentLineRow = pcnt.toInt() % rowNum
                    val slotsLeftInLine = rowNum - currentLineRow
                    val capacity = slotsLeftInLine * bookState.commentGridType
                    val chunkSize = min(commentUnits.size, capacity)
                    val currentChunk = commentUnits.take(chunkSize)
                    commentUnits = commentUnits.drop(chunkSize)

                    val nChunk = currentChunk.size
                    val startRow = pcnt.toInt()

                    if (config.commentGridType == 4) {
                        val rightLineChars = ceil(nChunk / 2.0).toInt()
                        for (k in 0 until nChunk) {
                            val unit = currentChunk[k]
                            val isRight = k < rightLineChars
                            val lineIndex = if (isRight) k else (k - rightLineChars)

                            val slotOffset = lineIndex / 2
                            val isTop = (lineIndex % 2 == 0)

                            val basePcnt: Float
                            if (nChunk <= 2) {
                                basePcnt = startRow.toFloat() + (if (k == 0) 0f else 0.5f)
                            } else {
                                if (isRight) {
                                    basePcnt = (startRow + slotOffset).toFloat() + (if (isTop) 0f else 0.25f)
                                } else {
                                    basePcnt = (startRow + slotOffset).toFloat() + (if (isTop) 0.5f else 0.75f)
                                }
                            }
                            currentPageChars.add(RenderChar(unit.mainChar, true, basePcnt, checkRotation(unit.mainChar), false, unit.tags))
                            unit.nops.forEach { nopChar ->
                                val nopTags = unit.tags - CharTag.RECT_FRAME - CharTag.CIRCLE_FRAME
                                currentPageChars.add(RenderChar(nopChar, true, basePcnt, checkRotation(nopChar), true, nopTags))
                            }
                        }
                        val slotsConsumed = ceil(rightLineChars / 2.0).toInt()
                        pcnt = (startRow + slotsConsumed).toFloat()
                    } else {
                        val hChunk = ceil(nChunk / 2.0).toInt()
                        for (k in 0 until nChunk) {
                            val unit = currentChunk[k]
                            val isRight = k < hChunk

                            val rowOffset = if (isRight) k else (k - hChunk)
                            val row = startRow + rowOffset
                            val basePcnt = row.toFloat() + (if (isRight) 0f else 0.5f)
                            currentPageChars.add(RenderChar(unit.mainChar, true, basePcnt, checkRotation(unit.mainChar), false, unit.tags))

                            unit.nops.forEach { nopChar ->
                                val nopTags = unit.tags - CharTag.RECT_FRAME - CharTag.CIRCLE_FRAME
                                currentPageChars.add(RenderChar(nopChar, true, basePcnt, checkRotation(nopChar), true, nopTags))
                            }
                        }
                        pcnt = (startRow + hChunk).toFloat()
                    }
                }

                pcnt = ceil(pcnt.toDouble()).toFloat()
                continue
            }
            // 抬头 / 进一字 (T) 解析逻辑
            if (c == "T") {
                if (i + 1 < textLength) {
                    val nextChar = charList[i + 1]
                    val activeTags = buildTags(tagBookline, tagRectFrame, tagCircleFrame, tagZoom, tagCircleNote, tagPointNote, tagLineNote).toMutableSet()
                    activeTags.add(CharTag.RAISED_HEAD)

                    currentPageChars.add(RenderChar(nextChar, false, pcnt, checkRotation(nextChar), false, activeTags))
                    i += 2
                    continue
                }
            }
            // D. 标准正文解析逻辑
            val isNop = isNopComma(c, config.textCommaNop)
            val activeTags = buildTags(tagBookline, tagRectFrame, tagCircleFrame, tagZoom, tagCircleNote, tagPointNote, tagLineNote)

            if (isNop) {
                val nopTags = activeTags - CharTag.RECT_FRAME - CharTag.CIRCLE_FRAME
                currentPageChars.add(RenderChar(c, false, pcnt - 1f, checkRotation(c), true, nopTags))
            } else {
                currentPageChars.add(RenderChar(c, false, pcnt, checkRotation(c), false, activeTags))
                pcnt += 1f
            }
            i++
        }

        // 把残余的最后一页压入集合
        if (currentPageChars.isNotEmpty()) {
            pages.add(BookPage(pageIndex, currentPageChars))
        }

        return@withContext pages
    }

    // 内部处理工具与正则引擎
    private fun preprocessText(raw: String, config: AncientBookState, rowNum: Int): String {
        val lines = raw.lines()
        val resultBuilder = StringBuilder()

        for (line in lines) {
            var text = line.replace(Regex("^\\s+"), "") // 去除段首空白
            if (text.isEmpty()) continue

            // 1. 自定义标点与数字替换 (格式类似 "，,|。.|1一")
            if (config.expReplaceComma.isNotBlank()) {
                config.expReplaceComma.split("|").forEach { kv ->
                    if (kv.length == 2) text = text.replace(kv[0].toString(), kv[1].toString())
                }
            }
            if (config.expReplaceNumber.isNotBlank()) {
                config.expReplaceNumber.split("|").forEach { kv ->
                    if (kv.length == 2) text = text.replace(kv[0].toString(), kv[1].toString())
                }
            }

            // 2. 标点删除与过滤
            if (config.expDeleteComma.isNotBlank()) {
                text = text.replace(Regex("[${config.expDeleteComma}]"), "")
            }
            if (config.ifNocomma && config.expNocomma.isNotBlank()) {
                text = text.replace(Regex("[${config.expNocomma}]"), "")
            }
            if (config.ifOnlyperiod && config.expOnlyperiod.isNotBlank()) {
                text = text.replace(Regex("[${config.expOnlyperiod}]"), "。")
                text = text.replace(Regex("。+"), "。")
                text = text.replace(Regex("^。"), "")
            }
            text = text.replace(AncientBookState.tagSpace, " ") // @ 替换为空格

            val textForCounting = text.replace(Regex("^T.{1}"), "")
            val charList: List<String> = textForCounting.codePoints()
                .mapToObj { String(Character.toChars(it)) }
                .collect(Collectors.toList())

            var occupiedSlots = 0
            var idx = 0
            val len = charList.size
            val hasLayoutControlCmd = text.contains(AncientBookState.tagNewpage) ||
                    text.contains(AncientBookState.tagHalfpage) ||
                    text.contains(AncientBookState.tagLastcol) ||
                    text.contains(AncientBookState.tagNewraw)

            while (idx < len) {
                val c = charList[idx]
                if (config.ifTagBookline && (c == "《" || c == "》")) { idx++; continue }
                if (config.ifTagRectframe && (c == "〔" || c == "〕")) { idx++; continue }
                if (config.ifTagTextzoom && (c == "（" || c == "）")) { idx++; continue }
                if (config.ifTagCircleframe && (c == "〈" || c == "〉")) { idx++; continue }
                if (config.ifTagCirclenote && (c == "｛" || c == "｝")) { idx++; continue }
                if (config.ifTagPointnote && (c == "＜" || c == "＞")) { idx++; continue }
                if (config.ifTagLinenote && (c == "［" || c == "］")) { idx++; continue }

                if (c == "【") {
                    idx++
                    var blockCommentCount = 0
                    while (idx < len && charList[idx] != "】") {
                        val cc = charList[idx]
                        if (config.ifTagBookline && (cc == "《" || cc == "》")){
                            idx++
                            continue
                        }
                        if (!isNopComma(cc, config.commentCommaNop)) {
                            blockCommentCount++
                        }
                        idx++
                    }
                    if (idx < len && charList[idx] == "】") { idx++ }
                    occupiedSlots += ceil(blockCommentCount / config.commentGridType.toDouble()).toInt()
                } else {
                    if (!isNopComma(c, config.textCommaNop) &&
                        c != AncientBookState.tagHalfpage &&
                        c != AncientBookState.tagNewpage &&
                        c != AncientBookState.tagLastcol
                    ) {
                        occupiedSlots++
                    }
                    idx++
                }
            }
            val paddingSpaces = if (hasLayoutControlCmd || occupiedSlots == 0) {
                0
            } else {
                val remainder = occupiedSlots % rowNum
                if (remainder == 0) 0 else rowNum - remainder
            }

            // 拼装：原始处理文本 + 用于补齐该列的空格
            resultBuilder.append(text)
            if (paddingSpaces in 1..<rowNum) {
                resultBuilder.append(" ".repeat(paddingSpaces))
            }
        }

        return resultBuilder.toString()
    }

    private fun isNopComma(c: String, nopConfigStr: String): Boolean {
        if (nopConfigStr.isBlank()) return false
        val cleanNopStr = nopConfigStr.replace("|", "")
        return cleanNopStr.contains(c)
    }

    /**
     * 拼音、英文字母在直排中需逆时针旋转 90 度
     */
    private fun checkRotation(c: String): Boolean {
        val regex = Regex("[a-zA-Zāáǎàōóǒòēéěèīíǐìūúǔùǖǘǚǜü]")
        return regex.matches(c)
    }

    private fun buildTags(bl: Boolean, rf: Boolean, cf: Boolean, z: Boolean, cn: Boolean, pn: Boolean, ln: Boolean): Set<CharTag> {
        val tags = mutableSetOf<CharTag>()
        if (bl) tags.add(CharTag.BOOK_LINE)
        if (rf) tags.add(CharTag.RECT_FRAME)
        if (cf) tags.add(CharTag.CIRCLE_FRAME)
        if (z) tags.add(CharTag.ZOOM_IN)
        if (cn) tags.add(CharTag.CIRCLE_NOTE)
        if (pn) tags.add(CharTag.POINT_NOTE)
        if (ln) tags.add(CharTag.LINE_NOTE)
        return tags
    }
}