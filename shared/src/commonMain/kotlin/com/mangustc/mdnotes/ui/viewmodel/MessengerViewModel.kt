package com.mangustc.mdnotes.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.mangustc.mdnotes.domain.models.Attachment
import com.mangustc.mdnotes.domain.models.FrontMatter
import com.mangustc.mdnotes.domain.models.LinkPreview
import com.mangustc.mdnotes.domain.models.MessageBody
import com.mangustc.mdnotes.domain.models.Note
import com.mangustc.mdnotes.domain.models.Project
import com.mangustc.mdnotes.domain.usecases.linkPreview.GetLinkPreviewInput
import com.mangustc.mdnotes.domain.usecases.linkPreview.GetLinkPreviewUseCase
import com.mangustc.mdnotes.domain.usecases.messenger.GetMessagesInput
import com.mangustc.mdnotes.domain.usecases.messenger.GetMessagesUseCase
import com.mangustc.mdnotes.domain.usecases.messenger.GetPinnedMessagesInput
import com.mangustc.mdnotes.domain.usecases.messenger.GetPinnedMessagesUseCase
import com.mangustc.mdnotes.domain.usecases.messenger.SendNoteInput
import com.mangustc.mdnotes.domain.usecases.messenger.SendNoteUseCase
import com.mangustc.mdnotes.domain.usecases.notes.DeleteNoteInput
import com.mangustc.mdnotes.domain.usecases.notes.DeleteNoteUseCase
import com.mangustc.mdnotes.domain.usecases.notes.ToggleNoteTagInput
import com.mangustc.mdnotes.domain.usecases.notes.ToggleNoteTagUseCase
import com.mangustc.mdnotes.ui.util.launchSafe
import com.mangustc.mdnotes.ui.util.runUseCase
import com.mangustc.mdnotes.ui.viewmodel.events.AppEvent
import com.mangustc.mdnotes.ui.viewmodel.events.ClipboardEvent
import com.mangustc.mdnotes.ui.viewmodel.events.NavigationEvent
import com.mangustc.mdnotes.ui.viewmodel.events.NotificationEvent
import com.mangustc.mdnotes.ui.viewmodel.session.AppEventBus
import com.mangustc.mdnotes.ui.viewmodel.session.EditorCoordinator
import com.mangustc.mdnotes.ui.viewmodel.session.ProjectSession
import com.mangustc.mdnotes.ui.viewmodel.session.SharedIntentPayload
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class MessengerUiState(
    val project: Project? = null,
    val isLoading: Boolean = true,
    val newNoteText: String = "",
    val linkPreviews: Map<String, LinkPreview?> = emptyMap(),
    val editingNote: Note? = null,
    val pinnedMessages: List<MessageBody> = emptyList(),
    val selectedNotes: Set<MessageBody> = emptySet(),
)

class MessengerViewModel(
    private val projectSession: ProjectSession,
    private val eventBus: AppEventBus,
    private val editorCoordinator: EditorCoordinator,
    private val getMessagesUseCase: GetMessagesUseCase,
    private val getPinnedMessagesUseCase: GetPinnedMessagesUseCase,
    private val deleteNoteUseCase: DeleteNoteUseCase,
    private val toggleNoteTagUseCase: ToggleNoteTagUseCase,
    private val getLinkPreviewUseCase: GetLinkPreviewUseCase,
    private val sendNoteUseCase: SendNoteUseCase,
) : ViewModel() {

    private data class LocalState(
        val newNoteText: String = "",
        val linkPreviews: Map<String, LinkPreview?> = emptyMap(),
        val editingNote: Note? = null,
    )

    private val _localState = MutableStateFlow(LocalState())
    private val _selectedNotes = MutableStateFlow<Set<MessageBody>>(emptySet())
    val selectedNotes: StateFlow<Set<MessageBody>> = _selectedNotes.asStateFlow()

    val pendingShareIntent: StateFlow<SharedIntentPayload?> = projectSession.pendingShareIntent

    @OptIn(ExperimentalCoroutinesApi::class)
    val notesPaged: Flow<PagingData<MessageBody>> = projectSession.project
        .flatMapLatest { project ->
            if (project == null) return@flatMapLatest emptyFlow()
            runUseCase(eventBus::emit) {
                getMessagesUseCase(GetMessagesInput(project))
            }.getOrElse { emptyFlow() }
        }
        .cachedIn(viewModelScope)

    @OptIn(ExperimentalCoroutinesApi::class)
    private val pinnedMessages: Flow<List<MessageBody>> = projectSession.project
        .flatMapLatest { project ->
            if (project == null) {
                flowOf(emptyList())
            } else {
                getPinnedMessagesUseCase(GetPinnedMessagesInput(project = project))
                    .catch { e ->
                        if (e !is Exception) throw e
                        e.printStackTrace()
                        eventBus.emit(NotificationEvent.FromException(e))
                        emit(emptyList())
                    }
            }
        }

    val uiState: StateFlow<MessengerUiState> = combine(
        projectSession.project,
        projectSession.isProjectLoading,
        _localState,
        pinnedMessages,
        _selectedNotes,
    ) { project, isLoading, local, pinned, selected ->
        MessengerUiState(
            project = project,
            isLoading = isLoading,
            newNoteText = local.newNoteText,
            linkPreviews = local.linkPreviews,
            editingNote = local.editingNote,
            pinnedMessages = pinned,
            selectedNotes = selected,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = MessengerUiState(
            project = projectSession.project.value,
            isLoading = projectSession.isProjectLoading.value,
        ),
    )

    fun emitEvent(event: AppEvent) {
        eventBus.emit(event)
    }

    fun consumeShareIntent(): SharedIntentPayload? = projectSession.consumeShareIntent()

    fun openNote(note: Note) {
        eventBus.emit(NavigationEvent.GoToEditor(note = note))
    }

    fun deleteNote(note: Note) {
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

    fun pinNote(note: Note) {
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

    fun onNewNoteTextChanged(text: String) {
        _localState.update { it.copy(newNoteText = text) }
    }

    fun startEditNote(message: MessageBody) {
        _localState.update {
            it.copy(
                editingNote = message.note,
                newNoteText = message.text,
            )
        }
    }

    fun cancelEditNote() {
        _localState.update {
            it.copy(editingNote = null, newNoteText = "")
        }
    }

    fun onSendNote(
        isEditedNote: Boolean,
        attachments: List<Attachment> = emptyList(),
        afterUpdate: () -> Unit = {},
    ) {
        viewModelScope.launchSafe(eventBus::emit) {
            val project = projectSession.project.value ?: return@launchSafe
            val local = _localState.value
            val text = local.newNoteText.trim()

            sendNoteUseCase(
                SendNoteInput(
                    project = project,
                    body = text,
                    attachments = attachments,
                    editNote = if (isEditedNote) {
                        local.editingNote ?: return@launchSafe
                    } else {
                        if (text.isBlank() && attachments.isEmpty()) return@launchSafe
                        null
                    },
                ),
            )

            _localState.update { state ->
                state.copy(
                    newNoteText = "",
                    editingNote = if (isEditedNote) null else state.editingNote,
                )
            }
            afterUpdate()
        }
    }

    fun ensureLinkPreview(url: String) {
        var shouldFetch = false
        _localState.update { state ->
            if (state.linkPreviews.containsKey(url)) {
                shouldFetch = false
                state
            } else {
                shouldFetch = true
                state.copy(linkPreviews = state.linkPreviews + (url to null))
            }
        }
        if (!shouldFetch) return

        viewModelScope.launchSafe(eventBus::emit) {
            val preview = getLinkPreviewUseCase(GetLinkPreviewInput(url = url))
            if (preview != null) {
                _localState.update {
                    it.copy(linkPreviews = it.linkPreviews + (url to preview))
                }
            }
        }
    }

    fun toggleNoteSelection(message: MessageBody) {
        _selectedNotes.update { sel ->
            if (sel.contains(message)) sel - message else sel + message
        }
    }

    fun clearSelection() {
        _selectedNotes.value = emptySet()
    }

    fun deleteSelectedNotes() {
        viewModelScope.launch {
            val project = projectSession.project.value ?: return@launch
            val messages = _selectedNotes.value
            messages.forEach { message ->
                runUseCase(eventBus::emit) {
                    deleteNoteUseCase(
                        DeleteNoteInput(
                            project = project,
                            note = message.note,
                        ),
                    )
                }
            }
            clearSelection()
        }
    }

    fun copySelectedNotesText() {
        val text = _selectedNotes.value.joinToString("\n\n") { message ->
            message.text
        }
        eventBus.emit(ClipboardEvent.Copy(text))
        clearSelection()
    }
}
