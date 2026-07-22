package icu.hearme.vrain.configure

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
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

    private val jsonFull = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
        isLenient = true
        encodeDefaults = true   // 👈强制序列化所有字段，即使它等于默认值
    }

    private val configCache = mutableMapOf<String, CanvasConfigData>()

    fun loadFromJson(jsonStr: String): CanvasConfigData {
        if (jsonStr.isBlank()) return CanvasConfigData()

        return try {
            json.decodeFromString(jsonStr)
        } catch (e: Exception){
            e.printStackTrace()
            CanvasConfigData()
        }
    }

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
    suspend fun loadConfig(configName: String): CanvasConfigData {
        if (configName.isBlank()) return CanvasConfigData()
        configCache[configName]?.let { return it }

        return withContext(Dispatchers.IO) {
            try {
                val jsonString = if (configName.startsWith(USER_PREFIX)){
                    LocalStorage.readText(configName) ?: ""
                } else {
                    val bytes = Res.readBytes("files/cfg/$configName")
                    bytes.decodeToString()
                }
                val configData: CanvasConfigData = loadFromJson(jsonString) { CanvasConfigData() }
                configCache[configName] = configData
                configData
            } catch (e: Exception) {
                e.printStackTrace()
                CanvasConfigData()
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

    fun convertToCfg(data: CanvasConfigData): String {
        val jsonObject = jsonFull.encodeToJsonElement(CanvasConfigData.serializer(), data).jsonObject
        val formatter = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        val readableTime = formatter.format(Date())

        val sb = StringBuilder()
        sb.append("# 自定义样式配置文件\n")
        sb.append("# 生成时间: ${readableTime}\n")
        sb.append("# 由vRainKt生成，项目地址：https://github.com/Crazydear/vRainKt \n\n")

        jsonObject.forEach { (key, jsonElement) ->
            if (key=="is_single_page") return sb.toString()
            var valueStr = jsonElement.jsonPrimitive.content.replace("null", "")
            valueStr = valueStr.removePrefix("bundle://img/")
            valueStr = valueStr.removeSuffix(".0")
            sb.append("$key=$valueStr\n")
        }
        return sb.toString()
    }

    fun clearCache() {
        configCache.clear()
    }
}