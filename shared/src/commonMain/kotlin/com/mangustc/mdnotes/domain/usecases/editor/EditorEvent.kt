package com.mangustc.mdnotes.domain.usecases.editor

import com.mangustc.mdnotes.domain.models.DomainFile
import com.mangustc.mdnotes.domain.models.Note

sealed interface EditorEvent {
    data class AttachPhoto(val path: DomainFile) : EditorEvent
    data class AttachFile(
        val path: DomainFile,
        val displayName: String? = null,
    ) : EditorEvent

    data class InsertNoteLink(val note: Note) : EditorEvent

    data object Bold : EditorEvent
    data object Italic : EditorEvent
    data object Code : EditorEvent
    data object Undo : EditorEvent
    data object Redo : EditorEvent
}