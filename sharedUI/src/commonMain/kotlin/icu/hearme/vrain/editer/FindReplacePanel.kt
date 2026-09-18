package icu.hearme.vrain.editer

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.*
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

object SearchMemory {
    var lastSearchQuery = ""
    var lastReplaceQuery = ""
}

@Composable
fun FindReplacePanel(
    modifier: Modifier = Modifier,
    searchQuery: String, onSearchQueryChange: (String) -> Unit,
    replaceQuery: String, onReplaceQueryChange: (String) -> Unit,
    isReplaceMode: Boolean, onToggleReplaceMode: () -> Unit,
    ignoreCase: Boolean, onIgnoreCaseChange: (Boolean) -> Unit,
    isRegex: Boolean, onIsRegexChange: (Boolean) -> Unit,
    inSelectionOnly: Boolean, onInSelectionOnlyChange: (Boolean) -> Unit,
    textFieldValue: TextFieldValue, targetSelectionRange: TextRange,
    onFindNext: () -> Unit, onFindPrev: () -> Unit,
    onReplace: () -> Unit, onReplaceAll: () -> Unit,
    onClose: () -> Unit
) {
    val matchCount = remember(textFieldValue.text, searchQuery, ignoreCase, isRegex, inSelectionOnly, targetSelectionRange) {
        if (searchQuery.isEmpty()) 0
        else try {
            val searchArea = if (inSelectionOnly) {
                val start = targetSelectionRange.min.coerceIn(0, textFieldValue.text.length)
                val end = targetSelectionRange.max.coerceIn(0, textFieldValue.text.length)
                textFieldValue.text.substring(start, end)
            } else textFieldValue.text

            if (isRegex) {
                Regex(searchQuery, if (ignoreCase) setOf(RegexOption.IGNORE_CASE) else emptySet()).findAll(searchArea).count()
            } else {
                var count = 0
                var idx = searchArea.indexOf(searchQuery, 0, ignoreCase)
                while (idx != -1) {
                    count++
                    idx = searchArea.indexOf(searchQuery, idx + searchQuery.length, ignoreCase)
                }
                count
            }
        } catch (e: Exception) { 0 }
    }
    val searchFocusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        searchFocusRequester.requestFocus()
    }

    Surface(modifier = modifier, color = MaterialTheme.colorScheme.surfaceVariant.copy(0.3f)) {
        Column(Modifier.padding(horizontal = 8.dp, vertical = 6.dp), Arrangement.spacedBy(6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                SmallIconButton(if (isReplaceMode) "▼" else "▶", onToggleReplaceMode)
                CompactTextField(searchQuery, onSearchQueryChange, "查找...", Modifier.weight(1f), searchFocusRequester)
                Text(
                    text = if (searchQuery.isNotEmpty()) "$matchCount 结果" else "",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.widthIn(min = 45.dp)
                )

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    ToggleChip(label = "忽略大小写", checked = ignoreCase, onCheckedChange = onIgnoreCaseChange)
                    ToggleChip(label = "正则", checked = isRegex, onCheckedChange = onIsRegexChange)
                    ToggleChip(label = "选中区", checked = inSelectionOnly, onCheckedChange = onInSelectionOnlyChange)
                }

                HorizontalDivider(Modifier.height(16.dp).width(1.dp), DividerDefaults.Thickness, DividerDefaults.color)

                Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                    SmallIconButton("▲", onFindPrev)
                    SmallIconButton("▼", onFindNext)
                    SmallIconButton("✕", onClose)
                }
            }

            if (isReplaceMode) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Spacer(modifier = Modifier.width(28.dp))
                    CompactTextField(replaceQuery, onReplaceQueryChange, "替换为...", Modifier.weight(1f))
                    Button(
                        onClick = onReplace,
                        modifier = Modifier.height(26.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp),
                        shape = RoundedCornerShape(4.dp)
                    ) { Text("替换", fontSize = 12.sp) }

                    OutlinedButton(
                        onClick = onReplaceAll,
                        modifier = Modifier.height(26.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp),
                        shape = RoundedCornerShape(4.dp)
                    ) { Text("全部", fontSize = 12.sp) }

                    Spacer(modifier = Modifier.weight(0.5f))
                }
            }
        }
    }
}

@Composable
private fun CompactTextField(
    value: String, onValueChange: (String) -> Unit, placeholder: String, modifier: Modifier = Modifier,
    focusRequester: FocusRequester = remember { FocusRequester() }
) {
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        singleLine = true,
        textStyle = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurface, fontSize = 12.sp),
        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
        modifier = modifier.height(26.dp).focusRequester(focusRequester),
        decorationBox = { innerTextField ->
            Row(
                modifier = Modifier.fillMaxSize()
                    .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(4.dp))
                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(4.dp))
                    .padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(modifier = Modifier.weight(1f)) {
                    if (value.isEmpty()) {
                        Text(placeholder, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f), fontSize = 12.sp)
                    }
                    innerTextField()
                }
            }
        }
    )
}

@Composable
private fun ToggleChip(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    val containerColor = if (checked) MaterialTheme.colorScheme.primaryContainer else Color.Transparent
    val contentColor = if (checked) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
    val borderColor = if (checked) MaterialTheme.colorScheme.primary.copy(alpha = 0.5f) else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)

    Surface(
        onClick = { onCheckedChange(!checked) },
        modifier = Modifier.height(24.dp),
        shape = RoundedCornerShape(4.dp),
        color = containerColor,
        border = BorderStroke(1.dp, borderColor)
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(horizontal = 6.dp)) {
            Text(text = label, fontSize = 11.sp, color = contentColor)
        }
    }
}

@Composable
private fun SmallIconButton(text: String, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        modifier = Modifier.size(24.dp), // 缩小按钮尺寸
        shape = RoundedCornerShape(4.dp),
        color = Color.Transparent,
        contentColor = MaterialTheme.colorScheme.onSurfaceVariant
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(text = text, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        }
    }
}


class SearchHighlightTransformation(
    private val searchQuery: String,
    private val isRegex: Boolean,
    private val ignoreCase: Boolean,
    private val inSelectionOnly: Boolean,
    private val targetSelectionRange: TextRange,
    private val currentSelection: TextRange,
    private val highlightColor: Color = Color(0xFFFFF59D),
    private val activeHighlightColor: Color = Color(0xFFFFB74D)
) : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        if (searchQuery.isEmpty()) return TransformedText(text, OffsetMapping.Identity)

        val builder = AnnotatedString.Builder(text)
        val searchStart = if (inSelectionOnly) targetSelectionRange.min.coerceIn(0, text.length) else 0
        val searchEnd = if (inSelectionOnly) targetSelectionRange.max.coerceIn(0, text.length) else text.length

        if (searchStart < searchEnd) {
            val searchText = text.text.substring(searchStart, searchEnd)
            val matchRanges = mutableListOf<IntRange>()

            if (isRegex) {
                try {
                    val regex = Regex(searchQuery, if (ignoreCase) setOf(RegexOption.IGNORE_CASE) else emptySet())
                    regex.findAll(searchText).forEach { matchRanges.add(it.range) }
                } catch (e: Exception) {}
            } else {
                var idx = searchText.indexOf(searchQuery, 0, ignoreCase)
                while (idx != -1) {
                    matchRanges.add(idx until (idx + searchQuery.length))
                    idx = searchText.indexOf(searchQuery, idx + searchQuery.length, ignoreCase)
                }
            }

            matchRanges.forEach { range ->
                val matchStart = searchStart + range.first
                val matchEnd = searchStart + range.last + 1
                val isActive = matchStart == currentSelection.min && matchEnd == currentSelection.max
                val bgColor = if (isActive) activeHighlightColor else highlightColor
                builder.addStyle(SpanStyle(background = bgColor, color = Color.Black), matchStart, matchEnd)
            }
        }
        return TransformedText(builder.toAnnotatedString(), OffsetMapping.Identity)
    }
}

fun performFindNext(
    currentValue: TextFieldValue, query: String,
    ignoreCase: Boolean, isRegex: Boolean, inSelectionOnly: Boolean, selectionRange: TextRange, forward: Boolean
): TextFieldValue {
    if (query.isEmpty()) return currentValue
    val text = currentValue.text
    val scopeStart = if (inSelectionOnly) selectionRange.min.coerceIn(0, text.length) else 0
    val scopeEnd = if (inSelectionOnly) selectionRange.max.coerceIn(0, text.length) else text.length
    if (scopeStart >= scopeEnd && inSelectionOnly) return currentValue

    val matches = mutableListOf<TextRange>()

    if (isRegex) {
        try {
            val regex = Regex(query, if (ignoreCase) setOf(RegexOption.IGNORE_CASE) else emptySet())
            regex.findAll(text.substring(scopeStart, scopeEnd)).forEach {
                matches.add(TextRange(scopeStart + it.range.first, scopeStart + it.range.last + 1))
            }
        } catch (e: Exception) {}
    } else {
        var idx = text.indexOf(query, scopeStart, ignoreCase)
        while (idx != -1 && idx < scopeEnd) {
            matches.add(TextRange(idx, idx + query.length))
            idx = text.indexOf(query, idx + query.length, ignoreCase)
        }
    }

    if (matches.isEmpty()) return currentValue

    val currentPos = if (forward) currentValue.selection.max else currentValue.selection.min
    val targetMatch = if (forward) {
        matches.firstOrNull { it.min >= currentPos } ?: matches.first()
    } else {
        matches.lastOrNull { it.max <= currentPos } ?: matches.last()
    }

    return currentValue.copy(selection = targetMatch)
}

fun performReplace(
    currentValue: TextFieldValue, findQuery: String, replaceQuery: String,
    ignoreCase: Boolean, isRegex: Boolean, inSelectionOnly: Boolean, selectionRange: TextRange
): TextFieldValue {
    if (findQuery.isEmpty()) return currentValue
    val text = currentValue.text
    val sel = currentValue.selection

    val scopeStart = if (inSelectionOnly) selectionRange.min.coerceIn(0, text.length) else 0
    val scopeEnd = if (inSelectionOnly) selectionRange.max.coerceIn(0, text.length) else text.length

    val selectedText = if (sel.min < sel.max) text.substring(sel.min, sel.max) else ""
    val isInScope = sel.min >= scopeStart && sel.max <= scopeEnd

    val isMatch = if (!isInScope || selectedText.isEmpty()) {
        false
    } else if (isRegex) {
        try {
            Regex(findQuery, if (ignoreCase) setOf(RegexOption.IGNORE_CASE) else emptySet()).matchEntire(selectedText) != null
        } catch (e: Exception) { false }
    } else {
        selectedText.equals(findQuery, ignoreCase = ignoreCase)
    }

    return if (isMatch) {
        val replacement = if (isRegex) {
            try {
                Regex(findQuery, if (ignoreCase) setOf(RegexOption.IGNORE_CASE) else emptySet()).replaceFirst(selectedText, replaceQuery)
            } catch (e: Exception) { replaceQuery }
        } else {
            replaceQuery
        }

        val newText = text.substring(0, sel.min) + replacement + text.substring(sel.max)
        val newValue = currentValue.copy(text = newText, selection = TextRange(sel.min + replacement.length))

        performFindNext(newValue, findQuery, ignoreCase, isRegex, inSelectionOnly, selectionRange, forward = true)
    } else {
        performFindNext(currentValue, findQuery, ignoreCase, isRegex, inSelectionOnly, selectionRange, forward = true)
    }
}

fun performReplaceAll(
    currentValue: TextFieldValue, findQuery: String, replaceQuery: String,
    ignoreCase: Boolean, isRegex: Boolean, inSelectionOnly: Boolean, selectionRange: TextRange
): TextFieldValue {
    if (findQuery.isEmpty()) return currentValue
    val text = currentValue.text

    val scopeStart = if (inSelectionOnly) selectionRange.min.coerceIn(0, text.length) else 0
    val scopeEnd = if (inSelectionOnly) selectionRange.max.coerceIn(0, text.length) else text.length
    if (scopeStart >= scopeEnd && inSelectionOnly) return currentValue

    val targetStr = text.substring(scopeStart, scopeEnd)

    val replacedSegment = if (isRegex) {
        try {
            Regex(findQuery, if (ignoreCase) setOf(RegexOption.IGNORE_CASE) else emptySet())
                .replace(targetStr, replaceQuery)
        } catch (e: Exception) { targetStr }
    } else {
        targetStr.replace(findQuery, replaceQuery, ignoreCase = ignoreCase)
    }

    val newText = text.substring(0, scopeStart) + replacedSegment + text.substring(scopeEnd)
    return currentValue.copy(text = newText, selection = TextRange(scopeStart + replacedSegment.length))
}