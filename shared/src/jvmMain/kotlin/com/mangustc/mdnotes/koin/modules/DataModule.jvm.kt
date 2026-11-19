package com.mangustc.mdnotes.koin.modules

import androidx.room.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import com.mangustc.mdnotes.data.database.NoteDb
import com.mangustc.mdnotes.data.project.DesktopPlatformListFilesHandler
import com.mangustc.mdnotes.domain.repositories.PlatformListFilesHandler
import com.russhwolf.settings.PreferencesSettings
import com.russhwolf.settings.Settings
import io.github.vinceglb.filekit.FileKit
import io.github.vinceglb.filekit.absolutePath
import io.github.vinceglb.filekit.createDirectories
import io.github.vinceglb.filekit.databasesDir
import io.github.vinceglb.filekit.div
import io.github.vinceglb.filekit.exists
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.okhttp.OkHttp
import org.koin.core.module.Module
import org.koin.dsl.module
import org.koin.plugin.module.dsl.create

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

private fun provideHttpClientEngine(): HttpClientEngine = OkHttp.create()

private fun provideFactory(): Settings.Factory =
    PreferencesSettings.Factory()

private fun providePlatformListFilesHandler(): PlatformListFilesHandler =
    DesktopPlatformListFilesHandler()

actual val platformDataModule: Module
    get() = module {
        single { create(::provideNoteDb) }
        single { create(::provideHttpClientEngine) }
        single { create(::provideFactory) }
        single { create(::providePlatformListFilesHandler) }
    }
