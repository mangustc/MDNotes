package com.mangustc.mdnotes

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import com.mangustc.mdnotes.koin.initKoin
import com.mangustc.mdnotes.ui.viewmodel.AppViewModel
import io.github.vinceglb.filekit.FileKit
import org.koin.compose.viewmodel.koinViewModel

fun main() {
    FileKit.init(appId = "MDNotes")

    initKoin()

    application {
        Window(
            onCloseRequest = ::exitApplication,
            title = "MDNotes",
        ) {
            val viewModel = koinViewModel<AppViewModel>()
            App(viewModel = viewModel)
        }
    }
}
