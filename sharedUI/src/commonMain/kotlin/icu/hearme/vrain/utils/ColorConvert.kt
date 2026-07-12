package icu.hearme.vrain.utils

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import kotlin.text.toString

object ColorConvert {

    fun String.toColor(): Color {
        if (this.isBlank()) return Color.Transparent
        val cleanStr = this.trim().lowercase()
        return try {
            when {
                cleanStr == "black" -> Color.Black
                cleanStr == "white" -> Color.White
                cleanStr == "red" -> Color.Red
                cleanStr == "blue" -> Color.Blue
                cleanStr.startsWith("#") -> {
                    val hexStr = cleanStr.removePrefix("#")
                    val argbHex = if (hexStr.length == 6) "ff$hexStr" else hexStr
                    Color(argbHex.toLong(16))
                }
                cleanStr.startsWith("rgb(") && cleanStr.endsWith(")") -> {
                    val values = cleanStr.substringAfter("rgb(").substringBefore(")")
                        .split(",").map { it.trim().toIntOrNull() ?: 0 }
                    if (values.size == 3) Color(red = values[0], green = values[1], blue = values[2])
                    else Color.Unspecified
                }
                else -> Color.Unspecified
            }
        } catch (e: Exception) {
            Color.Red
        }
    }

    fun Color.toConfigString(): String {
        if (this == Color.Unspecified) return "#000000"
        val argb = this.toArgb()
        val a = (argb shr 24 and 0xFF)
        val r = (argb shr 16 and 0xFF).toString(16).padStart(2, '0')
        val g = (argb shr 8 and 0xFF).toString(16).padStart(2, '0')
        val b = (argb and 0xFF).toString(16).padStart(2, '0')
        return if (a == 0xFF) "#$r$g$b" else "#${a.toString(16).padStart(2, '0')}$r$g$b"
    }
}