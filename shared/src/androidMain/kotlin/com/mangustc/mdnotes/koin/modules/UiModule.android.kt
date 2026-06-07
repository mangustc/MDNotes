package com.mangustc.mdnotes.koin.modules

import com.mangustc.mdnotes.ui.util.AndroidDateFormatter
import com.mangustc.mdnotes.ui.util.DateFormatter
import org.koin.core.module.Module
import org.koin.dsl.bind
import org.koin.dsl.module
import org.koin.plugin.module.dsl.single

actual val uiModule: Module
    get() = module {
        single<AndroidDateFormatter>() bind DateFormatter::class
    }