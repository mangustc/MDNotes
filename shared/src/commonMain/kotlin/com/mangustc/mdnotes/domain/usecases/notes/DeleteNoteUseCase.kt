package com.mangustc.mdnotes.domain.usecases.notes

import com.mangustc.mdnotes.domain.models.Note
import com.mangustc.mdnotes.domain.models.Project
import com.mangustc.mdnotes.domain.repositories.ProjectRepository
import com.mangustc.mdnotes.domain.usecases.UseCase

data class DeleteNoteInput(
    val project: Project,
    val note: Note,
)

class DeleteNoteUseCase(
    private val projectRepository: ProjectRepository,
) : UseCase<DeleteNoteInput, Unit> {
    override suspend fun invoke(input: DeleteNoteInput) {
        projectRepository.deleteFile(
            project = input.project,
            relativePath = input.note.projectFile.relativePath,
        )
    }
}
