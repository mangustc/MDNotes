package com.mangustc.mdnotes.domain.usecases.search

sealed interface SearchEvent {
    data object AppendTag : SearchEvent
    data object AppendName : SearchEvent
    data object ToggleNegation : SearchEvent
    data object Clear : SearchEvent
}