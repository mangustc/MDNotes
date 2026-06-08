package com.mangustc.mdnotes.koin.modules

import androidx.room.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import com.mangustc.mdnotes.data.database.LinkPreviewDao
import com.mangustc.mdnotes.data.database.NoteDao
import com.mangustc.mdnotes.data.database.NoteDb
import com.mangustc.mdnotes.data.database.ProjectDao
import com.mangustc.mdnotes.data.linkPreview.CommonLinkPreviewRepository
import com.mangustc.mdnotes.data.project.CommonProjectRepository
import com.mangustc.mdnotes.data.project.CommonSettingsRepository
import com.mangustc.mdnotes.data.project.DesktopPlatformListFilesHandler
import com.mangustc.mdnotes.data.sync.SyncRepositoryFactory
import com.mangustc.mdnotes.domain.repositories.LinkPreviewRepository
import com.mangustc.mdnotes.domain.repositories.PlatformListFilesHandler
import com.mangustc.mdnotes.domain.repositories.ProjectRepository
import com.mangustc.mdnotes.domain.repositories.SettingsRepository
import com.russhwolf.settings.PreferencesSettings
import com.russhwolf.settings.Settings
import io.github.vinceglb.filekit.FileKit
import io.github.vinceglb.filekit.absolutePath
import io.github.vinceglb.filekit.createDirectories
import io.github.vinceglb.filekit.databasesDir
import io.github.vinceglb.filekit.div
import io.github.vinceglb.filekit.exists
import io.ktor.client.HttpClient
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import org.koin.core.module.Module
import org.koin.dsl.bind
import org.koin.dsl.module
import org.koin.plugin.module.dsl.create
import org.koin.plugin.module.dsl.single

private fun provideNoteDb(): NoteDb {
    val directory = FileKit.databasesDir
    if (!directory.exists()) {
        directory.createDirectories()
    }
    val dbPath = (directory / "notes.db").absolutePath()
    val databaseBuilder = Room.databaseBuilder<NoteDb>(
        name = dbPath,
    )
    return databaseBuilder
        .setDriver(BundledSQLiteDriver())
        .fallbackToDestructiveMigration(true)
        .build()
}

private fun provideProjectDao(db: NoteDb): ProjectDao = db.projectDao()

private fun provideNoteDao(db: NoteDb): NoteDao = db.noteDao()

private fun provideLinkPreviewDao(db: NoteDb): LinkPreviewDao = db.linkPreviewDao()

private fun provideHttpClient(): HttpClient = HttpClient {
    install(ContentNegotiation) {
        json(
            Json {
                ignoreUnknownKeys = true
                prettyPrint = false
            },
        )
    }
}

private fun provideFactory(): Settings.Factory =
    PreferencesSettings.Factory()

private fun providePlatformListFilesHandler(): PlatformListFilesHandler =
    DesktopPlatformListFilesHandler()


actual val dataModule: Module
    get() = module {
        single { create(::provideNoteDb) }
        single { create(::provideProjectDao) }
        single { create(::provideNoteDao) }
        single { create(::provideLinkPreviewDao) }
        single { create(::provideHttpClient) }
        single { create(::provideFactory) }
        single { create(::providePlatformListFilesHandler) }

        single<CommonProjectRepository>() bind ProjectRepository::class
        single<CommonSettingsRepository>() bind SettingsRepository::class
        single<CommonLinkPreviewRepository>() bind LinkPreviewRepository::class
        single<SyncRepositoryFactory>()
    }
