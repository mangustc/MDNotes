package com.mangustc.mdnotes

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.mangustc.mdnotes.ui.AppScaffold
import com.mangustc.mdnotes.ui.MdnotesTheme
import com.mangustc.mdnotes.ui.viewmodel.AppViewModel
import org.jetbrains.compose.resources.painterResource

import mdnotes.shared.generated.resources.Res
import mdnotes.shared.generated.resources.compose_multiplatform

@Composable
fun App(viewModel: AppViewModel) {
    MdnotesTheme(darkTheme = false) {
        AppScaffold(appViewModel = viewModel)
    }
}