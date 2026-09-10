package icu.hearme.vrain.configure

import androidx.compose.foundation.VerticalScrollbar
import androidx.compose.foundation.rememberScrollbarAdapter
import androidx.compose.runtime.Composable

actual fun isDesktopPlatform(): Boolean = true

@Composable
actual fun PlatformScrollbar(
    scrollState: androidx.compose.foundation.ScrollState,
    modifier: androidx.compose.ui.Modifier
) {
    VerticalScrollbar(
        modifier = modifier,
        adapter = rememberScrollbarAdapter(scrollState)
    )
}