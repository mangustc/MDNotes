package com.mangustc.mdnotes.data.project

import android.content.Context
import androidx.documentfile.provider.DocumentFile
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.map
import com.mangustc.mdnotes.data.database.NoteDao
import com.mangustc.mdnotes.data.database.NoteEntity
import com.mangustc.mdnotes.data.database.ProjectDao
import com.mangustc.mdnotes.data.database.ProjectEntity
import com.mangustc.mdnotes.domain.exceptions.FileNotFoundException
import com.mangustc.mdnotes.domain.exceptions.FileNotReadableException
import com.mangustc.mdnotes.domain.exceptions.FileNotWritableException
import com.mangustc.mdnotes.domain.exceptions.ProjectAccessException
import com.mangustc.mdnotes.domain.models.DomainFile
import com.mangustc.mdnotes.domain.models.FrontMatter
import com.mangustc.mdnotes.domain.models.Note
import com.mangustc.mdnotes.domain.models.Project
import com.mangustc.mdnotes.domain.models.ProjectFile
import com.mangustc.mdnotes.domain.models.RelativePath
import com.mangustc.mdnotes.domain.models.SearchQuery
import com.mangustc.mdnotes.domain.repositories.ProjectRepository
import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.createDirectories
import io.github.vinceglb.filekit.delete
import io.github.vinceglb.filekit.dialogs.toAndroidUri
import io.github.vinceglb.filekit.div
import io.github.vinceglb.filekit.exists
import io.github.vinceglb.filekit.extension
import io.github.vinceglb.filekit.isDirectory
import io.github.vinceglb.filekit.isRegularFile
import io.github.vinceglb.filekit.lastModified
import io.github.vinceglb.filekit.name
import io.github.vinceglb.filekit.nameWithoutExtension
import io.github.vinceglb.filekit.readBytes
import io.github.vinceglb.filekit.readString
import io.github.vinceglb.filekit.write
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class AndroidProjectRepository(
    private val context: Context,
    private val noteDao: NoteDao,
    private val projectDao: ProjectDao,
) : ProjectRepository {
    override suspend fun getNotes(
        project: Project,
        query: SearchQuery,
        includeText: Boolean,
        includeFrontMatter: Boolean,
    ): List<Note> = withContext(Dispatchers.IO) {
        val sqlQuery = query.buildRoomRawQuery(project)
        val entities = noteDao.searchNotes(sqlQuery)

        entities.map { entity -> entity.toNote(project, includeText) }
    }

    override fun getNotesPaged(
        project: Project,
        query: SearchQuery,
        includeText: Boolean,
        includeFrontMatter: Boolean,
    ): Flow<PagingData<Note>> {
        val sqlQuery = query.buildRoomRawQuery(project)
        return Pager(
            config = PagingConfig(
                pageSize = 50,
                prefetchDistance = 20,
                enablePlaceholders = false,
            ),
        ) {
            noteDao.searchNotesPaged(sqlQuery)
        }.flow.map { pagingData ->
            pagingData.map { entity -> entity.toNote(project, includeText) }
        }
    }

    override suspend fun getNote(
        project: Project,
        relativePath: RelativePath,
        includeText: Boolean,
        includeFrontMatter: Boolean,
    ): Note = withContext(Dispatchers.IO) {
        val file = getFile(project, relativePath)
        val fullText = try {
            file.file.readString()
        } catch (e: Exception) {
            throw FileNotReadableException(relativePath.value, e)
        }
        val (frontMatter, text) = FrontMatter.splitFromContent(fullText)

        Note(
            name = file.nameWithoutExtension,
            projectFile = ProjectFile(
                domainFile = file,
                relativePath = relativePath,
            ),
            lastModified = file.file.lastModified().toEpochMilliseconds(),
            createdAt = frontMatter.toCreatedAtMillis(),
            tags = frontMatter.tags,
            body = if (includeText && includeFrontMatter) {
                fullText
            } else if (includeText || includeFrontMatter) buildString {
                if (includeFrontMatter) append(frontMatter.toString())
                if (includeText) append(text)
            } else {
                null
            },
        )
    }

    override suspend fun syncDatabase(project: Project) = withContext(Dispatchers.IO) {
        val projectId = getOrCreateProjectId(project)
            ?: throw ProjectAccessException(project.notesRelativePath.value)

        val notesDir =
            DocumentFile.fromTreeUri(
                context,
                getFile(project, project.notesRelativePath).file.toAndroidUri(),
            )
                ?: throw ProjectAccessException(project.notesRelativePath.value)
        val files =
            notesDir.listFiles().filter { it.name?.endsWith(".md") == true }

        val existingNotes = noteDao.searchNotes(SearchQuery().buildRoomRawQuery(project))
        val existingUris = existingNotes.associateBy { it.uri }

        files.forEach { file ->
            val uriStr = file.uri.toString()
            val cached = existingUris[uriStr]

            if (cached == null || file.lastModified() > cached.lastModified) {
                val fullText = try {
                    PlatformFile(file.uri).readString()
                } catch (e: Exception) {
                    throw FileNotReadableException(file.name ?: "", e)
                }
                val (frontMatter, body) = FrontMatter.splitFromContent(fullText)
                val tags = frontMatter.toTagString()
                val name = file.name?.removeSuffix(".md")
                    ?: throw FileNotReadableException(uriStr)

                val entity = NoteEntity(
                    id = cached?.id ?: 0,
                    uri = uriStr,
                    name = name,
                    lastModified = file.lastModified(),
                    createdAt = frontMatter.toCreatedAtMillis(),
                    tags = tags,
                    body = body,
                    projectId = projectId,
                )
                noteDao.insertNote(entity)
            }
        }

        val currentFileUris = files.map { it.uri.toString() }.toSet()
        existingNotes.forEach { cached ->
            if (cached.uri !in currentFileUris) noteDao.deleteByUri(cached.uri)
        }
    }

    override suspend fun buildProject(projectPath: DomainFile): Project =
        withContext(Dispatchers.IO) {
            val projectFile = projectPath.file
            if (!projectFile.isDirectory()) throw ProjectAccessException(projectPath.name)

            val notesFile = (projectPath / Project.DefaultNotesRelativePath).file
            if (notesFile.exists()) {
                if (!notesFile.isDirectory()) throw ProjectAccessException(projectPath.name)
            } else {
                notesFile.createDirectories()
            }

            val assetsFile = (projectPath / Project.DefaultAssetsRelativePath).file
            if (assetsFile.exists()) {
                if (!assetsFile.isDirectory()) throw ProjectAccessException(projectPath.name)
            } else {
                assetsFile.createDirectories()
            }

            Project(
                name = projectPath.name,
                rootDomainFile = DomainFile(projectFile),
                notesRelativePath = Project.DefaultNotesRelativePath,
                assetsRelativePath = Project.DefaultAssetsRelativePath,
            )
        }

    override suspend fun copyFromFileSystem(
        project: Project,
        fromPath: DomainFile,
        toDirPath: RelativePath,
    ): ProjectFile =
        withContext(Dispatchers.IO) {
            val bytes = try {
                fromPath.file.readBytes()
            } catch (e: Exception) {
                throw FileNotReadableException(fromPath.name, e)
            }

            writeFile(
                project = project,
                relativePath = toDirPath / RelativePath(fromPath.name),
                byteArray = bytes,
                fileExistsStrategy = ProjectRepository.FileExistsStrategy.AUTO_RENAME,
                createParents = true,
            )
        }

    override suspend fun getAllTags(project: Project): List<String> = withContext(Dispatchers.IO) {
        val projectId = getOrCreateProjectId(project)
            ?: throw ProjectAccessException(project.rootDomainFile.name)
        noteDao.getAllTags(projectId).flatMap { it.split(" ") }.filter { it.isNotBlank() }
            .distinct()
    }

    override suspend fun writeFile(
        project: Project,
        relativePath: RelativePath,
        byteArray: ByteArray,
        fileExistsStrategy: ProjectRepository.FileExistsStrategy,
        createParents: Boolean,
    ): ProjectFile = withContext(Dispatchers.IO) {
        val file = (project.rootDomainFile / relativePath).file.let { file ->
            if (file.exists()) {
                if (!file.isRegularFile()) throw FileNotWritableException(relativePath.value)
                when (fileExistsStrategy) {
                    ProjectRepository.FileExistsStrategy.OVERWRITE -> return@let file
                    ProjectRepository.FileExistsStrategy.AUTO_RENAME -> {
                        val name = file.nameWithoutExtension
                        val extension = file.extension
                        val parent = (project.rootDomainFile / relativePath.dirRelativePath).file
                        for (i in 1..MAX_RENAME_TRIES) {
                            val newFile = parent / "$name - $i.$extension"
                            if (!newFile.exists()) return@let newFile
                        }
                        throw FileNotWritableException(relativePath.value)
                    }
                }
            } else {
                if (createParents) {
                    try {
                        val parent = (project.rootDomainFile / relativePath.dirRelativePath).file
                        parent.let {
                            if (!it.exists()) it.createDirectories()
                        }
                    } catch (e: Exception) {
                        throw FileNotWritableException(relativePath.value, e)
                    }
                }
                file
            }
        }

        try {
            file.write(byteArray)
        } catch (e: Exception) {
            throw FileNotWritableException(relativePath.value, e)
        }

        ProjectFile(
            domainFile = DomainFile(file),
            relativePath = relativePath.dirRelativePath.resolve(RelativePath(file.name)),
        )
    }

    override suspend fun deleteFile(
        project: Project,
        relativePath: RelativePath,
    ) = withContext(Dispatchers.IO) {
        val file = getFile(project, relativePath)

        if (!file.file.isRegularFile()) {
            return@withContext
        }

        file.file.delete(mustExist = false)
    }

    override suspend fun copyFile(
        project: Project,
        relativePath: RelativePath,
        newRelativePath: RelativePath,
        fileExistsStrategy: ProjectRepository.FileExistsStrategy,
        createParents: Boolean,
    ): ProjectFile = withContext(Dispatchers.IO) {
        val bytes = readFile(project, relativePath)
        return@withContext writeFile(
            project,
            newRelativePath,
            bytes,
            fileExistsStrategy,
            createParents,
        )
    }

    override suspend fun moveFile(
        project: Project,
        relativePath: RelativePath,
        newRelativePath: RelativePath,
        fileExistsStrategy: ProjectRepository.FileExistsStrategy,
        createParents: Boolean,
    ): ProjectFile = withContext(Dispatchers.IO) {
        val newProjectFile =
            copyFile(project, relativePath, newRelativePath, fileExistsStrategy, createParents)
        deleteFile(project, relativePath)
        newProjectFile
    }

    override suspend fun readFile(
        project: Project,
        relativePath: RelativePath,
    ): ByteArray = withContext(Dispatchers.IO) {
        val file = getFile(project, relativePath)

        if (!file.file.isRegularFile()) {
            throw FileNotReadableException(relativePath.value)
        }

        try {
            file.file.readBytes()
        } catch (e: Exception) {
            throw FileNotReadableException(relativePath.value, e)
        }
    }

    override suspend fun getProjectFilesList(
        project: Project,
    ): List<ProjectFile> = withContext(Dispatchers.IO) {
        val rootUri = project.rootDomainFile.file.toAndroidUri()
        val rootDoc = DocumentFile.fromTreeUri(context, rootUri)
            ?: throw ProjectAccessException(project.rootDomainFile.name)

        val result = mutableListOf<ProjectFile>()
        walkDocumentTree(rootDoc, RelativePath(""), result)
        result.toList()
    }

    override suspend fun getProjectFile(
        project: Project,
        relativePath: RelativePath,
    ): ProjectFile = withContext(Dispatchers.IO) {
        return@withContext ProjectFile(
            domainFile = getFile(project, relativePath),
            relativePath = relativePath,
        )
    }

    private suspend fun getOrCreateProjectId(project: Project): Long? {
        val rootPath = project.rootDomainFile.path
        val existingId = projectDao.getProjectId(rootPath)
        if (existingId != null) return existingId

        val newId = projectDao.insertProject(
            ProjectEntity(
                name = project.name,
                rootPath = rootPath,
            ),
        )
        if (newId != -1L) return newId

        return projectDao.getProjectId(rootPath)
    }

    private fun walkDocumentTree(
        dir: DocumentFile,
        prefix: RelativePath,
        out: MutableList<ProjectFile>,
    ) {
        dir.listFiles().forEach { file ->
            val name = file.name ?: return@forEach
            val relPath = prefix.resolve(RelativePath(name))
            if (file.isDirectory) {
                walkDocumentTree(file, relPath, out)
            } else {
                out.add(
                    ProjectFile(
                        domainFile = DomainFile(PlatformFile(file.uri.toString())),
                        relativePath = relPath,
                    ),
                )
            }
        }
    }

    private fun getFile(project: Project, relativePath: RelativePath): DomainFile {
        val domainFile = project.rootDomainFile / relativePath
        if (!domainFile.file.exists()) throw FileNotFoundException(relativePath.value)
        return domainFile
    }

    companion object {
        const val MAX_RENAME_TRIES = 999
    }
}
