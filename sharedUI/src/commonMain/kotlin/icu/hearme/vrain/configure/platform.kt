package icu.hearme.vrain.configure

import androidx.compose.foundation.ScrollState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

expect fun isDesktopPlatform(): Boolean

@Composable
expect fun PlatformScrollbar(
    scrollState: ScrollState,
    modifier: Modifier = Modifier
)