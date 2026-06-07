package com.mangustc.mdnotes.ui.viewmodel.events

import com.mangustc.mdnotes.domain.models.FileSystemPath
import com.mangustc.mdnotes.domain.models.Note

sealed interface NavigationEvent : AppEvent {
    data class GoToEditor(val note: Note) : NavigationEvent
    data object GoBack : NavigationEvent
    data object OpenDrawer : NavigationEvent
    data object CloseDrawer : NavigationEvent
    data class OpenFile(val uri: FileSystemPath) : NavigationEvent
    data class OpenUrl(val url: String) : NavigationEvent
}