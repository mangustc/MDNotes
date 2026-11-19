package com.mangustc.mdnotes.data.project

import com.mangustc.mdnotes.data.database.NoteWithTags
import com.mangustc.mdnotes.domain.models.DomainFile
import com.mangustc.mdnotes.domain.models.Note
import com.mangustc.mdnotes.domain.models.Project
import com.mangustc.mdnotes.domain.models.ProjectFile
import com.mangustc.mdnotes.domain.models.RelativePath
import io.github.vinceglb.filekit.PlatformFile

fun NoteWithTags.toNote(project: Project, includeText: Boolean): Note = Note(
    name = note.name,
    projectFile = ProjectFile(
        domainFile = DomainFile(PlatformFile(note.uri)),
        relativePath = project.notesRelativePath.resolve(
            RelativePath("${note.name}.md"),
        ),
    ),
    lastModified = note.lastModified,
    createdAt = note.createdAt,
    body = if (includeText) note.body else null,
    tags = tags,
)