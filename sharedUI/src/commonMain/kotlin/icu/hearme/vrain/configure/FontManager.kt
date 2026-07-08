package icu.hearme.vrain.configure

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.text.font.FontFamily
import org.jetbrains.compose.resources.Font
import vrain.sharedui.generated.resources.Res
import vrain.sharedui.generated.resources.hana_minA
import vrain.sharedui.generated.resources.hana_minB
import vrain.sharedui.generated.resources.qiji_combo

data class FontOption(
    val id: String,
    val displayName: String
)

object FontManager {

    const val FONT_QIJI = "qiji_combo"
    const val FONT_HANAMINA = "HanaMinA"
    const val FONT_HANAMINB = "HanaMinB"
    const val FONT_LISHU = "cn_lishu"

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
        val hanaA = Font(Res.font.hana_minA)
        val hanaB = Font(Res.font.hana_minB)
        return remember(cleanName) {
            try {
                when {
                    cleanName.contains("qiji") -> FontFamily(qiji)
                    cleanName.contains("mina") -> FontFamily(hanaA)
                    cleanName.contains("minb") -> FontFamily(hanaB)
                    // cleanName.contains("lishu") -> FontFamily()

                    else -> FontFamily(qiji)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                FontFamily.Default
            }
        }
    }

    fun getAvailableFonts(): List<FontOption> {
        return listOf(
            FontOption(id = FONT_QIJI, displayName = "🌟 经典戚体 (默认)"),
            FontOption(id = FONT_HANAMINA, displayName = "✍️ 传统楷书 (未激活)"),
            FontOption(id = FONT_HANAMINB, displayName = "📖 匠人宋体 (未激活)")
        )
    }
}


