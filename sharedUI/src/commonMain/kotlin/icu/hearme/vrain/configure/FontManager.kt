package icu.hearme.vrain.configure

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import org.jetbrains.compose.resources.Font
import vrain.sharedui.generated.resources.HanaMinA
import vrain.sharedui.generated.resources.HanaMinB
import vrain.sharedui.generated.resources.KaiXinSong
import vrain.sharedui.generated.resources.KaiXinSongB
import vrain.sharedui.generated.resources.Res
import vrain.sharedui.generated.resources.qiji_combo

data class FontOption(
    val id: String,
    val displayName: String
)

object FontManager {

    const val FONT_QIJI = "qiji_combo"
    const val FONT_HANAMINA = "HanaMinA"
    const val FONT_HANAMINB = "HanaMinB"
    const val FONT_KX = "KaiXinSong"
    const val FONT_KXB = "KaiXinSongB"

    @Composable
    fun getFontFamily(fontConfigName: String?): FontFamily {
        val qiji = Font(Res.font.qiji_combo)
        if (fontConfigName.isNullOrBlank()) {
            return remember { FontFamily(qiji) }
        }

        val cleanName = remember(fontConfigName) {
            fontConfigName.substringBeforeLast(".")
                .lowercase()
                .replace("-", "_")
                .trim()
        }
        val hanaA = Font(Res.font.HanaMinA)
        val hanaB = Font(Res.font.HanaMinB)
        val kxA = Font(Res.font.KaiXinSong)
        val kxB = Font(Res.font.KaiXinSongB)
        return remember(cleanName) {
            try {
                when {
                    cleanName.contains("qiji") -> FontFamily(qiji)
                    cleanName.contains("mina") -> FontFamily(hanaA)
                    cleanName.contains("minb") -> FontFamily(hanaB)
                    cleanName.contains("kaixinsongb") -> FontFamily(kxB)
                    cleanName.contains("kaixinsong") -> FontFamily(kxA)
                    else -> FontFamily(qiji)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                FontFamily.Default
            }
        }
    }

    @Composable
    fun getFontFamily(fontArray: List<String>?): FontFamily {
        val qiji = Font(Res.font.qiji_combo)
        if (fontArray == null) return remember { FontFamily(qiji) }
        val fonts = mutableListOf<Font>()
        val hanaA = Font(Res.font.HanaMinA)
        val hanaB = Font(Res.font.HanaMinB)
        val kxA = Font(Res.font.KaiXinSong)
        val kxB = Font(Res.font.KaiXinSongB)
        fontArray.forEach { name ->
            when(name) {
                "qiji_combo.ttf" -> fonts.add(qiji)
                "HanaMinA.ttf" -> fonts.add(hanaA)
                "HanaMinB.ttf" -> fonts.add(hanaB)
                "KaiXinSong.ttf" -> fonts.add(kxA)
                "KaiXinSongB.ttf" -> fonts.add(kxB)
            }
        }
        return remember { FontFamily(fonts) }
    }

    fun getAvailableFonts(): List<FontOption> {
        return listOf(
            FontOption(id = FONT_QIJI, displayName = "🌟令东齐伋体"),
            FontOption(id = FONT_HANAMINA, displayName = "花园明朝(基础)"),
            FontOption(id = FONT_HANAMINB, displayName = "花园明朝体(扩展)"),
            FontOption(id = FONT_KX, displayName = "开心宋体"),
            FontOption(id = FONT_KXB, displayName = "开心宋体(扩展)")
        )
    }
}


