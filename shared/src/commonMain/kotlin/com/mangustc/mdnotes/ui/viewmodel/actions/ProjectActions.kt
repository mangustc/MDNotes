package com.mangustc.mdnotes.ui.viewmodel.actions

import com.mangustc.mdnotes.domain.models.DomainFile
import com.mangustc.mdnotes.domain.usecases.project.LoadSavedProjectUseCase
import com.mangustc.mdnotes.domain.usecases.project.SelectProjectInput
import com.mangustc.mdnotes.domain.usecases.project.SelectProjectUseCase
import com.mangustc.mdnotes.domain.usecases.project.SyncDatabaseInput
import com.mangustc.mdnotes.domain.usecases.project.SyncDatabaseUseCase
import com.mangustc.mdnotes.domain.usecases.settings.GetSettingsInput
import com.mangustc.mdnotes.domain.usecases.settings.GetSettingsUseCase
import com.mangustc.mdnotes.domain.usecases.sync.SyncProjectInput
import com.mangustc.mdnotes.domain.usecases.sync.SyncProjectUseCase
import com.mangustc.mdnotes.domain.usecases.sync.ValidSyncProvider
import com.mangustc.mdnotes.ui.util.launchSafe
import com.mangustc.mdnotes.ui.util.runUseCase
import com.mangustc.mdnotes.ui.viewmodel.AppDeps
import com.mangustc.mdnotes.ui.viewmodel.events.NotificationEvent
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.update

class ProjectActions(
    private val deps: AppDeps,
    private val syncProjectUseCase: SyncProjectUseCase,
    private val loadSavedProjectUseCase: LoadSavedProjectUseCase,
    private val selectProjectUseCase: SelectProjectUseCase,
    private val getSettingsUseCase: GetSettingsUseCase,
    private val syncDatabaseUseCase: SyncDatabaseUseCase,
) {
    class Factory(
        private val syncProjectUseCase: SyncProjectUseCase,
        private val loadSavedProjectUseCase: LoadSavedProjectUseCase,
        private val selectProjectUseCase: SelectProjectUseCase,
        private val getSettingsUseCase: GetSettingsUseCase,
        private val syncDatabaseUseCase: SyncDatabaseUseCase,
    ) {
        fun create(deps: AppDeps): ProjectActions = ProjectActions(
            deps = deps,
            syncProjectUseCase = syncProjectUseCase,
            loadSavedProjectUseCase = loadSavedProjectUseCase,
            selectProjectUseCase = selectProjectUseCase,
            getSettingsUseCase = getSettingsUseCase,
            syncDatabaseUseCase = syncDatabaseUseCase,
        )
    }

    fun onProjectSelected(projectPath: DomainFile) {
        deps.scope.launchSafe(deps.globalActions::onEvent) {
            val project = selectProjectUseCase(SelectProjectInput(projectPath = projectPath))
            val settings = getSettingsUseCase(GetSettingsInput(project = project))
            deps.uiState.update {
                it.copy(
                    project = project,
                    settings = settings,
                    messengerIsLoading = false,
                )
            }

            syncDatabaseUseCase(SyncDatabaseInput(project = project))
        }
    }

    fun loadSavedProject() {
        deps.scope.launchSafe(deps.globalActions::onEvent) {
            try {
                val project = loadSavedProjectUseCase(Unit) ?: return@launchSafe
                val settings = getSettingsUseCase(GetSettingsInput(project = project))
                deps.uiState.update {
                    it.copy(
                        project = project,
                        settings = settings,
                        messengerIsLoading = false,
                    )
                }

                syncDatabaseUseCase(SyncDatabaseInput(project = project))
            } finally {
                deps.uiState.update { it.copy(messengerIsLoading = false) }
            }
        }
    }

    private var syncJob: Job? = null
    fun syncNow() {
        if (syncJob?.isActive == true) return

        syncJob = deps.scope.launchSafe(deps.globalActions::onEvent) {
            val project = deps.uiState.value.project ?: return@launchSafe
            val settings = deps.uiState.value.settings ?: return@launchSafe

            try {
                if (settings.syncProvider == ValidSyncProvider.NONE) {
                    deps.globalActions.onEvent(NotificationEvent.SyncServiceIsNone)
                    return@launchSafe
                }

                deps.uiState.update { it.copy(isSyncInProgress = true) }
                deps.globalActions.flushEditorSave()
                syncProjectUseCase(
                    SyncProjectInput(
                        project = project,
                        settings = settings,
                    ),
                )
            } finally {
                deps.uiState.update { it.copy(isSyncInProgress = false) }

                runUseCase(deps.globalActions::onEvent) {
                    syncDatabaseUseCase(SyncDatabaseInput(project = project))
                }
            }
        }
    }
}
