package icu.hearme.vrain.configure

import android.graphics.BitmapFactory
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import java.io.File
import androidx.core.net.toUri

actual fun makeImageBitmapFromBytes(bytes: ByteArray): ImageBitmap {
    return BitmapFactory.decodeByteArray(bytes, 0, bytes.size).asImageBitmap()
}

@Composable
actual fun rememberLocalPlatformImage(pathOrUri: String): ImageBitmap? {
    val context = LocalContext.current
    var bitmapState by remember(pathOrUri) { mutableStateOf<ImageBitmap?>(null) }

    LaunchedEffect(pathOrUri) {
        val bitmap = when {
            pathOrUri.startsWith("sandbox://") -> {
                val file = File(context.filesDir, pathOrUri.removePrefix("sandbox://"))
                if (file.exists()) BitmapFactory.decodeFile(file.absolutePath) else null
            }
            pathOrUri.startsWith("content://") -> {
                context.contentResolver.openInputStream(pathOrUri.toUri()).use { BitmapFactory.decodeStream(it) }
            }
            else -> {
                if (File(pathOrUri).exists()) BitmapFactory.decodeFile(pathOrUri) else null
            }
        }
        bitmapState = bitmap?.asImageBitmap()
    }
    return bitmapState
}