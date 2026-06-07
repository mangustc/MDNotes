package com.mangustc.mdnotes.domain.usecases.project

import com.mangustc.mdnotes.domain.models.Project
import com.mangustc.mdnotes.domain.repositories.ProjectRepository
import com.mangustc.mdnotes.domain.usecases.UseCase

data class SyncDatabaseInput(
    val project: Project,
)

class SyncDatabaseUseCase(
    private val projectRepository: ProjectRepository,
) : UseCase<SyncDatabaseInput, Unit> {
    override suspend fun invoke(input: SyncDatabaseInput) {
        projectRepository.syncDatabase(input.project)
    }
}
