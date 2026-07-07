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
import icu.hearme.vrain.theme.AppTheme
import kotlinx.coroutines.launch

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
    val config = remember { AncientCanvasState(CanvasConfigData()) }

    LaunchedEffect(selectedFileName){
        if (selectedFileName.isNotBlank()){
            isLoading = true
            val ccd = ConfigManager.loadConfig(selectedFileName)
            config.applyNewConfig(ccd)
            isLoading = false
        }
    }

    LaunchedEffect(Unit){
        presetList = ConfigManager.fetchConfigList()
        userList = ConfigManager.fetchUserConfigList()
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

        StyleCustomizationScreen(
            state = config,
            onSaveClick = { newCustomMeta ->
                scope.launch {
                    userList = listOf(newCustomMeta) + userList
                    selectedTitle = newCustomMeta.displayName
                    selectedFileName = newCustomMeta.fileName
                }
            },
            onBackClick = {}
        )
    }
}
