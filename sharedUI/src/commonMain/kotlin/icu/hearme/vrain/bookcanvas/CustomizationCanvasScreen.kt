package icu.hearme.vrain.bookcanvas

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import icu.hearme.vrain.configure.AncientCanvasState
import icu.hearme.vrain.configure.ConfigManager
import icu.hearme.vrain.configure.ConfigMeta
import icu.hearme.vrain.configure.LocalStorage
import icu.hearme.vrain.configure.isDesktopPlatform
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@Composable
fun CustomizationCanvasScreen(
    state: AncientCanvasState,
    onSaveClick: suspend (ConfigMeta) -> Unit,
    onBackClick: () -> Unit,
    content: @Composable () -> Unit
) {
    val scope = rememberCoroutineScope()
    var isSaving by remember { mutableStateOf(false) }
    var showSaveDialog by remember { mutableStateOf(false) }
    var styleName by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            @OptIn(ExperimentalMaterial3Api::class)
            TopAppBar(
                title = { Text("自定义背景样式") },
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
                        CanvasControlPanel(state)
                    }
                }
            } else {
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
                        CanvasControlPanel(state)
                    }
                }
            }
        }

        if (showSaveDialog) {
            AlertDialog(
                onDismissRequest = { showSaveDialog = false },
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
                            val finalName = if (styleName.isNotBlank()) styleName else "自定义样式"
                            scope.launch {
                                isSaving = true
                                showSaveDialog = false
                                val newCustomMeta = ConfigManager.saveUserConfig(state.toData(),finalName)
                                onSaveClick(newCustomMeta)
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
                                    val cfg = ConfigManager.convertToCfg(canvasConfig = state.toData())
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
private fun CanvasControlPanel(state: AncientCanvasState) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surface).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        item {
            ControlSection(title = "1. 画布与基础属性") {
                SliderControl("画布宽度", state.canvasWidth, 500f..3000f) { state.canvasWidth = it }
                SliderControl("画布高度", state.canvasHeight, 500f..4000f) { state.canvasHeight = it }
                SliderControl("每页列数", state.leafCol.toFloat(), 4f..32f, steps = 13) {
                    state.leafCol = (it.roundToInt() / 2) * 2
                }
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

        // --- 2. 纸张留白 ---
        item {
            ControlSection(title = "2. 纸张留白 (Margins)") {
                SliderControl("上留白 (Top)", state.marginsTop, 0f..300f) { state.marginsTop = it }
                SliderControl("下留白 (Bottom)", state.marginsBottom, 0f..250f) { state.marginsBottom = it }
                SliderControl("左留白 (Left)", state.marginsLeft, 0f..250f) { state.marginsLeft = it }
                SliderControl("右留白 (Right)", state.marginsRight, 0f..250f) { state.marginsRight = it }
            }
        }

        if (state.bamboo) return@LazyColumn

        // --- 3. 多栏模式 ---
        item {
            ControlSection(title = "3. 多栏") {
                SwitchControl("多栏模式", state.ifMultirows) { state.ifMultirows = it }
                SliderControl("栏数", state.multirowsNum.toFloat(), 1f..5f, steps = 3) { state.multirowsNum = it.toInt() }
                SliderControl("分栏横线线宽", state.multirowsLinewidth, 0f..10f) { state.multirowsLinewidth = it }
                ColorPickerControl("栏内细线颜色", state.multirowsColcolor) { state.multirowsColcolor = it }
            }
        }

        // --- 5. 边框与间距 ---
        item {
            ControlSection(title = "4. 线装边框") {
                SliderControl("内细边框线宽", state.inlineWidth, 0f..20f) { state.inlineWidth = it }
                SliderControl("外粗边框线宽", state.outlineWidth, 0f..50f) { state.outlineWidth = it }
                SliderControl("内外框水平间距", state.outlineHMargin, 0f..100f) { state.outlineHMargin = it }
                SliderControl("内外框垂直间距", state.outlineVMargin, 0f..100f) { state.outlineVMargin = it }
                ColorPickerControl("内边框颜色", state.inlineColor) { state.inlineColor = it }
                ColorPickerControl("外边框颜色", state.outlineColor) { state.outlineColor = it }
            }
        }

        // --- 5. 鱼尾与中缝基线 ---
        item {
            ControlSection(title = "5. 版心与鱼尾") {
                SliderControl("版心宽度", state.leafCenterWidth, 0f..200f) {
                    state.leafCenterWidth = it
                    if (it < 1) { state.isFullpage = true }
                }
                SwitchControl("弧形鱼尾", state.ifFishflower) { state.ifFishflower = it }
                StringInputControl("鱼尾修饰图", state.fishFlowerImage) { state.fishFlowerImage = it }
                SliderControl("鱼尾线条宽度", state.fishLineWidth, 0f..20f) { state.fishLineWidth = it }
                SliderControl("鱼尾线条留白", state.fishLineMargin, 0f..200f) { state.fishLineMargin = it }
                ColorPickerControl("鱼尾线条颜色", state.fishLineColor) { state.fishLineColor = it }
            }
        }

        // --- 6. 上鱼尾 ---
        item {
            ControlSection(title = "6. 上鱼尾") {
                SliderControl("位置", state.fishTopY, 0f..1500f) { state.fishTopY = it }
                SliderControl("鱼身高度", state.fishTopRectHeight, 0f..200f) { state.fishTopRectHeight = it }
                SliderControl("鱼尾高度", state.fishTopTriaHeight, 0f..200f) { state.fishTopTriaHeight = it }
                SliderControl("版心分割线宽度", state.fishTopLinewidth, 0f..20f) { state.fishTopLinewidth = it }
                ColorPickerControl("鱼尾颜色", state.fishTopColor) { state.fishTopColor = it }
            }
        }

        // --- 7. 下鱼尾 ---
        item {
            ControlSection(title = "7. 下鱼尾") {
                SwitchControl("对鱼尾", state.fishBtmDirection == 1) { state.fishBtmDirection = if (it) 1 else 0 }
                SliderControl("位置", state.fishBtmY, 0f..1500f) { state.fishBtmY = it }
                SliderControl("鱼身高度", state.fishBtmRectHeight, 0f..200f) { state.fishBtmRectHeight = it }
                SliderControl("鱼尾高度", state.fishBtmTriaHeight, 0f..200f) { state.fishBtmTriaHeight = it }
                SliderControl("版心分割线宽度", state.fishBtmLinewidth, 0f..20f) { state.fishBtmLinewidth = it }
                ColorPickerControl("鱼尾颜色", state.fishBtmColor) { state.fishBtmColor = it }
            }
        }

        // --- 8. 书房题签与印记 ---
        item {
            ControlSection(title = "8. 题签与印记") {
                StringInputControl("书房名", state.logoText) { state.logoText = it }
                StringInputControl("字体", state.logoFont) { state.logoFont = it }
                StringInputControl("印记图片路径", state.logoImage) { state.logoImage = it }
                SliderControl("位置", state.logoY, 0f..3000f) { state.logoY = it }
                SliderControl("字体大小", state.logoFontSize, 0f..200f) { state.logoFontSize = it }
                ColorPickerControl("题签颜色", state.logoColor) { state.logoColor = it }
            }
        }
    }
}

