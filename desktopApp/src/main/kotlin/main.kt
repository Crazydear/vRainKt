import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPlacement
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import java.awt.Dimension
import icu.hearme.vrain.AppNew

fun main() = application {
    System.setOut(java.io.PrintStream(System.out, true, "UTF-8"))
    val windowState = rememberWindowState(WindowPlacement.Maximized)
    Window(
        title = "vRain",
        state = windowState,
        onCloseRequest = ::exitApplication,
    ) {
        window.minimumSize = Dimension(350, 600)
        AppNew()
    }
}

