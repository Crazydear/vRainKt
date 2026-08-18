package icu.hearme.vrain.configure

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import org.jetbrains.compose.resources.Font
import vrain.sharedui.generated.resources.HanaMinA
import vrain.sharedui.generated.resources.HanaMinB
import vrain.sharedui.generated.resources.KaiXinSongA
import vrain.sharedui.generated.resources.KaiXinSongB
import vrain.sharedui.generated.resources.Res
import vrain.sharedui.generated.resources.qiji_combo
import vrain.sharedui.generated.resources.simkai

data class FontOption(
    val id: String,
    val displayName: String
)

object FontManager {

    const val FONT_SIMKAI = "simkai"
    const val FONT_QIJI = "qiji_combo"
    const val FONT_HANAMINA = "HanaMinA"
    const val FONT_HANAMINB = "HanaMinB"
    const val FONT_KX = "KaiXinSongA"
    const val FONT_KXB = "KaiXinSongB"

    @Composable
    fun getFontFamily(fontConfigName: String?): FontFamily {
        val kaiti = Font(Res.font.simkai)

        if (fontConfigName.isNullOrBlank()) {
            return remember { FontFamily(kaiti) }
        }

        val cleanName = remember(fontConfigName) {
            fontConfigName.substringBeforeLast(".")
                .lowercase()
                .replace("-", "_")
                .trim()
        }
        val qiji = Font(Res.font.qiji_combo)
        val hanaA = Font(Res.font.HanaMinA)
        val hanaB = Font(Res.font.HanaMinB)
        val kxA = Font(Res.font.KaiXinSongA)
        val kxB = Font(Res.font.KaiXinSongB)

        return remember(cleanName) {
            try {
                when {
                    cleanName.contains("simkai") -> FontFamily(kaiti)
                    cleanName.contains("qiji") -> FontFamily(qiji)
                    cleanName.contains("mina") -> FontFamily(hanaA)
                    cleanName.contains("minb") -> FontFamily(hanaB)
                    cleanName.contains("kaixinsonga") -> FontFamily(kxA)
                    cleanName.contains("kaixinsongb") -> FontFamily(kxB)

                    else -> FontFamily(kaiti)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                FontFamily.Default
            }
        }
    }

    @Composable
    fun getFontFamily(fontArray: List<String>?): FontFamily {
        val kaiti = Font(Res.font.simkai)
        val qiji = Font(Res.font.qiji_combo)
        val hanaA = Font(Res.font.HanaMinA)
        val hanaB = Font(Res.font.HanaMinB)
        val kxA = Font(Res.font.KaiXinSongA)
        val kxB = Font(Res.font.KaiXinSongB)
        if (fontArray == null) return remember { FontFamily(qiji) }
        return remember(fontArray) {
            val fonts = mutableListOf<Font>()
            fontArray.forEachIndexed { index, name ->
                println("${index + 1}-$name")
                if (name.isNotBlank()) {
                    val cleanName = name.trim().lowercase().replace("-", "_")

                    when {
                        cleanName.contains("simkai") -> fonts.add(kaiti)
                        cleanName.contains("qiji") -> fonts.add(qiji)
                        cleanName.contains("hanamina") -> fonts.add(hanaA)
                        cleanName.contains("hanaminb") -> fonts.add(hanaB)
                        cleanName.contains("kaixinsonga") -> fonts.add(kxA)
                        cleanName.contains("kaixinsongb") -> fonts.add(kxB)
                    }
                }
            }
            if (fonts.isEmpty()) {
                fonts.add(kaiti)
            }
            FontFamily(fonts)
        }
    }

    fun getAvailableFonts(): List<FontOption> {
        return listOf(
            FontOption(id = FONT_QIJI, displayName = "令东齐伋体"),
            FontOption(id = FONT_HANAMINA, displayName = "花园明朝体(基础)"),
            FontOption(id = FONT_HANAMINB, displayName = "花园明朝体(扩展)"),
            FontOption(id = FONT_KX, displayName = "开心宋体"),
            FontOption(id = FONT_KXB, displayName = "开心宋体(扩展)"),
            FontOption(id = FONT_SIMKAI, displayName = "简楷"),
        )
    }
}


