package com.mangustc.mdnotes.koin.modules

import com.mangustc.mdnotes.ui.viewmodel.AppViewModel
import com.mangustc.mdnotes.ui.viewmodel.actions.DrawerActions
import com.mangustc.mdnotes.ui.viewmodel.actions.EditorActions
import com.mangustc.mdnotes.ui.viewmodel.actions.MessengerActions
import com.mangustc.mdnotes.ui.viewmodel.actions.ProjectActions
import com.mangustc.mdnotes.ui.viewmodel.actions.SettingsActions
import org.koin.dsl.module
import org.koin.plugin.module.dsl.factory
import org.koin.plugin.module.dsl.viewModel

val viewModelModule = module {
    factory<ProjectActions.Factory>()
    factory<SettingsActions.Factory>()
    factory<DrawerActions.Factory>()
    factory<EditorActions.Factory>()
    factory<MessengerActions.Factory>()
    viewModel<AppViewModel>()
}
