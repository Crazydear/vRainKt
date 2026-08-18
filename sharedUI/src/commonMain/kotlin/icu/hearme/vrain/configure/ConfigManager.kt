package icu.hearme.vrain.configure

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.encodeToJsonElement
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.jetbrains.compose.resources.ExperimentalResourceApi
import vrain.sharedui.generated.resources.Res
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Serializable
data class ConfigIndex(val canvasConfigs: List<ConfigMeta>)

@Serializable
data class ConfigMeta(val fileName: String, val displayName: String, val isUserCustom: Boolean = false)

object ConfigManager {
    const val USER_PREFIX = "user_cfg_"
    val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
        isLenient = true
    }

    val jsonFull = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
        isLenient = true
        encodeDefaults = true   // 👈强制序列化所有字段，即使它等于默认值
    }

    private val configCache = mutableMapOf<String, CanvasConfigData>()

    inline fun <reified T> loadFromJson(jsonStr: String, fallback: () -> T): T {
        if (jsonStr.isBlank()) return fallback()

        return try {
            json.decodeFromString<T>(jsonStr)
        } catch (e: Exception) {
            e.printStackTrace()
            fallback()
        }
    }

    @OptIn(ExperimentalResourceApi::class)
    suspend fun fetchConfigList(): List<ConfigMeta> {
        return withContext(Dispatchers.IO) {
            try {
                val bytes = Res.readBytes("files/cfg/index.json")
                val jsonString = bytes.decodeToString()
                val index = json.decodeFromString<ConfigIndex>(jsonString)
                index.canvasConfigs
            } catch (e: Exception) {
                e.printStackTrace()
                emptyList()
            }
        }
    }

    @OptIn(ExperimentalResourceApi::class)
    suspend fun loadConfig(configName: String): String {
        if (configName.isBlank()) return "{}"

        return withContext(Dispatchers.IO) {
            try {
                val jsonString = if (configName.startsWith(USER_PREFIX)){
                    LocalStorage.readText(configName) ?: ""
                } else {
                    val bytes = Res.readBytes("files/cfg/$configName")
                    bytes.decodeToString()
                }
                jsonString

            } catch (e: Exception) {
                e.printStackTrace()
                "{}"
            }
        }
    }

    suspend fun saveUserConfig(data: CanvasConfigData, styleName: String? = null, compress: Boolean = true): ConfigMeta {
        return withContext(Dispatchers.IO) {
            val timestamp = styleName ?: System.currentTimeMillis()
            val fileName = "$USER_PREFIX$timestamp.json"
            val jsonString = if (compress) {
                json.encodeToString(data)
            } else {
                jsonFull.encodeToString(data)
            }
            LocalStorage.saveText(fileName, jsonString)
            configCache[fileName] = data

            ConfigMeta(
                fileName = fileName,
                displayName = styleName ?: "自定义样式_$timestamp",
                isUserCustom = true
            )
        }
    }

    suspend fun fetchUserConfigList(): List<ConfigMeta> {
        return withContext(Dispatchers.IO) {
            try {
                val fileNames = LocalStorage.listFiles(USER_PREFIX)
                fileNames.map { fileName ->
                    val timePart = fileName.removePrefix(USER_PREFIX).removeSuffix(".json")
                    ConfigMeta(
                        fileName = fileName,
                        displayName = "自定义样式_$timePart",
                        isUserCustom = true
                    )
                }.sortedByDescending { it.fileName }
            } catch (e: Exception) {
                e.printStackTrace()
                emptyList()
            }
        }
    }

    inline fun <reified T> convertToCfg(data: T): String {
         val jsonObject = jsonFull.encodeToJsonElement(data).jsonObject

        val readableTime = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())

        val sb = StringBuilder().apply {
            append("# 自定义样式配置文件\n")
            append("# 生成时间: $readableTime\n")
            append("# 由vRainKt生成，项目地址：https://github.com/Crazydear/vRainKt \n\n")

            for ((key, jsonElement) in jsonObject) {
                if (key == "is_single_page") return@apply
                var valueStr = jsonElement.jsonPrimitive.content.replace("null", "")
                valueStr = valueStr.removePrefix("bundle://img/").removeSuffix(".0")
                append("$key=$valueStr\n")
            }
        }
        return sb.toString()
    }

    fun clearCache() {
        configCache.clear()
    }
}