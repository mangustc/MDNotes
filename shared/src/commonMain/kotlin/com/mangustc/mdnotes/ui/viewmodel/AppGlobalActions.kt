package com.mangustc.mdnotes.ui.viewmodel

import com.mangustc.mdnotes.ui.viewmodel.events.AppEvent

interface AppGlobalActions {
    suspend fun updateNoteLists()

    fun onEvent(event: AppEvent)
}
