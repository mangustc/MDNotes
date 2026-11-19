package com.mangustc.mdnotes.koin.modules

import com.mangustc.mdnotes.ui.viewmodel.EditorViewModel
import com.mangustc.mdnotes.ui.viewmodel.MainViewModel
import com.mangustc.mdnotes.ui.viewmodel.MessengerViewModel
import com.mangustc.mdnotes.ui.viewmodel.session.AppEventBus
import com.mangustc.mdnotes.ui.viewmodel.session.EditorCoordinator
import com.mangustc.mdnotes.ui.viewmodel.session.ProjectSession
import org.koin.dsl.module
import org.koin.plugin.module.dsl.single
import org.koin.plugin.module.dsl.viewModel

val viewModelModule = module {
    single<AppEventBus>()
    single<ProjectSession>()
    single<EditorCoordinator>()

    viewModel<MainViewModel>()
    viewModel<MessengerViewModel>()
    viewModel<EditorViewModel>()
}
