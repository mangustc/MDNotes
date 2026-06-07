package com.mangustc.mdnotes.domain.usecases.project

import com.mangustc.mdnotes.domain.models.Project
import com.mangustc.mdnotes.domain.repositories.ProjectRepository
import com.mangustc.mdnotes.domain.usecases.UseCase

data class GetAllTagsInput(
    val project: Project,
)

class GetAllTagsUseCase(
    private val projectRepository: ProjectRepository,
) : UseCase<GetAllTagsInput, List<String>> {
    override suspend fun invoke(input: GetAllTagsInput): List<String> {
        return projectRepository.getAllTags(input.project)
    }
}
