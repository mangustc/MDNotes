package com.mangustc.mdnotes.koin.modules

import com.mangustc.mdnotes.ui.settings.AndroidFullscreenDialogProperties
import com.mangustc.mdnotes.ui.util.AndroidDateFormatter
import com.mangustc.mdnotes.ui.util.AndroidRememberCameraLauncher
import com.mangustc.mdnotes.ui.util.DateFormatter
import com.mangustc.mdnotes.ui.util.FullscreenDialogProperties
import com.mangustc.mdnotes.ui.util.RememberCameraLauncher
import org.koin.core.module.Module
import org.koin.dsl.bind
import org.koin.dsl.module
import org.koin.plugin.module.dsl.single

actual val uiModule: Module
    get() = module {
        single<AndroidFullscreenDialogProperties>() bind FullscreenDialogProperties::class
        single<AndroidDateFormatter>() bind DateFormatter::class
        single<AndroidRememberCameraLauncher>() bind RememberCameraLauncher::class
    }