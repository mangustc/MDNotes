package com.mangustc.mdnotes.ui

import androidx.compose.runtime.Composable
import com.mangustc.mdnotes.ui.viewmodel.AppViewModel

@Composable
expect fun AppScaffold(appViewModel: AppViewModel)
