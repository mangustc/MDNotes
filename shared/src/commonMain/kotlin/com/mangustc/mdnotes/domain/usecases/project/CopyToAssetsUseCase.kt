package com.mangustc.mdnotes.domain.usecases.project

import com.mangustc.mdnotes.domain.models.FileSystemPath
import com.mangustc.mdnotes.domain.models.Project
import com.mangustc.mdnotes.domain.models.ProjectFile
import com.mangustc.mdnotes.domain.repositories.ProjectRepository
import com.mangustc.mdnotes.domain.usecases.UseCase

data class CopyToAssetsInput(
    val project: Project,
    val assetPath: FileSystemPath,
)

class CopyToAssetsUseCase(
    private val projectRepository: ProjectRepository,
) : UseCase<CopyToAssetsInput, ProjectFile> {
    override suspend fun invoke(input: CopyToAssetsInput): ProjectFile {
        return projectRepository.copyFromFileSystem(
            project = input.project,
            fromPath = input.assetPath,
            toDirPath = input.project.assetsRelativePath,
        )
    }
}
