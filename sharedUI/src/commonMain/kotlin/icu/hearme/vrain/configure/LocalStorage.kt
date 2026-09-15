package icu.hearme.vrain.configure

import java.io.File

expect object LocalStorage {
    fun saveText(fileName: String, content: String)

    fun saveText(file: File, content: String)

    fun readText(fileName: String): String?

    fun readText(file: File): String?

    fun listFiles(prefix: String): List<String>

    fun exportCfg(defaultName: String, fileContent: String, extension: String = "cfg")

    fun chooseFiles(title: String = "请选择文件", allowedExtensions: List<String> = listOf("txt", "md", "att"), isMultiple: Boolean = true): List<File>

    fun pickAndReadTextFile(onSuccess: (String) -> Unit, onError: (Throwable) -> Unit = {})
}