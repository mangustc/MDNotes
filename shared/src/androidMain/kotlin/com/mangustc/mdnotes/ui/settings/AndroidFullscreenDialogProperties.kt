package com.mangustc.mdnotes.ui.settings

import androidx.compose.ui.window.DialogProperties
import com.mangustc.mdnotes.ui.util.FullscreenDialogProperties

class AndroidFullscreenDialogProperties : FullscreenDialogProperties {
    override val dialogProperties: DialogProperties
        get() = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false,
        )
}