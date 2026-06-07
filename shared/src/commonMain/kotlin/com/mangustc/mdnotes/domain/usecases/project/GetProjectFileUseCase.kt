package com.mangustc.mdnotes.domain.usecases.project

import com.mangustc.mdnotes.domain.models.Project
import com.mangustc.mdnotes.domain.models.ProjectFile
import com.mangustc.mdnotes.domain.models.RelativePath
import com.mangustc.mdnotes.domain.repositories.ProjectRepository
import com.mangustc.mdnotes.domain.usecases.UseCase

data class GetProjectFileInput(
    val project: Project,
    val relativePath: RelativePath,
)

class GetProjectFileUseCase(
    private val projectRepository: ProjectRepository,
) : UseCase<GetProjectFileInput, ProjectFile> {
    override suspend fun invoke(input: GetProjectFileInput): ProjectFile {
        return projectRepository.getProjectFile(input.project, input.relativePath)
    }
}
