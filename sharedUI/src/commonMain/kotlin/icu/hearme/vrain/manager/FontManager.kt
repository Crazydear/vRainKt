package icu.hearme.vrain.manager

import androidx.compose.runtime.Composable
import androidx.compose.ui.text.font.FontFamily

data class FontOption(
    val id: String,
    val displayName: String,
    val isSystemFont: Boolean = false
)

expect object PlatformFontManager {
    fun getSystemFonts(): List<FontOption>
    fun getBuiltInFonts(): List<FontOption>

    @Composable
    fun getFontFamily(fontName: String): FontFamily

    @Composable
    fun getFontFamily(fontNames: List<String>): FontFamily
}

object FontManager {
    const val FONT_QIJI = "qiji_combo"

    fun getAvailableFonts(): List<FontOption> {
        val builtIn = PlatformFontManager.getBuiltInFonts()
        val system = PlatformFontManager.getSystemFonts()
        return builtIn + system
    }

    @Composable
    fun getFontFamily(fontConfigName: String?): FontFamily {
        val name = if (fontConfigName.isNullOrBlank()) FONT_QIJI else fontConfigName
        return PlatformFontManager.getFontFamily(name)
    }

    @Composable
    fun getFontFamily(fontArray: List<String>?): FontFamily {
        val names = if (fontArray.isNullOrEmpty()) listOf(FONT_QIJI) else fontArray
        return PlatformFontManager.getFontFamily(names)
    }
}

