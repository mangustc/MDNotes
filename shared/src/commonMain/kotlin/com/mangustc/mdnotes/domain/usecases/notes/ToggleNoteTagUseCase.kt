package com.mangustc.mdnotes.domain.usecases.notes

import com.mangustc.mdnotes.domain.models.FrontMatter
import com.mangustc.mdnotes.domain.models.Note
import com.mangustc.mdnotes.domain.models.NoteDocument
import com.mangustc.mdnotes.domain.models.Project
import com.mangustc.mdnotes.domain.repositories.ProjectRepository
import com.mangustc.mdnotes.domain.usecases.UseCase

data class ToggleNoteTagInput(
    val project: Project,
    val note: Note,
    val tag: String,
)

class ToggleNoteTagUseCase(
    private val projectRepository: ProjectRepository,
) : UseCase<ToggleNoteTagInput, FrontMatter> {
    override suspend fun invoke(input: ToggleNoteTagInput): FrontMatter {
        val fullText =
            projectRepository.readFile(input.project, input.note.projectFile.relativePath)
                .decodeToString()
        val document = NoteDocument.parse(fullText)

        val updatedFrontMatter = if (input.tag in document.frontMatter.tags) {
            document.frontMatter.withoutTag(input.tag)
        } else {
            document.frontMatter.withTag(input.tag)
        }

        projectRepository.writeFile(
            project = input.project,
            relativePath = input.note.projectFile.relativePath,
            byteArray = document.copy(frontMatter = updatedFrontMatter).serialize().toByteArray(),
            fileExistsStrategy = ProjectRepository.FileExistsStrategy.OVERWRITE,
        )

        return updatedFrontMatter
    }
}
