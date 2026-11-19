package com.mangustc.mdnotes.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mangustc.mdnotes.domain.models.Attachment
import com.mangustc.mdnotes.domain.usecases.project.GetAllTagsInput
import com.mangustc.mdnotes.domain.usecases.project.GetAllTagsUseCase
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
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update

@OptIn(ExperimentalCoroutinesApi::class)
class AppViewModel(
    private val getAllTagsUseCase: GetAllTagsUseCase,
) : ViewModel(), AppGlobalActions {
    private val _uiState = MutableStateFlow(AppUiState())

    private val deps by lazy {
        AppDeps(
            scope = viewModelScope,
            uiState = _uiState,
            globalActions = this,
        )
    }

    val project = ProjectActions(deps)
    val settings = SettingsActions(deps)
    val drawer = DrawerActions(deps)
    val editor = EditorActions(deps)
    val messenger = MessengerActions(deps)

    private val allProjectTagsFlow: Flow<List<String>> = _uiState
        .map { it.project }
        .distinctUntilChanged()
        .flatMapLatest { project ->
            if (project == null) {
                flowOf(emptyList())
            } else {
                getAllTagsUseCase(GetAllTagsInput(project = project))
                    .catch { e ->
                        if (e !is Exception) throw e
                        e.printStackTrace()
                        onEvent(NotificationEvent.FromException(e))
                        emit(emptyList())
                    }
            }
        }

    val uiState: StateFlow<AppUiState> = combine(
        _uiState,
        allProjectTagsFlow,
        messenger.pinnedMessages,
    ) { state, tags, pinnedMessages ->
        state.copy(
            allProjectTags = tags,
            messengerPinnedMessages = pinnedMessages,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = _uiState.value,
    )

    private val _navigationEvents = Channel<NavigationEvent>(Channel.BUFFERED)
    val navigationEvents = _navigationEvents.receiveAsFlow()

    private val _notificationEvents = Channel<NotificationEvent>(Channel.BUFFERED)
    val notificationEvents = _notificationEvents.receiveAsFlow()

    private val _clipboardEvents = Channel<ClipboardEvent>(Channel.BUFFERED)
    val clipboardEvents = _clipboardEvents.receiveAsFlow()

    private val _focusEvents = Channel<FocusEvent>(Channel.BUFFERED)
    val focusEvents = _focusEvents.receiveAsFlow()

    init {
        project.loadSavedProject()
    }

    override fun onEvent(event: AppEvent) {
        when (event) {
            is NavigationEvent -> _navigationEvents.trySend(event)
            is NotificationEvent -> _notificationEvents.trySend(event)
            is ClipboardEvent -> _clipboardEvents.trySend(event)
            is FocusEvent -> _focusEvents.trySend(event)
        }
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