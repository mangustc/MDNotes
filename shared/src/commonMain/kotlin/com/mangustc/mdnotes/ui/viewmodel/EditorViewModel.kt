package com.mangustc.mdnotes.ui.viewmodel

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.runtime.snapshotFlow
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.mangustc.mdnotes.domain.models.FrontMatter
import com.mangustc.mdnotes.domain.models.Note
import com.mangustc.mdnotes.domain.models.NoteDocument
import com.mangustc.mdnotes.domain.models.Project
import com.mangustc.mdnotes.domain.models.RelativePath
import com.mangustc.mdnotes.domain.models.Settings
import com.mangustc.mdnotes.domain.models.SpanInfo
import com.mangustc.mdnotes.domain.usecases.editor.ApplyEditorEventInput
import com.mangustc.mdnotes.domain.usecases.editor.ApplyEditorEventUseCase
import com.mangustc.mdnotes.domain.usecases.editor.EditorEvent
import com.mangustc.mdnotes.domain.usecases.editor.GetRealSpanInfoLinkTypeInput
import com.mangustc.mdnotes.domain.usecases.editor.GetRealSpanInfoLinkTypeUseCase
import com.mangustc.mdnotes.domain.usecases.notes.GetNoteInput
import com.mangustc.mdnotes.domain.usecases.notes.GetNoteUseCase
import com.mangustc.mdnotes.domain.usecases.notes.SaveNoteTextInput
import com.mangustc.mdnotes.domain.usecases.notes.SaveNoteTextUseCase
import com.mangustc.mdnotes.domain.usecases.project.GetAllTagsInput
import com.mangustc.mdnotes.domain.usecases.project.GetAllTagsUseCase
import com.mangustc.mdnotes.domain.usecases.project.GetNotesInput
import com.mangustc.mdnotes.domain.usecases.project.GetNotesUseCase
import com.mangustc.mdnotes.domain.usecases.project.GetProjectFileInput
import com.mangustc.mdnotes.domain.usecases.project.GetProjectFileUseCase
import com.mangustc.mdnotes.domain.usecases.search.ApplySearchEventInput
import com.mangustc.mdnotes.domain.usecases.search.ApplySearchEventUseCase
import com.mangustc.mdnotes.domain.usecases.search.SearchEvent
import com.mangustc.mdnotes.ui.components.ComposeTextState
import com.mangustc.mdnotes.ui.util.launchSafe
import com.mangustc.mdnotes.ui.util.runUseCase
import com.mangustc.mdnotes.ui.viewmodel.events.FocusEvent
import com.mangustc.mdnotes.ui.viewmodel.events.NavigationEvent
import com.mangustc.mdnotes.ui.viewmodel.events.NotificationEvent
import com.mangustc.mdnotes.ui.viewmodel.session.AppEventBus
import com.mangustc.mdnotes.ui.viewmodel.session.EditorCoordinator
import com.mangustc.mdnotes.ui.viewmodel.session.EditorSessionHandler
import com.mangustc.mdnotes.ui.viewmodel.session.ProjectSession
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.time.Duration.Companion.seconds

data class EditorUiState(
    val project: Project? = null,
    val reverseLayout: Boolean = Settings.EMPTY.reverseLayout,
    val activeNote: Note? = null,
    val frontMatter: FrontMatter? = null,
    val allProjectTags: List<String> = emptyList(),
    val isViewingMode: Boolean = true,
    val isLinkNoteDialogVisible: Boolean = false,
)

@OptIn(FlowPreview::class)
class EditorViewModel(
    private val projectSession: ProjectSession,
    private val eventBus: AppEventBus,
    private val editorCoordinator: EditorCoordinator,
    private val getAllTagsUseCase: GetAllTagsUseCase,
    private val getNotesUseCase: GetNotesUseCase,
    private val getRealSpanInfoLinkTypeUseCase: GetRealSpanInfoLinkTypeUseCase,
    private val getNoteUseCase: GetNoteUseCase,
    private val getProjectFileUseCase: GetProjectFileUseCase,
    private val saveNoteTextUseCase: SaveNoteTextUseCase,
    private val applyEditorEventUseCase: ApplyEditorEventUseCase,
    private val applySearchEventUseCase: ApplySearchEventUseCase,
) : ViewModel(), EditorSessionHandler {

    private data class LocalState(
        val activeNote: Note? = null,
        val frontMatter: FrontMatter? = null,
        val isLinkNoteDialogVisible: Boolean = false,
    )

    private val _localState = MutableStateFlow(LocalState())
    private val _isViewingMode = MutableStateFlow(true)
    val isViewingMode: StateFlow<Boolean> = _isViewingMode.asStateFlow()

    val state = ComposeTextState()
    val linkSearchState = ComposeTextState()

    @OptIn(ExperimentalCoroutinesApi::class)
    private val allProjectTagsFlow: Flow<List<String>> = projectSession.project
        .flatMapLatest { project ->
            if (project == null) {
                flowOf(emptyList())
            } else {
                getAllTagsUseCase(GetAllTagsInput(project = project))
                    .catch { e ->
                        if (e !is Exception) throw e
                        e.printStackTrace()
                        eventBus.emit(NotificationEvent.FromException(e))
                        emit(emptyList())
                    }
            }
        }

    val uiState: StateFlow<EditorUiState> = combine(
        projectSession.project,
        projectSession.settings,
        allProjectTagsFlow,
        _localState,
        _isViewingMode,
    ) { project, settings, tags, local, isViewingMode ->
        EditorUiState(
            project = project,
            reverseLayout = settings?.reverseLayout ?: Settings.EMPTY.reverseLayout,
            activeNote = local.activeNote,
            frontMatter = local.frontMatter,
            allProjectTags = tags,
            isViewingMode = isViewingMode,
            isLinkNoteDialogVisible = local.isLinkNoteDialogVisible,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = EditorUiState(
            project = projectSession.project.value,
            reverseLayout = projectSession.settings.value?.reverseLayout
                ?: Settings.EMPTY.reverseLayout,
        ),
    )

    @OptIn(ExperimentalCoroutinesApi::class)
    val linkSearchResultsPaged: Flow<PagingData<Note>> = combine(
        projectSession.project,
        snapshotFlow { linkSearchState.text },
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

    private val editorMutex = Mutex()
    private var lastSavedBodyRef: CharSequence? = null
    private var currentFmVersion: Long = 0L
    private var savedFmVersion: Long = 0L
    private var activeSessionRoutePath: RelativePath? = null

    init {
        editorCoordinator.register(this)
        viewModelScope.launch {
            snapshotFlow { state.text }
                .drop(1)
                .debounce(2.seconds)
                .collect { flushSave() }
        }
    }

    override fun onCleared() {
        super.onCleared()
        editorCoordinator.unregister(this)
    }

    fun clearFocus() {
        eventBus.emit(FocusEvent.ClearFocus)
    }

    fun onLinkSearchEvent(event: SearchEvent) {
        viewModelScope.launchSafe(eventBus::emit) {
            applySearchEventUseCase(
                ApplySearchEventInput(
                    event = event,
                    state = linkSearchState,
                ),
            )
        }
    }

    fun onCloseEditor() {
        viewModelScope.launch {
            editorMutex.withLock {
                flushSaveLocked()
                clearSessionLocked()
            }
            eventBus.emit(NavigationEvent.GoBack)
        }
    }

    fun onEditorDisposed(routePath: RelativePath) {
        viewModelScope.launch {
            editorMutex.withLock {
                if (activeSessionRoutePath == routePath) {
                    flushSaveLocked()
                    clearSessionLocked()
                }
            }
        }
    }

    fun saveNow() {
        viewModelScope.launch {
            flushSave()
        }
    }

    override suspend fun flushSave() {
        editorMutex.withLock {
            flushSaveLocked()
        }
    }

    override suspend fun onNoteDeleted(notePath: RelativePath) {
        editorMutex.withLock {
            if (_localState.value.activeNote?.projectFile?.relativePath == notePath) {
                clearSessionLocked()
            }
        }
    }

    override suspend fun onNoteRenamed(oldPath: RelativePath, renamedNote: Note) {
        editorMutex.withLock {
            if (_localState.value.activeNote?.projectFile?.relativePath == oldPath) {
                _localState.update { it.copy(activeNote = renamedNote) }
                editorCoordinator.setActiveNote(renamedNote)
            }
        }
    }

    override suspend fun onNoteFrontMatterUpdated(
        notePath: RelativePath,
        frontMatter: FrontMatter
    ) {
        editorMutex.withLock {
            if (_localState.value.activeNote?.projectFile?.relativePath == notePath) {
                _localState.update { it.copy(frontMatter = frontMatter) }
                savedFmVersion = currentFmVersion
            }
        }
    }

    fun showLinkNoteDialog() {
        _localState.update { it.copy(isLinkNoteDialogVisible = true) }
    }

    fun dismissLinkNoteDialog() {
        _localState.update { it.copy(isLinkNoteDialogVisible = false) }
        linkSearchState.setTextAndPlaceCursorAtEnd("")
    }

    @OptIn(ExperimentalFoundationApi::class)
    fun onEvent(event: EditorEvent) {
        viewModelScope.launchSafe(eventBus::emit) {
            val project = projectSession.project.value ?: return@launchSafe
            applyEditorEventUseCase(
                ApplyEditorEventInput(
                    project = project,
                    state = state,
                    event = event,
                ),
            )
            if (event is EditorEvent.InsertNoteLink) {
                dismissLinkNoteDialog()
            }
        }
    }

    fun openLink(span: SpanInfo.Link) {
        viewModelScope.launchSafe(eventBus::emit) {
            val project = projectSession.project.value ?: return@launchSafe

            val realLinkType = getRealSpanInfoLinkTypeUseCase(
                GetRealSpanInfoLinkTypeInput(
                    project = project,
                    span = span,
                ),
            )
            when (realLinkType) {
                SpanInfo.Link.LinkType.NOTE -> {
                    val note = getNoteUseCase(
                        GetNoteInput(
                            project = project,
                            relativePath = RelativePath(span.payload),
                        ),
                    )
                    eventBus.emit(NavigationEvent.GoToEditor(note = note))
                }

                SpanInfo.Link.LinkType.FILE -> {
                    val projectFile = getProjectFileUseCase(
                        GetProjectFileInput(
                            project = project,
                            relativePath = RelativePath(span.payload),
                        ),
                    )
                    eventBus.emit(NavigationEvent.OpenFile(projectFile.domainFile))
                }

                SpanInfo.Link.LinkType.HTTP -> {
                    eventBus.emit(NavigationEvent.OpenUrl(span.payload))
                }
            }
        }
    }

    @OptIn(ExperimentalFoundationApi::class)
    fun onNoteOpened(notePath: RelativePath) {
        viewModelScope.launch {
            editorMutex.withLock {
                flushSaveLocked()
                try {
                    val project = projectSession.project.value ?: throw Exception()
                    val note = runUseCase(eventBus::emit) {
                        getNoteUseCase(
                            GetNoteInput(
                                project = project,
                                relativePath = notePath,
                                includeText = true,
                                includeFrontMatter = true,
                            ),
                        )
                    }.getOrElse { return@withLock }
                    val (frontMatter, body) = NoteDocument.parse(
                        note.body ?: throw Exception(),
                    )
                    state.edit {
                        replace(0, length, body)
                    }
                    state.undoState.clearHistory()
                    lastSavedBodyRef = state.text
                    savedFmVersion = currentFmVersion
                    activeSessionRoutePath = notePath
                    _localState.update {
                        it.copy(
                            activeNote = note,
                            frontMatter = frontMatter,
                        )
                    }
                    _isViewingMode.value = true
                    editorCoordinator.setActiveNote(note)
                } catch (e: CancellationException) {
                    throw e
                } catch (_: Exception) {
                    eventBus.emit(NavigationEvent.GoBack)
                }
            }
        }
    }

    fun updateFmKey(oldKey: String, newKey: String) {
        if (oldKey == newKey || newKey.isBlank()) return
        updateFrontMatter { it?.withRenamedKey(oldKey, newKey) }
    }

    fun updateFmValue(key: String, value: String) {
        updateFrontMatter {
            it?.withField(key, FrontMatter.FrontMatterValue.Scalar(value))
        }
    }

    fun addFmProperty() {
        updateFrontMatter { current ->
            val fm = current ?: FrontMatter.Empty
            var newKey = "newProperty"
            var count = 1
            while (fm.fields.containsKey(newKey)) {
                newKey = "newProperty$count"
                count++
            }
            fm.withField(newKey, FrontMatter.FrontMatterValue.Scalar(""))
        }
    }

    fun addFmTag(tag: String) {
        updateFrontMatter { it?.withTag(tag) }
    }

    fun removeFmTag(tag: String) {
        updateFrontMatter { it?.withoutTag(tag) }
    }

    fun removeFmProperty(key: String) {
        updateFrontMatter { it?.withoutField(key) }
    }

    fun toggleViewingMode() {
        _isViewingMode.update { !it }
    }

    private fun updateFrontMatter(transform: (FrontMatter?) -> FrontMatter?) {
        viewModelScope.launch {
            editorMutex.withLock {
                _localState.update {
                    it.copy(frontMatter = transform(it.frontMatter))
                }
                currentFmVersion++
                flushSaveLocked()
            }
        }
    }

    private fun clearSessionLocked() {
        activeSessionRoutePath = null
        lastSavedBodyRef = null
        savedFmVersion = currentFmVersion
        _localState.update {
            it.copy(
                isLinkNoteDialogVisible = false,
                activeNote = null,
                frontMatter = null,
            )
        }
        editorCoordinator.setActiveNote(null)
    }

    private suspend fun flushSaveLocked() {
        val project = projectSession.project.value ?: return
        val note = _localState.value.activeNote ?: return
        val currentBodyRef = state.text
        val isBodyDirty = currentBodyRef !== lastSavedBodyRef
        val isFmDirty = currentFmVersion != savedFmVersion
        if (!isBodyDirty && !isFmDirty) return

        val fmVersionToSave = currentFmVersion
        val fm = _localState.value.frontMatter
        val textToSave = NoteDocument(
            frontMatter = fm ?: FrontMatter.Empty,
            body = currentBodyRef.toString(),
        ).serialize()

        runUseCase(eventBus::emit) {
            saveNoteTextUseCase(
                SaveNoteTextInput(
                    project = project,
                    note = note,
                    text = textToSave,
                ),
            )
        }.getOrElse { return }
        lastSavedBodyRef = currentBodyRef
        savedFmVersion = fmVersionToSave
    }
}
