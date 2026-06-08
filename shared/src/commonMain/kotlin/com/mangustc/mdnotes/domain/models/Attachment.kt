package com.mangustc.mdnotes.domain.models

sealed interface Attachment {
    enum class AttachmentType { FILE, IMAGE }

    val type: AttachmentType
    val domainFile: DomainFile
    val displayName: String

    data class PendingAttachment(
        override val type: AttachmentType,
        override val domainFile: DomainFile,
        override val displayName: String,
    ) : Attachment

    data class ProjectAttachment(
        override val type: AttachmentType,
        override val domainFile: DomainFile,
        override val displayName: String,
        val relativePath: RelativePath,
    ) : Attachment

    data class InvalidProjectAttachment(
        override val type: AttachmentType,
        override val domainFile: DomainFile,
        override val displayName: String,
        val relativePath: RelativePath,
    ) : Attachment
}