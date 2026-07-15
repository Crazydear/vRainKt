package icu.hearme.vrain

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import icu.hearme.vrain.configure.AncientCanvasState
import icu.hearme.vrain.configure.CanvasConfigData
import icu.hearme.vrain.configure.ConfigManager
import icu.hearme.vrain.configure.ConfigMeta
import icu.hearme.vrain.configure.PageSplitConfig
import icu.hearme.vrain.bookcanvas.StyleCustomizationScreen
import icu.hearme.vrain.configure.AncientBookState
import icu.hearme.vrain.configure.BookConfigData
import icu.hearme.vrain.engine.BookGridEngine
import icu.hearme.vrain.engine.BookPage
import icu.hearme.vrain.engine.BookTextEngine
import icu.hearme.vrain.theme.AppTheme
import icu.hearme.vrain.utils.ycxz

@OptIn(ExperimentalMaterial3Api::class)
@Preview
@Composable
fun App(
    onThemeChanged: @Composable (isDark: Boolean) -> Unit = {}
) = AppTheme(onThemeChanged) {
    var expanded by remember { mutableStateOf(false) }
    val psConfig by remember { mutableStateOf(PageSplitConfig()) }
    val scope = rememberCoroutineScope()

    var presetList by remember { mutableStateOf<List<ConfigMeta>>(emptyList()) }
    var userList by remember { mutableStateOf<List<ConfigMeta>>(emptyList()) }
    var selectedFileName by remember { mutableStateOf("") }
    var selectedTitle by remember { mutableStateOf("默认样式") }
    var isLoading by remember { mutableStateOf(false) }
    val canvasConfig = remember { AncientCanvasState(CanvasConfigData()) }
    val bookConfig = remember { AncientBookState(BookConfigData()) }

    val grid by remember(canvasConfig.configData, bookConfig.configData) {
        derivedStateOf {
            BookGridEngine.calculateGrid(canvasConfig, bookConfig)
        }
    }
    var pages by remember { mutableStateOf<List<BookPage>>(emptyList()) }

    LaunchedEffect(selectedFileName){
        if (selectedFileName.isNotBlank()){
            isLoading = true
            val ccd = ConfigManager.loadConfig(selectedFileName)
            canvasConfig.applyNewConfig(ccd)
            pages = BookTextEngine.parseTextToPages(ycxz, bookConfig, grid)

            isLoading = false
        }
    }

    LaunchedEffect(Unit){
        presetList = ConfigManager.fetchConfigList()
        userList = ConfigManager.fetchUserConfigList()
        pages = BookTextEngine.parseTextToPages(ycxz, bookConfig, grid)
    }

    Column(
        modifier = Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing).padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
            ExposedDropdownMenuBox(
                expanded = expanded,
                onExpandedChange = { expanded = !expanded },
                modifier = Modifier.weight(1f)
            ) {
                TextField(
                    value = selectedTitle,
                    onValueChange = {},
                    readOnly = true,
                    trailingIcon = {
                        if (isLoading) CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                        else ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
                    },
                    modifier = Modifier.menuAnchor()
                )

                ExposedDropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false },
                    modifier = Modifier.widthIn(max = 280.dp)
                ) {
                    if (userList.isNotEmpty()) {
                        DropdownMenuItem(
                            text = { Text("🌟 自定义样式", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary) },
                            onClick = {}
                        )
                        userList.forEach { act ->
                            DropdownMenuItem(
                                text = { Text(act.displayName) },
                                onClick = {
                                    selectedTitle = act.displayName
                                    selectedFileName = act.fileName
                                    expanded = false
                                }
                            )
                        }
                        HorizontalDivider()
                    }

                    DropdownMenuItem(
                        text = { Text("📚 内置样式", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary) },
                        onClick = {}
                    )
                    presetList.forEach { act ->
                        DropdownMenuItem(
                            text = { Text(act.displayName) },
                            onClick = {
                                selectedTitle = act.displayName
                                selectedFileName = act.fileName
                                expanded = false
                            }
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        BookReaderScreen(pages, grid, bookConfig, canvasConfig, psConfig)


//        StyleCustomizationScreen(
//            state = config,
//            onSaveClick = { newCustomMeta ->
//                scope.launch {
//                    userList = listOf(newCustomMeta) + userList
//                    selectedTitle = newCustomMeta.displayName
//                    selectedFileName = newCustomMeta.fileName
//                }
//            },
//            onBackClick = {}
//        )
    }
}
