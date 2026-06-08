package com.mangustc.mdnotes.domain.usecases.messenger

import com.mangustc.mdnotes.domain.markdown.MarkdownParser
import com.mangustc.mdnotes.domain.models.Attachment
import com.mangustc.mdnotes.domain.models.MessageBody
import com.mangustc.mdnotes.domain.models.Note
import com.mangustc.mdnotes.domain.models.ProjectFile
import com.mangustc.mdnotes.domain.models.RelativePath
import com.mangustc.mdnotes.domain.models.SpanInfo

suspend fun Note.toMessageBody(
    getProjectFile: suspend (RelativePath) -> ProjectFile,
): MessageBody {
    val body = this.body ?: ""
    val spans = MarkdownParser.parse(body)

    val attachments = mutableListOf<Attachment>()
    for (span in spans) {
        val (payload, attachmentType) = when (span) {
            is SpanInfo.Image -> Pair(span.payload, Attachment.AttachmentType.IMAGE)
            is SpanInfo.Link -> Pair(span.payload, Attachment.AttachmentType.FILE)
            else -> continue
        }
        val projectFile = try {
            getProjectFile(RelativePath(payload))
        } catch (e: Exception) {
            e.printStackTrace()
            attachments.add(
                Attachment.InvalidProjectAttachment(
                    type = Attachment.AttachmentType.FILE,
                    domainFile = projectFile.domainFile,
                    relativePath = projectFile.relativePath,
                    displayName = payload,
                ),
            )
            continue
        }
        attachments.add(
            Attachment.ProjectAttachment(
                type = attachmentType,
                domainFile = projectFile.domainFile,
                relativePath = projectFile.relativePath,
                displayName = payload,
            ),
        )
    }

    val text = MarkdownParser.stripAttachments(body, spans).ifBlank { "" }
    val links = Regex("""https?://[^\s<>"')]+""").findAll(text)

    return MessageBody(
        text = text,
        attachments = attachments.toList(),
        links = links,
        note = this,
    )
}