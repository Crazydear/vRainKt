package icu.hearme.vrain.bookcanvas

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import icu.hearme.vrain.configure.*
import icu.hearme.vrain.manager.ConfigManager
import icu.hearme.vrain.manager.ConfigMeta
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@Composable
fun CustomizationScreen(
    bookState: AncientBookState,
    canvasState: AncientCanvasState,
    onSaveBookClick: suspend (BookConfigData, String) -> Unit,
    onSaveCanvasClick: suspend (ConfigMeta) -> Unit,
    onBackClick: () -> Unit,
    content: @Composable () -> Unit
) {
    val scope = rememberCoroutineScope()
    var isSaving by remember { mutableStateOf(false) }

    var selectedTabIndex by remember { mutableStateOf(0) }
    val tabTitles = listOf("排版配置", "画布样式")

    var showBookSaveDialog by remember { mutableStateOf(false) }
    var showCanvasSaveDialog by remember { mutableStateOf(false) }
    var styleName by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            @OptIn(ExperimentalMaterial3Api::class)
            TopAppBar(
                title = {
                    Text(if (selectedTabIndex == 0) "书籍排版配置" else "自定义背景样式")
                },
                actions = {
                    Button(
                        onClick = {
                            styleName = if (selectedTabIndex == 0) bookState.title else ""
                            if (selectedTabIndex == 0) showBookSaveDialog = true else showCanvasSaveDialog = true
                        },
                        modifier = Modifier.padding(end = 8.dp),
                        enabled = !isSaving
                    ) {
                        if (isSaving) {
                            CircularProgressIndicator(Modifier.size(16.dp), MaterialTheme.colorScheme.onPrimary)
                        } else {
                            Text(if (selectedTabIndex == 0) "保存排版" else "保存样式")
                        }
                    }
                }
            )
        }
    ) { paddingValues ->
        BoxWithConstraints(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            val isWideScreen = maxWidth > 600.dp

            if (isWideScreen) {
                // 宽屏：左右分栏
                Row(modifier = Modifier.fillMaxSize()) {
                    Box(modifier = Modifier.weight(1.5f).fillMaxHeight()
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        content.invoke()
                    }

                    Box(modifier = Modifier.weight(1f).fillMaxHeight()
                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant)
                    ) {
                        ControlPanel(
                            selectedTabIndex = selectedTabIndex,
                            onTabSelected = { selectedTabIndex = it },
                            tabTitles = tabTitles,
                            bookState = bookState,
                            canvasState = canvasState
                        )
                    }
                }
            } else {
                // 窄屏：上下分栏
                Column(modifier = Modifier.fillMaxSize()) {
                    Box(modifier = Modifier.fillMaxWidth().weight(1f)
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        content.invoke()
                    }

                    Box(modifier = Modifier.fillMaxWidth().weight(1.2f)
                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
                        .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
                    ) {
                        ControlPanel(
                            selectedTabIndex = selectedTabIndex,
                            onTabSelected = { selectedTabIndex = it },
                            tabTitles = tabTitles,
                            bookState = bookState,
                            canvasState = canvasState
                        )
                    }
                }
            }
        }

        if (showBookSaveDialog) {
            AlertDialog(
                onDismissRequest = { showBookSaveDialog = false },
                title = { Text("保存当前排版配置") },
                text = {
                    OutlinedTextField(
                        value = styleName,
                        onValueChange = { styleName = it },
                        label = { Text("请输入配置名称") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            val finalName = styleName.ifBlank { "未命名排版" }
                            scope.launch {
                                isSaving = true
                                showBookSaveDialog = false
                                onSaveBookClick(bookState.toData(), finalName)
                                isSaving = false
                                onBackClick()
                            }
                        }
                    ) { Text("保存") }
                    if (isDesktopPlatform()) {
                        TextButton(
                            onClick = {
                                val finalName = styleName.ifBlank { "自定义排版" }
                                scope.launch {
                                    isSaving = true
                                    showBookSaveDialog = false
                                    val cfg = ConfigManager.convertToCfg(bookState.toData())
                                    LocalStorage.exportCfg(finalName, cfg, "cfg")
                                    isSaving = false
                                }
                            }
                        ) { Text("保存为cfg") }
                    }
                },
                dismissButton = { TextButton(onClick = { showBookSaveDialog = false }) { Text("取消") } }
            )
        }

        if (showCanvasSaveDialog) {
            AlertDialog(
                onDismissRequest = { showCanvasSaveDialog = false },
                title = { Text("保存为自定义样式") },
                text = {
                    OutlinedTextField(
                        value = styleName,
                        onValueChange = { styleName = it },
                        label = { Text("请输入样式名称") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            val finalName = styleName.ifBlank { "自定义样式" }
                            scope.launch {
                                isSaving = true
                                showCanvasSaveDialog = false
                                val newCustomMeta = ConfigManager.saveUserConfig(canvasState.toData(), finalName)
                                onSaveCanvasClick(newCustomMeta)
                                isSaving = false
                                onBackClick()
                            }
                        }
                    ) { Text("保存") }
                    if (isDesktopPlatform()) {
                        TextButton(
                            onClick = {
                                val finalName = styleName.ifBlank { "自定义样式" }
                                scope.launch {
                                    isSaving = true
                                    showCanvasSaveDialog = false
                                    val cfg = ConfigManager.convertToCfg(canvasState.toData())
                                    LocalStorage.exportCfg(finalName, cfg, "cfg")
                                    isSaving = false
                                }
                            }
                        ) { Text("保存为cfg") }
                    }
                },
                dismissButton = { TextButton(onClick = { showCanvasSaveDialog = false }) { Text("取消") } }
            )
        }
    }
}

@Composable
private fun ControlPanel(
    selectedTabIndex: Int,
    onTabSelected: (Int) -> Unit,
    tabTitles: List<String>,
    bookState: AncientBookState,
    canvasState: AncientCanvasState
) {
    Column(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surface)) {
        PrimaryTabRow(selectedTabIndex) {
            tabTitles.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTabIndex == index,
                    onClick = { onTabSelected(index) },
                    text = { Text(title) }
                )
            }
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            if (selectedTabIndex == 0) {
                bookSettingsItems(bookState)
            } else {
                canvasSettingsItems(canvasState)
            }
        }
    }
}

// 提取的排版配置列表
fun androidx.compose.foundation.lazy.LazyListScope.bookSettingsItems(state: AncientBookState) {
    item {
        ControlSection(title = "1. 基础设置") {
            StringInputControl("书名", state.title) { state.title = it }
            StringInputControl("作者", state.author) { state.author = it }
            StringInputControl("背景图ID", state.canvasId) { state.canvasId = it }
            SliderControl("每列字数", state.rowNum.toFloat(), 4f..50f) { state.rowNum = it.roundToInt() }
            SliderControl("列末字到边框距离微调", state.rowDeltaY, -50f..50f) { state.rowDeltaY = it }
            val mhlt = state.multirowsHorizontalLayout
            SwitchControl("当前排版模式:  ${if (mhlt == 1) "族谱" else "字典"}", mhlt == 1){
                state.multirowsHorizontalLayout = if (it)  1 else 2
            }
        }
    }

    item {
        ControlSection(title = "2. 封面") {
            SliderControl("封面标题字号", state.coverTitleFontSize, 5f..80f) { state.coverTitleFontSize = it.toIntFloat() }
            SliderControl("封面标题位置", state.coverTitleY, 0f..3000f) { state.coverTitleY = it.toIntFloat() }
            SliderControl("封面作者字号", state.coverAuthorFontSize, 5f..80f) { state.coverAuthorFontSize = it.toIntFloat() }
            SliderControl("封面作者位置", state.coverAuthorY, 0f..3000f) { state.coverAuthorY = it.toIntFloat() }
            ColorPickerControl("封面字体颜色", state.coverFontColor) { state.coverFontColor = it }
        }
    }
    item {
        ControlSection(title = "3. 版心") {
            SliderControl("版心标题字号", state.titleFontSize, 5f..80f) { state.titleFontSize = it.toIntFloat() }
            SliderControl("版心标题位置", state.titleY, 0f..3000f) { state.titleY = it.toIntFloat() }
            ColorPickerControl("版心标题颜色", state.titleFontColor) { state.titleFontColor = it }
            SliderControl("版心页码字号", state.pagerFontSize, 5f..80f) { state.pagerFontSize = it.toIntFloat() }
            SliderControl("版心页码位置", state.pagerY, 0f..3000f) { state.pagerY = it.toIntFloat() }
            ColorPickerControl("版心页码颜色", state.pagerFontColor) { state.pagerFontColor = it }
        }
    }
    item {
        ControlSection(title = "4. 标点符号处理") {
            SwitchControl("无标点模式", state.ifNocomma) { state.ifNocomma = it }
            SwitchControl("标点归一化", state.ifOnlyperiod) { state.ifOnlyperiod = it }
        }
    }
    item {
        ControlSection(title = "5. 特殊标记") {
            SwitchControl("书名号转波浪线", state.ifTagBookline) { state.ifTagBookline = it }
            SwitchControl("开启圆角方框", state.ifTagRectframe) { state.ifTagRectframe = it }
            SwitchControl("开启圆形边框", state.ifTagCircleframe) { state.ifTagCircleframe = it }
            SwitchControl("开启字体缩放", state.ifTagTextzoom) { state.ifTagTextzoom = it }
            SwitchControl("开启正文圈注", state.ifTagCirclenote) { state.ifTagCirclenote = it }
            SwitchControl("开启正文点注", state.ifTagPointnote) { state.ifTagPointnote = it }
            SwitchControl("开启正文线注", state.ifTagLinenote) { state.ifTagLinenote = it }
        }
    }
}

fun androidx.compose.foundation.lazy.LazyListScope.fontSettingsItems(state: AncientBookState) {
    item {
        ControlSection(title = "1. 全局字体设置") {
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically){
                FontSelectControl("字体1", state.font1, modifier = Modifier.weight(1f)){ state.font1 = it }
                SliderControl("旋转角度", state.font1Rotate.toFloat(), -15f..15f, modifier = Modifier.weight(1f)) {
                    state.font1Rotate = ((it * 100).roundToInt() / 100.0)
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically){
                FontSelectControl("字体2", state.font2, modifier = Modifier.weight(1f)){ state.font2 = it }
                SliderControl("旋转角度", state.font2Rotate.toFloat(), -15f..15f, modifier = Modifier.weight(1f)) {
                    state.font2Rotate = ((it * 100).roundToInt() / 100.0)
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically){
                FontSelectControl("字体3", state.font3, modifier = Modifier.weight(1f)){ state.font3 = it }
                SliderControl("旋转角度", state.font3Rotate.toFloat(), -15f..15f, modifier = Modifier.weight(1f)) {
                    state.font3Rotate = ((it * 100).roundToInt() / 100.0)
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically){
                FontSelectControl("字体4", state.font4, modifier = Modifier.weight(1f)){ state.font4 = it }
                SliderControl("旋转角度", state.font4Rotate.toFloat(), -15f..15f, modifier = Modifier.weight(1f)) {
                    state.font4Rotate = ((it * 100).roundToInt() / 100.0)
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically){
                FontSelectControl("字体5", state.font5, modifier = Modifier.weight(1f)){ state.font5 = it }
                SliderControl("旋转角度", state.font5Rotate.toFloat(), -15f..15f, modifier = Modifier.weight(1f)) {
                    state.font5Rotate = ((it * 100).roundToInt() / 100.0)
                }
            }
            SwitchControl("字体微调", state.ifFontMetricAdjust){ state.ifFontMetricAdjust = it }
            Row(horizontalArrangement = Arrangement.spacedBy(15.dp), verticalAlignment = Alignment.Top){
                SwitchControl("模拟加粗", state.ifFallbackBold, Modifier.weight(1f)){ state.ifFallbackBold = it }
                if (state.ifFallbackBold){
                    AnimatedVisibility(
                        visible = state.ifFallbackBold,
                        enter = expandVertically() + fadeIn(),
                        exit = shrinkVertically() + fadeOut(),
                        modifier = Modifier.weight(1.5f)
                    ) {
                        SliderControl("模拟加粗描边宽度", state.fallbackBoldStrokeWidth, 0.5f..2f) {
                            state.fallbackBoldStrokeWidth = ((it * 100).roundToInt() / 100.0).toFloat()
                        }
                    }
                } else {
                    Spacer(Modifier.weight(1.5f))
                }
            }
        }
    }

    item {
        ControlSection(title = "2. 正文字体设置") {
            StringInputControl("优先级序列", state.textFontsArray) { state.textFontsArray = it }
            SliderControl("字号1", state.textFont1Size, 5f..80f) { state.textFont1Size = it.toIntFloat() }
            SliderControl("字号2", state.textFont2Size, 5f..80f) { state.textFont2Size = it.toIntFloat() }
            SliderControl("字号3", state.textFont3Size, 5f..80f) { state.textFont3Size = it.toIntFloat() }
            SliderControl("字号4", state.textFont4Size, 5f..80f) { state.textFont4Size = it.toIntFloat() }
            SliderControl("字号5", state.textFont5Size, 5f..80f) { state.textFont5Size = it.toIntFloat() }
            ColorPickerControl("字体颜色", state.textFontColor) { state.textFontColor = it }
        }
    }

    item {
        ControlSection(title = "3. 批注字体设置") {
            StringInputControl("优先级序列", state.commentFontsArray) { state.commentFontsArray = it }
            SliderControl("字号1", state.commentFont1Size, 5f..80f) { state.commentFont1Size = it.toIntFloat() }
            SliderControl("字号2", state.commentFont2Size, 5f..80f) { state.commentFont2Size = it.toIntFloat() }
            SliderControl("字号3", state.commentFont3Size, 5f..80f) { state.commentFont3Size = it.toIntFloat() }
            SliderControl("字号4", state.commentFont4Size, 5f..80f) { state.commentFont4Size = it.toIntFloat() }
            SliderControl("字号5", state.commentFont5Size, 5f..80f) { state.commentFont5Size = it.toIntFloat() }
            ColorPickerControl("字体颜色", state.commentFontColor) { state.commentFontColor = it }
            Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                SwitchControl("紧凑排版", state.commentGridType == 4) { state.commentGridType = if (it) 4 else 2 }
                AnimatedVisibility(state.commentGridType == 4) {
                    SliderControl("字体缩放", state.commentFontZoom, 0.5f..1f, 19) { state.commentFontZoom = it }
                }
            }
        }
    }
}

// 提取的画布样式列表
fun androidx.compose.foundation.lazy.LazyListScope.canvasSettingsItems(state: AncientCanvasState) {
    item {
        ControlSection(title = "1. 画布与基础属性") {
            SliderControl("画布宽度", state.canvasWidth, 500f..3000f) { state.canvasWidth = it.toIntFloat() }
            SliderControl("画布高度", state.canvasHeight, 500f..4000f) { state.canvasHeight = it.toIntFloat() }
            SliderControl("每页列数", state.leafCol.toFloat(), 4f..36f, steps = 15) { state.leafCol = it.roundToInt() }
            ColorPickerControl("宣纸背景色", state.canvasColor) { state.canvasColor = it }
            StringInputControl("背景图片路径", state.canvasBackgroundImage) { state.canvasBackgroundImage = it }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                SwitchControl("竹简样式", state.bamboo, Modifier.weight(1f)) { state.bamboo = it }
                Spacer(modifier = Modifier.weight(0.3f))
                if (!state.bamboo) {
                    SwitchControl("做旧", state.isVintage, Modifier.weight(1f)) { state.isVintage = it }
                } else {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
    item {
        ControlSection(title = "2. 纸张留白 (Margins)") {
            SliderControl("上留白 (Top)", state.marginsTop, 0f..500f) { state.marginsTop = it.toIntFloat() }
            SliderControl("下留白 (Bottom)", state.marginsBottom, 0f..300f) { state.marginsBottom = it.toIntFloat() }
            SliderControl("左留白 (Left)", state.marginsLeft, 0f..250f) { state.marginsLeft = it.toIntFloat() }
            SliderControl("右留白 (Right)", state.marginsRight, 0f..250f) { state.marginsRight = it.toIntFloat() }
        }
    }
    if (!state.bamboo) {
        item {
            ControlSection(title = "3. 多栏") {
                SwitchControl("多栏模式", state.ifMultirows) { state.ifMultirows = it }
                SliderControl("栏数", state.multirowsNum.toFloat(), 1f..5f, steps = 3) { state.multirowsNum = it.toInt() }
                SliderControl("分栏横线线宽", state.multirowsLinewidth, 0f..10f) { state.multirowsLinewidth = it.toIntFloat() }
                ColorPickerControl("栏内细线颜色", state.multirowsColcolor) { state.multirowsColcolor = it }
            }
        }
    }
    item {
        ControlSection(title = "4. 线装边框") {
            SliderControl("内细边框线宽", state.inlineWidth, 0f..20f) { state.inlineWidth = it.toIntFloat() }
            SliderControl("外粗边框线宽", state.outlineWidth, 0f..50f) { state.outlineWidth = it.toIntFloat() }
            SliderControl("内外框水平间距", state.outlineHMargin, 0f..100f) { state.outlineHMargin = it.toIntFloat() }
            SliderControl("内外框垂直间距", state.outlineVMargin, 0f..100f) { state.outlineVMargin = it.toIntFloat() }
            ColorPickerControl("内边框颜色", state.inlineColor) { state.inlineColor = it }
            ColorPickerControl("外边框颜色", state.outlineColor) { state.outlineColor = it }
        }
    }
    item {
        ControlSection(title = "5. 版心与鱼尾") {
            SliderControl("版心宽度", state.leafCenterWidth, 0f..200f) {
                state.leafCenterWidth = it.toIntFloat()
                if (it < 1) { state.isFullpage = true }
            }
            SwitchControl("弧形鱼尾", state.ifFishflower) { state.ifFishflower = it }
            StringInputControl("鱼尾修饰图", state.fishFlowerImage) { state.fishFlowerImage = it }
            SliderControl("鱼尾线条宽度", state.fishLineWidth, 0f..20f) { state.fishLineWidth = it.toIntFloat() }
            SliderControl("鱼尾线条留白", state.fishLineMargin, 0f..200f) { state.fishLineMargin = it.toIntFloat() }
            ColorPickerControl("鱼尾线条颜色", state.fishLineColor) { state.fishLineColor = it }
        }
    }
    item {
        ControlSection(title = "6. 上鱼尾") {
            SliderControl("位置", state.fishTopY, 0f..state.canvasHeight / 2) { state.fishTopY = it.toIntFloat() }
            SliderControl("鱼身高度", state.fishTopRectHeight, 0f..200f) { state.fishTopRectHeight = it.toIntFloat() }
            SliderControl("鱼尾高度", state.fishTopTriaHeight, 0f..200f) { state.fishTopTriaHeight = it.toIntFloat() }
            SliderControl("版心分割线宽度", state.fishTopLinewidth, 0f..20f) { state.fishTopLinewidth = it.toIntFloat() }
            ColorPickerControl("鱼尾颜色", state.fishTopColor) { state.fishTopColor = it }
        }
    }
    item {
        ControlSection(title = "7. 下鱼尾") {
            SwitchControl("对鱼尾", state.fishBtmDirection == 1) { state.fishBtmDirection = if (it) 1 else 0 }
            SliderControl("位置", state.fishBtmY, 0f..state.canvasHeight) { state.fishBtmY = it.toIntFloat() }
            SliderControl("鱼身高度", state.fishBtmRectHeight, 0f..200f) { state.fishBtmRectHeight = it.toIntFloat() }
            SliderControl("鱼尾高度", state.fishBtmTriaHeight, 0f..200f) { state.fishBtmTriaHeight = it.toIntFloat() }
            SliderControl("版心分割线宽度", state.fishBtmLinewidth, 0f..20f) { state.fishBtmLinewidth = it.toIntFloat() }
            ColorPickerControl("鱼尾颜色", state.fishBtmColor) { state.fishBtmColor = it }
        }
    }
    item {
        ControlSection(title = "8. 题签与印记") {
            StringInputControl("书房名", state.logoText) { state.logoText = it }
            StringInputControl("字体", state.logoFont) { state.logoFont = it }
            StringInputControl("印记图片路径", state.logoImage) { state.logoImage = it }
            SliderControl("位置", state.logoY, 0f..state.canvasHeight) { state.logoY = it.toIntFloat() }
            SliderControl("字体大小", state.logoFontSize, 0f..200f) { state.logoFontSize = it.toIntFloat() }
            ColorPickerControl("题签颜色", state.logoColor) { state.logoColor = it }
        }
    }
}

private fun Float.toIntFloat() = this.roundToInt().toFloat()