package com.mangustc.mdnotes.ui.viewmodel.session

import com.mangustc.mdnotes.ui.viewmodel.events.AppEvent
import com.mangustc.mdnotes.ui.viewmodel.events.ClipboardEvent
import com.mangustc.mdnotes.ui.viewmodel.events.FocusEvent
import com.mangustc.mdnotes.ui.viewmodel.events.NavigationEvent
import com.mangustc.mdnotes.ui.viewmodel.events.NotificationEvent
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow

class AppEventBus {
    private val _navigationEvents = Channel<NavigationEvent>(Channel.BUFFERED)
    val navigationEvents: Flow<NavigationEvent> = _navigationEvents.receiveAsFlow()

    private val _notificationEvents = Channel<NotificationEvent>(Channel.BUFFERED)
    val notificationEvents: Flow<NotificationEvent> = _notificationEvents.receiveAsFlow()

    private val _clipboardEvents = Channel<ClipboardEvent>(Channel.BUFFERED)
    val clipboardEvents: Flow<ClipboardEvent> = _clipboardEvents.receiveAsFlow()

    private val _focusEvents = Channel<FocusEvent>(Channel.BUFFERED)
    val focusEvents: Flow<FocusEvent> = _focusEvents.receiveAsFlow()

    fun emit(event: AppEvent) {
        when (event) {
            is NavigationEvent -> _navigationEvents.trySend(event)
            is NotificationEvent -> _notificationEvents.trySend(event)
            is ClipboardEvent -> _clipboardEvents.trySend(event)
            is FocusEvent -> _focusEvents.trySend(event)
        }
    }
}
