package com.mangustc.mdnotes.domain.usecases.notes

import com.mangustc.mdnotes.domain.models.Note
import com.mangustc.mdnotes.domain.models.Project
import com.mangustc.mdnotes.domain.repositories.ProjectRepository
import com.mangustc.mdnotes.domain.usecases.UseCase

data class SaveNoteTextInput(
    val project: Project,
    val note: Note,
    val text: String,
)

class SaveNoteTextUseCase(
    val projectRepository: ProjectRepository,
) : UseCase<SaveNoteTextInput, Unit> {
    override suspend fun invoke(input: SaveNoteTextInput) {
        projectRepository.writeFile(
            project = input.project,
            relativePath = input.note.projectFile.relativePath,
            byteArray = input.text.toByteArray(),
            fileExistsStrategy = ProjectRepository.FileExistsStrategy.OVERWRITE,
        )
    }
}
