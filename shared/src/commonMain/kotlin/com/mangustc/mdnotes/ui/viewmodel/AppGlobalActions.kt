package com.mangustc.mdnotes.ui.viewmodel

import com.mangustc.mdnotes.domain.models.FrontMatter
import com.mangustc.mdnotes.domain.models.Note
import com.mangustc.mdnotes.domain.models.RelativePath
import com.mangustc.mdnotes.ui.viewmodel.events.AppEvent

interface AppGlobalActions {
    fun onEvent(event: AppEvent)

    suspend fun flushEditorSave()

    suspend fun onNoteDeleted(notePath: RelativePath)

    suspend fun onNoteRenamed(oldPath: RelativePath, renamedNote: Note)

    suspend fun onNoteFrontMatterUpdated(notePath: RelativePath, frontMatter: FrontMatter)
}
