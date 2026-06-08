package com.mangustc.mdnotes.domain.repositories

import com.mangustc.mdnotes.domain.models.DomainFile
import com.mangustc.mdnotes.domain.models.Project
import com.mangustc.mdnotes.domain.models.Settings

interface SettingsRepository {
    suspend fun setSettings(project: Project, settings: Settings): Settings
    suspend fun getSettings(project: Project): Settings
    suspend fun setProjectPath(path: DomainFile)
    suspend fun getProjectPath(): DomainFile?
}