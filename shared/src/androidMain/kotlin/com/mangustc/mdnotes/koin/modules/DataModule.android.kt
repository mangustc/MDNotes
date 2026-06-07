package com.mangustc.mdnotes.koin.modules

import android.content.Context
import androidx.room.Room
import com.mangustc.mdnotes.data.database.LinkPreviewDao
import com.mangustc.mdnotes.data.database.NoteDao
import com.mangustc.mdnotes.data.database.NoteDb
import com.mangustc.mdnotes.data.database.ProjectDao
import com.mangustc.mdnotes.data.linkPreview.CommonLinkPreviewRepository
import com.mangustc.mdnotes.data.project.AndroidPlatformPathHandler
import com.mangustc.mdnotes.data.project.AndroidProjectRepository
import com.mangustc.mdnotes.data.project.CommonSettingsRepository
import com.mangustc.mdnotes.data.sync.SyncRepositoryFactory
import com.mangustc.mdnotes.domain.repositories.LinkPreviewRepository
import com.mangustc.mdnotes.domain.repositories.PlatformPathHandler
import com.mangustc.mdnotes.domain.repositories.ProjectRepository
import com.mangustc.mdnotes.domain.repositories.SettingsRepository
import com.russhwolf.settings.Settings
import com.russhwolf.settings.SharedPreferencesSettings
import io.ktor.client.HttpClient
import io.ktor.client.engine.android.Android
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import org.koin.core.module.Module
import org.koin.dsl.bind
import org.koin.dsl.module
import org.koin.plugin.module.dsl.create
import org.koin.plugin.module.dsl.single

private fun provideNoteDb(context: Context): NoteDb {
    val appContext = context.applicationContext
    val dbFile = appContext.getDatabasePath("database-notes")
    val databaseBuilder = Room.databaseBuilder<NoteDb>(
        context = appContext,
        name = dbFile.absolutePath,
    )
    return databaseBuilder
        .fallbackToDestructiveMigration(true)
        .build()
}

private fun provideProjectDao(db: NoteDb): ProjectDao = db.projectDao()

private fun provideNoteDao(db: NoteDb): NoteDao = db.noteDao()

private fun provideLinkPreviewDao(db: NoteDb): LinkPreviewDao = db.linkPreviewDao()

private fun provideHttpClient(): HttpClient = HttpClient(Android) {
    install(ContentNegotiation) {
        json(
            Json {
                ignoreUnknownKeys = true
                prettyPrint = false
            },
        )
    }
}

private fun provideFactory(context: Context): Settings.Factory = SharedPreferencesSettings.Factory(context)

private fun providePlatformPathHandler(context: Context): PlatformPathHandler =
    AndroidPlatformPathHandler(context)


actual val dataModule: Module
    get() = module {
        single { create(::provideNoteDb) }
        single { create(::provideProjectDao) }
        single { create(::provideNoteDao) }
        single { create(::provideLinkPreviewDao) }
        single { create(::provideHttpClient) }
        single { create(::provideFactory) }
        single { create(::providePlatformPathHandler) }

        single<AndroidProjectRepository>() bind ProjectRepository::class
        single<CommonSettingsRepository>() bind SettingsRepository::class
        single<CommonLinkPreviewRepository>() bind LinkPreviewRepository::class
        single<SyncRepositoryFactory>()
    }