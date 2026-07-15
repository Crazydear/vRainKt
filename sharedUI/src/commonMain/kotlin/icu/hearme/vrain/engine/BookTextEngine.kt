package icu.hearme.vrain.engine

import icu.hearme.vrain.configure.AncientBookState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.ceil
import kotlin.math.min

data class BookPage(
    val pageIndex: Int,
    val chars: List<RenderChar>
)

/** 单个渲染字符的详尽指令集 */
data class RenderChar(
    val char: Char,             // 字符本身
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
    RAISED_HEAD,    // 抬头/顶格/进一字 (对应标识符 T)
}

object BookTextEngine {

    suspend fun parseTextToPages(rawText: String, bookState: AncientBookState, grid: BookGrid): List<BookPage> =
        withContext(Dispatchers.Default) {
        val config = bookState
        val rowNum = bookState.rowNum
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
        val textLength = processedText.length

        // 2. 逐字扫描状态机
        while (i < textLength) {
            // 如果指针达到页尾，且当前字符不是不占位的附庸标点，则翻页
            if (pcnt >= charsPerPage && !isNopComma(processedText[i], config.textCommaNop)) {
                nextPage()
                continue
            }

            val c = processedText[i]

            // A. 控制符处理：换段与排版跳跃 (对应 $, %, &)
            when (c) {
                '$' -> { // 前进半页或整页
                    pcnt = if (pcnt < charsPerPage / 2f) charsPerPage / 2f else charsPerPage.toFloat()
                    i++
                    continue
                }
                '%' -> { // 强制分页符
                    pcnt = if (pcnt > 1) charsPerPage.toFloat() else 0f
                    i++
                    continue
                }
                '&' -> { // 跳至本页最后一列
                    if (pcnt <= charsPerPage - rowNum) {
                        pcnt = (charsPerPage - rowNum).toFloat()
                    }
                    i++
                    continue
                }
            }

            // B. 特殊标记符开关处理
            if (config.ifTagBookline && c == '《') { tagBookline = true; i++; continue }
            if (config.ifTagBookline && c == '》') { tagBookline = false; i++; continue }

            if (config.ifTagRectframe && c == '〔') { tagRectFrame = true; i++; continue }
            if (config.ifTagRectframe && c == '〕') { tagRectFrame = false; i++; continue }

            if (config.ifTagTextzoom && c == '（') { tagZoom = true; i++; continue }
            if (config.ifTagTextzoom && c == '）') { tagZoom = false; i++; continue }

            if (config.ifTagCircleframe && c == '〈') { tagCircleFrame = true; i++; continue }
            if (config.ifTagCircleframe && c == '〉') { tagCircleFrame = false; i++; continue }

            if (config.ifTagCirclenote && c == '｛') { tagCircleNote = true; i++; continue }
            if (config.ifTagCirclenote && c == '｝') { tagCircleNote = false; i++; continue }

            if (config.ifTagPointnote && c == '＜') { tagPointNote = true; i++; continue }
            if (config.ifTagPointnote && c == '＞') { tagPointNote = false; i++; continue }

            if (config.ifTagLinenote && c == '［') { tagLineNote = true; i++; continue }
            if (config.ifTagLinenote && c == '］') { tagLineNote = false; i++; continue }

            // C. 夹批（双排小字）解析逻辑
            if (c == '【') {
                i++

                // 1. 提取夹批全部文本
                val commentContent = StringBuilder()
                while (i < textLength && processedText[i] != '】') {
                    commentContent.append(processedText[i])
                    i++
                }
                if (i < textLength && processedText[i] == '】') i++

                // 2. 将夹批内容聚类为“字元单元（Unit）”，合并紧跟其后的不占位标点 (Nop)
                class CommentUnit(val mainChar: Char, val nops: List<Char>)
                val units = mutableListOf<CommentUnit>()
                var j = 0
                val cLen = commentContent.length
                while (j < cLen) {
                    val cc = commentContent[j]
                    if (!isNopComma(cc, config.commentCommaNop)) {
                        val nops = mutableListOf<Char>()
                        j++
                        while (j < cLen && isNopComma(commentContent[j], config.commentCommaNop)) {
                            nops.add(commentContent[j])
                            j++
                        }
                        units.add(CommentUnit(cc, nops))
                    } else {
                        units.add(CommentUnit(cc, emptyList()))
                        j++
                    }
                }

                // 3. 按照“当前列剩余格子”精确切片投放
                var commentUnits = units.toList()
                while (commentUnits.isNotEmpty()) {
                    // 页满换页
                    if (pcnt >= charsPerPage) {
                        nextPage()
                    }

                    // 🌟 【修复关键点】：计算当前列（行高内）还剩多少空格
                    val currentLineRow = pcnt.toInt() % rowNum
                    val slotsLeftInLine = rowNum - currentLineRow
                    val capacity = slotsLeftInLine * 2 // 双排小字容量为格子数的 2 倍

                    // 切出绝不超过当前列容量的 Chunk
                    val chunkSize = min(commentUnits.size, capacity)
                    val currentChunk = commentUnits.take(chunkSize)
                    commentUnits = commentUnits.drop(chunkSize)

                    val nChunk = currentChunk.size
                    val hChunk = ceil(nChunk / 2.0).toInt() // 当前 Chunk 占用的实际行高格子数
                    val startRow = pcnt.toInt()

                    // 执行单列内“先右后左”排字
                    for (k in 0 until nChunk) {
                        val unit = currentChunk[k]
                        val isRight = k < hChunk

                        val rowOffset = if (isRight) k else (k - hChunk)
                        val row = startRow + rowOffset
                        val basePcnt = row.toFloat() + (if (isRight) 0f else 0.5f)

                        val activeTags = buildTags(tagBookline, tagRectFrame, tagCircleFrame, false, false, false, false)

                        // 投放主字
                        currentPageChars.add(
                            RenderChar(
                                char = unit.mainChar,
                                isComment = true,
                                pcntIndex = basePcnt,
                                isRotated = checkRotation(unit.mainChar),
                                isNop = false,
                                tags = activeTags
                            )
                        )

                        // 投放附属 Nop 标点
                        unit.nops.forEach { nopChar ->
                            currentPageChars.add(
                                RenderChar(
                                    char = nopChar,
                                    isComment = true,
                                    pcntIndex = basePcnt,
                                    isRotated = checkRotation(nopChar),
                                    isNop = true,
                                    tags = activeTags
                                )
                            )
                        }
                    }

                    // 推进指针到该 Chunk 的末尾
                    pcnt = (startRow + hChunk).toFloat()
                }

                pcnt = ceil(pcnt.toDouble()).toFloat()
                continue
            }
            // 抬头 / 进一字 (T) 解析逻辑
            if (c == 'T') {
                if (i + 1 < textLength) {
                    val nextChar = processedText[i + 1]
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

            if (isNop) { // 不占字位（如破折号下半段），依附于上一个正文字格 (pcnt - 1f)
                currentPageChars.add(RenderChar(c, false, pcnt - 1f, checkRotation(c), true, activeTags))
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
        val lines = raw.split("\n")
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
            text = text.replace("@", " ") // @ 替换为空格

            val textForCounting = text.replace(Regex("^T.{1}"), "")

            var occupiedSlots = 0
            var idx = 0
            val len = textForCounting.length

            while (idx < len) {
                val c = textForCounting[idx]
                if (c == '【') {
                    idx++
                    var blockCommentCount = 0
                    while (idx < len && textForCounting[idx] != '】') {
                        val cc = textForCounting[idx]
                        if (!isNopComma(cc, config.commentCommaNop)) {
                            blockCommentCount++
                        }
                        idx++
                    }
                    if (idx < len && textForCounting[idx] == '】') {
                        idx++
                    }
                    // 每一个独立的批注块单独向上取整！(100% 对应 Perl 中的 $rnum 计算)
                    occupiedSlots += ceil(blockCommentCount / 2.0).toInt()
                } else {
                    // 正文普通字，排除不占字位的正文标点后计入正文格数
                    if (!isNopComma(c, config.textCommaNop)) {
                        occupiedSlots++
                    }
                    idx++
                }
            }

            val remainder = occupiedSlots % rowNum
            val paddingSpaces = if (remainder == 0) 0 else rowNum - remainder

            // 拼装：原始处理文本 + 用于补齐该列的空格
            resultBuilder.append(text)
            if (paddingSpaces in 1..<rowNum) {
                resultBuilder.append(" ".repeat(paddingSpaces))
            }
        }

        return resultBuilder.toString()
    }

    private fun isNopComma(c: Char, nopConfigStr: String): Boolean {
        if (nopConfigStr.isBlank()) return false
        val cleanNopStr = nopConfigStr.replace("|", "")
        return cleanNopStr.contains(c)
    }

    private fun checkRotation(c: Char): Boolean {
        // 拼音、英文字母在直排中需逆时针旋转 90 度
        val regex = Regex("[a-zA-Zāáǎàōóǒòēéěèīíǐìūúǔùǖǘǚǜü]")
        return regex.matches(c.toString())
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