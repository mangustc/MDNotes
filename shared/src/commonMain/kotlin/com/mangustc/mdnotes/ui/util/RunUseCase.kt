package com.mangustc.mdnotes.ui.util

import com.mangustc.mdnotes.ui.viewmodel.events.AppEvent
import com.mangustc.mdnotes.ui.viewmodel.events.NotificationEvent
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

inline fun <R> runUseCase(onEvent: (AppEvent) -> Unit, block: () -> R): Result<R> =
    try {
        Result.success(block())
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        e.printStackTrace()
        onEvent(NotificationEvent.FromException(e))
        Result.failure(e)
    }

fun CoroutineScope.launchSafe(
    onEvent: (AppEvent) -> Unit,
    block: suspend CoroutineScope.() -> Unit,
): Job = launch {
    try {
        block()
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        e.printStackTrace()
        onEvent(NotificationEvent.FromException(e))
    }
}
