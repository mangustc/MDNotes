package com.mangustc.mdnotes.domain.usecases.messenger

import com.mangustc.mdnotes.domain.models.FrontMatter
import com.mangustc.mdnotes.domain.models.MessageBody
import com.mangustc.mdnotes.domain.models.Project
import com.mangustc.mdnotes.domain.models.SearchQuery
import com.mangustc.mdnotes.domain.repositories.ProjectRepository
import com.mangustc.mdnotes.domain.usecases.UseCase
import com.mangustc.mdnotes.domain.usecases.project.GetProjectFileInput
import com.mangustc.mdnotes.domain.usecases.project.GetProjectFileUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class GetPinnedMessagesInput(
    val project: Project,
)

class GetPinnedMessagesUseCase(
    private val projectRepository: ProjectRepository,
    private val getProjectFileUseCase: GetProjectFileUseCase,
) : UseCase<GetPinnedMessagesInput, List<MessageBody>> {
    override suspend fun invoke(input: GetPinnedMessagesInput): List<MessageBody> =
        withContext(Dispatchers.Default) {
            projectRepository.getNotes(
                input.project,
                SearchQuery(
                    tagFilters = listOf(FrontMatter.QUICK_NOTE_TAG, FrontMatter.PINNED_TAG),
                    sortBy = SearchQuery.SortBy.CREATED_AT,
                ),
                includeText = true,
                includeFrontMatter = false,
            ).map { note ->
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
