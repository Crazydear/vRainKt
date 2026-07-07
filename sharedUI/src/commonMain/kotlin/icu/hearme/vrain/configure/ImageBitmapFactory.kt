package icu.hearme.vrain.configure

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.ImageBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.compose.resources.ExperimentalResourceApi
import vrain.sharedui.generated.resources.Res

@OptIn(ExperimentalResourceApi::class)
@Composable
fun rememberImageBitmapFromString(pathOrUri: String?): ImageBitmap? {
    if (pathOrUri.isNullOrBlank()) return null

    var bitmapState by remember(pathOrUri) { mutableStateOf<ImageBitmap?>(null) }

    if (!pathOrUri.startsWith("bundle://")) {
        return rememberLocalPlatformImage(pathOrUri)
    }

    LaunchedEffect(pathOrUri) {
        withContext(Dispatchers.IO) {
            try {
                val fileName = pathOrUri.removePrefix("bundle://")
                val resourcePath = "files/$fileName"
                val bytes = Res.readBytes(resourcePath)
                bitmapState = makeImageBitmapFromBytes(bytes)
            } catch (e: Exception) {
                e.printStackTrace()
                bitmapState = null
            }
        }
    }
    return bitmapState
}

expect fun makeImageBitmapFromBytes(bytes: ByteArray): ImageBitmap

@Composable
expect fun rememberLocalPlatformImage(pathOrUri: String): ImageBitmap?