package icu.hearme.vrain.configure

import androidx.compose.foundation.ScrollState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

actual fun isDesktopPlatform(): Boolean = false

@Composable
actual fun PlatformScrollbar(
    scrollState: ScrollState,
    modifier: Modifier
) {
}