package com.mangustc.mdnotes.domain.usecases.notes

import com.mangustc.mdnotes.domain.models.FrontMatter
import com.mangustc.mdnotes.domain.models.Note
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
        val (frontMatter, body) = FrontMatter.splitFromContent(fullText)

        val updatedFrontMatter = if (input.tag in frontMatter.tags) {
            frontMatter.withoutTag(input.tag)
        } else {
            frontMatter.withTag(input.tag)
        }

        projectRepository.writeFile(
            project = input.project,
            relativePath = input.note.projectFile.relativePath,
            byteArray = "${updatedFrontMatter}\n$body".toByteArray(),
            fileExistsStrategy = ProjectRepository.FileExistsStrategy.OVERWRITE,
        )

        return updatedFrontMatter
    }
}
