package com.mangustc.mdnotes.ui.util

import androidx.compose.ui.window.DialogProperties

class DesktopFullscreenDialogProperties : FullscreenDialogProperties {
    override val dialogProperties: DialogProperties
        get() = DialogProperties(
            usePlatformDefaultWidth = false,
        )
}
