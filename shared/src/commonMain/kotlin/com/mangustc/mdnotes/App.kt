package com.mangustc.mdnotes

import androidx.compose.runtime.Composable
import com.mangustc.mdnotes.ui.AppScaffold
import com.mangustc.mdnotes.ui.MdnotesTheme
import com.mangustc.mdnotes.ui.viewmodel.AppViewModel

@Composable
fun App(viewModel: AppViewModel) {
    MdnotesTheme(darkTheme = false) {
        AppScaffold(appViewModel = viewModel)
    }
}