package icu.hearme.vrain

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.isMetaPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import icu.hearme.vrain.configure.AncientCanvasState
import icu.hearme.vrain.configure.CanvasConfigData
import icu.hearme.vrain.configure.ConfigManager
import icu.hearme.vrain.configure.ConfigMeta
import icu.hearme.vrain.bookcanvas.bookSettingsItems
import icu.hearme.vrain.bookcanvas.canvasSettingsItems
import icu.hearme.vrain.configure.AncientBookState
import icu.hearme.vrain.configure.BookConfigData
import icu.hearme.vrain.configure.ConfigManager.loadFromJson
import icu.hearme.vrain.configure.LocalStorage
import icu.hearme.vrain.configure.isDesktopPlatform
import icu.hearme.vrain.editer.TagToolbar
import icu.hearme.vrain.editer.TextEditor
import icu.hearme.vrain.editer.applyTagToSelection
import icu.hearme.vrain.editer.pickAndReadTextFile
import icu.hearme.vrain.engine.BookGridEngine
import icu.hearme.vrain.engine.BookPage
import icu.hearme.vrain.engine.BookTextEngine
import icu.hearme.vrain.theme.AppTheme
import icu.hearme.vrain.utils.ExportPdf
import icu.hearme.vrain.views.SplitButton
import icu.hearme.vrain.views.SplitMenuItem
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource
import vrain.sharedui.generated.resources.Res
import vrain.sharedui.generated.resources.ic_cfg_book
import vrain.sharedui.generated.resources.ic_cfg_canvas
import vrain.sharedui.generated.resources.ic_cfg_pdf
import vrain.sharedui.generated.resources.ic_edit
import vrain.sharedui.generated.resources.ic_files
import vrain.sharedui.generated.resources.ic_pdf
import kotlin.time.Duration.Companion.milliseconds

enum class NavPage(val title: String, val icon: DrawableResource, val canHide: Boolean = false) {
    EDIT("编辑", Res.drawable.ic_edit, true),
    FILES("文件列表", Res.drawable.ic_files),
    BOOKCFG("书籍配置", Res.drawable.ic_cfg_book),
    CANVASCFG("画布配置", Res.drawable.ic_cfg_canvas),
    PDFCFG("PDF配置", Res.drawable.ic_cfg_pdf),
}

@OptIn(ExperimentalMaterial3Api::class, FlowPreview::class)
@Preview
@Composable
fun AppNew(onThemeChanged: @Composable (isDark: Boolean) -> Unit = {}) = AppTheme(onThemeChanged) {
    val scope = rememberCoroutineScope()
    var currentNavPage by remember { mutableStateOf(NavPage.EDIT) }
    var presetList by remember { mutableStateOf<List<ConfigMeta>>(emptyList()) }
    var userList by remember { mutableStateOf<List<ConfigMeta>>(emptyList()) }
    var selectedFileName by remember { mutableStateOf("") }
    var selectedTitle by remember { mutableStateOf("默认样式") }
    var isLoading by remember { mutableStateOf(false) }
    val canvasConfig = remember { AncientCanvasState(CanvasConfigData()) }
    val bookConfig = remember { AncientBookState(BookConfigData()) }
    var content by remember { mutableStateOf("") }
    var showBookSaveDialog by remember { mutableStateOf(false) }
    var showCanvasSaveDialog by remember { mutableStateOf(false) }
    val grid by remember(canvasConfig.configData, bookConfig.configData) {
        derivedStateOf {
            BookGridEngine.calculateGrid(canvasConfig, bookConfig)
        }
    }
    var cioIndex by remember { mutableStateOf(1) }
    val canvasIDOPtions = remember(userList, presetList) {
        val menuOption = mutableListOf<SplitMenuItem>()
        if (userList.isNotEmpty()) {
            menuOption.add(SplitMenuItem("", onAction = {}, splitTitle = "🌟 自定义样式"))

            userList.forEach { act ->
                menuOption.add(
                    SplitMenuItem(act.displayName, onAction = {}){ index ->
                        selectedFileName = act.fileName
                        cioIndex = index
                    }
                )
            }
        }
        menuOption.add(SplitMenuItem("", onAction = {}, splitTitle = "📚 内置样式"))

        presetList.forEach { act ->
            menuOption.add(
                SplitMenuItem(act.displayName, onAction = {}){ index ->
                    selectedFileName = act.fileName
                    cioIndex = index
                }
            )
        }
        menuOption
    }
    var pages by remember { mutableStateOf<List<BookPage>>(emptyList()) }

    var textFieldValue by remember { mutableStateOf(TextFieldValue("")) }
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
        if (currentNavPage.canHide) { isPreviewVisible = !isPreviewVisible }
        else { isPreviewVisible = true }
    }

    var isExporting by remember { mutableStateOf(false) }
    var progressRatio by remember { mutableStateOf(0f) }
    var progressText by remember { mutableStateOf("") }
    var isSingle by remember { mutableStateOf(false) }
    var isSaving by remember { mutableStateOf(false) }
    var styleName by remember { mutableStateOf("") }
    val exportPdf = {
        scope.launch {
            isExporting = true
            progressRatio = 0f
            progressText = "初始化..."
            try {
                ExportPdf.createPdf(pages, bookConfig, canvasConfig, isSingle){ current, total ->
                    progressRatio = current.toFloat() / total
                    progressText = "导出中 $current / $total"
                }
            } finally {
                isExporting = false
                progressText = "就绪"
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

    LaunchedEffect(bookConfig.configData, grid, content) {
        pages = BookTextEngine.parseTextToPages(content, bookConfig, grid)
    }

    LaunchedEffect(selectedFileName){
        if (selectedFileName.isNotBlank()){
            bookConfig.canvasId = selectedFileName.replace(".json", "")
            isLoading = true
            val cfg = ConfigManager.loadConfig(selectedFileName)
            val ccd = loadFromJson(cfg){ CanvasConfigData() }
            canvasConfig.applyNewConfig(ccd)
            isLoading = false
        }
    }

    LaunchedEffect(Unit) {
        val cfg = ConfigManager.loadConfig("book_default.json")
        val bkc: BookConfigData = loadFromJson(cfg){ BookConfigData() }
        bookConfig.applyNewConfig(bkc)
        presetList = ConfigManager.fetchConfigList()
        userList = ConfigManager.fetchUserConfigList()
        snapshotFlow { textFieldValue.text }
            .debounce(700.milliseconds)
            .collect { text -> content = text }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("古籍排版编辑器", maxLines = 1) },
                actions = {
                    if (currentNavPage.canHide) {
                        TooltipBox(
                            TooltipDefaults.rememberTooltipPositionProvider(TooltipAnchorPosition.Above),
                            tooltip = { PlainTooltip { Text("Ctrl+P") } },
                            state = rememberTooltipState()
                        ) {
                            FilledTonalButton(togglePreview, Modifier.padding(end = 8.dp)) {
                                Text(if (isPreviewVisible) "📖 关闭预览" else "💻 打开预览")
                            }
                        }
                    }

                    if (isDesktopPlatform()) {
                        SplitButton(menuOptions, Modifier.wrapContentWidth(), 0,!isExporting)
                    }
                }
            )
        },
        bottomBar = {
            Surface(
                color = MaterialTheme.colorScheme.surfaceColorAtElevation(2.dp),
                modifier = Modifier.fillMaxWidth().height(36.dp)
            ) {
                Row(modifier = Modifier.padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = progressText,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    ) { innerPadding ->
        val focusRequester = remember { FocusRequester() }

        LaunchedEffect(Unit) {
            focusRequester.requestFocus()
        }
        Row(modifier = Modifier.padding(innerPadding).fillMaxSize()
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
            SideNavigation(currentNavPage){ currentNavPage = it }

            ResizableWorkspace(isPreviewVisible, { BookReaderScreen(pages, grid, bookConfig, canvasConfig) }){
                Column(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Row(Modifier.fillMaxWidth()){
                        Text(
                            text = currentNavPage.title,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(Modifier.weight(1f))
                        when (currentNavPage) {
                            NavPage.BOOKCFG -> {
                                Button(
                                    onClick = {
                                        styleName =  bookConfig.title
                                        showBookSaveDialog = true
                                    },
                                    modifier = Modifier.padding(end = 8.dp),
                                    enabled = !isSaving
                                ) {
                                    if (isSaving) {
                                        CircularProgressIndicator(Modifier.size(16.dp), MaterialTheme.colorScheme.onPrimary)
                                    } else {
                                        Text("保存排版")
                                    }
                                }
                            }
                            NavPage.CANVASCFG -> {
                                Button(
                                    onClick = {
                                        styleName = ""
                                        showCanvasSaveDialog = true
                                    },
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
                            else -> {}
                        }
                    }

                    HorizontalDivider()

                    when (currentNavPage) {
                        NavPage.EDIT -> {
                            Column(modifier = Modifier.weight(1f).fillMaxWidth()) {
                                TagToolbar(onApplyTag = { textFieldValue = applyTagToSelection(textFieldValue, it) })
                                TextEditor(textFieldValue, { textFieldValue = it}, focusRequester)
                            }
                        }
                        NavPage.BOOKCFG -> {
                            LazyColumn(
                                modifier = Modifier.fillMaxSize().padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                bookSettingsItems(bookConfig)
                            }
                        }
                        NavPage.CANVASCFG -> {
                            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                                Text("画布样式")
                                SplitButton(canvasIDOPtions, Modifier, cioIndex)
                            }
                            LazyColumn(
                                modifier = Modifier.fillMaxSize().padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                canvasSettingsItems(canvasConfig)
                            }
                        }

                        NavPage.PDFCFG -> {

                        }
                        NavPage.FILES -> {

                        }

                    }
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
                        scope.launch {
                            isSaving = true
                            showBookSaveDialog = false
                            isSaving = false
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
                                val cfg = ConfigManager.convertToCfg(bookConfig.toData())
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
                            val newCustomMeta = ConfigManager.saveUserConfig(canvasConfig.toData(), finalName)
                            userList = listOf(newCustomMeta) + userList
                            selectedTitle = newCustomMeta.displayName
                            selectedFileName = newCustomMeta.fileName
                            isSaving = false
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
                                val cfg = ConfigManager.convertToCfg(canvasConfig.toData())
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

@Composable
private fun ResizableWorkspace(showRightSpace: Boolean, rightSpace: @Composable () -> Unit, leftSpace: @Composable () -> Unit) {
    BoxWithConstraints(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        var leftWeight by remember { mutableFloatStateOf(0.4f) }
        val maxWidthPx = constraints.maxWidth.toFloat()

        Row(modifier = Modifier.fillMaxSize()) {
            OutlinedCard(
                modifier = Modifier.weight(if (showRightSpace) leftWeight else 1f).fillMaxHeight(),
                colors = CardDefaults.outlinedCardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                leftSpace.invoke()
            }
            if (showRightSpace) {
                Box(
                    modifier = Modifier.width(16.dp).fillMaxHeight()
                        .pointerInput(Unit) {
                            detectHorizontalDragGestures { change, dragAmount ->
                                change.consume()
                                val weightChange = dragAmount / maxWidthPx
                                leftWeight = (leftWeight + weightChange).coerceIn(0.2f, 0.8f)
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier.width(4.dp).height(64.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(MaterialTheme.colorScheme.outlineVariant)
                    )
                }

                ElevatedCard(
                    modifier = Modifier.weight(1f - leftWeight).fillMaxHeight(),
                    elevation = CardDefaults.elevatedCardElevation(defaultElevation = 4.dp),
                    colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    rightSpace.invoke()
                }
            }
        }
    }
}

@Composable
private fun SideNavigation(selectedPage: NavPage, onPageSelected: (NavPage) -> Unit) {
    NavigationRail(
        containerColor = MaterialTheme.colorScheme.surface,
        modifier = Modifier.fillMaxHeight()
    ) {
        Spacer(Modifier.height(8.dp))
        NavPage.entries.forEach { page ->
            NavigationRailItem(
                icon = { Image(painterResource(page.icon), contentDescription = page.title) },
                label = { Text(page.title) },
                selected = selectedPage == page,
                onClick = { onPageSelected(page) }
            )
        }
    }
}