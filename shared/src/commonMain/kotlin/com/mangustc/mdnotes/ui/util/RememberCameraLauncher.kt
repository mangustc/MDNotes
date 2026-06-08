package com.mangustc.mdnotes.ui.util

import androidx.compose.runtime.Composable
import com.mangustc.mdnotes.domain.models.DomainFile

interface RememberCameraLauncher {
    interface CameraLauncher {
        fun launch()
    }

    @Composable
    fun rememberCameraLauncher(onResult: (DomainFile?) -> Unit): CameraLauncher?
}
