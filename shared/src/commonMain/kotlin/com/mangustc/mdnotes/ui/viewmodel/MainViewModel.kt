package com.mangustc.mdnotes.ui.viewmodel

import androidx.compose.runtime.snapshotFlow
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.mangustc.mdnotes.domain.models.DomainFile
import com.mangustc.mdnotes.domain.models.FrontMatter
import com.mangustc.mdnotes.domain.models.Note
import com.mangustc.mdnotes.domain.models.Project
import com.mangustc.mdnotes.domain.models.Settings
import com.mangustc.mdnotes.domain.usecases.notes.CreateNoteInput
import com.mangustc.mdnotes.domain.usecases.notes.CreateNoteUseCase
import com.mangustc.mdnotes.domain.usecases.notes.DeleteNoteInput
import com.mangustc.mdnotes.domain.usecases.notes.DeleteNoteUseCase
import com.mangustc.mdnotes.domain.usecases.notes.RenameNoteInput
import com.mangustc.mdnotes.domain.usecases.notes.RenameNoteUseCase
import com.mangustc.mdnotes.domain.usecases.notes.ToggleNoteTagInput
import com.mangustc.mdnotes.domain.usecases.notes.ToggleNoteTagUseCase
import com.mangustc.mdnotes.domain.usecases.project.GetNotesInput
import com.mangustc.mdnotes.domain.usecases.project.GetNotesUseCase
import com.mangustc.mdnotes.domain.usecases.project.LoadSavedProjectUseCase
import com.mangustc.mdnotes.domain.usecases.project.SelectProjectInput
import com.mangustc.mdnotes.domain.usecases.project.SelectProjectUseCase
import com.mangustc.mdnotes.domain.usecases.project.SyncDatabaseInput
import com.mangustc.mdnotes.domain.usecases.project.SyncDatabaseUseCase
import com.mangustc.mdnotes.domain.usecases.search.ApplySearchEventInput
import com.mangustc.mdnotes.domain.usecases.search.ApplySearchEventUseCase
import com.mangustc.mdnotes.domain.usecases.search.SearchEvent
import com.mangustc.mdnotes.domain.usecases.settings.GetSettingsInput
import com.mangustc.mdnotes.domain.usecases.settings.GetSettingsUseCase
import com.mangustc.mdnotes.domain.usecases.settings.SetSettingsInput
import com.mangustc.mdnotes.domain.usecases.settings.SetSettingsUseCase
import com.mangustc.mdnotes.domain.usecases.sync.SyncProjectInput
import com.mangustc.mdnotes.domain.usecases.sync.SyncProjectUseCase
import com.mangustc.mdnotes.domain.usecases.sync.ValidSyncProvider
import com.mangustc.mdnotes.ui.components.ComposeTextState
import com.mangustc.mdnotes.ui.util.launchSafe
import com.mangustc.mdnotes.ui.util.runUseCase
import com.mangustc.mdnotes.ui.viewmodel.events.AppEvent
import com.mangustc.mdnotes.ui.viewmodel.events.NavigationEvent
import com.mangustc.mdnotes.ui.viewmodel.events.NotificationEvent
import com.mangustc.mdnotes.ui.viewmodel.session.AppEventBus
import com.mangustc.mdnotes.ui.viewmodel.session.EditorCoordinator
import com.mangustc.mdnotes.ui.viewmodel.session.ProjectSession
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update

sealed interface ActiveDialog {
    data object None : ActiveDialog
    data class CreateNote(val nameInput: String = "") : ActiveDialog
    data class DeleteNote(val note: Note) : ActiveDialog
    data class RenameNote(val note: Note, val nameInput: String) : ActiveDialog
    data class ShowInfo(val note: Note) : ActiveDialog
    data object Settings : ActiveDialog
}

data class MainUiState(
    val project: Project? = null,
    val settings: Settings? = null,
    val isSyncInProgress: Boolean = false,
    val activeDialog: ActiveDialog = ActiveDialog.None,
    val activeNote: Note? = null,
)

class MainViewModel(
    private val projectSession: ProjectSession,
    private val eventBus: AppEventBus,
    private val editorCoordinator: EditorCoordinator,
    private val syncProjectUseCase: SyncProjectUseCase,
    private val loadSavedProjectUseCase: LoadSavedProjectUseCase,
    private val selectProjectUseCase: SelectProjectUseCase,
    private val getSettingsUseCase: GetSettingsUseCase,
    private val setSettingsUseCase: SetSettingsUseCase,
    private val syncDatabaseUseCase: SyncDatabaseUseCase,
    private val getNotesUseCase: GetNotesUseCase,
    private val createNoteUseCase: CreateNoteUseCase,
    private val deleteNoteUseCase: DeleteNoteUseCase,
    private val renameNoteUseCase: RenameNoteUseCase,
    private val toggleNoteTagUseCase: ToggleNoteTagUseCase,
    private val applySearchEventUseCase: ApplySearchEventUseCase,
) : ViewModel() {

    private val _activeDialog = MutableStateFlow<ActiveDialog>(ActiveDialog.None)

    val navigationEvents = eventBus.navigationEvents
    val notificationEvents = eventBus.notificationEvents
    val clipboardEvents = eventBus.clipboardEvents
    val focusEvents = eventBus.focusEvents

    val searchState = ComposeTextState()

    val uiState: StateFlow<MainUiState> = combine(
        projectSession.project,
        projectSession.settings,
        projectSession.isSyncInProgress,
        _activeDialog,
        editorCoordinator.activeNote,
    ) { project, settings, isSyncInProgress, activeDialog, activeNote ->
        MainUiState(
            project = project,
            settings = settings,
            isSyncInProgress = isSyncInProgress,
            activeDialog = activeDialog,
            activeNote = activeNote,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = MainUiState(
            project = projectSession.project.value,
            settings = projectSession.settings.value,
            isSyncInProgress = projectSession.isSyncInProgress.value,
            activeDialog = _activeDialog.value,
            activeNote = editorCoordinator.activeNote.value,
        ),
    )

    @OptIn(ExperimentalCoroutinesApi::class)
    val searchResultsPaged: Flow<PagingData<Note>> = combine(
        projectSession.project,
        snapshotFlow { searchState.text },
    ) { project, text -> project to text.toString() }
        .flatMapLatest { (project, queryStr) ->
            if (project == null) return@flatMapLatest emptyFlow()

            runUseCase(eventBus::emit) {
                getNotesUseCase(
                    GetNotesInput(
                        project = project,
                        searchQueryString = queryStr,
                    ),
                )
            }.getOrElse { emptyFlow() }
        }
        .cachedIn(viewModelScope)

    init {
        loadSavedProject()
    }

    fun onEvent(event: AppEvent) {
        eventBus.emit(event)
    }

    // --- Project ---

    fun onProjectSelected(projectPath: DomainFile) {
        viewModelScope.launchSafe(eventBus::emit) {
            val project = selectProjectUseCase(SelectProjectInput(projectPath = projectPath))
            val settings = getSettingsUseCase(GetSettingsInput(project = project))
            projectSession.setProject(project, settings)
            projectSession.setProjectLoading(false)

            syncDatabaseUseCase(SyncDatabaseInput(project = project))
        }
    }

    fun loadSavedProject() {
        viewModelScope.launchSafe(eventBus::emit) {
            try {
                val project = loadSavedProjectUseCase(Unit) ?: return@launchSafe
                val settings = getSettingsUseCase(GetSettingsInput(project = project))
                projectSession.setProject(project, settings)
                projectSession.setProjectLoading(false)

                syncDatabaseUseCase(SyncDatabaseInput(project = project))
            } finally {
                projectSession.setProjectLoading(false)
            }
        }
    }

    private var syncJob: Job? = null

    fun syncNow() {
        if (syncJob?.isActive == true) return

        syncJob = viewModelScope.launchSafe(eventBus::emit) {
            val project = projectSession.project.value ?: return@launchSafe
            val settings = projectSession.settings.value ?: return@launchSafe

            try {
                if (settings.syncProvider == ValidSyncProvider.NONE) {
                    eventBus.emit(NotificationEvent.SyncServiceIsNone)
                    return@launchSafe
                }

                projectSession.setSyncInProgress(true)
                editorCoordinator.flushSave()
                syncProjectUseCase(
                    SyncProjectInput(
                        project = project,
                        settings = settings,
                    ),
                )
            } finally {
                projectSession.setSyncInProgress(false)

                runUseCase(eventBus::emit) {
                    syncDatabaseUseCase(SyncDatabaseInput(project = project))
                }
            }
        }
    }

    // --- Settings ---

    fun showSettings() {
        _activeDialog.value = ActiveDialog.Settings
    }

    fun dismissSettings() {
        _activeDialog.value = ActiveDialog.None
    }

    fun openYandexLink() {
        eventBus.emit(
            NavigationEvent.OpenUrl("https://oauth.yandex.ru/authorize?response_type=token&client_id=e3812fd74d90456e8670d8d39d878a5c"),
        )
    }

    fun setReverseLayout(reverseLayout: Boolean) {
        val settings = projectSession.settings.value ?: return
        updateSettings(settings.copy(reverseLayout = reverseLayout))
    }

    fun setSyncProvider(provider: ValidSyncProvider) {
        val settings = projectSession.settings.value ?: return
        updateSettings(settings.copy(syncProvider = provider))
    }

    fun setYandexOauthToken(token: String) {
        val settings = projectSession.settings.value ?: return
        updateSettings(settings.copy(yandexOauthToken = token))
    }

    private fun updateSettings(newSettings: Settings) =
        viewModelScope.launchSafe(eventBus::emit) {
            val project = projectSession.project.value ?: return@launchSafe
            projectSession.setSettings(newSettings)
            setSettingsUseCase(
                SetSettingsInput(
                    project = project,
                    newSettings = newSettings,
                ),
            )
        }

    // --- Drawer & Dialogs ---

    fun onSearchEvent(event: SearchEvent) {
        viewModelScope.launchSafe(eventBus::emit) {
            applySearchEventUseCase(
                ApplySearchEventInput(
                    event = event,
                    state = searchState,
                ),
            )
        }
    }

    fun onNoteSelected(note: Note) {
        eventBus.emit(NavigationEvent.GoToEditor(note = note))
    }

    fun dismissDialog() {
        _activeDialog.value = ActiveDialog.None
    }

    fun showCreateNoteDialog() {
        _activeDialog.value = ActiveDialog.CreateNote()
    }

    fun updateNewNoteName(name: String) {
        _activeDialog.update { current ->
            val dialog = current as? ActiveDialog.CreateNote ?: return@update current
            dialog.copy(nameInput = name)
        }
    }

    fun onCreateNote(name: String) {
        viewModelScope.launchSafe(eventBus::emit) {
            val project = projectSession.project.value ?: return@launchSafe
            val note = createNoteUseCase(
                CreateNoteInput(
                    project = project,
                    name = name,
                    initialText = "# $name\n\n",
                ),
            )
            eventBus.emit(NavigationEvent.GoToEditor(note))
        }
    }

    fun showNoteDeleteDialog(note: Note) {
        _activeDialog.value = ActiveDialog.DeleteNote(note)
    }

    fun onDeleteNote(note: Note) {
        viewModelScope.launchSafe(eventBus::emit) {
            val project = projectSession.project.value ?: return@launchSafe
            val isDeletingActive =
                note.projectFile.relativePath == editorCoordinator.activeNote.value?.projectFile?.relativePath
            if (isDeletingActive) {
                editorCoordinator.onNoteDeleted(note.projectFile.relativePath)
            }
            deleteNoteUseCase(
                DeleteNoteInput(
                    project = project,
                    note = note,
                ),
            )
            if (isDeletingActive) {
                eventBus.emit(NavigationEvent.GoBack)
            }
        }
    }

    fun showNoteShowInfoDialog(note: Note) {
        _activeDialog.value = ActiveDialog.ShowInfo(note)
    }

    fun showNoteRenameDialog(note: Note) {
        _activeDialog.value = ActiveDialog.RenameNote(
            note = note,
            nameInput = note.name,
        )
    }

    fun onRenameNameInputChanged(newName: String) {
        _activeDialog.update { current ->
            val dialog = current as? ActiveDialog.RenameNote ?: return@update current
            dialog.copy(nameInput = newName)
        }
    }

    fun onRenameNote(note: Note, newName: String) {
        viewModelScope.launchSafe(eventBus::emit) {
            val project = projectSession.project.value ?: return@launchSafe
            val oldPath = note.projectFile.relativePath
            if (oldPath == editorCoordinator.activeNote.value?.projectFile?.relativePath) {
                editorCoordinator.flushSave()
            }
            val renamedNote = renameNoteUseCase(
                RenameNoteInput(
                    project = project,
                    note = note,
                    newName = newName,
                ),
            )
            editorCoordinator.onNoteRenamed(oldPath, renamedNote)
        }
    }

    fun onPinNote(note: Note) {
        viewModelScope.launchSafe(eventBus::emit) {
            val project = projectSession.project.value ?: return@launchSafe
            val notePath = note.projectFile.relativePath
            if (notePath == editorCoordinator.activeNote.value?.projectFile?.relativePath) {
                editorCoordinator.flushSave()
            }
            val updatedFrontMatter = toggleNoteTagUseCase(
                ToggleNoteTagInput(
                    project = project,
                    note = note,
                    tag = FrontMatter.PINNED_TAG,
                ),
            )
            editorCoordinator.onNoteFrontMatterUpdated(notePath, updatedFrontMatter)
        }
    }
}
