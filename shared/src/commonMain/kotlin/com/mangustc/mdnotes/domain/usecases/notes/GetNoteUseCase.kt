package com.mangustc.mdnotes.domain.usecases.notes

import com.mangustc.mdnotes.domain.models.Note
import com.mangustc.mdnotes.domain.models.Project
import com.mangustc.mdnotes.domain.models.RelativePath
import com.mangustc.mdnotes.domain.repositories.ProjectRepository
import com.mangustc.mdnotes.domain.usecases.UseCase

data class GetNoteInput(
    val project: Project,
    val relativePath: RelativePath,
    val includeText: Boolean = false,
    val includeFrontMatter: Boolean = false,
)

class GetNoteUseCase(
    private val projectRepository: ProjectRepository,
) : UseCase<GetNoteInput, Note> {
    override suspend fun invoke(input: GetNoteInput): Note {
        val note = projectRepository.getNote(
            project = input.project,
            relativePath = input.relativePath,
            includeText = input.includeText,
            includeFrontMatter = input.includeFrontMatter,
        )

        return note
    }
}
