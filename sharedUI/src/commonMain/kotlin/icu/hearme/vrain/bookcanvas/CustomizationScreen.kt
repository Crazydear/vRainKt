package icu.hearme.vrain.bookcanvas

import androidx.compose.animation.AnimatedVisibility
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
            SliderControl("标题字号", state.coverTitleFontSize, 5f..150f) { state.coverTitleFontSize = it.roundTo() }
            SliderControl("标题位置", state.coverTitleY, 0f..3000f) { state.coverTitleY = it.roundTo() }
            SliderControl("作者字号", state.coverAuthorFontSize, 5f..120f) { state.coverAuthorFontSize = it.roundTo() }
            SliderControl("作者位置", state.coverAuthorY, 0f..3000f) { state.coverAuthorY = it.roundTo() }
            ColorPickerControl("字体颜色", state.coverFontColor) { state.coverFontColor = it }
        }
    }
    item {
        ControlSection(title = "3. 版心") {
            SliderControl("标题字号", state.titleFontSize, 5f..80f) { state.titleFontSize = it.roundTo() }
            SliderControl("标题位置", state.titleY, 0f..3000f) { state.titleY = it.roundTo() }
            ColorPickerControl("标题颜色", state.titleFontColor) { state.titleFontColor = it }
            SliderControl("标题行距", state.titleYdis, 1f..5f) { state.titleYdis = it.roundTo(2) }
            StringInputControl("标题后缀", state.titlePostfix ?: ""){ state.titlePostfix = it.ifEmpty { null } }
            SwitchControl("生成目录", state.titleDirectory) { state.titleDirectory = it }
            SliderControl("页码字号", state.pagerFontSize, 5f..80f) { state.pagerFontSize = it.roundTo() }
            SliderControl("页码位置", state.pagerY, 0f..3000f) { state.pagerY = it.roundTo() }
            ColorPickerControl("页码颜色", state.pagerFontColor) { state.pagerFontColor = it }
        }
    }
    item {
        ControlSection(title = "4. 标点符号处理") {
            StringInputControl("标点替换规则", state.expReplaceComma) { state.expReplaceComma = it }
            StringInputControl("数字替换规则", state.expReplaceNumber) { state.expReplaceNumber = it }
            StringInputControl("标点删除规则", state.expDeleteComma) { state.expDeleteComma = it }
            SwitchControl("无标点模式", state.ifNocomma, { state.ifNocomma = it }) {
                StringInputControl("", state.expNocomma) { state.expNocomma = it.toList().joinToString("|") }
            }
            AnimatedVisibility(!state.ifNocomma) {
                SwitchControl("标点归一化", state.ifOnlyperiod, { state.ifOnlyperiod = it }) {
                    StringInputControl("", state.expOnlyperiod) { state.expOnlyperiod = it.toList().joinToString("|") }
                }
            }
            AnimatedVisibility(!state.ifNocomma) {
                ControlSection("正文标点符号", initiallyExpanded = false) {
                    StringInputControl("不占独立位置", state.textCommaNop) {
                        state.textCommaNop = it.toList().joinToString("|")
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically){
                        SliderControl("缩放比例", state.textCommaNopSize, 0.3f..1.5f, modifier = Modifier.weight(1f)){
                            state.textCommaNopSize = it.roundTo(2)
                        }
                        SliderControl("横向偏移", state.textCommaNopX, 0f..1f, modifier = Modifier.weight(1f)){
                            state.textCommaNopX = it.roundTo(2)
                        }
                        SliderControl("纵向偏移", state.textCommaNopY, -0.5f..1.5f, modifier = Modifier.weight(1f)){
                            state.textCommaNopY = it.roundTo(2)
                        }
                    }
                    StringInputControl("旋转标点", state.textComma90) { state.textComma90 = it }
                    Row(horizontalArrangement = Arrangement.spacedBy(5.dp), verticalAlignment = Alignment.CenterVertically) {
                        SliderControl("缩放比例", state.textComma90Size, 0.3f..1.5f, modifier = Modifier.weight(1f)){
                            state.textComma90Size = it.roundTo(2)
                        }
                        SliderControl("横向偏移", state.textComma90X, 0f..1f, modifier = Modifier.weight(1f)){
                            state.textComma90X = it.roundTo(2)
                        }
                        SliderControl("纵向偏移", state.textComma90Y, -0.5f..1.5f, modifier = Modifier.weight(1f)){
                            state.textComma90Y = it.roundTo(2)
                        }
                    }
                }
            }
            AnimatedVisibility(!state.ifNocomma) {
                ControlSection("批注标点符号", initiallyExpanded = false) {
                    StringInputControl("不占独立位置", state.commentCommaNop) {
                        state.commentCommaNop = it.toList().joinToString("|")
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(5.dp), verticalAlignment = Alignment.CenterVertically) {
                        SliderControl("缩放比例", state.commentCommaNopSize, 0.3f..1.5f, modifier = Modifier.weight(1f)) {
                            state.commentCommaNopSize = it.roundTo(2)
                        }
                        SliderControl("横向偏移", state.commentCommaNopX, 0f..1f, modifier = Modifier.weight(1f)) {
                            state.commentCommaNopX = it.roundTo(2)
                        }
                        SliderControl("纵向偏移", state.commentCommaNopY, -0f..1.5f, modifier = Modifier.weight(1f)) {
                            state.commentCommaNopY = it.roundTo(2)
                        }
                    }
                    StringInputControl("旋转标点", state.commentComma90){ state.commentComma90 = it }
                    Row(horizontalArrangement = Arrangement.spacedBy(5.dp), verticalAlignment = Alignment.CenterVertically) {
                        SliderControl("缩放比例", state.commentComma90Size, 0.3f..1.5f, modifier = Modifier.weight(1f)) {
                            state.commentComma90Size = it.roundTo(2)
                        }
                        SliderControl("横向偏移", state.commentComma90X, 0f..1f, modifier = Modifier.weight(1f)) {
                            state.commentComma90X = it.roundTo(2)
                        }
                        SliderControl("纵向偏移", state.commentComma90Y, -0.5f..1.5f, modifier = Modifier.weight(1f)) {
                            state.commentComma90Y = it.roundTo(2)
                        }
                    }
                }
            }
            ColorPickerControl("颜色", state.commaColor) { state.commaColor = it }
        }
    }
    item {
        ControlSection(title = "5. 特殊标记") {
            SwitchControl("书名号转换", state.ifTagBookline, { state.ifTagBookline = it }) {
                SliderControl("线宽", state.bookLineWidth, 1f..10f) { state.bookLineWidth = it.roundTo(1) }
            }
            AnimatedVisibility(state.ifTagBookline) {
                ColorPickerControl("线色", state.bookLineColor) { state.bookLineColor = it }
            }
            HorizontalDivider()

            SwitchControl("正文圈注", state.ifTagCirclenote,  { state.ifTagCirclenote = it }){
                SliderControl("线宽", state.textNoteOw, 1f..10f) { state.textNoteOw = it.roundTo(1) }
            }
            AnimatedVisibility(state.ifTagCirclenote) {
                Column {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                        SliderControl("圈注大小", state.textNoteOr, 0.1f..1f, modifier = Modifier.weight(1f)) {
                            state.textNoteOr = it.roundTo(2)
                        }
                        SliderControl("横向偏移", state.textNoteOx, -1f..1f, modifier = Modifier.weight(1f)) {
                            state.textNoteOx = it.roundTo(2)
                        }
                        SliderControl("纵向偏移", state.textNoteOy, -0.5f..1.5f, modifier = Modifier.weight(1f)){
                            state.textNoteOy = it.roundTo(2)
                        }
                    }
                    ColorPickerControl("颜色", state.textNoteOc){ state.textNoteOc = it }
                }
            }
            HorizontalDivider()

            SwitchControl("正文点注", state.ifTagPointnote, { state.ifTagPointnote = it }){
                SliderControl("缩放比例", state.textNotePs, 0.3f..1.5f) { state.textNotePs = it.roundTo(2) }
            }
            AnimatedVisibility(state.ifTagPointnote) {
                Column {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                        SliderControl("横向偏移", state.textNotePx, -1f..1f, modifier = Modifier.weight(1f)) {
                            state.textNotePx = it.roundTo(2)
                        }
                        SliderControl("纵向偏移", state.textNotePy, -0.5f..1.5f, modifier = Modifier.weight(1f)){
                            state.textNotePy = it.roundTo(2)
                        }
                    }
                    ColorPickerControl("颜色", state.textNotePc){ state.textNotePc = it }
                }
            }
            HorizontalDivider()

            SwitchControl("正文线注", state.ifTagLinenote, { state.ifTagLinenote = it }) {
                SliderControl("线宽", state.textNoteLw, 0.3f..10f) { state.textNoteLw = it.roundTo(2) }
            }
            AnimatedVisibility(state.ifTagLinenote) {
                Column {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                        SliderControl("横向偏移", state.textNoteLx, -1f..1f, modifier = Modifier.weight(1f)) {
                            state.textNoteLx = it.roundTo(2)
                        }
                        SliderControl("纵向偏移", state.textNoteLy, -0.5f..1.5f, modifier = Modifier.weight(1f)) {
                            state.textNoteLy = it.roundTo(2)
                        }
                    }
                    ColorPickerControl("颜色", state.textNoteLc) { state.textNoteLc = it }
                }
            }
        }
    }
    item {
        ControlSection("6. 字符底框"){
            SwitchControl("圆角方框", state.ifTagRectframe, { state.ifTagRectframe = it }) {
                SwitchControl("外边框", state.rectType == 0) { state.rectType = if (it) 0 else 1 }
            }
            AnimatedVisibility(state.ifTagRectframe) {
                Column {
                    ColorPickerControl("字体色", state.rectFcolor) { state.rectFcolor = it }
                    ColorPickerControl("背景色", state.rectBcolor) { state.rectBcolor = it }
                    ControlSection("正文") {
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                            SliderControl("字体缩放", state.textRectF, 0.3f..1f, modifier = Modifier.weight(1f)) {
                                state.textRectF = it.roundTo(2)
                            }
                            SliderControl("圆角半径", state.textRectR, 0f..15f, modifier = Modifier.weight(1f)) {
                                state.textRectR = it.roundTo(2)
                            }
                            SliderControl("纵向偏移", state.textRectY, -1f..1f, modifier = Modifier.weight(1f)) {
                                state.textRectY = it.roundTo(2)
                            }
                            SliderControl("TRH", state.textRectH, -1f..1f, modifier = Modifier.weight(1f)) {
                                state.textRectH = it.roundTo(2)
                            }
                        }
                    }
                    ControlSection("批注") {
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                            SliderControl("字体缩放", state.commRectF, 0.3f..1f, modifier = Modifier.weight(1f)) {
                                state.commRectF = it.roundTo(2)
                            }
                            SliderControl("圆角半径", state.commRectR, 0f..15f, modifier = Modifier.weight(1f)) {
                                state.commRectR = it.roundTo(2)
                            }
                            SliderControl("纵向偏移", state.commRectY, -1f..1f, modifier = Modifier.weight(1f)) {
                                state.commRectY = it.roundTo(2)
                            }
                            SliderControl("CRH", state.commRectH, -1f..1f, modifier = Modifier.weight(1f)) {
                                state.commRectH = it.roundTo(2)
                            }
                        }
                    }
                }
            }
            HorizontalDivider()
            SwitchControl("圆形边框", state.ifTagCircleframe, { state.ifTagCircleframe = it }) {
                SwitchControl("外边框", state.circleType == 0) { state.circleType = if (it) 0 else 1 }
            }
            AnimatedVisibility(state.ifTagCircleframe) {
                Column {
                    ColorPickerControl("字体色", state.circleFcolor) { state.circleFcolor = it }
                    ColorPickerControl("背景色", state.circleBcolor) { state.circleBcolor = it }
                    ControlSection("正文") {
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                            SliderControl("字体缩放", state.textCircleF, 0.3f..1f, modifier = Modifier.weight(1f)) {
                                state.textCircleF = it.roundTo(2)
                            }
                            SliderControl("半径比例", state.textCircleR, 0.5f..1.5f, modifier = Modifier.weight(1f)) {
                                state.textCircleR = it.roundTo(2)
                            }
                            SliderControl("纵向偏移", state.textCircleY, -1f..1f, modifier = Modifier.weight(1f)) {
                                state.textCircleY = it.roundTo(2)
                            }
                        }
                    }
                    ControlSection("批注") {
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                            SliderControl("字体缩放", state.commCircleF, 0.3f..1f, modifier = Modifier.weight(1f)) {
                                state.commCircleF = it.roundTo(2)
                            }
                            SliderControl("半径比例", state.commCircleR, 0.5f..1.5f, modifier = Modifier.weight(1f)) {
                                state.commCircleR = it.roundTo(2)
                            }
                            SliderControl("纵向偏移", state.commCircleY, -1f..1f, modifier = Modifier.weight(1f)) {
                                state.commCircleY = it.roundTo(2)
                            }
                        }
                    }
                }
            }
        }
    }
}

fun androidx.compose.foundation.lazy.LazyListScope.fontSettingsItems(state: AncientBookState) {
    item {
        ControlSection(title = "1. 全局字体设置") {
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                FontSelectControl("字体1", state.font1, modifier = Modifier.weight(1f)){ state.font1 = it }
                SliderControl("旋转角度", state.font1Rotate.toFloat(), -15f..15f, modifier = Modifier.weight(1f)) {
                    state.font1Rotate = it.roundTo(2).toDouble()
                    state.font2Rotate = it.roundTo(2).toDouble()
                    state.font3Rotate = it.roundTo(2).toDouble()
                    state.font4Rotate = it.roundTo(2).toDouble()
                    state.font5Rotate = it.roundTo(2).toDouble()
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                FontSelectControl("字体2", state.font2, modifier = Modifier.weight(1f)){ state.font2 = it }
                SliderControl("旋转角度", state.font2Rotate.toFloat(), -15f..15f, modifier = Modifier.weight(1f)) {
                    state.font2Rotate = it.roundTo(2).toDouble()
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                FontSelectControl("字体3", state.font3, modifier = Modifier.weight(1f)){ state.font3 = it }
                SliderControl("旋转角度", state.font3Rotate.toFloat(), -15f..15f, modifier = Modifier.weight(1f)) {
                    state.font3Rotate = it.roundTo(2).toDouble()
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                FontSelectControl("字体4", state.font4, modifier = Modifier.weight(1f)){ state.font4 = it }
                SliderControl("旋转角度", state.font4Rotate.toFloat(), -15f..15f, modifier = Modifier.weight(1f)) {
                    state.font4Rotate = it.roundTo(2).toDouble()
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                FontSelectControl("字体5", state.font5, modifier = Modifier.weight(1f)){ state.font5 = it }
                SliderControl("旋转角度", state.font5Rotate.toFloat(), -15f..15f, modifier = Modifier.weight(1f)) {
                    state.font5Rotate = it.roundTo(2).toDouble()
                }
            }
            SwitchControl("字体微调", state.ifFontMetricAdjust) { state.ifFontMetricAdjust = it }

            SwitchControl("模拟加粗", state.ifFallbackBold, { state.ifFallbackBold = it }) {
                SliderControl("", state.fallbackBoldStrokeWidth, 0.5f..2f) {
                    state.fallbackBoldStrokeWidth = it.roundTo(2)
                }
            }
        }
    }
    item {
        ControlSection(title = "2. 正文字体设置") {
            StringInputControl("优先级序列", state.textFontsArray) { state.textFontsArray = it }
            SliderControl("字号1", state.textFont1Size, 5f..80f) {
                state.textFont1Size = it.toIntFloat()
                state.textFont2Size = it.toIntFloat()
                state.textFont3Size = it.toIntFloat()
                state.textFont4Size = it.toIntFloat()
                state.textFont5Size = it.toIntFloat()
            }
            FlowRow(horizontalArrangement = Arrangement.spacedBy(20.dp), maxItemsInEachRow = 2){
                SliderControl("字号2", state.textFont2Size, 5f..80f, modifier = Modifier.weight(1f)) { state.textFont2Size = it.toIntFloat() }
                SliderControl("字号3", state.textFont3Size, 5f..80f, modifier = Modifier.weight(1f)) { state.textFont3Size = it.toIntFloat() }
                SliderControl("字号4", state.textFont4Size, 5f..80f, modifier = Modifier.weight(1f)) { state.textFont4Size = it.toIntFloat() }
                SliderControl("字号5", state.textFont5Size, 5f..80f, modifier = Modifier.weight(1f)) { state.textFont5Size = it.toIntFloat() }
            }
            ColorPickerControl("字体颜色", state.textFontColor) { state.textFontColor = it }
            SwitchControl("字体缩放", state.ifTagTextzoom, { state.ifTagTextzoom = it }) {
                SliderControl("", state.textZoom, 0.8f..2f) { state.textZoom = it.roundTo(2) }
            }
        }
    }
    item {
        ControlSection(title = "3. 批注字体设置") {
            StringInputControl("优先级序列", state.commentFontsArray) { state.commentFontsArray = it }
            SliderControl("字号1", state.commentFont1Size, 5f..80f) {
                state.commentFont1Size = it.toIntFloat()
                state.commentFont2Size = it.toIntFloat()
                state.commentFont3Size = it.toIntFloat()
                state.commentFont4Size = it.toIntFloat()
                state.commentFont5Size = it.toIntFloat()
            }
            FlowRow(horizontalArrangement = Arrangement.spacedBy(20.dp), maxItemsInEachRow = 2) {
                SliderControl("字号2", state.commentFont2Size, 5f..80f, modifier = Modifier.weight(1f)) { state.commentFont2Size = it.toIntFloat() }
                SliderControl("字号3", state.commentFont3Size, 5f..80f, modifier = Modifier.weight(1f)) { state.commentFont3Size = it.toIntFloat() }
                SliderControl("字号4", state.commentFont4Size, 5f..80f, modifier = Modifier.weight(1f)) { state.commentFont4Size = it.toIntFloat() }
                SliderControl("字号5", state.commentFont5Size, 5f..80f, modifier = Modifier.weight(1f)) { state.commentFont5Size = it.toIntFloat() }
            }
            ColorPickerControl("字体颜色", state.commentFontColor) { state.commentFontColor = it }
            SwitchControl("紧凑排版", state.commentGridType == 4, { state.commentGridType = if (it) 4 else 2 }) {
                SliderControl("", state.commentFontZoom, 0.5f..1f) { state.commentFontZoom = it.roundTo(2) }
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
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
                SwitchControl("竹简样式", state.bamboo, Modifier.weight(1f)) { state.bamboo = it }
                Spacer(modifier = Modifier.weight(0.3f))
                Row(Modifier.weight(1f)){
                    AnimatedVisibility(!state.bamboo){
                        SwitchControl("做旧", state.isVintage) { state.isVintage = it }
                    }
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
    item {
        ControlSection(title = "3. 线装边框") {
            SliderControl("内细边框线宽", state.inlineWidth, 0f..20f) { state.inlineWidth = it.toIntFloat() }
            SliderControl("外粗边框线宽", state.outlineWidth, 0f..50f) { state.outlineWidth = it.toIntFloat() }
            SliderControl("内外框水平间距", state.outlineHMargin, 0f..100f) { state.outlineHMargin = it.toIntFloat() }
            SliderControl("内外框垂直间距", state.outlineVMargin, 0f..100f) { state.outlineVMargin = it.toIntFloat() }
            ColorPickerControl("内边框颜色", state.inlineColor) { state.inlineColor = it }
            ColorPickerControl("外边框颜色", state.outlineColor) { state.outlineColor = it }
        }
    }
    item {
        ControlSection(title = "4. 版心与鱼尾") {
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
        ControlSection(title = "5. 上鱼尾") {
            SliderControl("位置", state.fishTopY, 0f..state.canvasHeight / 2) { state.fishTopY = it.toIntFloat() }
            SliderControl("鱼身高度", state.fishTopRectHeight, 0f..200f) { state.fishTopRectHeight = it.toIntFloat() }
            SliderControl("鱼尾高度", state.fishTopTriaHeight, 0f..200f) { state.fishTopTriaHeight = it.toIntFloat() }
            SliderControl("版心分割线宽度", state.fishTopLinewidth, 0f..20f) { state.fishTopLinewidth = it.toIntFloat() }
            ColorPickerControl("鱼尾颜色", state.fishTopColor) { state.fishTopColor = it }
        }
    }
    item {
        ControlSection(title = "6. 下鱼尾") {
            SwitchControl("对鱼尾", state.fishBtmDirection == 1) { state.fishBtmDirection = if (it) 1 else 0 }
            SliderControl("位置", state.fishBtmY, 0f..state.canvasHeight) { state.fishBtmY = it.toIntFloat() }
            SliderControl("鱼身高度", state.fishBtmRectHeight, 0f..200f) { state.fishBtmRectHeight = it.toIntFloat() }
            SliderControl("鱼尾高度", state.fishBtmTriaHeight, 0f..200f) { state.fishBtmTriaHeight = it.toIntFloat() }
            SliderControl("版心分割线宽度", state.fishBtmLinewidth, 0f..20f) { state.fishBtmLinewidth = it.toIntFloat() }
            ColorPickerControl("鱼尾颜色", state.fishBtmColor) { state.fishBtmColor = it }
        }
    }
    item {
        ControlSection(title = "7. 题签与印记") {
            Row(horizontalArrangement = Arrangement.spacedBy(15.dp), verticalAlignment = Alignment.CenterVertically){
                StringInputControl("书房名", state.logoText, Modifier.weight(1f)) { state.logoText = it }
                FontSelectControl("字体", state.logoFont, Modifier.weight(1f)) { state.logoFont = it }
            }
            StringInputControl("印记图片路径", state.logoImage) { state.logoImage = it }
            SliderControl("位置", state.logoY, 0f..state.canvasHeight) { state.logoY = it.toIntFloat() }
            SliderControl("字体大小", state.logoFontSize, 5f..80f) { state.logoFontSize = it.toIntFloat() }
            ColorPickerControl("题签颜色", state.logoColor) { state.logoColor = it }
        }
    }
    item {
        AnimatedVisibility(!state.bamboo) {
            ControlSection(title = "8. 多栏") {
                SwitchControl("多栏模式", state.ifMultirows) { state.ifMultirows = it }
                SliderControl("栏数", state.multirowsNum.toFloat(), 1f..5f, steps = 3) { state.multirowsNum = it.toInt() }
                SliderControl("分栏横线线宽", state.multirowsLinewidth, 0f..10f) { state.multirowsLinewidth = it.toIntFloat() }
                ColorPickerControl("栏内细线颜色", state.multirowsColcolor) { state.multirowsColcolor = it }
            }
        }
    }
}

private fun Float.toIntFloat() = this.roundToInt().toFloat()

private fun Float.roundTo(decimals: Int = 0): Float {
    var multiplier = 1f
    repeat(decimals) { multiplier *= 10f }
    return (this * multiplier).roundToInt() / multiplier
}