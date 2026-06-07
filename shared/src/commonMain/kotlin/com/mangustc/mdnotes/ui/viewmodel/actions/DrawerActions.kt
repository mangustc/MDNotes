package com.mangustc.mdnotes.ui.viewmodel.actions

import androidx.compose.runtime.snapshotFlow
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.mangustc.mdnotes.domain.models.FrontMatter
import com.mangustc.mdnotes.domain.models.Note
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
import com.mangustc.mdnotes.domain.usecases.search.ApplySearchEventInput
import com.mangustc.mdnotes.domain.usecases.search.ApplySearchEventUseCase
import com.mangustc.mdnotes.domain.usecases.search.SearchEvent
import com.mangustc.mdnotes.ui.components.ComposeTextState
import com.mangustc.mdnotes.ui.util.runUseCase
import com.mangustc.mdnotes.ui.viewmodel.AppDeps
import com.mangustc.mdnotes.ui.viewmodel.events.NavigationEvent
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class DrawerActions(
    private val deps: AppDeps,
) : KoinComponent {
    private val getNotesUseCase: GetNotesUseCase by inject()
    private val createNoteUseCase: CreateNoteUseCase by inject()
    private val deleteNoteUseCase: DeleteNoteUseCase by inject()
    private val renameNoteUseCase: RenameNoteUseCase by inject()
    private val toggleNoteTagUseCase: ToggleNoteTagUseCase by inject()
    private val applySearchEventUseCase: ApplySearchEventUseCase by inject()

    val searchState = ComposeTextState()

    @OptIn(ExperimentalCoroutinesApi::class)
    val searchResultsPaged: Flow<PagingData<Note>> = combine(
        deps.uiState.map { it.project }.distinctUntilChanged(),
        snapshotFlow { searchState.text },
    ) { project, text -> project to text.toString() }
        .flatMapLatest { (project, queryStr) ->
            if (project == null) return@flatMapLatest emptyFlow()

            runUseCase(deps.globalActions::onEvent) {
                getNotesUseCase(
                    GetNotesInput(
                        project = project,
                        searchQueryString = queryStr,
                    ),
                )
            }.getOrElse { emptyFlow() }
        }
        .cachedIn(deps.scope)

    fun onSearchEvent(event: SearchEvent) {
        deps.scope.launch {
            runUseCase(deps.globalActions::onEvent) {
                applySearchEventUseCase(
                    ApplySearchEventInput(
                        event = event,
                        state = searchState,
                    ),
                )
            }.getOrElse { return@launch }
        }
    }

    fun onNoteSelected(note: Note) {
        deps.globalActions.onEvent(NavigationEvent.CloseDrawer)
        deps.globalActions.onEvent(NavigationEvent.GoToEditor(note = note))
    }

    fun showCreateNoteDialog() {
        deps.uiState.update { it.copy(isCreateNoteDialogVisible = true) }
    }

    fun dismissCreateNoteDialog() {
        deps.uiState.update { it.copy(isCreateNoteDialogVisible = false, newNoteNameInput = "") }
    }

    fun updateNewNoteName(name: String) {
        deps.uiState.update { it.copy(newNoteNameInput = name) }
    }

    fun onCreateNote() {
        deps.scope.launch {
            val project = deps.uiState.value.project ?: return@launch
            val name = deps.uiState.value.newNoteNameInput
            val note = runUseCase(deps.globalActions::onEvent) {
                createNoteUseCase(
                    CreateNoteInput(
                        project = project,
                        name = name,
                        initialText = "# $name\n\n",
                    ),
                )
            }.getOrElse { return@launch }
            deps.globalActions.updateNoteLists()
            deps.globalActions.onEvent(NavigationEvent.GoToEditor(note))
            deps.globalActions.onEvent(NavigationEvent.CloseDrawer)
        }
    }

    fun showNoteDeleteDialog(note: Note) {
        deps.uiState.update { it.copy(isNoteDeleteDialogVisible = true, dialogNote = note) }
    }

    fun dismissNoteDeleteDialog() {
        deps.uiState.update { it.copy(isNoteDeleteDialogVisible = false, dialogNote = null) }
    }

    fun onDeleteNote(note: Note) {
        deps.scope.launch {
            val project = deps.uiState.value.project ?: return@launch
            runUseCase(deps.globalActions::onEvent) {
                deleteNoteUseCase(
                    DeleteNoteInput(
                        project = project,
                        note = note,
                    ),
                )
            }.getOrElse { return@launch }
            deps.globalActions.updateNoteLists()

            val activeNote = deps.uiState.value.activeNote
            if (note.projectFile.relativePath == activeNote?.projectFile?.relativePath)
                deps.globalActions.onEvent(NavigationEvent.GoBack)
        }
    }

    fun showNoteShowInfoDialog(note: Note) {
        deps.uiState.update { it.copy(isNoteShowInfoDialogVisible = true, dialogNote = note) }
    }

    fun dismissNoteShowInfoDialog() {
        deps.uiState.update { it.copy(isNoteShowInfoDialogVisible = false, dialogNote = null) }
    }

    fun showNoteRenameDialog(note: Note) {
        deps.uiState.update {
            it.copy(
                isNoteRenameDialogVisible = true,
                noteRenameInput = note.name,
                dialogNote = note,
            )
        }
    }

    fun dismissNoteRenameDialog() {
        deps.uiState.update {
            it.copy(isNoteRenameDialogVisible = false, noteRenameInput = "", dialogNote = null)
        }
    }

    fun onRenameNameInputChanged(newName: String) {
        deps.uiState.update { it.copy(noteRenameInput = newName) }
    }

    fun onRenameNote(note: Note, newName: String) {
        deps.scope.launch {
            val project = deps.uiState.value.project ?: return@launch
            runUseCase(deps.globalActions::onEvent) {
                renameNoteUseCase(
                    RenameNoteInput(
                        project = project,
                        note = note,
                        newName = newName,
                    ),
                )
            }.getOrElse { return@launch }
            deps.globalActions.updateNoteLists()
        }
    }

    fun onPinNote(note: Note) {
        deps.scope.launch {
            val project = deps.uiState.value.project ?: return@launch
            runUseCase(deps.globalActions::onEvent) {
                toggleNoteTagUseCase(
                    ToggleNoteTagInput(
                        project = project,
                        note = note,
                        tag = FrontMatter.PINNED_TAG,
                    ),
                )
            }.getOrElse { return@launch }
            deps.globalActions.updateNoteLists()
        }
    }
}