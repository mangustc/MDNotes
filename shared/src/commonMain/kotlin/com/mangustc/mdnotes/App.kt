package com.mangustc.mdnotes

import androidx.compose.runtime.Composable
import coil3.ImageLoader
import coil3.compose.setSingletonImageLoaderFactory
import com.mangustc.mdnotes.ui.AppScaffold
import com.mangustc.mdnotes.ui.MdnotesTheme
import com.mangustc.mdnotes.ui.viewmodel.AppViewModel
import io.github.vinceglb.filekit.coil.addPlatformFileSupport

@Composable
fun App(viewModel: AppViewModel) {
    setSingletonImageLoaderFactory { context ->
        ImageLoader.Builder(context)
            .components {
                addPlatformFileSupport()
            }
            .build()
    }

    MdnotesTheme(darkTheme = true) {
        AppScaffold(appViewModel = viewModel)
    }
}