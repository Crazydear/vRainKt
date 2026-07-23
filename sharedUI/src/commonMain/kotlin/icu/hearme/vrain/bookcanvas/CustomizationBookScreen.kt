package icu.hearme.vrain.bookcanvas

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import icu.hearme.vrain.configure.AncientBookState
import icu.hearme.vrain.configure.BookConfigData
import icu.hearme.vrain.configure.ConfigManager
import icu.hearme.vrain.configure.LocalStorage
import icu.hearme.vrain.configure.isDesktopPlatform
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@Composable
fun CustomizationBookScreen(
    state: AncientBookState,
    onSaveClick: suspend (BookConfigData, String) -> Unit,
    onBackClick: () -> Unit,
    content: @Composable () -> Unit
) {
    val scope = rememberCoroutineScope()
    var isSaving by remember { mutableStateOf(false) }
    var showSaveDialog by remember { mutableStateOf(false) }
    var styleName by remember { mutableStateOf(state.title) }

    Scaffold(
        topBar = {
            @OptIn(ExperimentalMaterial3Api::class)
            TopAppBar(
                title = { Text("古籍排版配置") },
                navigationIcon = { TextButton(onClick = onBackClick) { Text("返回") } },
                actions = {
                    Button(
                        onClick = { showSaveDialog = true },
                        modifier = Modifier.padding(end = 8.dp),
                        enabled = !isSaving
                    ) {
                        if (isSaving) {
                            CircularProgressIndicator(Modifier.size(16.dp), MaterialTheme.colorScheme.onPrimary)
                        } else {
                            Text("保存样式")
                        }
                    }
                }
            )
        }
    ) { paddingValues ->
        BoxWithConstraints(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            val isWideScreen = maxWidth > 600.dp

            if (isWideScreen) {
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
                        BookControlPanel(state)
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
                        BookControlPanel(state)
                    }
                }
            }
        }

        // 保存对话框
        if (showSaveDialog) {
            AlertDialog(
                onDismissRequest = { showSaveDialog = false },
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
                                showSaveDialog = false
                                onSaveClick(state.toData(), finalName)
                                isSaving = false
                                onBackClick()
                            }
                        }
                    ) {
                        Text("保存")
                    }
                    if (isDesktopPlatform()) {
                        TextButton(
                            onClick = {
                                val finalName = if (styleName.isNotBlank()) styleName else "自定义样式"
                                scope.launch {
                                    isSaving = true
                                    showSaveDialog = false
                                    val cfg = ConfigManager.convertToCfg(state.toData())
                                    LocalStorage.exportCfg(finalName, cfg)
                                    isSaving = false
                                }
                            }
                        ) {
                            Text("保存为cfg")
                        }
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showSaveDialog = false }) {
                        Text("取消")
                    }
                }
            )
        }
    }
}

@Composable
private fun BookControlPanel(state: AncientBookState) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surface).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        // --- 1. 基础信息与布局 ---
        item {
            ControlSection(title = "1. 基础设置") {
                StringInputControl("书名", state.title) { state.title = it }
                StringInputControl("作者", state.author) { state.author = it }
                StringInputControl("背景图ID", state.canvasId) { state.canvasId = it }
                SliderControl("每列字数", state.rowNum.toFloat(), 4f..50f) {
                    state.rowNum = it.roundToInt()
                }
                SliderControl("列末字到边框距离微调", state.rowDeltaY, 0f..50f) { state.rowDeltaY = it }
            }
        }

        item {
            ControlSection(title = "2. 正文和批注字体设置") {
                FontListControl("正文字体优先级序列", state.textFontsArray) { state.textFontsArray = it }
                SliderControl("正文字体字号", state.textFont1Size, 5f..80f) { state.textFont1Size = it }
                ColorPickerControl("正文字体颜色", state.textFontColor) { state.textFontColor = it }
                HorizontalDivider(modifier = Modifier.padding(top = 4.dp, bottom = 4.dp))
                FontListControl("批注字体优先级序列", state.commentFontsArray) { state.commentFontsArray = it }
                SliderControl("批注字体字号", state.commentFont1Size, 5f..80f) { state.commentFont1Size = it }
                ColorPickerControl("批注字体颜色", state.commentFontColor) { state.commentFontColor = it }
            }
        }

        item {
            ControlSection(title = "3. 封面") {
                SliderControl("封面标题字号", state.coverTitleFontSize, 50f..300f) { state.coverTitleFontSize = it }
                SliderControl("封面标题位置", state.coverTitleY, 0f..2000f) { state.coverTitleY = it }
                SliderControl("封面作者字号", state.coverAuthorFontSize, 30f..150f) { state.coverAuthorFontSize = it }
                SliderControl("封面作者位置", state.coverAuthorY, 0f..2000f) { state.coverAuthorY = it }
                ColorPickerControl("封面字体颜色", state.coverFontColor) { state.coverFontColor = it }
            }
        }

        item {
            ControlSection(title = "4. 版心") {
                SliderControl("版心标题字号", state.titleFontSize, 5f..120f) { state.titleFontSize = it }
                SliderControl("版心标题位置", state.titleY, 0f..3000f) { state.titleY = it }
                ColorPickerControl("版心标题颜色", state.titleFontColor) { state.titleFontColor = it }
                SliderControl("版心页码字号", state.pagerFontSize, 10f..100f) { state.pagerFontSize = it }
                SliderControl("版心页码位置", state.pagerY, 0f..2000f) { state.pagerY = it }
                ColorPickerControl("版心页码颜色", state.pagerFontColor) { state.pagerFontColor = it }
            }
        }

        item {
            ControlSection(title = "5. 标点符号处理") {
                SwitchControl("无标点模式", state.ifNocomma) { state.ifNocomma = it }
                SwitchControl("标点归一化", state.ifOnlyperiod) { state.ifOnlyperiod = it }
            }
        }

        // --- 6. 修饰标记 ---
        item {
            ControlSection(title = "6. 特殊标记") {
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
}