package com.mangustc.mdnotes.ui.viewmodel

import com.mangustc.mdnotes.domain.models.Attachment
import com.mangustc.mdnotes.domain.models.FrontMatter
import com.mangustc.mdnotes.domain.models.LinkPreview
import com.mangustc.mdnotes.domain.models.MessageBody
import com.mangustc.mdnotes.domain.models.Note
import com.mangustc.mdnotes.domain.models.Project
import com.mangustc.mdnotes.domain.models.Settings

sealed interface ActiveDialog {
    data object None : ActiveDialog
    data class CreateNote(val nameInput: String = "") : ActiveDialog
    data class DeleteNote(val note: Note) : ActiveDialog
    data class RenameNote(val note: Note, val nameInput: String) : ActiveDialog
    data class ShowInfo(val note: Note) : ActiveDialog
    data object Settings : ActiveDialog
}

data class AppUiState(
    val project: Project? = null,
    val error: String? = null,
    val activeNote: Note? = null,
    val allProjectTags: List<String> = emptyList(),
    val isSyncInProgress: Boolean = false,
    val settings: Settings? = null,

    val activeDialog: ActiveDialog = ActiveDialog.None,

    val isLinkNoteDialogVisible: Boolean = false,
    val editorFrontMatter: FrontMatter? = null,
    val isViewingMode: Boolean = true,

    val messengerIsLoading: Boolean = true,
    val messengerNewNoteText: String = "",
    val messengerLinkPreviews: Map<String, LinkPreview?> = emptyMap(),
    val messengerEditingNote: Note? = null,
    val messengerPinnedMessages: List<MessageBody> = emptyList(),
    val messengerSelectedNotes: Set<MessageBody> = emptySet(),

    val pendingIntentAttachments: List<Attachment> = emptyList(),
)