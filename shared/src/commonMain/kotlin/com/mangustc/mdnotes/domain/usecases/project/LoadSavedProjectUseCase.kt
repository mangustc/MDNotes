package com.mangustc.mdnotes.domain.usecases.project

import com.mangustc.mdnotes.domain.models.Project
import com.mangustc.mdnotes.domain.repositories.ProjectRepository
import com.mangustc.mdnotes.domain.repositories.SettingsRepository
import com.mangustc.mdnotes.domain.usecases.UseCase

class LoadSavedProjectUseCase(
    private val projectRepository: ProjectRepository,
    private val settingsRepository: SettingsRepository,
) : UseCase<Unit, Project?> {
    override suspend fun invoke(input: Unit): Project? {
        val savedProjectPath = settingsRepository.getProjectPath() ?: return null
        return projectRepository.buildProject(savedProjectPath)
    }
}
