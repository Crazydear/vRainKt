package icu.hearme.vrain.manager

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import icu.hearme.vrain.R

actual object PlatformFontManager {
    actual fun getSystemFonts(): List<FontOption> {
        return listOf(
            FontOption("sans-serif", "系统无衬线 (Sans Serif)", true),
            FontOption("serif", "系统衬线 (Serif)", true),
            FontOption("monospace", "系统等宽 (Monospace)", true)
        )
    }

    actual fun getBuiltInFonts(): List<FontOption> {
        return listOf(
            FontOption(id = FontManager.FONT_QIJI, displayName = "令东齐伋体"),
            FontOption(id = "HanaMinA", displayName = "花园明朝体(基础)"),
            FontOption(id = "HanaMinB", displayName = "花园明朝体(扩展)"),
            FontOption(id = "KaiXinSongA", displayName = "开心宋体(基础)"),
            FontOption(id = "KaiXinSongB", displayName = "开心宋体(扩展)"),
        )
    }

    @Composable
    private fun getComposeResourceFont(cleanName: String): Font? {
        return when {
            cleanName.contains("qiji") -> Font(R.font.qiji_combo)
            cleanName.contains("mina") || cleanName.contains("hanamina") -> Font(R.font.hanamina)
            cleanName.contains("minb") || cleanName.contains("hanaminb") -> Font(R.font.hanaminb)
            cleanName.contains("kaixinsonga") -> Font(R.font.kaixinsonga)
            cleanName.contains("kaixinsongb") -> Font(R.font.kaixinsongb)
            else -> null
        }
    }

    @Composable
    actual fun getFontFamily(fontName: String): FontFamily {
        val cleanName = remember(fontName) {
            fontName.substringBeforeLast(".").lowercase().replace("-", "_").trim()
        }
        val resFont = getComposeResourceFont(cleanName)
        if (resFont != null) {
            return remember(resFont) { FontFamily(resFont) }
        }
        return remember(fontName) {
            when (fontName) {
                "sans-serif" -> FontFamily.SansSerif
                "serif" -> FontFamily.Serif
                "monospace" -> FontFamily.Monospace
                else -> FontFamily.Default
            }
        }
    }

    @Composable
    actual fun getFontFamily(fontNames: List<String>): FontFamily {
        val fonts = mutableListOf<Font>()

        fontNames.forEach { name ->
            if (name.isNotBlank()) {
                val cleanName = name.trim().lowercase().replace("-", "_")
                val resFont = getComposeResourceFont(cleanName)
                if (resFont != null) {
                    fonts.add(resFont)
                }
            }
        }

        return remember(fonts) {
            if (fonts.isNotEmpty()) FontFamily(fonts) else FontFamily.Default
        }
    }
}