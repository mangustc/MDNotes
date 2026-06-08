package com.mangustc.mdnotes.koin.modules

import com.mangustc.mdnotes.ui.util.DateFormatter
import com.mangustc.mdnotes.ui.util.DesktopDateFormatter
import com.mangustc.mdnotes.ui.util.DesktopFullscreenDialogProperties
import com.mangustc.mdnotes.ui.util.DesktopRememberCameraLauncher
import com.mangustc.mdnotes.ui.util.FullscreenDialogProperties
import com.mangustc.mdnotes.ui.util.RememberCameraLauncher
import org.koin.core.module.Module
import org.koin.dsl.bind
import org.koin.dsl.module
import org.koin.plugin.module.dsl.single

actual val uiModule: Module
    get() = module {
        single<DesktopFullscreenDialogProperties>() bind FullscreenDialogProperties::class
        single<DesktopDateFormatter>() bind DateFormatter::class
        single<DesktopRememberCameraLauncher>() bind RememberCameraLauncher::class
    }
