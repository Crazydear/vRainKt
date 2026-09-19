package icu.hearme.vrain.bookcanvas

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.ContextMenuArea
import androidx.compose.foundation.ContextMenuItem
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import icu.hearme.vrain.configure.LocalStorage
import icu.hearme.vrain.configure.LocalStorage.chooseFiles
import icu.hearme.vrain.configure.getZhPageNum
import icu.hearme.vrain.utils.dragContainer
import icu.hearme.vrain.utils.rememberDragDropState
import java.io.File
import java.util.UUID

enum class FileType(val ext: String) {
    TXT("txt"),
    MD("md"),
    ATT("att");

    companion object {
        fun fromExtension(ext: String): FileType? {
            return entries.find { it.ext.equals(ext, ignoreCase = true) }
        }
    }
}

object FileCategory {
    const val PREFACE = "序言"
    const val CATALOG = "目录"
    const val BODY = "正文"
    const val APPENDIX = "附言"
}

// 文件数据模型
data class FileInfo(
    val id: Int = -1,
    val name: String,
    val type: FileType,
    val file: File,
    val category: String = FileCategory.BODY,
    val uniqueKey: String = UUID.randomUUID().toString()
) {
    val tpost: String
        get() = when (category) {
            FileCategory.PREFACE -> "序"
            FileCategory.CATALOG -> "目录"
            FileCategory.APPENDIX -> "附"
            else -> getZhPageNum(id)
        }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FileManagerComponent(
    fileList: List<FileInfo>,
    modifier: Modifier = Modifier,
    onFileDoubleClick: (file: FileInfo, content: String?) -> Unit,
    onFileListUpdated: (fileList: List<FileInfo>) -> Unit
) {
    val listState = rememberLazyListState()
    val dragDropState = rememberDragDropState(
        lazyListState = listState,
        onMove = { fromIndex, toIndex ->
            val fromItem = fileList.getOrNull(fromIndex)
            val toItem = fileList.getOrNull(toIndex)
            if (fromItem == null || toItem == null) return@rememberDragDropState false
            if (fromItem.category != FileCategory.BODY || toItem.category != FileCategory.BODY) {
                return@rememberDragDropState false
            }
            val tempList = fileList.toMutableList()
            val item = tempList.removeAt(fromIndex)
            tempList.add(toIndex, item)
            onFileListUpdated(reorderAndAssignIds(tempList))
            true
        }
    )

    val blankContextMenuItems = {
        listOf(
            ContextMenuItem("导入文件...") {
                val selectedFiles = chooseFiles()
                if (selectedFiles.isNotEmpty()) {
                    val newFileInfos = selectedFiles.mapNotNull { file ->
                        val ext = file.extension
                        val type = FileType.fromExtension(ext)
                        if (type != null) {
                            FileInfo(-1, file.nameWithoutExtension, type, file, uniqueKey = UUID.randomUUID().toString())
                        } else {
                            null
                        }
                    }
                    val tempList = fileList.toMutableList()
                    tempList.addAll(newFileInfos)
                    onFileListUpdated(reorderAndAssignIds(tempList))
                }
            }
        )
    }

    ContextMenuArea(items = blankContextMenuItems) {
        Box(modifier = modifier.fillMaxSize().background(Color(0xFFF5F5F5)).padding(16.dp)) {
            if (fileList.isEmpty()) {
                Text("右键此处导入文件", Modifier.align(Alignment.Center), Color.Gray)
            } else {
                LazyColumn(Modifier.fillMaxSize().dragContainer(dragDropState), listState, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    itemsIndexed(fileList, key = { _, file -> file.uniqueKey }) { index, file ->
                        val isDragging = index == dragDropState.draggingItemIndex
                        val elevation by animateFloatAsState(if (isDragging) 8f else 0f)
                        val scale by animateFloatAsState(if (isDragging) 1.05f else 1f)
                        val yOffset = if (isDragging) {
                            dragDropState.draggingItemOffset - (listState.layoutInfo.visibleItemsInfo.find { it.index == index }?.offset ?: 0)
                        } else 0f

                        Box(
                            modifier = Modifier.fillMaxWidth().zIndex(if (isDragging) 1f else 0f)
                                .graphicsLayer {
                                    scaleX = scale
                                    scaleY = scale
                                    shadowElevation = elevation
                                    translationY = yOffset
                                }
                                .animateItem()
                        ) {
                            FileItemRow(
                                file = file,
                                onDoubleClick = { onFileDoubleClick(file, LocalStorage.readText(file.file)) },
                                onSetCategory = { newCategory ->
                                    val tempList = fileList.toMutableList()
                                    if (newCategory != FileCategory.BODY) {
                                        val existingIndex = tempList.indexOfFirst { it.category == newCategory }
                                        if (existingIndex != -1) {
                                            tempList[existingIndex] = tempList[existingIndex].copy(category = FileCategory.BODY)
                                        }
                                    }
                                    val currentIndex = tempList.indexOfFirst { it.uniqueKey == file.uniqueKey }
                                    if (currentIndex != -1) {
                                        tempList[currentIndex] = tempList[currentIndex].copy(category = newCategory)
                                        onFileListUpdated(reorderAndAssignIds(tempList))
                                    }
                                },
                                onRemove = {
                                    val tempList = fileList.toMutableList()
                                    tempList.removeAll { it.uniqueKey == file.uniqueKey }
                                    onFileListUpdated(reorderAndAssignIds(tempList))
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun FileItemRow(file: FileInfo, onDoubleClick: () -> Unit, onSetCategory: (String) -> Unit, onRemove: () -> Unit) {
    val fileContextMenuItems = {
        listOf(
            ContextMenuItem("设为序言") { onSetCategory(FileCategory.PREFACE) },
            ContextMenuItem("设为目录") { onSetCategory(FileCategory.CATALOG) },
            ContextMenuItem("设为正文") { onSetCategory(FileCategory.BODY) },
            ContextMenuItem("设为附言") { onSetCategory(FileCategory.APPENDIX) },
            ContextMenuItem("----------") { },
            ContextMenuItem("移除") { onRemove() }
        )
    }

    ContextMenuArea(items = fileContextMenuItems) {
        Surface(
            shape = RoundedCornerShape(8.dp),
            color = Color.White,
            shadowElevation = 2.dp,
            modifier = Modifier.fillMaxWidth().combinedClickable(onClick = {}, onDoubleClick = onDoubleClick)
        ) {
            Row(Modifier.padding(12.dp), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = file.type.ext.uppercase(),
                        fontSize = 12.sp,
                        color = Color.White,
                        modifier = Modifier
                            .background(
                                color = when (file.type) {
                                    FileType.TXT -> Color(0xFF4CAF50)
                                    FileType.MD -> Color(0xFF2196F3)
                                    FileType.ATT -> Color(0xFFFF9800)
                                },
                                shape = RoundedCornerShape(4.dp)
                            )
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))

                    Text(
                        text = "${file.name}.${file.type.ext} [${file.tpost}]",
                        style = MaterialTheme.typography.bodyLarge
                    )
                }

                Text(
                    text = file.category,
                    style = MaterialTheme.typography.bodySmall,
                    color = when (file.category) {
                        FileCategory.PREFACE -> Color(0xFFD32F2F)
                        FileCategory.CATALOG -> Color(0xFF9C27B0)
                        FileCategory.APPENDIX -> Color(0xFF1976D2)
                        else -> Color.DarkGray
                    },
                    modifier = Modifier
                        .background(Color(0xFFE0E0E0), RoundedCornerShape(12.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        }
    }
}

private fun reorderAndAssignIds(currentList: List<FileInfo>): List<FileInfo> {
    val preface = currentList.find { it.category == FileCategory.PREFACE }
    val catalog = currentList.find { it.category == FileCategory.CATALOG }
    val appendix = currentList.find { it.category == FileCategory.APPENDIX }
    val bodies = currentList.filter { it.category == FileCategory.BODY }

    val newList = mutableListOf<FileInfo>()

    preface?.let { newList.add(it.copy(id = 0)) }
    catalog?.let { newList.add(it.copy(id = 0)) }

    var bodyIdCounter = 1
    bodies.forEach { body ->
        newList.add(body.copy(id = bodyIdCounter++))
    }
    appendix?.let { newList.add(it.copy(id = 999)) }
    return newList
}