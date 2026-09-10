package icu.hearme.vrain.bookcanvas

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import icu.hearme.vrain.configure.AncientBookState
import icu.hearme.vrain.configure.AncientCanvasState
import icu.hearme.vrain.engine.BookPage
import icu.hearme.vrain.utils.preViewPdfPage
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.milliseconds

@Composable
fun PdfPagePreviewer(page: BookPage, bookConfig: AncientBookState, canvasConfig: AncientCanvasState, modifier: Modifier = Modifier) {
    var previewBitmap by remember { mutableStateOf<ImageBitmap?>(null) }
    var isRendering by remember { mutableStateOf(false) }

    LaunchedEffect(page, bookConfig.configData, canvasConfig.configData) {
        delay(500.milliseconds)
        isRendering = true
        try {
            previewBitmap = preViewPdfPage(page, bookConfig, canvasConfig)
        } finally {
            isRendering = false
        }
    }

    Box(modifier = modifier.background(Color.DarkGray).fillMaxSize(), contentAlignment = Alignment.Center) {
        previewBitmap?.let {
            Surface(color = Color.White) { Image(bitmap = it, contentDescription = null) }
        }
        if (isRendering) {
            CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
        }
    }
}