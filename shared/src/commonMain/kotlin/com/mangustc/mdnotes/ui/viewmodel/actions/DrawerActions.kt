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
import com.mangustc.mdnotes.ui.viewmodel.ActiveDialog
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
        deps.globalActions.onEvent(NavigationEvent.GoToEditor(note = note))
    }

    fun dismissDialog() {
        deps.uiState.update { it.copy(activeDialog = ActiveDialog.None) }
    }

    fun showCreateNoteDialog() {
        deps.uiState.update { it.copy(activeDialog = ActiveDialog.CreateNote()) }
    }

    fun updateNewNoteName(name: String) {
        deps.uiState.update { state ->
            val dialog = state.activeDialog as? ActiveDialog.CreateNote ?: return@update state
            state.copy(activeDialog = dialog.copy(nameInput = name))
        }
    }

    fun onCreateNote(name: String) {
        deps.scope.launch {
            val project = deps.uiState.value.project ?: return@launch
            val note = runUseCase(deps.globalActions::onEvent) {
                createNoteUseCase(
                    CreateNoteInput(
                        project = project,
                        name = name,
                        initialText = "# $name\n\n",
                    ),
                )
            }.getOrElse { return@launch }
            deps.globalActions.onEvent(NavigationEvent.GoToEditor(note))
        }
    }

    fun showNoteDeleteDialog(note: Note) {
        deps.uiState.update { it.copy(activeDialog = ActiveDialog.DeleteNote(note)) }
    }

    fun onDeleteNote(note: Note) {
        deps.scope.launch {
            val project = deps.uiState.value.project ?: return@launch
            val isDeletingActive =
                note.projectFile.relativePath == deps.uiState.value.activeNote?.projectFile?.relativePath
            if (isDeletingActive) {
                deps.globalActions.onNoteDeleted(note.projectFile.relativePath)
            }
            runUseCase(deps.globalActions::onEvent) {
                deleteNoteUseCase(
                    DeleteNoteInput(
                        project = project,
                        note = note,
                    ),
                )
            }.getOrElse { return@launch }

            if (isDeletingActive) {
                deps.globalActions.onEvent(NavigationEvent.GoBack)
            }
        }
    }

    fun showNoteShowInfoDialog(note: Note) {
        deps.uiState.update { it.copy(activeDialog = ActiveDialog.ShowInfo(note)) }
    }

    fun showNoteRenameDialog(note: Note) {
        deps.uiState.update {
            it.copy(
                activeDialog = ActiveDialog.RenameNote(
                    note = note,
                    nameInput = note.name,
                ),
            )
        }
    }

    fun onRenameNameInputChanged(newName: String) {
        deps.uiState.update { state ->
            val dialog = state.activeDialog as? ActiveDialog.RenameNote ?: return@update state
            state.copy(activeDialog = dialog.copy(nameInput = newName))
        }
    }

    fun onRenameNote(note: Note, newName: String) {
        deps.scope.launch {
            val project = deps.uiState.value.project ?: return@launch
            val oldPath = note.projectFile.relativePath
            if (oldPath == deps.uiState.value.activeNote?.projectFile?.relativePath) {
                deps.globalActions.flushEditorSave()
            }
            val renamedNote = runUseCase(deps.globalActions::onEvent) {
                renameNoteUseCase(
                    RenameNoteInput(
                        project = project,
                        note = note,
                        newName = newName,
                    ),
                )
            }.getOrElse { return@launch }
            deps.globalActions.onNoteRenamed(oldPath, renamedNote)
        }
    }

    fun onPinNote(note: Note) {
        deps.scope.launch {
            val project = deps.uiState.value.project ?: return@launch
            val notePath = note.projectFile.relativePath
            if (notePath == deps.uiState.value.activeNote?.projectFile?.relativePath) {
                deps.globalActions.flushEditorSave()
            }
            val updatedFrontMatter = runUseCase(deps.globalActions::onEvent) {
                toggleNoteTagUseCase(
                    ToggleNoteTagInput(
                        project = project,
                        note = note,
                        tag = FrontMatter.PINNED_TAG,
                    ),
                )
            }.getOrElse { return@launch }
            deps.globalActions.onNoteFrontMatterUpdated(notePath, updatedFrontMatter)
        }
    }
}