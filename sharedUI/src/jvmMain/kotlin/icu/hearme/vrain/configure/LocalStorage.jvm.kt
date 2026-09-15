package icu.hearme.vrain.configure

import java.awt.Desktop
import java.awt.FileDialog
import java.awt.Frame
import java.io.File
import java.nio.charset.Charset
import javax.swing.JFileChooser
import javax.swing.UIManager
import javax.swing.filechooser.FileNameExtensionFilter
import kotlin.getValue

actual object LocalStorage {
    val baseDir = System.getProperty("compose.application.resources.dir")
        ?: System.getProperty("user.dir")
    private val cfgDir by lazy {
        File(baseDir, "data/cfg").apply {
            if (!exists()) mkdirs()
        }
    }

    actual fun saveText(fileName: String, content: String) {
        File(cfgDir, fileName).writeText(content)
    }

    actual fun saveText(file: File, content: String) {
        if (file.exists() && file.canWrite()) {
            file.writeText(content)
        }
    }

    actual fun readText(fileName: String): String? {
        val file = File(cfgDir, fileName)
        return readText(file)
    }

    actual fun readText(file: File): String? {
        return if (file.exists()) {
            try {
                file.readText(Charsets.UTF_8)
            } catch (e: Exception) {
                try {
                    file.readText(Charset.forName("GBK"))
                } catch (e2: Exception) {
                    file.readText(Charset.defaultCharset())
                }
            }
        } else {
            null
        }
    }

    actual fun listFiles(prefix: String): List<String> {
        return cfgDir.listFiles()
            ?.filter { it.isFile && it.name.startsWith(prefix) }
            ?.map { it.name }
            ?: emptyList()
    }

    actual fun exportCfg(defaultName: String, fileContent: String, extension: String) {
        java.awt.EventQueue.invokeLater {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName())
            } catch (e: Exception) {
                e.printStackTrace()
            }
            val cfgFilter = FileNameExtensionFilter("vRain配置文件 (*.cfg)", "cfg")
            val txtFilter = FileNameExtensionFilter("文本文件 (*.txt)", "txt")

            val chooser = JFileChooser().apply {
                dialogTitle = "另存为..."
                selectedFile = File(defaultName)

                addChoosableFileFilter(cfgFilter)
                addChoosableFileFilter(txtFilter)
                fileFilter = if (extension == "cfg") cfgFilter else txtFilter
            }

            val result = chooser.showSaveDialog(null)

            if (result == JFileChooser.APPROVE_OPTION) {
                var targetFile = chooser.selectedFile

                val hasValidExtension = targetFile.name.endsWith(".cfg", ignoreCase = true) ||
                        targetFile.name.endsWith(".txt", ignoreCase = true)

                if (!hasValidExtension) {
                    val extension = if (chooser.fileFilter == txtFilter) ".txt" else ".cfg"
                    targetFile = File(targetFile.parent, "${targetFile.name}$extension")
                }

                try {
                    targetFile.writeText(fileContent)
                    println("文件成功导出至: ${targetFile.absolutePath}")
                    openAndHighlightFile(targetFile)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    private fun openAndHighlightFile(file: File) {
        if (!file.exists()) return
        val os = System.getProperty("os.name").lowercase()

        try {
            when {
                os.contains("win") -> {
                    Runtime.getRuntime().exec(arrayOf("explorer.exe", "/select,${file.absolutePath}"))
                }
                os.contains("mac") -> {
                    Runtime.getRuntime().exec(arrayOf("open", "-R", file.absolutePath))
                }
                else -> {
                    if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.OPEN)) {
                        Desktop.getDesktop().open(file.parentFile)
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            try {
                if (Desktop.isDesktopSupported()) {
                    Desktop.getDesktop().open(file.parentFile)
                }
            } catch (ex: Exception) {
                ex.printStackTrace()
            }
        }
    }

    actual fun chooseFiles(title: String, allowedExtensions: List<String>, isMultiple: Boolean): List<File> {
        val hiddenFrame = Frame()
        val fileDialog = FileDialog(hiddenFrame, title, FileDialog.LOAD).apply {
            isMultipleMode = isMultipleMode

            setFilenameFilter { _, name ->
                val ext = name.substringAfterLast(".", "").lowercase()
                allowedExtensions.isEmpty() || allowedExtensions.contains(ext)
            }
            isVisible = true
        }
        val selectedFiles = fileDialog.files.toList()
        hiddenFrame.dispose()
        return selectedFiles
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