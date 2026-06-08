package com.mangustc.mdnotes.ui.util

import androidx.compose.runtime.Composable
import com.mangustc.mdnotes.domain.models.DomainFile

class DesktopRememberCameraLauncher : RememberCameraLauncher {
    @Composable
    override fun rememberCameraLauncher(onResult: (DomainFile?) -> Unit): RememberCameraLauncher.CameraLauncher? =
        null
}