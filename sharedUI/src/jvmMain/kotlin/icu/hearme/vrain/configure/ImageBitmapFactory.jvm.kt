package icu.hearme.vrain.configure

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.res.loadImageBitmap
import org.jetbrains.compose.resources.decodeToImageBitmap
import java.io.File
import java.io.InputStream

actual fun makeImageBitmapFromBytes(bytes: ByteArray): ImageBitmap {
    return bytes.inputStream().use { it.readAllBytes().decodeToImageBitmap() }
}

@Composable
actual fun rememberLocalPlatformImage(pathOrUri: String): ImageBitmap? {
    var bitmapState by remember(pathOrUri) { mutableStateOf<ImageBitmap?>(null) }
    LaunchedEffect(pathOrUri) {
        val cleanPath = pathOrUri.removePrefix("file://")
        val file = File(cleanPath)
        if (file.exists()) {
            bitmapState = file.inputStream().use { it.readAllBytes().decodeToImageBitmap() }
        }
    }
    return bitmapState
}