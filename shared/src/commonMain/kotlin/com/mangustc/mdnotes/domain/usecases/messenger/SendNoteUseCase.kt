package com.mangustc.mdnotes.domain.usecases.messenger

import com.mangustc.mdnotes.domain.models.Attachment
import com.mangustc.mdnotes.domain.models.FrontMatter
import com.mangustc.mdnotes.domain.models.Note
import com.mangustc.mdnotes.domain.models.NoteDocument
import com.mangustc.mdnotes.domain.models.Project
import com.mangustc.mdnotes.domain.repositories.ProjectRepository
import com.mangustc.mdnotes.domain.usecases.UseCase
import com.mangustc.mdnotes.domain.usecases.notes.CreateNoteInput
import com.mangustc.mdnotes.domain.usecases.notes.CreateNoteUseCase
import com.mangustc.mdnotes.domain.usecases.notes.SaveNoteTextInput
import com.mangustc.mdnotes.domain.usecases.notes.SaveNoteTextUseCase
import com.mangustc.mdnotes.domain.usecases.project.CopyToAssetsInput
import com.mangustc.mdnotes.domain.usecases.project.CopyToAssetsUseCase
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.format.Padding
import kotlinx.datetime.format.char
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock

data class SendNoteInput(
    val project: Project,
    val body: String,
    val attachments: List<Attachment>,
    val editNote: Note? = null,
)

class SendNoteUseCase(
    private val projectRepository: ProjectRepository,
    private val createNoteUseCase: CreateNoteUseCase,
    private val copyToAssetsUseCase: CopyToAssetsUseCase,
    private val saveNoteTextUseCase: SaveNoteTextUseCase,
) : UseCase<SendNoteInput, Unit> {
    override suspend fun invoke(input: SendNoteInput) {
        val isEditedNote = input.editNote != null

        val attachmentLines = buildString {
            input.attachments.forEach { attachment ->
                val label = attachment.displayName
                    .replace("[", "\\[")
                    .replace("]", "\\]")
                val firstPart = when (attachment.type) {
                    Attachment.AttachmentType.IMAGE -> "![$label]"
                    Attachment.AttachmentType.FILE -> "[$label]"
                }
                when (attachment) {
                    is Attachment.PendingAttachment -> {
                        val projectFile =
                            copyToAssetsUseCase(
                                CopyToAssetsInput(
                                    project = input.project,
                                    assetPath = attachment.domainFile,
                                ),
                            )
                        append("\n$firstPart(<${projectFile.relativePath.value}>)")
                    }

                    is Attachment.ProjectAttachment -> {
                        if (isEditedNote) append("\n$firstPart(<${attachment.relativePath.value}>)")
                    }

                    is Attachment.InvalidProjectAttachment -> {}
                }
            }
        }

        val bodyContent = when {
            input.body.isNotEmpty() && attachmentLines.isNotEmpty() ->
                "${input.body}$attachmentLines"

            input.body.isNotEmpty() -> input.body
            attachmentLines.isNotEmpty() -> attachmentLines.trimStart()
            else -> ""
        }

        if (isEditedNote) {
            val targetNote = input.editNote
            val baseText = projectRepository.readFile(
                project = input.project,
                relativePath = targetNote.projectFile.relativePath,
            ).decodeToString()

            val document = NoteDocument.parse(baseText)
            val updatedBody =
                if (bodyContent.isNotEmpty() && document.frontMatter.fields.isNotEmpty()) {
                    "\n$bodyContent"
                } else {
                    bodyContent
                }
            val finalContent = document.copy(body = updatedBody).serialize()

            saveNoteTextUseCase(
                SaveNoteTextInput(
                    project = input.project,
                    note = targetNote,
                    text = finalContent,
                ),
            )
        } else {
            if (bodyContent.isBlank()) return

            val localDateTime = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
            val customFormat = LocalDateTime.Format {
                year()
                monthNumber(Padding.ZERO)
                day(padding = Padding.ZERO)
                char('_')
                hour(Padding.ZERO)
                minute(Padding.ZERO)
                second(Padding.ZERO)
            }
            val timestamp = customFormat.format(localDateTime)
            val name = "fleeting-$timestamp"
            val tags = listOf(FrontMatter.QUICK_NOTE_TAG)

            createNoteUseCase(
                CreateNoteInput(
                    project = input.project,
                    name = name,
                    tags = tags,
                    initialText = "\n$bodyContent",
                ),
            )
        }
    }
}
