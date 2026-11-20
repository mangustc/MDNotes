package com.mangustc.mdnotes.domain.repositories

import com.mangustc.mdnotes.domain.models.Settings

fun interface SyncRepositoryFactory {
    fun create(settings: Settings): SyncRepository?
}
