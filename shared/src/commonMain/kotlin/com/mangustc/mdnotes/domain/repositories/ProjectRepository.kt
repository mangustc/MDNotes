package com.mangustc.mdnotes.domain.repositories

import androidx.paging.PagingData
import com.mangustc.mdnotes.domain.models.FileSystemPath
import com.mangustc.mdnotes.domain.models.Note
import com.mangustc.mdnotes.domain.models.Project
import com.mangustc.mdnotes.domain.models.ProjectFile
import com.mangustc.mdnotes.domain.models.RelativePath
import com.mangustc.mdnotes.domain.models.SearchQuery
import kotlinx.coroutines.flow.Flow

interface ProjectRepository {
    enum class FileExistsStrategy { OVERWRITE, AUTO_RENAME }

    suspend fun getNotes(
        project: Project,
        query: SearchQuery = SearchQuery(),
        includeText: Boolean = false,
        includeFrontMatter: Boolean = true,
    ): List<Note>

    fun getNotesPaged(
        project: Project,
        query: SearchQuery = SearchQuery(),
        includeText: Boolean = false,
        includeFrontMatter: Boolean = true,
    ): Flow<PagingData<Note>>

    suspend fun getNote(
        project: Project,
        relativePath: RelativePath,
        includeText: Boolean = false,
        includeFrontMatter: Boolean = true,
    ): Note

    suspend fun buildProject(projectPath: FileSystemPath): Project
    suspend fun syncDatabase(project: Project)
    suspend fun copyFromFileSystem(
        project: Project,
        fromPath: FileSystemPath,
        toDirPath: RelativePath,
    ): ProjectFile

    suspend fun getAllTags(project: Project): List<String>
    suspend fun writeFile(
        project: Project,
        relativePath: RelativePath,
        byteArray: ByteArray,
        fileExistsStrategy: FileExistsStrategy,
        createParents: Boolean = true,
    ): ProjectFile

    suspend fun deleteFile(
        project: Project,
        relativePath: RelativePath,
    )

    suspend fun copyFile(
        project: Project,
        relativePath: RelativePath,
        newRelativePath: RelativePath,
        fileExistsStrategy: FileExistsStrategy,
        createParents: Boolean = true,
    ): ProjectFile

    suspend fun moveFile(
        project: Project,
        relativePath: RelativePath,
        newRelativePath: RelativePath,
        fileExistsStrategy: FileExistsStrategy,
        createParents: Boolean = true,
    ): ProjectFile

    suspend fun readFile(
        project: Project,
        relativePath: RelativePath,
    ): ByteArray

    suspend fun getProjectFilesList(
        project: Project,
    ): List<ProjectFile>

    suspend fun getProjectFile(
        project: Project,
        relativePath: RelativePath,
    ): ProjectFile
}