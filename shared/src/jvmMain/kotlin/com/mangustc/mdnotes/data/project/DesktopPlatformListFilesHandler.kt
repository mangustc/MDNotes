package com.mangustc.mdnotes.data.project

import com.mangustc.mdnotes.domain.models.DomainFile
import com.mangustc.mdnotes.domain.models.Project
import com.mangustc.mdnotes.domain.models.ProjectFile
import com.mangustc.mdnotes.domain.models.RelativePath
import com.mangustc.mdnotes.domain.repositories.PlatformListFilesHandler
import io.github.vinceglb.filekit.PlatformFile
import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem

class DesktopPlatformListFilesHandler : PlatformListFilesHandler {
    override suspend fun getDirectoryFiles(directory: DomainFile): List<DomainFile>? {
        val dirPath = Path(directory.path)
        val metadata = SystemFileSystem.metadataOrNull(dirPath)

        if (metadata == null || !metadata.isDirectory) return null

        return try {
            SystemFileSystem.list(dirPath).map { childPath ->
                DomainFile(PlatformFile(childPath))
            }
        } catch (_: Exception) {
            null
        }
    }

    override suspend fun getAllProjectFiles(project: Project): List<ProjectFile>? {
        val rootPath = Path(project.rootDomainFile.path)
        val metadata = SystemFileSystem.metadataOrNull(rootPath)

        if (metadata == null || !metadata.isDirectory) return null

        val result = mutableListOf<ProjectFile>()
        walkDirectory(rootPath, RelativePath(""), result)
        return result.toList()
    }

    private fun walkDirectory(
        dir: Path,
        prefix: RelativePath,
        out: MutableList<ProjectFile>,
    ) {
        try {
            SystemFileSystem.list(dir).forEach { childPath ->
                val name = childPath.name
                val relPath = prefix.resolve(RelativePath(name))

                val metadata = SystemFileSystem.metadataOrNull(childPath)

                if (metadata != null) {
                    if (metadata.isDirectory) {
                        walkDirectory(childPath, relPath, out)
                    } else if (metadata.isRegularFile) {
                        out.add(
                            ProjectFile(
                                domainFile = DomainFile(PlatformFile(childPath.toString())),
                                relativePath = relPath,
                            ),
                        )
                    }
                }
            }
        } catch (_: Exception) {
        }
    }
}