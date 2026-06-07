package com.mangustc.mdnotes.koin.modules

import com.mangustc.mdnotes.ui.viewmodel.AppViewModel
import org.koin.dsl.module
import org.koin.plugin.module.dsl.viewModel

val viewModelModule = module {
    viewModel<AppViewModel>()
}
