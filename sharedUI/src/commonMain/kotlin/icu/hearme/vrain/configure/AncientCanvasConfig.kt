package icu.hearme.vrain.configure

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import icu.hearme.vrain.utils.ColorConvert.toColor
import icu.hearme.vrain.utils.ColorConvert.toConfigString
import kotlinx.serialization.Serializable

enum class AncientBookSplitType {
    SPLIT_BY_PAGE,
    FULL_PAGE,
    AUTO
}

class PageSplitConfig(
    pageNumberInitial: Int = 1,
    splitTypeInitial: AncientBookSplitType = AncientBookSplitType.FULL_PAGE,
){
    var pageNumber: MutableState<Int> = mutableStateOf(pageNumberInitial)
    var splitType: MutableState<AncientBookSplitType> = mutableStateOf(splitTypeInitial)
}

@Serializable
data class CanvasConfigData(
    // 宣纸背景图
    val canvas_background_image: String? = null,
    // 叶宽、高
    val canvas_width: Float = 2480f,
    val canvas_height: Float = 1860f,
    val canvas_color: String = "#FFFFFF",
    // 上、下、左、右留白
    val margins_top: Float = 150f,
    val margins_bottom: Float = 50f,
    val margins_left: Float = 50f,
    val margins_right: Float = 50f,
    // 每叶行数，叶心宽度，叶心分割线宽度
    val leaf_col: Int = 24,
    val leaf_center_width: Float = 120f,
    // 多栏模式
    val if_multirows: Int = 0,              // 是否多栏模式
    val multirows_num: Int = 1,                     // 栏数
    val multirows_linewidth: Float = 2f,            // 分栏横线线宽
    val multirows_colcolor: String = "#FFFFFF",     // 栏内细线颜色
    // 花鱼尾
    val if_fishflower: Int = 1,              // 0：三角鱼尾， 1：弧形鱼尾
    val fish_flower_image: String = "bundle://img/3leaves.png", // 鱼尾修饰图
    // 上鱼尾位置、颜色、鱼身、鱼尾高度，上鱼尾上版心分割线宽度
    val fish_top_y: Float = 450f,
    val fish_top_color: String = "#000000",
    val fish_top_rectheight: Float = 50f,
    val fish_top_triaheight: Float = 30f,
    val fish_top_linewidth: Float = 15f,
    // 下鱼尾
    val fish_btm_direction: Int = 1,                // 0向下，顺鱼尾，1向上，对鱼尾
    val fish_btm_y: Float = 1550f,
    val fish_btm_color: String = "#000000",
    val fish_btm_rectheight: Float = 50f,
    val fish_btm_triaheight: Float = 30f,           // 与fish_btm_rectangle同时设置为0时为单鱼尾，此时下鱼尾萎缩为双横线
    val fish_btm_linewidth: Float = 15f,
    // 鱼尾线条
    val fish_line_color: String = "#000000",
    val fish_line_width: Float = 0f,
    val fish_line_margin: Float = 5f,
    // 内细线宽、颜色，外粗线宽、颜色，内外线水平、垂直间距
    val inline_width: Float = 1f,
    val inline_color: String = "#000000",
    val outline_width: Float = 5f,
    val outline_color: String = "#000000",
    val outline_hmargin: Float = 5f,
    val outline_vmargin: Float = 5f,
    // 书房名、字体、字体大小、位置
    val logo_image: String? = null,
    val logo_text: String? = null,
    val logo_y: Float = 1640f,
    val logo_color: String = "#FFFFFF",
    val logo_font: String = "qiji-combo.ttf",
    val logo_font_size: Float = 40f,
    // 是否是单页模式
    val is_single_page: Boolean = false,
    val is_vintage: Boolean = false,        // 做旧
    val is_bamboo: Boolean = false          // 竹简
)


class AncientCanvasState(initialData: CanvasConfigData) {
    var configData by mutableStateOf(initialData)
        private set

    fun applyNewConfig(data: CanvasConfigData) {
        configData = data
    }

    fun toData(): CanvasConfigData {
        return configData
    }

    var canvasBackgroundImage: String?
        get() = configData.canvas_background_image
        set(value) { configData = configData.copy(canvas_background_image = value) }

    var canvasWidth: Float
        get() = configData.canvas_width
        set(value) { configData = configData.copy(canvas_width = value) }

    var canvasHeight: Float
        get() = configData.canvas_height
        set(value) { configData = configData.copy(canvas_height = value) }

    val widthDp: Dp
        get() = (configData.canvas_width / 254 * 72).dp

    val heightDp: Dp
        get() = (configData.canvas_height / 254 * 72).dp

    var marginsTop: Float
        get() = configData.margins_top
        set(value) { configData = configData.copy(margins_top = value) }

    var marginsBottom: Float
        get() = configData.margins_bottom
        set(value) { configData = configData.copy(margins_bottom = value) }

    var marginsLeft: Float
        get() = configData.margins_left
        set(value) { configData = configData.copy(margins_left = value) }

    var marginsRight: Float
        get() = configData.margins_right
        set(value) { configData = configData.copy(margins_right = value) }

    var leafCol: Int
        get() = configData.leaf_col
        set(value) { configData = configData.copy(leaf_col = value) }

    var leafCenterWidth: Float
        get() = configData.leaf_center_width
        set(value) { configData = configData.copy(leaf_center_width = value) }

    var ifMultirows: Boolean
        get() = configData.if_multirows == 1
        set(value) { configData = configData.copy(if_multirows = if (value) 1 else 0) }

    var multirowsNum: Int
        get() = configData.multirows_num
        set(value) { configData = configData.copy(multirows_num = value) }

    var multirowsLinewidth: Float
        get() = configData.multirows_linewidth
        set(value) { configData = configData.copy(multirows_linewidth = value) }

    var ifFishflower: Boolean
        get() = configData.if_fishflower == 1
        set(value) { configData = configData.copy(if_fishflower = if (value) 1 else 0) }

    var fishFlowerImage: String
        get() = configData.fish_flower_image
        set(value) { configData = configData.copy(fish_flower_image = value) }

    var fishTopY: Float
        get() = configData.fish_top_y
        set(value) { configData = configData.copy(fish_top_y = value) }

    var fishTopRectHeight: Float
        get() = configData.fish_top_rectheight
        set(value) { configData = configData.copy(fish_top_rectheight = value) }

    var fishTopTriaHeight: Float
        get() = configData.fish_top_triaheight
        set(value) { configData = configData.copy(fish_top_triaheight = value) }

    var fishTopLinewidth: Float
        get() = configData.fish_top_linewidth
        set(value) { configData = configData.copy(fish_top_linewidth = value) }

    var fishBtmDirection: Int
        get() = configData.fish_btm_direction
        set(value) { configData = configData.copy(fish_btm_direction = value) }

    var fishBtmY: Float
        get() = configData.fish_btm_y
        set(value) { configData = configData.copy(fish_btm_y = value) }

    var fishBtmRectHeight: Float
        get() = configData.fish_btm_rectheight
        set(value) { configData = configData.copy(fish_btm_rectheight = value) }

    var fishBtmTriaHeight: Float
        get() = configData.fish_btm_triaheight
        set(value) { configData = configData.copy(fish_btm_triaheight = value) }

    var fishBtmLinewidth: Float
        get() = configData.fish_btm_linewidth
        set(value) { configData = configData.copy(fish_btm_linewidth = value) }

    var fishLineWidth: Float
        get() = configData.fish_line_width
        set(value) { configData = configData.copy(fish_line_width = value) }

    var fishLineMargin: Float
        get() = configData.fish_line_margin
        set(value) { configData = configData.copy(fish_line_margin = value) }

    var inlineWidth: Float
        get() = configData.inline_width
        set(value) { configData = configData.copy(inline_width = value) }

    var outlineWidth: Float
        get() = configData.outline_width
        set(value) { configData = configData.copy(outline_width = value) }

    var outlineHMargin: Float
        get() = configData.outline_hmargin
        set(value) { configData = configData.copy(outline_hmargin = value) }

    var outlineVMargin: Float
        get() = configData.outline_vmargin
        set(value) { configData = configData.copy(outline_vmargin = value) }

    var logoImage: String?
        get() = configData.logo_image
        set(value) { configData = configData.copy(logo_image = value) }

    var logoText: String?
        get() = configData.logo_text
        set(value) { configData = configData.copy(logo_text = value) }

    var logoY: Float
        get() = configData.logo_y
        set(value) { configData = configData.copy(logo_y = value) }

    var logoFont: String
        get() = configData.logo_font
        set(value) { configData = configData.copy(logo_font = value) }

    var logoFontSize: Float
        get() = configData.logo_font_size
        set(value) { configData = configData.copy(logo_font_size = value) }

    var canvasColor: Color
        get() = configData.canvas_color.toColor()
        set(value) { configData = configData.copy(canvas_color = value.toConfigString()) }

    var multirowsColcolor: Color
        get() = configData.multirows_colcolor.toColor()
        set(value) { configData = configData.copy(multirows_colcolor = value.toConfigString()) }

    var fishTopColor: Color
        get() = configData.fish_top_color.toColor()
        set(value) { configData = configData.copy(fish_top_color = value.toConfigString()) }

    var fishBtmColor: Color
        get() = configData.fish_btm_color.toColor()
        set(value) { configData = configData.copy(fish_btm_color = value.toConfigString()) }

    var fishLineColor: Color
        get() = configData.fish_line_color.toColor()
        set(value) { configData = configData.copy(fish_line_color = value.toConfigString()) }

    var inlineColor: Color
        get() = configData.inline_color.toColor()
        set(value) { configData = configData.copy(inline_color = value.toConfigString()) }

    var outlineColor: Color
        get() = configData.outline_color.toColor()
        set(value) { configData = configData.copy(outline_color = value.toConfigString()) }

    var logoColor: Color
        get() = configData.logo_color.toColor()
        set(value) { configData = configData.copy(logo_color = value.toConfigString()) }

    var isVintage: Boolean      // 做旧
        get() = configData.is_vintage
        set(value) { configData = configData.copy(is_vintage = value)}

    var bamboo: Boolean         // 竹简
        get() = configData.is_bamboo
        set(value) {configData = configData.copy(is_bamboo = value)}

    var isFullpage: Boolean
        get() = configData.is_single_page
        set(value) { configData = configData.copy(is_single_page = value) }
}