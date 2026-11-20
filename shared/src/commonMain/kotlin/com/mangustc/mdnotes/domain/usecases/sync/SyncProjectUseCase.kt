package com.mangustc.mdnotes.domain.usecases.sync

import com.mangustc.mdnotes.domain.exceptions.SyncStateException
import com.mangustc.mdnotes.domain.models.Project
import com.mangustc.mdnotes.domain.models.RelativePath
import com.mangustc.mdnotes.domain.models.Settings
import com.mangustc.mdnotes.domain.repositories.ProjectRepository
import com.mangustc.mdnotes.domain.repositories.SyncRepository
import com.mangustc.mdnotes.domain.repositories.SyncRepositoryFactory
import com.mangustc.mdnotes.domain.usecases.UseCase
import dev.whyoleg.cryptography.CryptographyProvider
import dev.whyoleg.cryptography.DelicateCryptographyApi
import dev.whyoleg.cryptography.algorithms.MD5
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlin.time.Clock

data class SyncProjectInput(
    val project: Project,
    val settings: Settings,
)

class SyncProjectUseCase(
    private val projectRepository: ProjectRepository,
    private val syncRepositoryFactory: SyncRepositoryFactory,
) : UseCase<SyncProjectInput, Unit> {
    @OptIn(DelicateCryptographyApi::class)
    private val md5Provider = CryptographyProvider.Default.get(MD5).hasher()
    private val json = Json { ignoreUnknownKeys = true; prettyPrint = true }

    override suspend fun invoke(input: SyncProjectInput) {
        val syncRepository = syncRepositoryFactory.create(input.settings) ?: return
        sync(input.project, syncRepository)
    }

    private suspend fun sync(
        project: Project,
        syncRepository: SyncRepository,
    ) = withContext(Dispatchers.IO) {
        val remoteRoot = RelativePath(project.name)

        val localManifest = SyncManifest(
            lastSyncTimestamp = Clock.System.now().epochSeconds,
            files = snapshotLocal(project),
        )

        val baseManifest = try {
            projectRepository.readFile(project, SyncManifest.ProjectRelativePath).let {
                json.decodeFromString<SyncManifest>(it.decodeToString())
            }
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            null
        } ?: SyncManifest.Empty

        val remoteManifest =
            syncRepository.downloadFile(remoteRoot.resolve(SyncManifest.ProjectRelativePath))
                ?.let {
                    runCatching {
                        json.decodeFromString<SyncManifest>(it.decodeToString())
                    }.getOrNull()
                } ?: SyncManifest.Empty

        if (localManifest.files == baseManifest.files && remoteManifest.files == baseManifest.files) {
            return@withContext
        }

        val allPaths =
            (localManifest.files.keys + remoteManifest.files.keys + baseManifest.files.keys).toSet()
        val actions = allPaths.map { path ->
            val relativePath = RelativePath(path)
            decide(
                relativePath,
                localManifest.files[path],
                remoteManifest.files[path],
                baseManifest.files[path],
            )
        }

        val semaphore = Semaphore(MAX_CONCURRENT_CONNECTIONS)
        val finalFiles = coroutineScope {
            actions.map { action ->
                async {
                    semaphore.withPermit {
                        val remotePath = remoteRoot.resolve(action.relativePath)
                        when (action) {
                            is SyncFileAction.Upload, is SyncFileAction.ConflictUpload -> {
                                val bytes = projectRepository.readFile(project, action.relativePath)
                                syncRepository.uploadFile(remotePath, bytes)
                                action.relativePath.value to md5(bytes)
                            }

                            is SyncFileAction.Download -> {
                                val bytes = syncRepository.downloadFile(remotePath)
                                    ?: throw SyncStateException()
                                projectRepository.writeFile(
                                    project = project,
                                    relativePath = action.relativePath,
                                    byteArray = bytes,
                                    fileExistsStrategy = ProjectRepository.FileExistsStrategy.OVERWRITE,
                                )
                                action.relativePath.value to md5(bytes)
                            }

                            is SyncFileAction.DeleteLocal -> {
                                projectRepository.deleteFile(
                                    project = project,
                                    relativePath = action.relativePath,
                                )
                                null
                            }

                            is SyncFileAction.DeleteRemote -> {
                                syncRepository.deleteFile(remotePath)
                                null
                            }

                            is SyncFileAction.NoOp -> {
                                localManifest.files[action.relativePath.value]
                                    ?.let { hash -> action.relativePath.value to hash }
                            }
                        }
                    }
                }
            }.awaitAll().filterNotNull().toMap()
        }

        val manifest = SyncManifest(
            lastSyncTimestamp = Clock.System.now().epochSeconds,
            files = finalFiles,
        )

        val newManifestBytes = json.encodeToString(manifest).encodeToByteArray()
        syncRepository.uploadFile(
            remoteRoot.resolve(SyncManifest.ProjectRelativePath),
            newManifestBytes,
        )
        projectRepository.writeFile(
            project = project,
            relativePath = SyncManifest.ProjectRelativePath,
            byteArray = newManifestBytes,
            fileExistsStrategy = ProjectRepository.FileExistsStrategy.OVERWRITE,
        )
    }

    private fun decide(
        path: RelativePath,
        l: String?,
        r: String?,
        b: String?,
    ): SyncFileAction = when {
        l == r -> SyncFileAction.NoOp(path)
        l != null && r == null -> {
            if (b != null && l == b) SyncFileAction.DeleteLocal(path) else SyncFileAction.Upload(
                path
            )
        }

        l == null && r != null -> {
            if (b != null && r == b) SyncFileAction.DeleteRemote(path) else SyncFileAction.Download(
                path
            )
        }

        r == b -> SyncFileAction.Upload(path)
        l == b -> SyncFileAction.Download(path)
        else -> SyncFileAction.ConflictUpload(path)
    }

    private suspend fun snapshotLocal(
        project: Project,
    ): Map<String, String> = coroutineScope {
        val semaphore = Semaphore(MAX_CONCURRENT_LOCAL_READS)
        projectRepository.getProjectFilesList(project).mapNotNull { file ->
            if (file.relativePath == SyncManifest.ProjectRelativePath) {
                null
            } else {
                async {
                    semaphore.withPermit {
                        val bytes = projectRepository.readFile(project, file.relativePath)
                        file.relativePath.value to md5(bytes)
                    }
                }
            }
        }.awaitAll().toMap()
    }

    private suspend fun md5(bytes: ByteArray): String =
        md5Provider.hash(bytes).toHexString()

    companion object {
        const val MAX_CONCURRENT_CONNECTIONS = 10
        const val MAX_CONCURRENT_LOCAL_READS = 4
    }
}