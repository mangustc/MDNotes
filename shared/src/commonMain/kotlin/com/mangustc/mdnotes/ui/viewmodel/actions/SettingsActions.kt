package com.mangustc.mdnotes.ui.viewmodel.actions

import com.mangustc.mdnotes.domain.models.Settings
import com.mangustc.mdnotes.domain.usecases.settings.SetSettingsInput
import com.mangustc.mdnotes.domain.usecases.settings.SetSettingsUseCase
import com.mangustc.mdnotes.domain.usecases.sync.ValidSyncProvider
import com.mangustc.mdnotes.ui.util.launchSafe
import com.mangustc.mdnotes.ui.viewmodel.ActiveDialog
import com.mangustc.mdnotes.ui.viewmodel.AppDeps
import com.mangustc.mdnotes.ui.viewmodel.events.NavigationEvent
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class SettingsActions(
    private val deps: AppDeps,
    private val setSettingsUseCase: SetSettingsUseCase,
) {
    class Factory(
        private val setSettingsUseCase: SetSettingsUseCase,
    ) {
        fun create(deps: AppDeps): SettingsActions = SettingsActions(
            deps = deps,
            setSettingsUseCase = setSettingsUseCase,
        )
    }

    fun showSettings() {
        deps.uiState.update { it.copy(activeDialog = ActiveDialog.Settings) }
    }

    fun dismissSettings() {
        deps.uiState.update { it.copy(activeDialog = ActiveDialog.None) }
    }

    fun openYandexLink() {
        deps.scope.launch {
            deps.globalActions.onEvent(event = NavigationEvent.OpenUrl("https://oauth.yandex.ru/authorize?response_type=token&client_id=e3812fd74d90456e8670d8d39d878a5c"))
        }
    }

    fun setReverseLayout(reverseLayout: Boolean) {
        deps.scope.launch {
            val settings = deps.uiState.value.settings ?: return@launch
            val newSettings = settings.copy(
                reverseLayout = reverseLayout,
            )
            updateSettings(newSettings)
        }
    }

    fun setSyncProvider(provider: ValidSyncProvider) {
        deps.scope.launch {
            val settings = deps.uiState.value.settings ?: return@launch
            val newSettings = settings.copy(
                syncProvider = provider,
            )
            updateSettings(newSettings)
        }
    }

    fun setYandexOauthToken(token: String) {
        deps.scope.launch {
            val settings = deps.uiState.value.settings ?: return@launch
            val newSettings = settings.copy(
                yandexOauthToken = token,
            )
            updateSettings(newSettings)
        }
    }

    private fun updateSettings(newSettings: Settings) =
        deps.scope.launchSafe(deps.globalActions::onEvent) {
            val project = deps.uiState.value.project ?: return@launchSafe
            deps.uiState.update { it.copy(settings = newSettings) }
            setSettingsUseCase(
                SetSettingsInput(
                    project = project,
                    newSettings = newSettings,
                ),
            )
        }
}
