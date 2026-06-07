package com.mangustc.mdnotes.domain.usecases.messenger

import androidx.paging.PagingData
import androidx.paging.map
import com.mangustc.mdnotes.domain.models.FrontMatter
import com.mangustc.mdnotes.domain.models.MessageBody
import com.mangustc.mdnotes.domain.models.Project
import com.mangustc.mdnotes.domain.models.SearchQuery
import com.mangustc.mdnotes.domain.repositories.ProjectRepository
import com.mangustc.mdnotes.domain.usecases.FlowUseCase
import com.mangustc.mdnotes.domain.usecases.project.GetProjectFileInput
import com.mangustc.mdnotes.domain.usecases.project.GetProjectFileUseCase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

data class GetMessagesInput(
    val project: Project,
)

class GetMessagesUseCase(
    private val projectRepository: ProjectRepository,
    private val getProjectFileUseCase: GetProjectFileUseCase,
) : FlowUseCase<GetMessagesInput, PagingData<MessageBody>> {
    override fun invoke(input: GetMessagesInput): Flow<PagingData<MessageBody>> =
        projectRepository.getNotesPaged(
            input.project,
            SearchQuery(
                tagFilters = listOf(FrontMatter.QUICK_NOTE_TAG),
                sortBy = SearchQuery.SortBy.CREATED_AT,
            ),
            includeText = true,
            includeFrontMatter = false,
        ).map { pagingData ->
            pagingData.map { note ->
                note.toMessageBody {
                    getProjectFileUseCase(
                        GetProjectFileInput(
                            project = input.project,
                            relativePath = it,
                        ),
                    )
                }
            }
        }
}
