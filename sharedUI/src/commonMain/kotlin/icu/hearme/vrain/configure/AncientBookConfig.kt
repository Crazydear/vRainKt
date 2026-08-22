package icu.hearme.vrain.configure

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import icu.hearme.vrain.utils.ColorConvert.toColor
import icu.hearme.vrain.utils.ColorConvert.toConfigString
import kotlinx.serialization.Serializable

@Serializable
data class BookConfigData(
    val title: String = "",
    val author: String = "",
    val canvas_id: String = "18_blue",          // 古籍刻本背景图ID
    val row_num: Int = 30,                      // 每列字数
    val row_delta_y: Float = 8f,                // 列最后字符到边框距离

    val multirows_horizontal_layout: Int = 1,
    // 字体
    val font1: String = "qiji_combo.ttf",
    val font2: String = "HanaMinA.ttf",
    val font3: String = "HanaMinB.ttf",
    val font4: String? = "KaiXinSongA.ttf",
    val font5: String? = "KaiXinSongB.ttf",
    val try_st: Int? = null,     // 不建议开启！字体不支持时尝试繁简、简繁转换，也许会改善字体支持情况，但很可能出现语境不符

    // 字体旋转角度
    val font1_rotate: Double = 0.0,
    val font2_rotate: Double = 0.0,
    val font3_rotate: Double = 0.0,
    val font4_rotate: Double = 0.0,
    val font5_rotate: Double = 0.0,

    // 正文字体大小、颜色
    val text_fonts_array: String = "12345",
    val text_font1_size: Float = 60f,
    val text_font2_size: Float = 50f,
    val text_font3_size: Float = 50f,
    val text_font4_size: Float = 50f,
    val text_font5_size: Float = 50f,
    val text_font_color: String = "black",

    // 批注字体字号、颜色
    val comment_fonts_array: String ="12345",
    val comment_font1_size: Float = 40f,
    val comment_font2_size: Float = 36f,
    val comment_font3_size: Float = 36f,
    val comment_font4_size: Float = 36f,
    val comment_font5_size: Float = 36f,
    val comment_font_color: String = "black",
    val comment_grid_type: Int = 4,             // 新增参数，批注占正文的字符数
    val comment_font_zoom: Float = 0.75f,       // 新增参数，紧凑批注缩放比例

    val if_font_metric_adjust: Int = 0,         // 字体度量微调
    val if_fallback_bold: Int = 0,              // 回退字体模拟加粗
    val fallback_bold_stroke_width: Float = 1f, // 模拟加粗描边宽度，建议0.5~2.0

    // 封面标题字体字号、颜色、高度
    val cover_title_font_size: Float = 120f,
    val cover_title_y: Float = 200f,
    val cover_author_font_size: Float = 60f,
    val cover_author_y: Float = 600f,
    val cover_font_color: String = "black",

    // 版心标题字体字号、颜色、高度、字间距比例
    val if_tpcenter: Int = 1,               // 版心标题页码是否居中，1时居中，0是居左侧
    val title_font_size: Float = 65f,
    val title_font_color: String = "black",
    val title_y: Float = 1250f,
    val title_ydis: Float = 1.25f,
    val title_postfix: String? = "卷X",  // 版心标题后缀，X会自动替换，若存在保存前言、序的000.txt文件，将自动更新为序，若存在保存后记、附录的999.txt文件，将自动更新为附，不需要后缀时置空即可
    val title_directory: Int = 0,       // 根据标题自动添加PDF目录

    // 版心页码字体字号、颜色、高度
    val pager_font_size: Float = 30f,
    val pager_font_color: String = "black",
    val pager_y: Float = 540f,

    // 标点符号处理规则，顺序：替换->删除->模式（有标点，无标点，归一化）
    val exp_replace_comma: String = ",，|.。|:：|;；|!！|?？|(（|)）|（〔|）〕|{〔|}〕|<〔|>〕|[〔|]〕|“「|”」|‘『|’』|⋯…",
    val exp_replace_number: String = "1一|2二|3三|4四|5五|6六|7七|8八|9九|0〇|１一|２二|３三|４四|５五|６六|７七|８八|９九|０〇",
    val exp_delete_comma: String = "．|　|-|─||〖|〗", // 删除的标点符号，以|分隔
    val if_nocomma: Int = 0, // 无标点符号模式
    val exp_nocomma: String = "、|，|。|：|；|！|？|〔|〕|「|」|『|』", // 无标点符号模式下过滤的标点符号, 以|分隔，if_nocomma为1时有效
    val if_onlyperiod: Int = 1, // 标点符号归一化为句号
    val exp_onlyperiod: String = "、|，|。|：|；|！|？|〔|〕|「|」|『|』", // 归一化为句号的标点符号，以|分隔，if_onlyperiod为1时有效
    val comma_color: String? = "red",

    // 正文标点符号
    val text_comma_nop: String = "、|，|。|：|；|！|？",        // 不占独立字符位置的标点符号
    val text_comma_nop_size: Float = 0.6f,      // 不占独立字符位置标点符号大小缩放
    val text_comma_nop_x: Float = 0.7f,         // 不占独立字符位置标点符号横向位置调整，越大越往右移
    val text_comma_nop_y: Float = 0.2f,         // 不占独立字符位置标点符号纵向位置调整，越大越往下移
    val text_comma_90: String = "「」『』〔〕…",  // 旋转90度的标点符号
    val text_comma_90_size: Float = 0.8f,       // 旋转90度标点符号大小缩放
    val text_comma_90_x: Float = 0.35f,         // 旋转90度标点符号横向位置调整，越大越往右移
    val text_comma_90_y: Float = 0.6f,          // 旋转90度标点符号纵向位置调整，越大越往上移

    // 批注标点符号
    val comment_comma_nop: String = "、|，|。|：|；|！|？",
    val comment_comma_nop_size: Float = 0.7f,
    val comment_comma_nop_x: Float = 0.65f,
    val comment_comma_nop_y: Float = 0.3f,
    val comment_comma_90: String = "「」『』〔〕…",
    val comment_comma_90_size: Float = 0.8f,
    val comment_comma_90_x: Float = 0.15f,
    val comment_comma_90_y: Float = 0.5f,
    // 书名号文字左侧波浪线
    val if_book_vline: Int = 1,                 // 将书名号《》转换为侧重点线
    val book_line_width: Float = 1f,            // 侧线宽度
    val book_line_color: String = "black",      // 侧线颜色

    val if_tag_bookline: Int = 1,

    val if_tag_circlenote: Int = 1,
    val text_note_ox: Float = 0.1f,
    val text_note_oy: Float = 0.4f,
    val text_note_or: Float = 0f,
    val text_note_ow: Float = 1f,
    val text_note_oc: String = "#000000",

    val if_tag_pointnote: Int = 1,
    val text_note_px: Float = 0f,
    val text_note_py: Float = 0f,
    val text_note_ps: Float = 0f,
    val text_note_pc: String = "#000000",

    val if_tag_linenote: Int = 1,
    val text_note_lx: Float = 0f,
    val text_note_ly: Float = 0f,
    val text_note_lw: Float = 1f,
    val text_note_lc: String = "#000000",
    // 字符底框参数
    val if_tag_rectframe: Int = 1,
    // 圆角方框
    val rect_type: Int = 0,
    val rect_bcolor: String = "#000000",
    val rect_fcolor: String = "#FFFFFF",
    val text_rect_y: Float = 0f,
    val text_rect_h: Float = 0f,
    val text_rect_r: Float = 10f,
    val text_rect_f: Float = 1f,
    val comm_rect_y: Float = 0f,
    val comm_rect_h: Float = 0f,
    val comm_rect_r: Float = 5f,
    val comm_rect_f: Float = 1f,

    val if_tag_circleframe: Int = 1,
    // 圆形框
    val circle_type: Int = 0,
    val circle_bcolor: String = "#000000",
    val circle_fcolor: String = "#FFFFFF",
    val text_circle_y: Float = 0f,
    val text_circle_r: Float = 0f,
    val text_circle_f: Float = 1f,
    val comm_circle_y: Float = 0f,
    val comm_circle_r: Float = 0f,
    val comm_circle_f: Float = 1f,

    // 正文字符缩放
    val if_tag_textzoom: Int = 0,
    val text_zoom: Float = 1.0f,

)

class AncientBookState(initialData: BookConfigData) {
    var configData by mutableStateOf(initialData)
        private set

    fun applyNewConfig(data: BookConfigData) {
        configData = data
    }

    fun toData(): BookConfigData {
        return configData
    }

    var title: String
        get() = configData.title
        set(value) { configData = configData.copy(title = value) }

    var author: String
        get() = configData.author
        set(value) { configData = configData.copy(author = value) }

    var canvasId: String            //古籍刻本背景图ID
        get() = configData.canvas_id
        set(value) { configData = configData.copy(canvas_id = value) }

    var rowNum: Int                     //每列字数
        get() = configData.row_num
        set(value) { configData = configData.copy(row_num = value) }

    var rowDeltaY: Float                //列最后字符到边框距离
        get() = configData.row_delta_y
        set(value) { configData = configData.copy(row_delta_y = value) }

    var multirowsHorizontalLayout: Int
        get() = configData.multirows_horizontal_layout
        set(value) { configData = configData.copy(multirows_horizontal_layout = value) }

    //字体
    val font1: String
        get() = configData.font1

    val font2: String
        get() = configData.font2
    val font3: String
        get() = configData.font3
    val font4: String?
        get() = configData.font4
    val font5: String?
        get() = configData.font5

    var trySt: Boolean    // 不建议开启！字体不支持时尝试繁简、简繁转换，也许会改善字体支持情况，但很可能出现语境不符
        get() = configData.try_st == 1
        set(value) { configData.copy(try_st = if (value) 1 else null) }

    // 字体旋转角度
    var font1Rotate: Double
        get() = configData.font1_rotate
        set(value) { configData = configData.copy(font1_rotate = value) }

    var font2Rotate: Double
        get() = configData.font2_rotate
        set(value) { configData = configData.copy(font2_rotate = value) }

    var font3Rotate: Double
        get() = configData.font3_rotate
        set(value) { configData = configData.copy(font3_rotate = value) }

    var font4Rotate: Double
        get() = configData.font4_rotate
        set(value) { configData = configData.copy(font4_rotate = value) }

    var font5Rotate: Double
        get() = configData.font5_rotate
        set(value) { configData = configData.copy(font5_rotate = value) }

    // 正文字体大小、颜色
    var textFontsArray: String
        get() = configData.text_fonts_array
        set(value) { configData = configData.copy(text_fonts_array = value)}

    var textFont1Size: Float
        get() = configData.text_font1_size
        set(value) { configData = configData.copy(text_font1_size = value) }

    var textFont2Size: Float
        get() = configData.text_font2_size
        set(value) { configData = configData.copy(text_font2_size = value) }

    var textFont3Size: Float
        get() = configData.text_font3_size
        set(value) { configData = configData.copy(text_font3_size = value) }

    var textFont4Size: Float
        get() = configData.text_font4_size
        set(value) { configData = configData.copy(text_font4_size = value) }

    var textFont5Size: Float
        get() = configData.text_font5_size
        set(value) { configData = configData.copy(text_font5_size = value) }

    var textFontColor: Color
        get() = configData.text_font_color.toColor()
        set(value) { configData = configData.copy(text_font_color = value.toConfigString())}

    // 批注字体大小、颜色
    var commentFontsArray: String
        get() = configData.comment_fonts_array
        set(value) { configData = configData.copy(comment_fonts_array = value)}

    var commentFont1Size: Float
        get() = configData.comment_font1_size
        set(value) { configData = configData.copy(comment_font1_size = value) }

    var commentFont2Size: Float
        get() = configData.comment_font2_size
        set(value) { configData = configData.copy(comment_font2_size = value) }

    var commentFont3Size: Float
        get() = configData.comment_font3_size
        set(value) { configData = configData.copy(comment_font3_size = value) }

    var commentFont4Size: Float
        get() = configData.comment_font4_size
        set(value) { configData = configData.copy(comment_font4_size = value) }

    var commentFont5Size: Float
        get() = configData.comment_font5_size
        set(value) { configData = configData.copy(comment_font5_size = value) }

    var commentFontColor: Color
        get() = configData.comment_font_color.toColor()
        set(value) { configData = configData.copy(comment_font_color = value.toConfigString()) }

    var commentGridType: Int
        get() = configData.comment_grid_type
        set(value) { configData = configData.copy(comment_grid_type = if (value == 4) value else 2) }

    var commentFontZoom: Float
        get() = configData.comment_font_zoom
        set(value) { configData = configData.copy(comment_font_zoom = value) }

    // 封面标题字体大小、颜色、高度
    var coverTitleFontSize: Float
        get() = configData.cover_title_font_size
        set(value) { configData = configData.copy(cover_title_font_size = value) }

    var coverTitleY: Float
        get() = configData.cover_title_y
        set(value) { configData = configData.copy(cover_title_y = value) }

    var coverAuthorFontSize: Float
        get() = configData.cover_author_font_size
        set(value) { configData = configData.copy(cover_author_font_size = value) }

    var coverAuthorY: Float
        get() = configData.cover_author_y
        set(value) { configData = configData.copy(cover_author_y = value)}

    var coverFontColor: Color
        get() = configData.cover_font_color.toColor()
        set(value) { configData = configData.copy(cover_font_color = value.toConfigString()) }

    // 版心标题字体大小、颜色、高度、字间距比例
    var ifTpcenter: Boolean     // 版心标题页码是否居中，true时居中，false是居左侧
        get() = configData.if_tpcenter == 1
        set(value) { configData = configData.copy(if_tpcenter = if (value) 1 else 0)}

    var titleFontSize: Float
        get() = configData.title_font_size
        set(value) { configData = configData.copy(title_font_size = value) }

    var titleFontColor: Color
        get() = configData.title_font_color.toColor()
        set(value) { configData = configData.copy(title_font_color = value.toConfigString()) }

    var titleY: Float
        get() = configData.title_y
        set(value) { configData = configData.copy(title_y = value) }

    var titleYdis: Float
        get() = configData.title_ydis
        set(value) { configData = configData.copy(title_ydis = value) }

    var titlePostfix: String?
        get() = configData.title_postfix
        set(value) { configData = configData.copy(title_postfix = value) }

    var titleDirectory: Boolean      //根据标题自动添加PDF目录
        get() = configData.title_directory == 1
        set(value) { configData = configData.copy(title_directory = if (value) 1 else 0)}

    // 版心页码字体大小、颜色、高度
    var pagerFontSize: Float
        get() = configData.pager_font_size
        set(value) { configData = configData.copy(pager_font_size = value) }

    var pagerFontColor: Color
        get() = configData.pager_font_color.toColor()
        set(value) { configData = configData.copy(value.toConfigString()) }

    var pagerY: Float
        get() = configData.pager_y
        set(value) { configData = configData.copy(pager_y = value) }

    // 标点符号处理规则，顺序：替换->删除->模式（有标点，无标点，归一化）
    val expReplaceComma: String
        get() = configData.exp_replace_comma
    val expReplaceNumber: String
        get() = configData.exp_replace_number
    val expDeleteComma: String
        get() = configData.exp_delete_comma

    var ifNocomma: Boolean
        get() = configData.if_nocomma == 1
        set(value) { configData = configData.copy(if_nocomma = if (value) 1 else 0)}

    val commaColor: Color
        get() = configData.comma_color?.toColor() ?: Color(0xFFF62727)

    val expNocomma: String
        get() = configData.exp_nocomma

    var ifOnlyperiod: Boolean
        get() = configData.if_onlyperiod == 1
        set(value) { configData = configData.copy(if_onlyperiod = if (value) 1 else 0)}

    val expOnlyperiod: String
        get() = configData.exp_onlyperiod

    // 正文标点符号
    val textCommaNop: String
        get() = configData.text_comma_nop
    val textCommaNopSize: Float
        get() = configData.text_comma_nop_size
    val textCommaNopX: Float
        get() = configData.text_comma_nop_x
    val textCommaNopY: Float
        get() = configData.text_comma_nop_y
    val textComma90: String
        get() = configData.text_comma_90
    val textComma90Size: Float
        get() = configData.text_comma_90_size
    val textComma90X: Float
        get() = configData.text_comma_90_x
    val textComma90Y: Float
        get() = configData.text_comma_90_y

    // 批注标点符号
    val commentCommaNop: String
        get() = configData.comment_comma_nop
    val commentCommaNopSize: Float
        get() = configData.comment_comma_nop_size
    val commentCommaNopX: Float
        get() = configData.comment_comma_nop_x
    val commentCommaNopY: Float
        get() = configData.comment_comma_nop_y
    val commentComma90: String
        get() = configData.comment_comma_90
    val commentComma90Size: Float
        get() = configData.comment_comma_90_size
    val commentComma90X: Float
        get() = configData.comment_comma_90_x
    val commentComma90Y: Float
        get() = configData.comment_comma_90_y

    val ifBookVline: Boolean
        get() = configData.if_book_vline == 1
    val bookLineWidth: Float
        get() = configData.book_line_width
    val bookLineColor: Color
        get() = configData.book_line_color.toColor()

    var ifTagBookline: Boolean
        get() = configData.if_tag_bookline == 1
        set(value) { configData = configData.copy(if_tag_bookline = if (value) 1 else 0) }

    var ifTagCirclenote: Boolean
        get() = configData.if_tag_circlenote == 1
        set(value) { configData = configData.copy(if_tag_circlenote = if (value) 1 else 0) }
    val textNoteOx: Float
        get() = configData.text_note_ox
    val textNoteOy: Float
        get() = configData.text_note_oy
    val textNoteOr: Float
        get() = configData.text_note_or
    val textNoteOw: Float
        get() = configData.text_note_ow
    val textNoteOc: Color
        get() = configData.text_note_oc.toColor()

    var ifTagPointnote: Boolean
        get() = configData.if_tag_pointnote == 1
        set(value) { configData = configData.copy(if_tag_pointnote = if (value) 1 else 0) }
    val textNotePx: Float
        get() = configData.text_note_px
    val textNotePy: Float
        get() = configData.text_note_py
    val textNotePs: Float
        get() = configData.text_note_ps
    val textNotePc: Color
        get() = configData.text_note_pc.toColor()

    var ifTagLinenote: Boolean
        get() = configData.if_tag_linenote == 1
        set(value) { configData = configData.copy(if_tag_linenote = if (value) 1 else 0) }
    val textNoteLx: Float
        get() = configData.text_note_lx
    val textNoteLy: Float
        get() = configData.text_note_ly
    val textNoteLw: Float
        get() = configData.text_note_lw
    val textNoteLc: Color
        get() = configData.text_note_lc.toColor()
    // 字符底框参数
    var ifTagRectframe: Boolean
        get() = configData.if_tag_rectframe == 1
        set(value) { configData = configData.copy(if_tag_rectframe = if (value) 1 else 0) }
    // 圆角方框
    val rectType: Int
        get() = configData.rect_type
    val rectBcolor: Color
        get() = configData.rect_bcolor.toColor()
    val rectFcolor: Color
        get() = configData.rect_fcolor.toColor()
    val textRectY: Float
        get() = configData.text_rect_y
    val textRectH: Float
        get() = configData.text_rect_h
    val textRectR: Float
        get() = configData.text_rect_r
    val textRectF: Float
        get() = configData.text_rect_f
    val commRectY: Float
        get() = configData.comm_rect_y
    val commRectH: Float
        get() = configData.comm_rect_h
    val commRectR: Float
        get() = configData.comm_rect_r
    val commRectF: Float
        get() = configData.comm_rect_f
    var ifTagCircleframe: Boolean
        get() = configData.if_tag_circleframe == 1
        set(value) { configData = configData.copy(if_tag_circleframe = if (value) 1 else 0) }
    // 圆形框
    val circleType: Int
        get() = configData.circle_type
    val circleBcolor: Color
        get() = configData.circle_bcolor.toColor()
    val circleFcolor: Color
        get() = configData.circle_fcolor.toColor()
    val textCircleY: Float
        get() = configData.text_circle_y
    val textCircleR: Float
        get() = configData.text_circle_r
    val textCircleF: Float
        get() = configData.text_circle_f
    val commCircleY: Float
        get() = configData.comm_circle_y
    val commCircleR: Float
        get() = configData.comm_circle_r
    val commCircleF: Float
        get() = configData.comm_circle_f

    // 正文字符缩放
    var ifTagTextzoom: Boolean
        get() = configData.if_tag_textzoom == 1
        set(value) { configData = configData.copy(if_tag_textzoom = if (value) 1 else 0) }
    val textZoom: Float
        get() = configData.text_zoom
    // 字体度量微调
    var ifFontMetricAdjust: Boolean
        get() = configData.if_font_metric_adjust == 1
        set(value) { configData = configData.copy(if_font_metric_adjust = if (value) 1 else 0) }
    // 回退字体模拟加粗
    var ifFallbackBold: Boolean
        get() = configData.if_fallback_bold == 1
        set(value) { configData = configData.copy(if_fallback_bold = if (value) 1 else 0) }
    var fallbackBoldStrokeWidth: Float
        get() = configData.fallback_bold_stroke_width
        set(value) { configData = configData.copy(fallback_bold_stroke_width = value)}

    fun getFontList(fonts_array: String): List<String> {
        val fontMap = mapOf('1' to font1, '2' to font2, '3' to font3, '4' to font4, '5' to font5)
        return fonts_array.mapNotNull { char ->
            fontMap[char]?.replace("-", "_")
        }
    }

    fun getFonts(): List<Triple<Float?, Float?, Double?>> {
        val tfsMap = listOf(textFont1Size, textFont2Size, textFont3Size, textFont4Size, textFont5Size)
        val cfsMap = listOf(commentFont1Size, commentFont2Size, commentFont3Size, commentFont4Size, commentFont5Size)
        val rotateMap = listOf(font1Rotate, font2Rotate, font3Rotate, font4Rotate, font5Rotate)
        return listOf(font1, font2, font3, font4, font5).mapIndexedNotNull { index, _ ->
            Triple(tfsMap[index], cfsMap[index], rotateMap[index])
        }
    }

    companion object {
        // 全局标记符号，修改无效
        val tagComment: String = "【】"         // 标识批注文字
        val tagNewpage: Char = '%'             // 分页符号
        val tagHalfpage: Char = '$'            // 半页分页符号
        val tagLastcol: Char = '&'             // 跳至本页最后一列，用于卷回文本末行文字
        val tagNewraw: Char = '^'              // 多栏模式下跳至下一栏
        val tagBookilne: String = "《》"        // 书名号转换为字符侧边线
        val tagSpace: Char = '@'               // 代表空格
        val tagRect: String = "〔〕"            // 为字符添加圆角方框
        val tagCircle: String = "〈〉"          // 为字符添加圆框
        val tagZoomtext: String = "（）"        // 括号内字体大小可调整
        val tagTextFocusO: String = "{}"       // 文字右侧添加圆形重点符
        val tagTextFocusP: String = "＜＞"      // 文字右侧添加顿点重点符
        val tagTextFoucsL: String = "［］"      // 文字右侧添加实线重点符
    }
}