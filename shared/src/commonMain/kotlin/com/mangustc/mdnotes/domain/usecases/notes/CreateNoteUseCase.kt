package com.mangustc.mdnotes.domain.usecases.notes

import com.mangustc.mdnotes.domain.models.FrontMatter
import com.mangustc.mdnotes.domain.models.Note
import com.mangustc.mdnotes.domain.models.NoteDocument
import com.mangustc.mdnotes.domain.models.Project
import com.mangustc.mdnotes.domain.models.RelativePath
import com.mangustc.mdnotes.domain.repositories.ProjectRepository
import com.mangustc.mdnotes.domain.usecases.UseCase
import com.mangustc.mdnotes.domain.util.sanitizeFileName
import kotlin.time.Clock

data class CreateNoteInput(
    val project: Project,
    val name: String,
    val tags: List<String> = emptyList(),
    val initialText: String = "",
    val includeText: Boolean = false,
    val includeFrontMatter: Boolean = false,
)

class CreateNoteUseCase(
    private val projectRepository: ProjectRepository,
    private val getNoteUseCase: GetNoteUseCase,
) : UseCase<CreateNoteInput, Note> {
    override suspend fun invoke(input: CreateNoteInput): Note {
        val frontMatter = FrontMatter(
            fields = buildMap {
                put(
                    FrontMatter.CREATED_AT_FIELD,
                    FrontMatter.FrontMatterValue.Scalar(Clock.System.now().toString()),
                )
                put(
                    FrontMatter.TAGS_FIELD,
                    FrontMatter.FrontMatterValue.StringList(input.tags),
                )
            },
        )
        val initialContent = NoteDocument(
            frontMatter = frontMatter,
            body = input.initialText,
        ).serialize()

        val sanitizedNoteName = sanitizeFileName("${input.name}.md", "New Note")

        val newProjectFile = projectRepository.writeFile(
            project = input.project,
            relativePath = input.project.notesRelativePath.resolve(RelativePath(sanitizedNoteName)),
            byteArray = initialContent.toByteArray(),
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
