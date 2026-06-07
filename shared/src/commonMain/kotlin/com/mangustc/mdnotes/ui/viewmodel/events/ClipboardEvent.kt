package com.mangustc.mdnotes.ui.viewmodel.events

sealed interface ClipboardEvent : AppEvent {
    data class Copy(val text: String) : ClipboardEvent
}