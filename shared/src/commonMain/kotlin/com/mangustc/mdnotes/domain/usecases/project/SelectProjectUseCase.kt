package com.mangustc.mdnotes.domain.usecases.project

import com.mangustc.mdnotes.domain.models.DomainFile
import com.mangustc.mdnotes.domain.models.Project
import com.mangustc.mdnotes.domain.repositories.ProjectRepository
import com.mangustc.mdnotes.domain.repositories.SettingsRepository
import com.mangustc.mdnotes.domain.usecases.UseCase

data class SelectProjectInput(
    val projectPath: DomainFile,
)

class SelectProjectUseCase(
    private val projectRepository: ProjectRepository,
    private val settingsRepository: SettingsRepository,
) : UseCase<SelectProjectInput, Project> {
    override suspend fun invoke(input: SelectProjectInput): Project {
        settingsRepository.setProjectPath(input.projectPath)
        return projectRepository.buildProject(input.projectPath)
    }
}
