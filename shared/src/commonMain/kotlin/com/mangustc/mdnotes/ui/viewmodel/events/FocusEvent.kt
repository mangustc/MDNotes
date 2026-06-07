package com.mangustc.mdnotes.ui.viewmodel.events

sealed interface FocusEvent : AppEvent {
    data object ClearFocus : FocusEvent
}