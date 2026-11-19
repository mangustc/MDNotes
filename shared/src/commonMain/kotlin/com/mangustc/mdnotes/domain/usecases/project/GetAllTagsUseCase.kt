package com.mangustc.mdnotes.domain.usecases.project

import com.mangustc.mdnotes.domain.models.Project
import com.mangustc.mdnotes.domain.repositories.ProjectRepository
import com.mangustc.mdnotes.domain.usecases.FlowUseCase
import kotlinx.coroutines.flow.Flow

data class GetAllTagsInput(
    val project: Project,
)

class GetAllTagsUseCase(
    private val projectRepository: ProjectRepository,
) : FlowUseCase<GetAllTagsInput, List<String>> {
    override fun invoke(input: GetAllTagsInput): Flow<List<String>> =
        projectRepository.getAllTags(input.project)
}
