package icu.hearme.vrain.views

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.painterResource
import vrain.sharedui.generated.resources.Res
import vrain.sharedui.generated.resources.ic_arrow_down_s

data class SplitMenuItem(
    val text: String,
    val iconPainter: Painter? = null,
    val iconVector: ImageVector? = null,
    val onAction: () -> Unit
)

@Composable
fun SplitButton(
    options: List<SplitMenuItem>,
    modifier: Modifier = Modifier,
    defaultSelectedIndex: Int = 0,
    enabled: Boolean = true
) {
    var expanded by remember { mutableStateOf(false) }
    var selectedIndex by remember { mutableStateOf(defaultSelectedIndex) }

    val selectedOption = options.getOrNull(selectedIndex) ?: return

    val borderColor = Color.Gray.copy(alpha = if (enabled) 0.5f else 0.2f)
    val contentColor = Color.DarkGray
    val arrowColor = Color(0xFFB75E54)

    val alpha = if (enabled) 1f else 0.38f

    Row(
        modifier = modifier.height(IntrinsicSize.Min)
            .border(1.dp, borderColor, RoundedCornerShape(6.dp))
            .clip(RoundedCornerShape(6.dp))
            .background(Color.White),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier.clickable(enabled = enabled) { selectedOption.onAction() }
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            RenderIcon(selectedOption.iconPainter, selectedOption.iconVector,
                contentDescription = selectedOption.text,
                modifier = Modifier.size(18.dp)
            )
            Text(
                text = selectedOption.text,
                color = contentColor.copy(alpha = alpha), // 应用透明度
                style = MaterialTheme.typography.bodyMedium
            )
        }

        Box(modifier = Modifier.fillMaxHeight().width(1.dp).background(borderColor))

        Box {
            Box(
                modifier = Modifier
                    .clickable(enabled = enabled) { expanded = true } // 传入 enabled
                    .fillMaxHeight()
                    .padding(horizontal = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painterResource(Res.drawable.ic_arrow_down_s),
                    contentDescription = "展开菜单",
                    tint = arrowColor.copy(alpha = alpha), // 应用透明度
                    modifier = Modifier.size(20.dp)
                )
            }

            DropdownMenu(expanded,{ expanded = false }) {
                options.forEachIndexed { index, item ->
                    if (selectedIndex != index) {
                        DropdownMenuItem(
                            text = { Text(item.text) },
                            leadingIcon = {
                                RenderIcon(item.iconPainter, item.iconVector, null, modifier = Modifier.size(18.dp))
                            },
                            onClick = {
                                selectedIndex = index
                                expanded = false
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun RenderIcon(
    painter: Painter?,
    vector: ImageVector?,
    contentDescription: String?,
    modifier: Modifier = Modifier
) {
    when {
        painter != null -> {
            Image(
                painter = painter,
                contentDescription = contentDescription,
                modifier = modifier
            )
        }
        vector != null -> {
            Image(
                imageVector = vector,
                contentDescription = contentDescription,
                modifier = modifier
            )
        }
    }
}