package com.mangustc.mdnotes.ui.util

import com.mangustc.mdnotes.ui.viewmodel.events.AppEvent
import com.mangustc.mdnotes.ui.viewmodel.events.NotificationEvent

inline fun <R> runUseCase(onEvent: (AppEvent) -> Unit, block: () -> R): Result<R> =
    try {
        Result.success(block())
    } catch (e: Exception) {
        e.printStackTrace()
        onEvent(NotificationEvent.FromException(e))
        Result.failure(e)
    }
