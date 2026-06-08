package com.mangustc.mdnotes.data.project

import android.content.Context
import androidx.documentfile.provider.DocumentFile
import com.mangustc.mdnotes.domain.models.DomainFile
import com.mangustc.mdnotes.domain.models.Project
import com.mangustc.mdnotes.domain.models.ProjectFile
import com.mangustc.mdnotes.domain.models.RelativePath
import com.mangustc.mdnotes.domain.repositories.PlatformListFilesHandler
import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.dialogs.toAndroidUri

class AndroidPlatformListFilesHandler(
    private val context: Context,
) : PlatformListFilesHandler {
    override suspend fun getDirectoryFiles(directory: DomainFile): List<DomainFile>? {
        val rootDoc = DocumentFile.fromTreeUri(context, directory.file.toAndroidUri())
            ?: return null
        return rootDoc.listFiles().map { documentFile ->
            DomainFile(PlatformFile(documentFile.uri))
        }
    }

    override suspend fun getAllProjectFiles(
        project: Project,
    ): List<ProjectFile>? {
        val rootUri = project.rootDomainFile.file.toAndroidUri()
        val rootDoc = DocumentFile.fromTreeUri(context, rootUri)
            ?: return null

        val result = mutableListOf<ProjectFile>()
        walkDocumentTree(rootDoc, RelativePath(""), result)
        return result.toList()
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
}