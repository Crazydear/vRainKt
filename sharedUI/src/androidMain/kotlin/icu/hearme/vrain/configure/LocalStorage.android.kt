package icu.hearme.vrain.configure

import android.annotation.SuppressLint
import android.content.Context
import java.io.File

@SuppressLint("StaticFieldLeak")
object AndroidAppProvider {
    lateinit var context: Context
}

actual object LocalStorage {
    private val baseDir by lazy {
        File(AndroidAppProvider.context.filesDir, "cfg").apply {
            if (!exists()) mkdirs()
        }
    }

    actual fun saveText(fileName: String, content: String) {
        File(baseDir, fileName).writeText(content)
    }

    actual fun readText(fileName: String): String? {
        val file = File(baseDir, fileName)
        return if (file.exists()) file.readText() else null
    }

    actual fun listFiles(prefix: String): List<String> {
        return baseDir.listFiles()
            ?.filter { it.isFile && it.name.startsWith(prefix) }
            ?.map { it.name }
            ?: emptyList()
    }

    actual fun exportCfg(defaultName: String, fileContent: String) {
    }

}