package icu.hearme.vrain.editer

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TooltipAnchorPosition
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.isMetaPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import icu.hearme.vrain.BookReaderScreen
import icu.hearme.vrain.configure.AncientBookState
import icu.hearme.vrain.configure.AncientCanvasState
import icu.hearme.vrain.configure.LocalStorage
import icu.hearme.vrain.configure.PlatformScrollbar
import icu.hearme.vrain.configure.isDesktopPlatform
import icu.hearme.vrain.engine.BookGrid
import icu.hearme.vrain.engine.BookPage
import icu.hearme.vrain.engine.BookTextEngine
import icu.hearme.vrain.utils.ExportPdf
import icu.hearme.vrain.views.SplitButton
import icu.hearme.vrain.views.SplitMenuItem
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.painterResource
import vrain.sharedui.generated.resources.Res
import vrain.sharedui.generated.resources.ic_pdf
import java.awt.FileDialog
import java.awt.Frame
import java.io.File
import java.nio.charset.Charset
import kotlin.time.Duration.Companion.milliseconds

@OptIn(ExperimentalMaterial3Api::class, FlowPreview::class)
@Composable
fun TagEditorScreen(
    initialText: String,
    bookConfig: AncientBookState,
    canvasConfig: AncientCanvasState,
    grid: BookGrid,
    onSaveText: (String) -> Unit
) {
    var textFieldValue by remember { mutableStateOf(TextFieldValue(initialText)) }
    var pages by remember { mutableStateOf<List<BookPage>>(emptyList()) }
    val scope = rememberCoroutineScope()
    var isPreviewVisible by remember { mutableStateOf(true) }

    val handleImport = {
        pickAndReadTextFile(
            onSuccess = { loadedText ->
                textFieldValue = TextFieldValue(text = loadedText, selection = TextRange(0))
            }
        )
    }

    val handleExport = {
        LocalStorage.exportCfg(bookConfig.title, textFieldValue.text, "txt")
    }

    val togglePreview = {
        isPreviewVisible = !isPreviewVisible
    }

    var isExporting by remember { mutableStateOf(false) }
    var progressRatio by remember { mutableStateOf(0f) }
    var progressText by remember { mutableStateOf("") }
    var isSingle by remember { mutableStateOf(false) }
    val exportPdf = {
        scope.launch {
            isExporting = true
            progressRatio = 0f
            progressText = "初始化..."
            try {
                ExportPdf.createPdf(pages, bookConfig, canvasConfig, isSingle){ current, total ->
                    progressRatio = current.toFloat() / total
                    progressText = "$current / $total"
                }
            } finally {
                isExporting = false
            }
        }
    }

    val menuOptions = listOf(
        SplitMenuItem(
            text = "导出PDF",
            painterResource(Res.drawable.ic_pdf),
            onAction = {
                isSingle = false
                exportPdf()
            }
        ),
        SplitMenuItem(
            text = "裁剪为单页PDF",
            painterResource(Res.drawable.ic_pdf),
            onAction = {
                isSingle = true
                exportPdf()
            }
        )
    )

    LaunchedEffect(textFieldValue.text, bookConfig.configData, grid) {
        pages = BookTextEngine.parseTextToPages(textFieldValue.text, bookConfig, grid)
    }

    LaunchedEffect(Unit) {
        snapshotFlow { textFieldValue.text }
            .debounce(500.milliseconds)
            .collect { text ->
                onSaveText(text)
            }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("古籍排版编辑器", maxLines = 1) },
                actions = {
                    TooltipBox(
                        TooltipDefaults.rememberTooltipPositionProvider(TooltipAnchorPosition.Above),
                        tooltip = { PlainTooltip { Text("Ctrl+P") } },
                        state = rememberTooltipState()
                    ) {
                        FilledTonalButton(togglePreview, Modifier.padding(end = 8.dp)) {
                            Text(if (isPreviewVisible) "📖 关闭预览" else "💻 打开预览")
                        }
                    }

                    if (isDesktopPlatform()) {
                        TooltipBox(
                            TooltipDefaults.rememberTooltipPositionProvider(TooltipAnchorPosition.Above),
                            tooltip = { PlainTooltip { Text("Ctrl+O") } },
                            state = rememberTooltipState()
                        ) {
                            OutlinedButton(handleImport, Modifier.padding(end = 8.dp)) {
                                Text("导入")
                            }
                        }

                        TooltipBox(
                            TooltipDefaults.rememberTooltipPositionProvider(TooltipAnchorPosition.Above),
                            tooltip = { PlainTooltip { Text("Ctrl+S") } },
                            state = rememberTooltipState()
                        ) {
                            Button(handleExport, Modifier.padding(end = 8.dp)) {
                                Text("导出")
                            }
                        }

                        SplitButton(menuOptions, Modifier.wrapContentWidth(), 0,!isExporting)
                    }
                }
            )
        }
    ) { paddingValues ->
        val focusRequester = remember { FocusRequester() }

        LaunchedEffect(Unit) {
            focusRequester.requestFocus()
        }

        BoxWithConstraints(
            modifier = Modifier.fillMaxSize().padding(paddingValues)
                .onPreviewKeyEvent { event ->
                    val isCmdOrCtrl = event.isCtrlPressed || event.isMetaPressed
                    if (event.type == KeyEventType.KeyDown && isCmdOrCtrl) {
                        when (event.key){
                            Key.P -> { togglePreview(); return@onPreviewKeyEvent true }
                            Key.O -> { handleImport(); return@onPreviewKeyEvent true }
                            Key.S -> { handleExport(); return@onPreviewKeyEvent true }
                        }
                    }
                    false
                }
        ) {
            val isWideScreen = maxWidth > 800.dp

            @Composable
            fun ScrollableTextEditor() {
                val scrollState = rememberScrollState()

                BoxWithConstraints(
                    modifier = Modifier.fillMaxSize().padding(8.dp)
                        .border(1.dp, MaterialTheme.colorScheme.outline, MaterialTheme.shapes.extraSmall)
                        .background(MaterialTheme.colorScheme.surface)
                ) {
                    val containerMaxHeight = maxHeight
                    Box(modifier = Modifier.fillMaxSize().verticalScroll(scrollState)) {
                        BasicTextField(
                            value = textFieldValue,
                            onValueChange = { textFieldValue = it },
                            modifier = Modifier.fillMaxWidth().heightIn(min = containerMaxHeight)
                                .padding(12.dp).padding(end = 12.dp)
                                .focusRequester(focusRequester)
                                .onPreviewKeyEvent { event ->
                                    handleEditorKeyEvent(event, textFieldValue, { textFieldValue = it })
                                },
                            textStyle = MaterialTheme.typography.bodyLarge.copy(MaterialTheme.colorScheme.onSurface),
                            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                            decorationBox = { innerTextField ->
                                Box {
                                    if (textFieldValue.text.isEmpty()) {
                                        Text(
                                            text = "支持快捷键：Ctrl+Shift+B(书名)、Ctrl+S(导出)...",
                                            style = MaterialTheme.typography.bodyLarge,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    innerTextField()
                                }
                            }
                        )
                    }

                    PlatformScrollbar(
                        scrollState = scrollState,
                        modifier = Modifier.align(Alignment.CenterEnd).fillMaxHeight().padding(2.dp, 4.dp)
                    )
                }
            }

            if (isWideScreen) {
                Row(modifier = Modifier.fillMaxSize()) {
                    Column(
                        modifier = Modifier.weight(1f).fillMaxHeight()
                            .border(1.dp, MaterialTheme.colorScheme.outlineVariant)
                    ) {
                        TagToolbar(
                            onApplyTag = { tag ->
                                textFieldValue = applyTagToSelection(textFieldValue, tag)
                            }
                        )
                        ScrollableTextEditor()
                    }

                    AnimatedVisibility(
                        visible = isPreviewVisible,
                        modifier = Modifier.weight(1f).fillMaxHeight(),
                        enter = expandHorizontally(expandFrom = Alignment.End),
                        exit = shrinkHorizontally(shrinkTowards = Alignment.End)
                    ) {
                        Box(
                            modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surfaceVariant),
                            contentAlignment = Alignment.Center
                        ) {
                            BookReaderScreen(pages, grid, bookConfig, canvasConfig)
                        }
                    }
                }
            } else {
                Column(modifier = Modifier.fillMaxSize()) {
                    AnimatedVisibility(
                        visible = isPreviewVisible,
                        modifier = Modifier.weight(1f).fillMaxWidth(),
                        enter = expandVertically(expandFrom = Alignment.Bottom),
                        exit = shrinkVertically(shrinkTowards = Alignment.Bottom)
                    ) {
                        Box(
                            modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surfaceVariant),
                            contentAlignment = Alignment.Center
                        ) {
                            BookReaderScreen(pages, grid, bookConfig, canvasConfig)
                        }
                    }
                    Column(modifier = Modifier.weight(1f).fillMaxWidth()) {
                        TagToolbar(onApplyTag = { textFieldValue = applyTagToSelection(textFieldValue, it) })
                        ScrollableTextEditor()
                    }
                }
            }
        }
    }
}

fun pickAndReadTextFile(onSuccess: (String) -> Unit, onError: (Throwable) -> Unit = {}) {
    try {
        val dialog = FileDialog(null as Frame?, "选择古籍原始文本文件", FileDialog.LOAD).apply {
            filenameFilter = java.io.FilenameFilter { _, name ->
                name.endsWith(".txt", ignoreCase = true) || name.endsWith(".md", ignoreCase = true)
            }
            isVisible = true
        }

        val directory = dialog.directory
        val file = dialog.file

        if (directory != null && file != null) {
            val selectedFile = File(directory, file)
            val content = try {
                selectedFile.readText(Charsets.UTF_8)
            } catch (e: Exception) {
                try {
                    selectedFile.readText(Charset.forName("GBK"))
                } catch (e2: Exception) {
                    selectedFile.readText(Charset.defaultCharset())
                }
            }
            onSuccess(content)
        }
    } catch (e: Throwable) {
        onError(e)
    }
}

