package com.mangustc.mdnotes.koin

import com.mangustc.mdnotes.koin.modules.dataModule
import com.mangustc.mdnotes.koin.modules.domainModule
import com.mangustc.mdnotes.koin.modules.uiModule
import com.mangustc.mdnotes.koin.modules.viewModelModule
import org.koin.core.context.startKoin
import org.koin.dsl.KoinAppDeclaration

fun initKoin(appDeclaration: KoinAppDeclaration = {}) = startKoin {
    appDeclaration()
    modules(
        dataModule,
        domainModule,
        uiModule,
        viewModelModule,
    )
}