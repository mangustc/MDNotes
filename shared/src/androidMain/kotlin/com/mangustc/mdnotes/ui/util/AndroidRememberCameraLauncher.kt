package com.mangustc.mdnotes.ui.util

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.mangustc.mdnotes.domain.models.DomainFile
import io.github.vinceglb.filekit.dialogs.compose.rememberCameraPickerLauncher

class AndroidRememberCameraLauncher : RememberCameraLauncher {
    @Composable
    override fun rememberCameraLauncher(onResult: (DomainFile?) -> Unit): RememberCameraLauncher.CameraLauncher {
        val launcher = rememberCameraPickerLauncher { file ->
            onResult(if (file != null) DomainFile(file) else null)
        }
        return remember(launcher) {
            object : RememberCameraLauncher.CameraLauncher {
                override fun launch() {
                    launcher.launch()
                }
            }
        }
    }
}