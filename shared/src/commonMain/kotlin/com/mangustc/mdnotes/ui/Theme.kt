package com.mangustc.mdnotes.ui

import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.expressiveLightColorScheme
import androidx.compose.runtime.Composable

val Typography = Typography()

val DarkColorScheme = darkColorScheme()

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
val LightColorScheme = expressiveLightColorScheme()

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun MdnotesTheme(
    darkTheme: Boolean,
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialExpressiveTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content,
    )
}