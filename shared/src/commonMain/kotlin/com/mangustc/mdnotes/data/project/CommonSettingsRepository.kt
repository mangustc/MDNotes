package com.mangustc.mdnotes.data.project

import com.mangustc.mdnotes.domain.models.DomainFile
import com.mangustc.mdnotes.domain.models.Project
import com.mangustc.mdnotes.domain.models.Settings
import com.mangustc.mdnotes.domain.repositories.PlatformPathHandler
import com.mangustc.mdnotes.domain.repositories.SettingsRepository
import com.russhwolf.settings.Settings.Factory
import io.github.vinceglb.filekit.PlatformFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json

class CommonSettingsRepository(
    private val settingsFactory: Factory,
    private val platformPathHandler: PlatformPathHandler,
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

        platformPathHandler.takePersistablePathPermission(path)

        prefs.putString(KEY_PROJECT_URI, path.path)
    }

    override suspend fun getProjectPath(): DomainFile? = withContext(Dispatchers.IO) {
        val prefs = settingsFactory.create(KEY_PROJECT_URI)
        val uriString = prefs.getStringOrNull(KEY_PROJECT_URI) ?: return@withContext null
        DomainFile(PlatformFile(uriString))
    }

    companion object {
        private const val KEY_SETTINGS = "settings"
        private const val KEY_PREFIX_PROJECT = "settings_"
        private const val KEY_PROJECT_URI = "project_uri"
    }
}
