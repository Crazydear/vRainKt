package icu.hearme.vrain.manager

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.platform.Typeface as ComposeTypeface
import androidx.compose.ui.text.platform.Font as DesktopFont
import icu.hearme.vrain.configure.LocalStorage
import org.apache.fontbox.ttf.TrueTypeFont
import org.apache.pdfbox.pdmodel.font.FontMappers
import org.jetbrains.skia.FontMgr
import org.jetbrains.skia.FontStyle
import java.awt.Font
import java.io.File

actual object PlatformFontManager {
    private val fontDirectory = File(LocalStorage.baseDir, "font")

    private val builtInFontFiles: Map<String, File> by lazy {
        val map = mutableMapOf<String, File>()
        if (fontDirectory.exists() && fontDirectory.isDirectory) {
            fontDirectory.walkTopDown()
                .filter { it.isFile && it.extension.lowercase() in listOf("ttf", "ttc", "otf") }
                .forEach { file ->
                    val cleanName = file.nameWithoutExtension.replace("-", "_").trim()
                    map[cleanName] = file
                }
        } else {
            println("警告: 内置字体目录不存在 -> ${fontDirectory.absolutePath}")
        }
        map
    }

    actual fun getSystemFonts(): List<FontOption> {
        val fontMgr = FontMgr.default
        val fonts = mutableListOf<FontOption>()
        val chineseKeywords = listOf(
            "宋", "黑", "楷", "明", "仿宋", "隶书", "cjk",
            "song", "kai", "ming", "fang", "sun", "deng", "yahei", "yuanti", "xihei"
        )
        for (i in 0 until fontMgr.familiesCount) {
            val familyName = fontMgr.getFamilyName(i)
            val skiaTypeface = fontMgr.matchFamilyStyle(familyName, FontStyle.NORMAL)
            if (skiaTypeface != null) {
                val hasBasicChinese = skiaTypeface.getUTF32Glyph('中'.code) != 0.toShort() ||
                        skiaTypeface.getUTF32Glyph('文'.code) != 0.toShort()
                val hasExtensionGlyph = skiaTypeface.getUTF32Glyph(0x20000) != 0.toShort()

                val lowerName = familyName.lowercase()
                val hasChineseNameHint = chineseKeywords.any { lowerName.contains(it) }
                if (hasBasicChinese || hasExtensionGlyph || hasChineseNameHint) {
                    fonts.add(FontOption(familyName, familyName, true))
                }
            }
        }
        return fonts.sortedBy { it.displayName }
    }

    actual fun getBuiltInFonts(): List<FontOption> {
        return builtInFontFiles.map { (cleanName, file) ->
            val displayName = getFontDisplayName(file)
            FontOption(id = cleanName, displayName = displayName, isSystemFont = false)
        }.sortedBy { it.displayName }
    }

    fun getFileForBuiltInFont(cleanName: String): File? {
        builtInFontFiles[cleanName]?.let { return it }
        val cN = cleanName.lowercase()

        return when {
            cN.contains("qiji") -> builtInFontFiles["qiji_combo"]
            cN.contains("mina") || cN.contains("hanamina") -> builtInFontFiles["HanaMinA"]
            cN.contains("minb") || cN.contains("hanaminb") -> builtInFontFiles["HanaMinB"]
            cN.contains("kaixinsongb") -> builtInFontFiles["KaixinSongB"]
            cN.contains("kaixinsong") -> builtInFontFiles["KaixinSong"]
            else -> null
        }
    }

    @Composable
    actual fun getFontFamily(fontName: String): FontFamily {
        return remember(fontName) {
            val cleanName = fontName.substringBeforeLast(".").replace("-", "_").trim()

            val file = getFileForBuiltInFont(cleanName)
            if (file != null) {
                return@remember FontFamily(DesktopFont(file))
            }

            val font = fontName.substringBeforeLast(".")
            val typeface = FontMgr.default.matchFamilyStyle(font, FontStyle.NORMAL)
            if (typeface != null) {
                return@remember FontFamily(ComposeTypeface(typeface))
            }
            FontFamily.Default
        }
    }

    @Composable
    actual fun getFontFamily(fontNames: List<String>): FontFamily {
        return remember(fontNames) {
            val desktopFonts = mutableListOf<androidx.compose.ui.text.font.Font>()
            var systemFallback: FontFamily? = null

            fontNames.forEach { name ->
                if (name.isNotBlank()) {
                    val cleanName = name.trim().replace("-", "_")

                    val file = getFileForBuiltInFont(cleanName)
                    if (file != null) {
                        desktopFonts.add(DesktopFont(file))
                    } else if (systemFallback == null) {
                        val fontName = name.substringBeforeLast(".")
                        val typeface = FontMgr.default.matchFamilyStyle(fontName, FontStyle.NORMAL)
                        if (typeface != null) {
                            systemFallback = FontFamily(ComposeTypeface(typeface))
                        }
                    }
                }
            }

            if (desktopFonts.isNotEmpty()) FontFamily(desktopFonts)
            else systemFallback ?: FontFamily.Default
        }
    }

    private fun getFontDisplayName(file: File): String {
        return runCatching {
            val font = Font.createFont(Font.TRUETYPE_FONT, file)
            font.family
        }.getOrNull()?.takeIf { it.isNotBlank() } ?: file.nameWithoutExtension
    }
}

object PDFFontManager {
    private val fontAliasMap = mapOf(
        // 1. Windows 核心基础字体
        "仿宋" to "FangSong",
        "黑体" to "SimHei",
        "宋体" to "SimSun",
        "新宋体" to "NSimSun",
        "楷体" to "KaiTi",
        "微软雅黑" to "Microsoft YaHei",
        "等线" to "DengXian",

        // 2. 华文系列 (Office 及 macOS 常备)
        "华文楷体" to "STKaiti",
        "华文行楷" to "STXingkai",
        "华文仿宋" to "STFangsong",
        "华文宋体" to "STSong",
        "华文细黑" to "STXihei",
        "华文彩云" to "STCaiyun",
        "华文琥珀" to "STHupo",
        "华文隶书" to "STLiti",
        "华文新魏" to "STXinwei",
        "华文中宋" to "STZhongsong",
        "华文黑体" to "STHeiti",

        // 3. 其他 Windows 经典常用字体
        "隶书" to "LiSu",
        "幼圆" to "YouYuan",
        "方正舒体" to "FZShuTi",
        "方正姚体" to "FZYaoti",

        // 4. macOS 独有常用中文字体
        "苹方-简" to "PingFang SC",
        "苹方-繁" to "PingFang TC",
        "冬青黑体" to "Hiragino Sans GB",

        // 5. 常见开源/跨平台字体
        "思源黑体" to "Source Han Sans SC",
        "思源宋体" to "Source Han Serif SC",
        "文泉驿微米黑" to "WenQuanYi Micro Hei"
    )

    /** 获取系统字体 */
    fun loadSystemFont(familyName: String): TrueTypeFont? {
        val realName = fontAliasMap[familyName] ?: familyName
        val mapping = FontMappers.instance().getTrueTypeFont(realName, null)
        return if (mapping != null && !mapping.isFallback) {
            mapping.font
        } else {
            null
        }
    }
}