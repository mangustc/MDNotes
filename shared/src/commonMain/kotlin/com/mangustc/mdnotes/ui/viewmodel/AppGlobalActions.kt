package com.mangustc.mdnotes.ui.viewmodel

import com.mangustc.mdnotes.ui.viewmodel.events.AppEvent

interface AppGlobalActions {
    fun onEvent(event: AppEvent)
}
