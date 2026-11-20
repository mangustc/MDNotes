package com.mangustc.mdnotes.koin.modules

import com.mangustc.mdnotes.data.database.LinkPreviewDao
import com.mangustc.mdnotes.data.database.NoteDao
import com.mangustc.mdnotes.data.database.NoteDb
import com.mangustc.mdnotes.data.database.ProjectDao
import com.mangustc.mdnotes.data.linkPreview.CommonLinkPreviewRepository
import com.mangustc.mdnotes.data.project.CommonProjectRepository
import com.mangustc.mdnotes.data.project.CommonSettingsRepository
import com.mangustc.mdnotes.data.sync.CommonSyncRepositoryFactory
import com.mangustc.mdnotes.domain.repositories.LinkPreviewRepository
import com.mangustc.mdnotes.domain.repositories.ProjectRepository
import com.mangustc.mdnotes.domain.repositories.SettingsRepository
import com.mangustc.mdnotes.domain.repositories.SyncRepositoryFactory
import io.ktor.client.HttpClient
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import org.koin.core.module.Module
import org.koin.dsl.bind
import org.koin.dsl.module
import org.koin.plugin.module.dsl.create
import org.koin.plugin.module.dsl.single

expect val platformDataModule: Module

private fun provideProjectDao(db: NoteDb): ProjectDao = db.projectDao()

private fun provideNoteDao(db: NoteDb): NoteDao = db.noteDao()

private fun provideLinkPreviewDao(db: NoteDb): LinkPreviewDao = db.linkPreviewDao()

private fun provideHttpClient(engine: HttpClientEngine): HttpClient = HttpClient(engine) {
    install(ContentNegotiation) {
        json(
            Json {
                ignoreUnknownKeys = true
                prettyPrint = false
            },
        )
    }
}

val dataModule: Module
    get() = module {
        includes(platformDataModule)

        single { create(::provideProjectDao) }
        single { create(::provideNoteDao) }
        single { create(::provideLinkPreviewDao) }
        single { create(::provideHttpClient) }

        single<CommonProjectRepository>() bind ProjectRepository::class
        single<CommonSettingsRepository>() bind SettingsRepository::class
        single<CommonLinkPreviewRepository>() bind LinkPreviewRepository::class
        single<CommonSyncRepositoryFactory>() bind SyncRepositoryFactory::class
    }
