package com.mangustc.mdnotes

import androidx.compose.runtime.Composable
import coil3.ImageLoader
import coil3.compose.setSingletonImageLoaderFactory
import com.mangustc.mdnotes.ui.AppScaffold
import com.mangustc.mdnotes.ui.MdnotesTheme
import io.github.vinceglb.filekit.coil.addPlatformFileSupport

@Composable
fun App() {
    setSingletonImageLoaderFactory { context ->
        ImageLoader.Builder(context)
            .components {
                addPlatformFileSupport()
            }
            .build()
    }

    MdnotesTheme(darkTheme = false) {
        AppScaffold()
    }
}
