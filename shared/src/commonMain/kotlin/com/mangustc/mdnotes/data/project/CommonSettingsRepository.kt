package com.mangustc.mdnotes.data.project

import com.mangustc.mdnotes.domain.models.DomainFile
import com.mangustc.mdnotes.domain.models.Project
import com.mangustc.mdnotes.domain.models.Settings
import com.mangustc.mdnotes.domain.repositories.SettingsRepository
import com.russhwolf.settings.Settings.Factory
import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.bookmarkData
import io.github.vinceglb.filekit.exists
import io.github.vinceglb.filekit.fromBookmarkData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json

class CommonSettingsRepository(
    private val settingsFactory: Factory,
) : SettingsRepository {
    private val json = Json { ignoreUnknownKeys = true; prettyPrint = false }

    override suspend fun setSettings(
        project: Project,
        settings: Settings,
    ): Settings = withContext(Dispatchers.IO) {
        val prefs = settingsFactory.create("${KEY_PREFIX_PROJECT}${project.name}")
        val settingsJson = json.encodeToString(settings)
        prefs.putString(KEY_SETTINGS, settingsJson)
        settings
    }

    override suspend fun getSettings(project: Project): Settings = withContext(Dispatchers.IO) {
        val prefs = settingsFactory.create("${KEY_PREFIX_PROJECT}${project.name}")
        val settingsJson = prefs.getStringOrNull(KEY_SETTINGS)
        val settings = runCatching {
            if (settingsJson == null) return@runCatching null
            json.decodeFromString<Settings>(settingsJson)
        }.getOrNull() ?: Settings.EMPTY
        settings
    }

    override suspend fun setProjectPath(path: DomainFile) = withContext(Dispatchers.IO) {
        val prefs = settingsFactory.create(KEY_PROJECT_URI)
        val bookmark = path.file.bookmarkData()
        prefs.putString(KEY_PROJECT_URI, bookmark.bytes.decodeToString())
    }

    override suspend fun getProjectPath(): DomainFile? = withContext(Dispatchers.IO) {
        val prefs = settingsFactory.create(KEY_PROJECT_URI)
        val uriString = prefs.getStringOrNull(KEY_PROJECT_URI) ?: return@withContext null
        try {
            val file = PlatformFile.fromBookmarkData(uriString.toByteArray())

            if (file.exists()) {
                DomainFile(file)
            } else {
                prefs.remove(KEY_PROJECT_URI)
                null
            }
        } catch (_: Exception) {
            prefs.remove(KEY_PROJECT_URI)
            null
        }
    }

    companion object {
        private const val KEY_SETTINGS = "settings"
        private const val KEY_PREFIX_PROJECT = "settings_"
        private const val KEY_PROJECT_URI = "project_uri"
    }
}
