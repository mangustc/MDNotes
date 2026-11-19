package com.mangustc.mdnotes

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import com.mangustc.mdnotes.koin.initKoin
import io.github.vinceglb.filekit.FileKit

fun main() {
    FileKit.init(appId = "MDNotes")

    initKoin()

    application {
        Window(
            onCloseRequest = ::exitApplication,
            title = "MDNotes",
        ) {
            App()
        }
    }
}
