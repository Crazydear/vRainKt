package icu.hearme.vrain.configure

expect object LocalStorage {
    fun saveText(fileName: String, content: String)

    fun readText(fileName: String): String?

    fun listFiles(prefix: String): List<String>

    fun exportCfg(defaultName: String, fileContent: String, extension: String = "txt")
}