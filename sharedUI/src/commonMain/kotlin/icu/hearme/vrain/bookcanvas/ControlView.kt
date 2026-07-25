package icu.hearme.vrain.bookcanvas

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import icu.hearme.vrain.configure.FontManager.getAvailableFonts
import icu.hearme.vrain.configure.FontManager.getFontFamily
import icu.hearme.vrain.configure.FontOption
import org.jetbrains.compose.resources.painterResource
import vrain.sharedui.generated.resources.Res
import vrain.sharedui.generated.resources.ic_arrow_down
import vrain.sharedui.generated.resources.ic_check

@Composable
fun ControlSection(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.surfaceContainerLow,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                content()
            }
        }
    }
}

@Composable
fun SliderControl(label: String, value: Float, range: ClosedFloatingPointRange<Float>, steps: Int = 0, onValueChange: (Float) -> Unit) {
    Column(modifier = Modifier.padding(vertical = 4.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(text = label, style = MaterialTheme.typography.bodyMedium)
            Text(text = value.toInt().toString(), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
        }
        Slider(value = value, onValueChange = onValueChange, valueRange = range, steps = steps)
    }
}

@Composable
fun SwitchControl(label: String, checked: Boolean, modifier: Modifier = Modifier,onCheckedChange: (Boolean) -> Unit) {
    Row(modifier = modifier.fillMaxWidth().padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, style = MaterialTheme.typography.bodyMedium)
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
fun StringInputControl(label: String, value: String?, onValueChange: (String) -> Unit) {
    Row(
        modifier = Modifier.padding(vertical = 4.dp).fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(end = 4.dp)
        )
        BasicTextField(
            value = value ?: "",
            onValueChange = onValueChange,
            modifier = Modifier.weight(1f).heightIn(min = 36.dp)
                .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(4.dp))
                .padding(horizontal = 8.dp),
            singleLine = true,
            textStyle = MaterialTheme.typography.bodyMedium.copy(
                color = MaterialTheme.colorScheme.onSurface
            ),
            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
            decorationBox = { innerTextField ->
                Box(Modifier.fillMaxSize(), Alignment.CenterStart) {
                    innerTextField()
                }
            }
        )
    }
}

@Composable
fun ColorPickerControl(label: String, currentColor: Color, onColorSelected: (Color) -> Unit) {
    val presetColors = listOf(
        Color(0xFFEEEEEE), // 默认白纸
        Color(0xFFF5E8D0), // 泛黄旧纸
        Color(0xFFFFFFFF), // 纯白
        Color(0xFFf5f5f5),
        Color(0xFF333333), // 浅墨
        Color.Black,       // 经典墨黑
        Color(0xFF874434), // 传统朱砂
        Color(0xFFE9313E),
        Color(0xFF0E6696),
        Color(0xFF1E3A8A),  // 藏蓝
    )

    Column(modifier = Modifier.padding(vertical = 8.dp)) {
        Text(text = label, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(bottom = 8.dp))
        Row(modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            presetColors.forEach { color ->
                val isSelected = currentColor == color
                Box(modifier = Modifier.size(36.dp).clip(CircleShape).background(color)
                    .border(
                        width = if (isSelected) 3.dp else 1.dp,
                        color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Gray,
                        shape = CircleShape
                    )
                    .clickable { onColorSelected(color) },
                    contentAlignment = Alignment.Center
                ) {
                    if (isSelected) {
                        val iconTint = if (color == Color.White || color == Color(0xFFEEEEEE) || color == Color(0xFFF5E8D0)) Color.Black else Color.White
                        Icon(painterResource(Res.drawable.ic_check), contentDescription = null, tint = iconTint, modifier = Modifier.size(20.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun FontControl(index: Int, fontOption: FontOption) {
    val ff = getFontFamily(fontOption.id)
    Row(
        modifier = Modifier.padding(vertical = 4.dp).fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = index.toString(),
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(end = 4.dp)
        )
        Text(
            text = fontOption.displayName,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(end = 15.dp),
            fontFamily = ff
        )
    }
}

@Composable
fun FontListControl(label: String, value: String?, onValueChange: (String) -> Unit){
    var isFontsExpanded by remember { mutableStateOf(false) }
    val rotation by animateFloatAsState(
        targetValue = if (isFontsExpanded) 180f else 0f,
        label = "iconRotation"
    )

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.weight(1f)) {
                StringInputControl(label, value, onValueChange)
            }
            IconButton(onClick = { isFontsExpanded = !isFontsExpanded }) {
                Icon(
                    painter = painterResource(Res.drawable.ic_arrow_down),
                    contentDescription = if (isFontsExpanded) "收起" else "展开",
                    modifier = Modifier.rotate(rotation),
                    tint = Color.Black
                )
            }
        }
        AnimatedVisibility(visible = isFontsExpanded) {
            Column(Modifier.fillMaxWidth().padding(start = 16.dp, end = 8.dp, bottom = 8.dp)) {
                val fontsList = getAvailableFonts()
                fontsList.forEachIndexed { index, font ->
                    FontControl(index+1, font)
                }
            }
        }
    }
}

