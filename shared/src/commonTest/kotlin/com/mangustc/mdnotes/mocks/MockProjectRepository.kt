package com.mangustc.mdnotes.mocks

import androidx.paging.PagingData
import com.mangustc.mdnotes.domain.models.DomainFile
import com.mangustc.mdnotes.domain.models.Note
import com.mangustc.mdnotes.domain.models.Project
import com.mangustc.mdnotes.domain.models.ProjectFile
import com.mangustc.mdnotes.domain.models.RelativePath
import com.mangustc.mdnotes.domain.models.SearchQuery
import com.mangustc.mdnotes.domain.repositories.ProjectRepository
import io.github.vinceglb.filekit.PlatformFile
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class MockProjectRepository(
    initialFiles: Map<String, ByteArray> = emptyMap(),
) : ProjectRepository {

    private val mutex = Mutex()
    private val files = initialFiles.mapValues { it.value.copyOf() }.toMutableMap()

    val writtenPaths = mutableListOf<String>()
    val deletedPaths = mutableListOf<String>()

    var onAfterRead: (suspend (RelativePath, ByteArray) -> Unit)? = null

    suspend fun putFile(path: String, content: String) = putBytes(path, content.encodeToByteArray())

    suspend fun putBytes(path: String, bytes: ByteArray) = mutex.withLock {
        files[path] = bytes.copyOf()
    }

    suspend fun getFileText(path: String): String? = mutex.withLock {
        files[path]?.decodeToString()
    }

    suspend fun getFileBytes(path: String): ByteArray? = mutex.withLock {
        files[path]?.copyOf()
    }

    suspend fun allFilePaths(): Set<String> = mutex.withLock {
        files.keys.toSet()
    }

    override fun getNotes(
        project: Project,
        query: SearchQuery,
        includeText: Boolean,
        includeFrontMatter: Boolean,
    ): Flow<List<Note>> = emptyFlow()

    override fun getNotesPaged(
        project: Project,
        query: SearchQuery,
        includeText: Boolean,
        includeFrontMatter: Boolean,
    ): Flow<PagingData<Note>> = emptyFlow()

    override suspend fun getNote(
        project: Project,
        relativePath: RelativePath,
        includeText: Boolean,
        includeFrontMatter: Boolean,
    ): Note = error("getNote not configured in MockProjectRepository")

    override suspend fun buildProject(projectPath: DomainFile): Project =
        Project(
            name = projectPath.name,
            rootDomainFile = projectPath,
            notesRelativePath = Project.DefaultNotesRelativePath,
            assetsRelativePath = Project.DefaultAssetsRelativePath,
        )

    override suspend fun syncDatabase(project: Project) = Unit

    override suspend fun copyFromFileSystem(
        project: Project,
        fromPath: DomainFile,
        toDirPath: RelativePath,
    ): ProjectFile = error("copyFromFileSystem not configured in MockProjectRepository")

    override fun getAllTags(project: Project): Flow<List<String>> = emptyFlow()

    override suspend fun writeFile(
        project: Project,
        relativePath: RelativePath,
        byteArray: ByteArray,
        fileExistsStrategy: ProjectRepository.FileExistsStrategy,
        createParents: Boolean,
    ): ProjectFile = mutex.withLock {
        files[relativePath.value] = byteArray.copyOf()
        writtenPaths += relativePath.value
        ProjectFile(
            domainFile = DomainFile(PlatformFile("${project.rootDomainFile.path}/${relativePath.value}")),
            relativePath = relativePath,
        )
    }

    override suspend fun deleteFile(
        project: Project,
        relativePath: RelativePath,
    ): Unit = mutex.withLock {
        files.remove(relativePath.value)
        deletedPaths += relativePath.value
    }

    override suspend fun copyFile(
        project: Project,
        relativePath: RelativePath,
        newRelativePath: RelativePath,
        fileExistsStrategy: ProjectRepository.FileExistsStrategy,
        createParents: Boolean,
    ): ProjectFile = mutex.withLock {
        val bytes = files[relativePath.value] ?: throw NoSuchElementException(relativePath.value)
        files[newRelativePath.value] = bytes.copyOf()
        ProjectFile(
            domainFile = DomainFile(PlatformFile("${project.rootDomainFile.path}/${newRelativePath.value}")),
            relativePath = newRelativePath,
        )
    }

    override suspend fun moveFile(
        project: Project,
        relativePath: RelativePath,
        newRelativePath: RelativePath,
        fileExistsStrategy: ProjectRepository.FileExistsStrategy,
        createParents: Boolean,
    ): ProjectFile = mutex.withLock {
        val bytes =
            files.remove(relativePath.value) ?: throw NoSuchElementException(relativePath.value)
        files[newRelativePath.value] = bytes
        ProjectFile(
            domainFile = DomainFile(PlatformFile("${project.rootDomainFile.path}/${newRelativePath.value}")),
            relativePath = newRelativePath,
        )
    }

    override suspend fun readFile(
        project: Project,
        relativePath: RelativePath,
    ): ByteArray {
        val bytes = mutex.withLock {
            files[relativePath.value]?.copyOf()
                ?: throw NoSuchElementException("Local file not found: ${relativePath.value}")
        }
        onAfterRead?.invoke(relativePath, bytes)
        return bytes
    }

    override suspend fun getProjectFilesList(
        project: Project,
    ): List<ProjectFile> = mutex.withLock {
        files.keys.map { path ->
            ProjectFile(
                domainFile = DomainFile(PlatformFile("${project.rootDomainFile.path}/$path")),
                relativePath = RelativePath(path),
            )
        }
    }

    override suspend fun getProjectFile(
        project: Project,
        relativePath: RelativePath,
    ): ProjectFile = ProjectFile(
        domainFile = DomainFile(PlatformFile("${project.rootDomainFile.path}/${relativePath.value}")),
        relativePath = relativePath,
    )
}
