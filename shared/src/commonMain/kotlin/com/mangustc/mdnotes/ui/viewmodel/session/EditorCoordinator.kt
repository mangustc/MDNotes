package com.mangustc.mdnotes.ui.viewmodel.session

import com.mangustc.mdnotes.domain.models.FrontMatter
import com.mangustc.mdnotes.domain.models.Note
import com.mangustc.mdnotes.domain.models.RelativePath
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

interface EditorSessionHandler {
    suspend fun flushSave()

    suspend fun onNoteDeleted(notePath: RelativePath)

    suspend fun onNoteRenamed(oldPath: RelativePath, renamedNote: Note)

    suspend fun onNoteFrontMatterUpdated(notePath: RelativePath, frontMatter: FrontMatter)
}

class EditorCoordinator {
    private val _activeNote = MutableStateFlow<Note?>(null)
    val activeNote: StateFlow<Note?> = _activeNote.asStateFlow()

    private var handler: EditorSessionHandler? = null

    fun register(handler: EditorSessionHandler) {
        this.handler = handler
    }

    fun unregister(handler: EditorSessionHandler) {
        if (this.handler === handler) {
            this.handler = null
        }
    }

    fun setActiveNote(note: Note?) {
        _activeNote.value = note
    }

    suspend fun flushSave() {
        handler?.flushSave()
    }

    suspend fun onNoteDeleted(notePath: RelativePath) {
        handler?.onNoteDeleted(notePath)
    }

    suspend fun onNoteRenamed(oldPath: RelativePath, renamedNote: Note) {
        handler?.onNoteRenamed(oldPath, renamedNote)
    }

    suspend fun onNoteFrontMatterUpdated(notePath: RelativePath, frontMatter: FrontMatter) {
        handler?.onNoteFrontMatterUpdated(notePath, frontMatter)
    }
}
