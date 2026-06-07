package com.mangustc.mdnotes.data.sync

import com.mangustc.mdnotes.domain.models.Settings
import com.mangustc.mdnotes.domain.repositories.SyncRepository
import com.mangustc.mdnotes.domain.usecases.sync.ValidSyncProvider
import io.ktor.client.HttpClient

class SyncRepositoryFactory(
    private val client: HttpClient,
) {
    fun create(settings: Settings): SyncRepository? {
        return when (settings.syncProvider) {
            ValidSyncProvider.YANDEX -> YandexSyncRepository(
                oauthToken = settings.yandexOauthToken,
                client = client,
            )

            ValidSyncProvider.NONE -> null
        }
    }
}