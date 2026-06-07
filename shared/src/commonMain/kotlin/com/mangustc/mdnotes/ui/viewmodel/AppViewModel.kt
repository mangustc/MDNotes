package com.mangustc.mdnotes.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mangustc.mdnotes.domain.models.Attachment
import com.mangustc.mdnotes.domain.usecases.project.GetAllTagsInput
import com.mangustc.mdnotes.domain.usecases.project.GetAllTagsUseCase
import com.mangustc.mdnotes.domain.usecases.project.SyncDatabaseInput
import com.mangustc.mdnotes.domain.usecases.project.SyncDatabaseUseCase
import com.mangustc.mdnotes.ui.util.runUseCase
import com.mangustc.mdnotes.ui.viewmodel.actions.DrawerActions
import com.mangustc.mdnotes.ui.viewmodel.actions.EditorActions
import com.mangustc.mdnotes.ui.viewmodel.actions.MessengerActions
import com.mangustc.mdnotes.ui.viewmodel.actions.ProjectActions
import com.mangustc.mdnotes.ui.viewmodel.actions.SettingsActions
import com.mangustc.mdnotes.ui.viewmodel.events.AppEvent
import com.mangustc.mdnotes.ui.viewmodel.events.ClipboardEvent
import com.mangustc.mdnotes.ui.viewmodel.events.FocusEvent
import com.mangustc.mdnotes.ui.viewmodel.events.NavigationEvent
import com.mangustc.mdnotes.ui.viewmodel.events.NotificationEvent
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update

@OptIn(ExperimentalCoroutinesApi::class)
class AppViewModel(
    private val syncDatabaseUseCase: SyncDatabaseUseCase,
    private val getAllTagsUseCase: GetAllTagsUseCase,
) : ViewModel(), AppGlobalActions {
    private val _uiState = MutableStateFlow(AppUiState())
    val uiState: StateFlow<AppUiState> = _uiState.asStateFlow()

    private val deps by lazy {
        AppDeps(
            scope = viewModelScope,
            uiState = _uiState,
            globalActions = this,
        )
    }

    val project = ProjectActions(deps)
    val settings = SettingsActions(deps)

    init {
        project.loadSavedProject()
    }

    val drawer = DrawerActions(deps)
    val editor = EditorActions(deps)
    val messenger = MessengerActions(deps)

    private val _navigationEvents = Channel<NavigationEvent>(Channel.BUFFERED)
    val navigationEvents = _navigationEvents.receiveAsFlow()

    private val _notificationEvents = Channel<NotificationEvent>(Channel.BUFFERED)
    val notificationEvents = _notificationEvents.receiveAsFlow()

    private val _clipboardEvents = Channel<ClipboardEvent>(Channel.BUFFERED)
    val clipboardEvents = _clipboardEvents.receiveAsFlow()

    private val _focusEvents = Channel<FocusEvent>(Channel.BUFFERED)
    val focusEvents = _focusEvents.receiveAsFlow()

    override fun onEvent(event: AppEvent) {
        when (event) {
            is NavigationEvent -> _navigationEvents.trySend(event)
            is NotificationEvent -> _notificationEvents.trySend(event)
            is ClipboardEvent -> _clipboardEvents.trySend(event)
            is FocusEvent -> _focusEvents.trySend(event)
        }
    }

    override suspend fun updateNoteLists() {
        val project = _uiState.value.project ?: return
        runUseCase(::onEvent) {
            syncDatabaseUseCase(
                SyncDatabaseInput(
                    project = project,
                ),
            )
        }.getOrElse { return }
        _uiState.update {
            it.copy(
                allProjectTags = getAllTagsUseCase(
                    GetAllTagsInput(
                        project = project,
                    ),
                ),
            )
        }
        messenger.updateMessages()
    }

    fun onShareIntent(text: String?, attachments: List<Attachment>) {
        _uiState.update { state ->
            state.copy(
                messengerNewNoteText = if (!text.isNullOrEmpty()) text else state.messengerNewNoteText,
                pendingIntentAttachments = attachments,
            )
        }
    }

    fun consumePendingIntentAttachments(): List<Attachment> {
        val pending = _uiState.value.pendingIntentAttachments
        if (pending.isNotEmpty()) {
            _uiState.update { it.copy(pendingIntentAttachments = emptyList()) }
        }
        return pending
    }
}