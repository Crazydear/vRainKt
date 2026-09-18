package icu.hearme.vrain.editer

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DividerDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TooltipAnchorPosition
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isAltPressed
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.isMetaPressed
import androidx.compose.ui.input.key.isShiftPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import icu.hearme.vrain.configure.PlatformScrollbar

@Composable
fun TextEditor(textFieldValue: TextFieldValue, onValueChange: (TextFieldValue) -> Unit, focusRequester: FocusRequester, modifier: Modifier = Modifier) {
    val scrollState = rememberScrollState()
    var showSearchPanel by remember { mutableStateOf(false) }
    var isReplaceMode by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf(SearchMemory.lastSearchQuery) }
    var replaceQuery by remember { mutableStateOf(SearchMemory.lastReplaceQuery) }
    var ignoreCase by remember { mutableStateOf(true) }
    var isRegex by remember { mutableStateOf(false) }
    var inSelectionOnly by remember { mutableStateOf(false) }
    var textLayoutResult by remember { mutableStateOf<TextLayoutResult?>(null) }
    var targetSelectionRange by remember { mutableStateOf(TextRange.Zero) }
    val searchVisualTransformation = remember(searchQuery, isRegex, ignoreCase, inSelectionOnly, targetSelectionRange, textFieldValue.selection, showSearchPanel) {
        if (showSearchPanel && searchQuery.isNotEmpty()) {
            SearchHighlightTransformation(searchQuery, isRegex, ignoreCase, inSelectionOnly, targetSelectionRange, textFieldValue.selection)
        } else {
            VisualTransformation.None
        }
    }

    LaunchedEffect(textFieldValue.selection) {
        if (showSearchPanel && searchQuery.isNotEmpty() && textLayoutResult != null) {
            val cursorPosition = textFieldValue.selection.start.coerceIn(0, textFieldValue.text.length)
            try {
                val line = textLayoutResult!!.getLineForOffset(cursorPosition)
                val lineTop = textLayoutResult!!.getLineTop(line)
                val lineBottom = textLayoutResult!!.getLineBottom(line)
                val currentScroll = scrollState.value
                val viewportHeight = scrollState.viewportSize

                if (lineTop < currentScroll) {
                    scrollState.animateScrollTo(lineTop.toInt().coerceAtLeast(0))
                } else if (lineBottom > currentScroll + viewportHeight) {
                    scrollState.animateScrollTo((lineBottom - viewportHeight).toInt().coerceAtLeast(0))
                }
            } catch (e: Exception) { }
        }
    }

    Column(modifier = modifier.padding(8.dp)
            .border(1.dp, MaterialTheme.colorScheme.outline, MaterialTheme.shapes.extraSmall)
            .background(MaterialTheme.colorScheme.surface)
    ) {
        BoxWithConstraints(modifier = Modifier.weight(1f)) {
            val containerMaxHeight = maxHeight

            Box(modifier = Modifier.fillMaxSize().verticalScroll(scrollState)) {
                BasicTextField(
                    value = textFieldValue,
                    onValueChange = onValueChange,
                    onTextLayout = { result -> textLayoutResult = result },
                    visualTransformation = searchVisualTransformation,
                    modifier = Modifier.fillMaxWidth().heightIn(min = containerMaxHeight)
                        .padding(12.dp).padding(end = 12.dp)
                        .focusRequester(focusRequester)
                        .onPreviewKeyEvent { event ->
                            handleEditorKeyEvent(
                                event = event,
                                textFieldValue = textFieldValue,
                                onValueChange = onValueChange,
                                onOpenFind = {
                                    showSearchPanel = true
                                    isReplaceMode = false
                                    if (!textFieldValue.selection.collapsed) {
                                        targetSelectionRange = textFieldValue.selection
                                        val start = textFieldValue.selection.min.coerceAtLeast(0)
                                        val end = textFieldValue.selection.max.coerceAtMost(textFieldValue.text.length)
                                        val selectedText = textFieldValue.text.substring(start, end)
                                        if (selectedText.isNotBlank()) {
                                            searchQuery = selectedText
                                            SearchMemory.lastSearchQuery = selectedText
                                        }
                                    }
                                },
                                onOpenReplace = {
                                    showSearchPanel = true
                                    isReplaceMode = true
                                    if (!textFieldValue.selection.collapsed) {
                                        targetSelectionRange = textFieldValue.selection
                                        val start = textFieldValue.selection.min.coerceAtLeast(0)
                                        val end = textFieldValue.selection.max.coerceAtMost(textFieldValue.text.length)
                                        val selectedText = textFieldValue.text.substring(start, end)
                                        if (selectedText.isNotBlank()) {
                                            searchQuery = selectedText
                                            SearchMemory.lastSearchQuery = selectedText
                                        }
                                    }
                                },
                                onCloseFind = { showSearchPanel = false }
                            )
                        },
                    textStyle = MaterialTheme.typography.bodyLarge.copy(MaterialTheme.colorScheme.onSurface),
                    cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                    decorationBox = { innerTextField ->
                        Box {
                            if (textFieldValue.text.isEmpty()) {
                                Text(
                                    text = "支持快捷键：Ctrl+F(查找)、Ctrl+R(替换)...",
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

        if (showSearchPanel) {
            HorizontalDivider(Modifier, DividerDefaults.Thickness, color = MaterialTheme.colorScheme.outlineVariant)
            FindReplacePanel(
                modifier = Modifier.fillMaxWidth(),
                searchQuery = searchQuery,
                onSearchQueryChange = {
                    searchQuery = it
                    SearchMemory.lastSearchQuery = it
                },
                replaceQuery = replaceQuery,
                onReplaceQueryChange = {
                    replaceQuery = it
                    SearchMemory.lastReplaceQuery = it
                },
                isReplaceMode = isReplaceMode,
                onToggleReplaceMode = { isReplaceMode = !isReplaceMode },
                ignoreCase = ignoreCase,
                onIgnoreCaseChange = { ignoreCase = it },
                isRegex = isRegex,
                onIsRegexChange = { isRegex = it },
                inSelectionOnly = inSelectionOnly,
                onInSelectionOnlyChange = { enabled ->
                    inSelectionOnly = enabled
                    if (enabled && !textFieldValue.selection.collapsed) {
                        targetSelectionRange = textFieldValue.selection
                    }
                },
                textFieldValue = textFieldValue,
                targetSelectionRange = targetSelectionRange,
                onFindNext = { onValueChange(performFindNext(textFieldValue, searchQuery, ignoreCase, isRegex, inSelectionOnly, targetSelectionRange, true)) },
                onFindPrev = { onValueChange(performFindNext(textFieldValue, searchQuery, ignoreCase, isRegex, inSelectionOnly, targetSelectionRange, false)) },
                onReplace = { onValueChange(performReplace(textFieldValue, searchQuery, replaceQuery, ignoreCase, isRegex, inSelectionOnly, targetSelectionRange)) },
                onReplaceAll = { onValueChange(performReplaceAll(textFieldValue, searchQuery, replaceQuery, ignoreCase, isRegex, inSelectionOnly, targetSelectionRange)) },
                onClose = { showSearchPanel = false }
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TagToolbar(onApplyTag: (AncientTag) -> Unit, modifier: Modifier = Modifier) {
    Surface(tonalElevation = 2.dp, modifier = modifier.fillMaxWidth()) {
        FlowRow(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp).focusProperties { canFocus = false },
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalArrangement = Arrangement.spacedBy(0.dp)
        ) {
            AncientTag.entries.forEach { tag ->
                val displayLabel = buildString {
                    append(tag.label)
                    append(" ")
                    if (tag.endTag.isNotEmpty()) append("${tag.startTag}${tag.endTag}") else append(tag.startTag)
                }

                val filterChip = @Composable {
                    FilterChip(
                        selected = false,
                        onClick = { onApplyTag(tag) },
                        modifier = Modifier.focusProperties { canFocus = false },
                        label = { Text(displayLabel, style = MaterialTheme.typography.labelSmall) },
                        colors = FilterChipDefaults.filterChipColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    )
                }
                if (!tag.shortcutHint.isNullOrEmpty()) {
                    TooltipBox(
                        positionProvider = TooltipDefaults.rememberTooltipPositionProvider(
                            TooltipAnchorPosition.Above
                        ),
                        tooltip = { PlainTooltip { Text(text = tag.shortcutHint) } },
                        state = rememberTooltipState()
                    ) {
                        filterChip()
                    }
                } else {
                    filterChip()
                }
            }
        }
    }
}

private var isCtrlHeld = false

fun handleEditorKeyEvent(
    event: KeyEvent, textFieldValue: TextFieldValue, onValueChange: (TextFieldValue) -> Unit,
    onOpenFind: () -> Unit = {}, onOpenReplace: () -> Unit = {}, onCloseFind: () -> Unit = {}
): Boolean {
    if (event.key == Key.CtrlLeft || event.key == Key.CtrlRight) {
        if (event.type == KeyEventType.KeyDown) isCtrlHeld = true
        if (event.type == KeyEventType.KeyUp) isCtrlHeld = false
    }
    val isCmdOrCtrl = isCtrlHeld || event.isCtrlPressed || event.isMetaPressed
    val isShift = event.isShiftPressed
    val isAlt = event.isAltPressed

    if (event.key == Key.Escape) {
        if (event.type == KeyEventType.KeyDown) onCloseFind()
        return true
    }

    if (isCmdOrCtrl && !isShift && !isAlt) {
        if (event.key == Key.F) {
            if (event.type == KeyEventType.KeyDown) onOpenFind()
            return true
        }
        if (event.key == Key.R || event.key == Key.H) {
            if (event.type == KeyEventType.KeyDown) onOpenReplace()
            return true
        }
    }

    val headingLevel = when (event.key) {
        Key.One, Key.NumPad1 -> 1
        Key.Two, Key.NumPad2 -> 2
        Key.Three, Key.NumPad3 -> 3
        Key.Four, Key.NumPad4 -> 4
        Key.Five, Key.NumPad5 -> 5
        Key.Six, Key.NumPad6 -> 6
        else -> null
    }

    if (isCmdOrCtrl && !isShift && !isAlt) {
        if (headingLevel != null) {
            if (event.type == KeyEventType.KeyDown) {
                if (headingLevel < 4) {
                    val targetTag = when (headingLevel) {
                        1 -> AncientTag.FOCUS_CIRCLE
                        2 -> AncientTag.FOCUS_POINT
                        3 -> AncientTag.FOCUS_LINE
                        else -> null
                    }
                    targetTag?.let { onValueChange(applyTagToSelection(textFieldValue, it)) }
                } else {
                    onValueChange(duplicateLineAsHeading(textFieldValue, headingLevel - 3))
                }
            }
            return true
        }
        if (event.type == KeyEventType.Unknown) return true
    }

    if (isCmdOrCtrl && isShift) {
        val targetTag = when (event.key) {
            Key.B -> AncientTag.BOOK_LINE
            Key.C -> AncientTag.COMMENT
            Key.R -> AncientTag.RECT
            Key.O -> AncientTag.CIRCLE
            Key.Z -> AncientTag.ZOOM
            Key.Enter -> AncientTag.NEW_PAGE
            else -> null
        }
        if (targetTag != null) {
            if (event.type == KeyEventType.KeyDown) {
                onValueChange(applyTagToSelection(textFieldValue, targetTag))
            }
            return true
        }
        if (event.type == KeyEventType.Unknown) return true
    }

    if (isAlt && isShift && event.key == Key.Enter) {
        if (event.type == KeyEventType.KeyDown) {
            onValueChange(applyTagToSelection(textFieldValue, AncientTag.HALF_PAGE))
        }
        return true
    }
    return false
}

fun applyTagToSelection(currentValue: TextFieldValue, tag: AncientTag): TextFieldValue {
    val text = currentValue.text
    val selection = currentValue.selection
    val min = selection.min
    val max = selection.max

    return if (selection.collapsed) {
        val newText = text.substring(0, min) + tag.startTag + tag.endTag + text.substring(max)
        TextFieldValue(newText, TextRange(min + tag.startTag.length))
    } else {
        val selectedText = text.substring(min, max)
        val newText = text.substring(0, min) + tag.startTag + selectedText + tag.endTag + text.substring(max)
        val newCursorPos = max + tag.startTag.length + tag.endTag.length
        TextFieldValue(newText, TextRange(newCursorPos))
    }
}

fun duplicateLineAsHeading(currentValue: TextFieldValue, level: Int): TextFieldValue {
    val text = currentValue.text
    val selection = currentValue.selection

    val lineStart = text.lastIndexOf('\n', (selection.min - 1).coerceAtLeast(0)).let { if (it == -1) 0 else it + 1 }
    val lineEnd = text.indexOf('\n', selection.min).let { if (it == -1) text.length else it }
    val currentLine = text.substring(lineStart, lineEnd)
    val hashMatch = Regex("^#+").find(currentLine)
    val existingHashCount = hashMatch?.value?.length ?: 0

    return if (existingHashCount > 0) {
        val totalHashes = existingHashCount + level
        val updatedLine = if (totalHashes > 6) currentLine.replaceFirst(Regex("^#+\\s?"), "") else "#".repeat(level) + currentLine
        val newText = text.substring(0, lineStart) + updatedLine + text.substring(lineEnd)
        val delta = updatedLine.length - currentLine.length
        val newStart = (selection.start + delta).coerceIn(lineStart, lineStart + updatedLine.length)
        val newEnd = (selection.end + delta).coerceIn(lineStart, lineStart + updatedLine.length)
        TextFieldValue(newText, TextRange(newStart, newEnd))
    } else {
        val headingLine = "#".repeat(level) + " " + currentLine + "\n"
        val newText = text.substring(0, lineStart) + headingLine + text.substring(lineStart)
        val offset = headingLine.length
        val newSelection = TextRange(selection.start + offset, selection.end + offset)
        TextFieldValue(newText, newSelection)
    }
}