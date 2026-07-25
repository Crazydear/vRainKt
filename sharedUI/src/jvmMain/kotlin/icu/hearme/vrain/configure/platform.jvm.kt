package icu.hearme.vrain.configure

import androidx.compose.foundation.VerticalScrollbar
import androidx.compose.foundation.rememberScrollbarAdapter

actual fun isDesktopPlatform(): Boolean = true

@androidx.compose.runtime.Composable
actual fun PlatformScrollbar(
    scrollState: androidx.compose.foundation.ScrollState,
    modifier: androidx.compose.ui.Modifier
) {
    VerticalScrollbar(
        modifier = modifier,
        adapter = rememberScrollbarAdapter(scrollState)
    )
}