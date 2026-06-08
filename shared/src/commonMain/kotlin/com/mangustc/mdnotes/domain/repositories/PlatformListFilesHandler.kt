package com.mangustc.mdnotes.domain.repositories

import com.mangustc.mdnotes.domain.models.DomainFile
import com.mangustc.mdnotes.domain.models.Project
import com.mangustc.mdnotes.domain.models.ProjectFile

interface PlatformListFilesHandler {
    suspend fun getDirectoryFiles(directory: DomainFile): List<DomainFile>?
    suspend fun getAllProjectFiles(project: Project): List<ProjectFile>?
}