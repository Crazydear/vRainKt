@file:Suppress("EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING")

package icu.hearme.vrain.configure

import java.awt.Desktop
import java.io.File
import javax.swing.JFileChooser
import javax.swing.UIManager
import javax.swing.filechooser.FileNameExtensionFilter
import kotlin.getValue

actual object LocalStorage {
    private val baseDir by lazy {
        File(System.getProperty("user.home"), ".vRain/cfg").apply {
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
        java.awt.EventQueue.invokeLater {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName())
            } catch (e: Exception) {
                e.printStackTrace()
            }

            val chooser = JFileChooser().apply {
                dialogTitle = "另存为..."
                selectedFile = File(defaultName)

                val cfgFilter = FileNameExtensionFilter("vRain配置文件 (*.cfg)", "cfg")
                fileFilter = cfgFilter
            }

            val result = chooser.showSaveDialog(null)

            if (result == JFileChooser.APPROVE_OPTION) {
                var targetFile = chooser.selectedFile

                if (!targetFile.name.endsWith(".cfg", ignoreCase = true)) {
                    targetFile = File(targetFile.parent, "${targetFile.name}.cfg")
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
//                    Runtime.getRuntime().exec("explorer.exe /select,${file.absolutePath}")
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
}