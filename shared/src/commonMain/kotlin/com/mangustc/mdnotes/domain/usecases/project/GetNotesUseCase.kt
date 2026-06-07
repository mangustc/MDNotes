package com.mangustc.mdnotes.domain.usecases.project

import androidx.paging.PagingData
import com.mangustc.mdnotes.domain.models.FrontMatter
import com.mangustc.mdnotes.domain.models.Note
import com.mangustc.mdnotes.domain.models.Project
import com.mangustc.mdnotes.domain.models.SearchQuery
import com.mangustc.mdnotes.domain.repositories.ProjectRepository
import com.mangustc.mdnotes.domain.usecases.FlowUseCase
import kotlinx.coroutines.flow.Flow

data class GetNotesInput(
    val project: Project,
    val searchQueryString: String,
)

class GetNotesUseCase(
    private val projectRepository: ProjectRepository,
) : FlowUseCase<GetNotesInput, PagingData<Note>> {
    override fun invoke(input: GetNotesInput): Flow<PagingData<Note>> =
        projectRepository.getNotesPaged(
            project = input.project,
            query = SearchQuery.parse(input.searchQueryString.trim()).let {
                it.copy(
                    negatedTagFilters = it.negatedTagFilters + FrontMatter.QUICK_NOTE_TAG,
                    pinnedFirst = true,
                )
            },
        )
}
