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

    actual fun saveText(file: File, content: String) {
        if (file.exists() && file.canWrite()) {
            file.writeText(content)
        }
    }

    actual fun readText(fileName: String): String? {
        val file = File(baseDir, fileName)
        return if (file.exists()) file.readText() else null
    }

    actual fun readText(file: File): String? {
        return if (file.exists()) file.readText() else null
    }

    actual fun listFiles(prefix: String): List<String> {
        return baseDir.listFiles()
            ?.filter { it.isFile && it.name.startsWith(prefix) }
            ?.map { it.name }
            ?: emptyList()
    }

    actual fun exportCfg(defaultName: String, fileContent: String, extension: String) {
    }

    actual fun chooseFiles(title: String, allowedExtensions: List<String>, isMultiple: Boolean): List<File> {
        return emptyList()
    }

    actual fun pickAndReadTextFile(onSuccess: (String) -> Unit, onError: (Throwable) -> Unit) {
        try {
            val file = chooseFiles(isMultiple = false).first()
            val content = readText(file) ?: ""
            onSuccess(content)
        } catch (e: Throwable) {
            onError(e)
        }
    }
}