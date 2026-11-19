package com.mangustc.mdnotes.ui.viewmodel.actions

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.runtime.snapshotFlow
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.mangustc.mdnotes.domain.models.FrontMatter
import com.mangustc.mdnotes.domain.models.Note
import com.mangustc.mdnotes.domain.models.NoteDocument
import com.mangustc.mdnotes.domain.models.RelativePath
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
import com.mangustc.mdnotes.ui.viewmodel.AppDeps
import com.mangustc.mdnotes.ui.viewmodel.events.NavigationEvent
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.time.Duration.Companion.seconds

@OptIn(FlowPreview::class)
class EditorActions(
    private val deps: AppDeps,
    private val getNotesUseCase: GetNotesUseCase,
    private val getRealSpanInfoLinkTypeUseCase: GetRealSpanInfoLinkTypeUseCase,
    private val getNoteUseCase: GetNoteUseCase,
    private val getProjectFileUseCase: GetProjectFileUseCase,
    private val saveNoteTextUseCase: SaveNoteTextUseCase,
    private val applyEditorEventUseCase: ApplyEditorEventUseCase,
    private val applySearchEventUseCase: ApplySearchEventUseCase,
) {
    class Factory(
        private val getNotesUseCase: GetNotesUseCase,
        private val getRealSpanInfoLinkTypeUseCase: GetRealSpanInfoLinkTypeUseCase,
        private val getNoteUseCase: GetNoteUseCase,
        private val getProjectFileUseCase: GetProjectFileUseCase,
        private val saveNoteTextUseCase: SaveNoteTextUseCase,
        private val applyEditorEventUseCase: ApplyEditorEventUseCase,
        private val applySearchEventUseCase: ApplySearchEventUseCase,
    ) {
        fun create(deps: AppDeps): EditorActions = EditorActions(
            deps = deps,
            getNotesUseCase = getNotesUseCase,
            getRealSpanInfoLinkTypeUseCase = getRealSpanInfoLinkTypeUseCase,
            getNoteUseCase = getNoteUseCase,
            getProjectFileUseCase = getProjectFileUseCase,
            saveNoteTextUseCase = saveNoteTextUseCase,
            applyEditorEventUseCase = applyEditorEventUseCase,
            applySearchEventUseCase = applySearchEventUseCase,
        )
    }

    val state = ComposeTextState()
    val linkSearchState = ComposeTextState()

    @OptIn(ExperimentalCoroutinesApi::class)
    val linkSearchResultsPaged: Flow<PagingData<Note>> = combine(
        deps.uiState.map { it.project }.distinctUntilChanged(),
        snapshotFlow { linkSearchState.text },
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

    fun onLinkSearchEvent(event: SearchEvent) {
        deps.scope.launchSafe(deps.globalActions::onEvent) {
            applySearchEventUseCase(
                ApplySearchEventInput(
                    event = event,
                    state = linkSearchState,
                ),
            )
        }
    }

    private val editorMutex = Mutex()
    private var lastSavedBodyRef: CharSequence? = null
    private var currentFmVersion: Long = 0L
    private var savedFmVersion: Long = 0L
    private var activeSessionRoutePath: RelativePath? = null

    fun onCloseEditor() {
        deps.scope.launch {
            editorMutex.withLock {
                flushSaveLocked()
                clearSessionLocked()
            }
            deps.globalActions.onEvent(NavigationEvent.GoBack)
        }
    }

    fun onEditorDisposed(routePath: RelativePath) {
        deps.scope.launch {
            editorMutex.withLock {
                if (activeSessionRoutePath == routePath) {
                    flushSaveLocked()
                    clearSessionLocked()
                }
            }
        }
    }

    fun saveNow() {
        deps.scope.launch {
            flushSave()
        }
    }

    suspend fun flushSave() {
        editorMutex.withLock {
            flushSaveLocked()
        }
    }

    suspend fun onNoteDeleted(notePath: RelativePath) {
        editorMutex.withLock {
            if (deps.uiState.value.activeNote?.projectFile?.relativePath == notePath) {
                clearSessionLocked()
            }
        }
    }

    suspend fun onNoteRenamed(oldPath: RelativePath, renamedNote: Note) {
        editorMutex.withLock {
            if (deps.uiState.value.activeNote?.projectFile?.relativePath == oldPath) {
                deps.uiState.update { it.copy(activeNote = renamedNote) }
            }
        }
    }

    suspend fun onNoteFrontMatterUpdated(notePath: RelativePath, frontMatter: FrontMatter) {
        editorMutex.withLock {
            if (deps.uiState.value.activeNote?.projectFile?.relativePath == notePath) {
                deps.uiState.update { it.copy(editorFrontMatter = frontMatter) }
                savedFmVersion = currentFmVersion
            }
        }
    }

    fun showLinkNoteDialog() {
        deps.uiState.update { it.copy(isLinkNoteDialogVisible = true) }
    }

    fun dismissLinkNoteDialog() {
        deps.uiState.update { it.copy(isLinkNoteDialogVisible = false) }
        linkSearchState.setTextAndPlaceCursorAtEnd("")
    }

    init {
        deps.scope.launch {
            snapshotFlow { state.text }
                .drop(1)
                .debounce(2.seconds)
                .collect { flushSave() }
        }
    }

    @OptIn(ExperimentalFoundationApi::class)
    fun onEvent(event: EditorEvent) {
        deps.scope.launchSafe(deps.globalActions::onEvent) {
            val project = deps.uiState.value.project ?: return@launchSafe
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
        deps.scope.launchSafe(deps.globalActions::onEvent) {
            val project = deps.uiState.value.project ?: return@launchSafe

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
                    deps.globalActions.onEvent(NavigationEvent.GoToEditor(note = note))
                }

                SpanInfo.Link.LinkType.FILE -> {
                    val projectFile = getProjectFileUseCase(
                        GetProjectFileInput(
                            project = project,
                            relativePath = RelativePath(span.payload),
                        ),
                    )
                    deps.globalActions.onEvent(NavigationEvent.OpenFile(projectFile.domainFile))
                }

                SpanInfo.Link.LinkType.HTTP -> {
                    deps.globalActions.onEvent(NavigationEvent.OpenUrl(span.payload))
                }
            }
        }
    }

    @OptIn(ExperimentalFoundationApi::class)
    fun onNoteOpened(notePath: RelativePath) {
        deps.scope.launch {
            editorMutex.withLock {
                flushSaveLocked()
                try {
                    val project = deps.uiState.value.project ?: throw Exception()
                    val note = runUseCase(deps.globalActions::onEvent) {
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
                    deps.uiState.update {
                        it.copy(
                            activeNote = note,
                            editorFrontMatter = frontMatter,
                            isViewingMode = true,
                        )
                    }
                } catch (e: CancellationException) {
                    throw e
                } catch (_: Exception) {
                    deps.globalActions.onEvent(NavigationEvent.GoBack)
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
        deps.uiState.update { it.copy(isViewingMode = !it.isViewingMode) }
    }

    private fun updateFrontMatter(transform: (FrontMatter?) -> FrontMatter?) {
        deps.scope.launch {
            editorMutex.withLock {
                deps.uiState.update {
                    it.copy(editorFrontMatter = transform(it.editorFrontMatter))
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
        deps.uiState.update {
            it.copy(
                isLinkNoteDialogVisible = false,
                activeNote = null,
                editorFrontMatter = null,
            )
        }
    }

    private suspend fun flushSaveLocked() {
        val project = deps.uiState.value.project ?: return
        val note = deps.uiState.value.activeNote ?: return
        val currentBodyRef = state.text
        val isBodyDirty = currentBodyRef !== lastSavedBodyRef
        val isFmDirty = currentFmVersion != savedFmVersion
        if (!isBodyDirty && !isFmDirty) return

        val fmVersionToSave = currentFmVersion
        val fm = deps.uiState.value.editorFrontMatter
        val textToSave = NoteDocument(
            frontMatter = fm ?: FrontMatter.Empty,
            body = currentBodyRef.toString(),
        ).serialize()

        runUseCase(deps.globalActions::onEvent) {
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
