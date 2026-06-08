package com.mangustc.mdnotes.data.project

import com.mangustc.mdnotes.data.database.NoteEntity
import com.mangustc.mdnotes.domain.models.DomainFile
import com.mangustc.mdnotes.domain.models.Note
import com.mangustc.mdnotes.domain.models.Project
import com.mangustc.mdnotes.domain.models.ProjectFile
import com.mangustc.mdnotes.domain.models.RelativePath
import io.github.vinceglb.filekit.PlatformFile

fun NoteEntity.toNote(project: Project, includeText: Boolean): Note = Note(
    name = name,
    projectFile = ProjectFile(
        domainFile = DomainFile(PlatformFile(uri)),
        relativePath = project.notesRelativePath.resolve(
            RelativePath("${name}.md"),
        ),
    ),
    lastModified = lastModified,
    createdAt = createdAt,
    body = if (includeText) body else null,
    tags = if (tags.isNotEmpty()) tags.split(" ") else emptyList(),
)