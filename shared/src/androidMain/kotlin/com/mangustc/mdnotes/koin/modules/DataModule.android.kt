package com.mangustc.mdnotes.koin.modules

import android.content.Context
import androidx.room.Room
import com.mangustc.mdnotes.data.database.NoteDb
import com.mangustc.mdnotes.data.project.AndroidPlatformListFilesHandler
import com.mangustc.mdnotes.domain.repositories.PlatformListFilesHandler
import com.russhwolf.settings.Settings
import com.russhwolf.settings.SharedPreferencesSettings
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.android.Android
import org.koin.core.module.Module
import org.koin.dsl.module
import org.koin.plugin.module.dsl.create

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

private fun provideHttpClientEngine(): HttpClientEngine = Android.create()

private fun provideFactory(context: Context): Settings.Factory =
    SharedPreferencesSettings.Factory(context)

private fun providePlatformListFilesHandler(context: Context): PlatformListFilesHandler =
    AndroidPlatformListFilesHandler(context)

actual val platformDataModule: Module
    get() = module {
        single { create(::provideNoteDb) }
        single { create(::provideHttpClientEngine) }
        single { create(::provideFactory) }
        single { create(::providePlatformListFilesHandler) }
    }
