package com.mangustc.mdnotes.ui.settings

import androidx.compose.ui.window.DialogProperties

class AndroidSettingsDialogProperties : SettingsDialogProperties {
    override val dialogProperties: DialogProperties
        get() = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false,
        )
}