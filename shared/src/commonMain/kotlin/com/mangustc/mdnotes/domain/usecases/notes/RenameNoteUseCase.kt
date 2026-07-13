package com.mangustc.mdnotes.domain.usecases.notes

import com.mangustc.mdnotes.domain.models.Note
import com.mangustc.mdnotes.domain.models.Project
import com.mangustc.mdnotes.domain.models.RelativePath
import com.mangustc.mdnotes.domain.repositories.ProjectRepository
import com.mangustc.mdnotes.domain.usecases.UseCase
import com.mangustc.mdnotes.domain.util.sanitizeFileName

data class RenameNoteInput(
    val project: Project,
    val note: Note,
    val newName: String,
    val includeText: Boolean = false,
    val includeFrontMatter: Boolean = false,
)

class RenameNoteUseCase(
    private val projectRepository: ProjectRepository,
    private val getNoteUseCase: GetNoteUseCase,
) : UseCase<RenameNoteInput, Note> {
    override suspend fun invoke(input: RenameNoteInput): Note {
        val sanitizedNoteName = sanitizeFileName("${input.newName}.md", "New Note")

        val newProjectFile = projectRepository.moveFile(
            project = input.project,
            relativePath = input.note.projectFile.relativePath,
            newRelativePath = input.note.projectFile.relativePath.parent.resolve(
                RelativePath(sanitizedNoteName),
            ),
            fileExistsStrategy = ProjectRepository.FileExistsStrategy.AUTO_RENAME,
        )
        val note = getNoteUseCase(
            GetNoteInput(
                project = input.project,
                relativePath = newProjectFile.relativePath,
                includeText = input.includeText,
                includeFrontMatter = input.includeFrontMatter,
            ),
        )

        return note
    }
}
