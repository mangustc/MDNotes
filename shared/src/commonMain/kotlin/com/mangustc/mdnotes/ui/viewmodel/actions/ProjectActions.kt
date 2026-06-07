package com.mangustc.mdnotes.ui.viewmodel.actions

import com.mangustc.mdnotes.domain.models.FileSystemPath
import com.mangustc.mdnotes.domain.usecases.project.LoadSavedProjectUseCase
import com.mangustc.mdnotes.domain.usecases.project.SelectProjectInput
import com.mangustc.mdnotes.domain.usecases.project.SelectProjectUseCase
import com.mangustc.mdnotes.domain.usecases.settings.GetSettingsInput
import com.mangustc.mdnotes.domain.usecases.settings.GetSettingsUseCase
import com.mangustc.mdnotes.domain.usecases.sync.SyncProjectInput
import com.mangustc.mdnotes.domain.usecases.sync.SyncProjectUseCase
import com.mangustc.mdnotes.domain.usecases.sync.ValidSyncProvider
import com.mangustc.mdnotes.ui.util.runUseCase
import com.mangustc.mdnotes.ui.viewmodel.AppDeps
import com.mangustc.mdnotes.ui.viewmodel.events.NotificationEvent
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class ProjectActions(
    private val deps: AppDeps,
) : KoinComponent {
    private val syncProjectUseCase: SyncProjectUseCase by inject()
    private val loadSavedProjectUseCase: LoadSavedProjectUseCase by inject()
    private val selectProjectUseCase: SelectProjectUseCase by inject()
    private val getSettingsUseCase: GetSettingsUseCase by inject()

    fun onProjectSelected(projectPath: FileSystemPath) {
        deps.scope.launch {
            val project = runUseCase(deps.globalActions::onEvent) {
                selectProjectUseCase(
                    SelectProjectInput(
                        projectPath = projectPath,
                    ),
                )
            }.getOrElse { return@launch }
            val settings = runUseCase(deps.globalActions::onEvent) {
                getSettingsUseCase(
                    GetSettingsInput(
                        project = project,
                    ),
                )
            }.getOrElse { return@launch }
            deps.uiState.update { it.copy(project = project, settings = settings) }
            deps.globalActions.updateNoteLists()
        }
    }

    fun loadSavedProject() {
        deps.scope.launch {
            val project = runUseCase(deps.globalActions::onEvent) {
                loadSavedProjectUseCase(Unit)
            }.getOrElse { return@launch }
            if (project != null) {
                val settings = runUseCase(deps.globalActions::onEvent) {
                    getSettingsUseCase(
                        GetSettingsInput(
                            project = project,
                        ),
                    )
                }.getOrElse { return@launch }
                deps.uiState.update { it.copy(project = project, settings = settings) }
                deps.globalActions.updateNoteLists()
            } else {
                deps.uiState.update { it.copy(messengerIsLoading = false) }
            }
        }
    }

    private var syncJob: Job? = null
    fun syncNow() {
        if (syncJob?.isActive == true) return

        syncJob = deps.scope.launch {
            val project = deps.uiState.value.project ?: return@launch
            val settings = deps.uiState.value.settings ?: return@launch

            try {
                if (settings.syncProvider == ValidSyncProvider.NONE) {
                    deps.globalActions.onEvent(NotificationEvent.SyncServiceIsNone)
                    return@launch
                }

                deps.uiState.update { it.copy(isSyncInProgress = true) }
                runUseCase(deps.globalActions::onEvent) {
                    syncProjectUseCase(
                        SyncProjectInput(
                            project = project,
                            settings = settings,
                        ),
                    )
                }.getOrElse { return@launch }
            } finally {
                deps.uiState.update { it.copy(isSyncInProgress = false) }
                deps.globalActions.updateNoteLists()
            }
        }
    }
}