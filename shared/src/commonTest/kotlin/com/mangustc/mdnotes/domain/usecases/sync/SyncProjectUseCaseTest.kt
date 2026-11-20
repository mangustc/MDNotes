package com.mangustc.mdnotes.domain.usecases.sync

import com.mangustc.mdnotes.domain.exceptions.SyncStateException
import com.mangustc.mdnotes.domain.models.DomainFile
import com.mangustc.mdnotes.domain.models.Project
import com.mangustc.mdnotes.domain.models.Settings
import com.mangustc.mdnotes.mocks.MockProjectRepository
import com.mangustc.mdnotes.mocks.MockSyncRepository
import dev.whyoleg.cryptography.CryptographyProvider
import dev.whyoleg.cryptography.DelicateCryptographyApi
import dev.whyoleg.cryptography.algorithms.MD5
import io.github.vinceglb.filekit.PlatformFile
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SyncProjectUseCaseTest {

    private val json = Json { ignoreUnknownKeys = true; prettyPrint = true }

    @OptIn(DelicateCryptographyApi::class)
    private val md5Hasher = CryptographyProvider.Default.get(MD5).hasher()

    private val project = Project(
        name = "vault",
        rootDomainFile = DomainFile(PlatformFile("/test/vault")),
        notesRelativePath = Project.DefaultNotesRelativePath,
        assetsRelativePath = Project.DefaultAssetsRelativePath,
    )

    private val enabledSettings = Settings(
        reverseLayout = false,
        syncProvider = ValidSyncProvider.YANDEX,
        yandexOauthToken = "fake-token",
    )

    private val manifestPath = SyncManifest.ProjectRelativePath.value
    private val remoteManifestPath = "${project.name}/$manifestPath"

    private suspend fun md5(text: String): String =
        md5Hasher.hash(text.encodeToByteArray()).toHexString()

    private fun encodeManifest(files: Map<String, String>, timestamp: Long = 100L): ByteArray =
        json.encodeToString(SyncManifest(lastSyncTimestamp = timestamp, files = files))
            .encodeToByteArray()

    private suspend fun readLocalManifest(repo: MockProjectRepository): SyncManifest =
        json.decodeFromString(requireNotNull(repo.getFileText(manifestPath)))

    private suspend fun readRemoteManifest(repo: MockSyncRepository): SyncManifest =
        json.decodeFromString(requireNotNull(repo.getFileText(remoteManifestPath)))

    private fun createUseCase(
        projectRepo: MockProjectRepository,
        syncRepo: MockSyncRepository?,
    ): SyncProjectUseCase = SyncProjectUseCase(
        projectRepository = projectRepo,
        syncRepositoryFactory = { settings ->
            if (settings.syncProvider == ValidSyncProvider.NONE) null else syncRepo
        },
    )

    @Test
    fun syncProviderNone_doesNothing() = runTest {
        val localRepo = MockProjectRepository(mapOf("notes/a.md" to "hello".encodeToByteArray()))
        val syncRepo = MockSyncRepository()
        val useCase = createUseCase(localRepo, syncRepo)

        useCase(SyncProjectInput(project, Settings.EMPTY))

        assertTrue(syncRepo.uploadedPaths.isEmpty())
        assertTrue(syncRepo.downloadedPaths.isEmpty())
        assertTrue(syncRepo.deletedPaths.isEmpty())
        assertTrue(localRepo.writtenPaths.isEmpty())
    }

    @Test
    fun noChangesSinceBase_skipsAllTransfersAndManifestWrites() = runTest {
        val hashA = md5("unchanged")
        val baseBytes = encodeManifest(mapOf("notes/a.md" to hashA))

        val localRepo = MockProjectRepository(
            mapOf(
                "notes/a.md" to "unchanged".encodeToByteArray(),
                manifestPath to baseBytes,
            ),
        )
        val syncRepo = MockSyncRepository(
            mapOf(
                "vault/notes/a.md" to "unchanged".encodeToByteArray(),
                remoteManifestPath to baseBytes,
            ),
        )

        createUseCase(localRepo, syncRepo).invoke(SyncProjectInput(project, enabledSettings))

        assertTrue(syncRepo.uploadedPaths.isEmpty())
        assertTrue(syncRepo.deletedPaths.isEmpty())
        assertEquals(listOf(remoteManifestPath), syncRepo.downloadedPaths)
        assertTrue(localRepo.writtenPaths.isEmpty())
        assertTrue(localRepo.deletedPaths.isEmpty())
    }

    @Test
    fun initialSync_uploadsAllLocalFilesAndWritesManifestsExcludingManifestItself() = runTest {
        val localRepo = MockProjectRepository(
            mapOf(
                "notes/a.md" to "alpha".encodeToByteArray(),
                "assets/pic.png" to "pngdata".encodeToByteArray(),
            ),
        )
        val syncRepo = MockSyncRepository()

        createUseCase(localRepo, syncRepo).invoke(SyncProjectInput(project, enabledSettings))

        assertEquals("alpha", syncRepo.getFileText("vault/notes/a.md"))
        assertEquals("pngdata", syncRepo.getFileText("vault/assets/pic.png"))

        val expectedFiles = mapOf(
            "notes/a.md" to md5("alpha"),
            "assets/pic.png" to md5("pngdata"),
        )
        val localManifest = readLocalManifest(localRepo)
        val remoteManifest = readRemoteManifest(syncRepo)
        assertEquals(expectedFiles, localManifest.files)
        assertEquals(expectedFiles, remoteManifest.files)
        assertFalse(manifestPath in localManifest.files)
    }

    @Test
    fun freshLocalVault_downloadsAllRemoteFiles() = runTest {
        val remoteManifestBytes = encodeManifest(
            mapOf("notes/remote.md" to md5("from cloud")),
        )
        val localRepo = MockProjectRepository()
        val syncRepo = MockSyncRepository(
            mapOf(
                "vault/notes/remote.md" to "from cloud".encodeToByteArray(),
                remoteManifestPath to remoteManifestBytes,
            ),
        )

        createUseCase(localRepo, syncRepo).invoke(SyncProjectInput(project, enabledSettings))

        assertEquals("from cloud", localRepo.getFileText("notes/remote.md"))
        assertEquals(
            mapOf("notes/remote.md" to md5("from cloud")),
            readLocalManifest(localRepo).files
        )
        assertEquals(
            mapOf("notes/remote.md" to md5("from cloud")),
            readRemoteManifest(syncRepo).files
        )
    }

    @Test
    fun localOnlyModification_uploadsLocalVersion() = runTest {
        val baseBytes = encodeManifest(mapOf("notes/a.md" to md5("v1")))
        val localRepo = MockProjectRepository(
            mapOf(
                "notes/a.md" to "v2-local".encodeToByteArray(),
                manifestPath to baseBytes,
            ),
        )
        val syncRepo = MockSyncRepository(
            mapOf(
                "vault/notes/a.md" to "v1".encodeToByteArray(),
                remoteManifestPath to baseBytes,
            ),
        )

        createUseCase(localRepo, syncRepo).invoke(SyncProjectInput(project, enabledSettings))

        assertEquals("v2-local", syncRepo.getFileText("vault/notes/a.md"))
        assertEquals("v2-local", localRepo.getFileText("notes/a.md"))
        assertEquals(md5("v2-local"), readRemoteManifest(syncRepo).files["notes/a.md"])
    }

    @Test
    fun remoteOnlyModification_downloadsRemoteVersion() = runTest {
        val baseBytes = encodeManifest(mapOf("notes/a.md" to md5("v1")))
        val remoteManifestBytes = encodeManifest(mapOf("notes/a.md" to md5("v2-remote")))
        val localRepo = MockProjectRepository(
            mapOf(
                "notes/a.md" to "v1".encodeToByteArray(),
                manifestPath to baseBytes,
            ),
        )
        val syncRepo = MockSyncRepository(
            mapOf(
                "vault/notes/a.md" to "v2-remote".encodeToByteArray(),
                remoteManifestPath to remoteManifestBytes,
            ),
        )

        createUseCase(localRepo, syncRepo).invoke(SyncProjectInput(project, enabledSettings))

        assertEquals("v2-remote", localRepo.getFileText("notes/a.md"))
        assertEquals(md5("v2-remote"), readLocalManifest(localRepo).files["notes/a.md"])
    }

    @Test
    fun remoteDeletedWhileLocalUnchanged_deletesLocalFile() = runTest {
        val baseBytes = encodeManifest(mapOf("notes/old.md" to md5("v1")))
        val remoteManifestBytes = encodeManifest(emptyMap())
        val localRepo = MockProjectRepository(
            mapOf(
                "notes/old.md" to "v1".encodeToByteArray(),
                manifestPath to baseBytes,
            ),
        )
        val syncRepo = MockSyncRepository(
            mapOf(remoteManifestPath to remoteManifestBytes),
        )

        createUseCase(localRepo, syncRepo).invoke(SyncProjectInput(project, enabledSettings))

        assertNull(localRepo.getFileText("notes/old.md"))
        assertTrue(readLocalManifest(localRepo).files.isEmpty())
        assertTrue(readRemoteManifest(syncRepo).files.isEmpty())
    }

    @Test
    fun localDeletedWhileRemoteUnchanged_deletesRemoteFile() = runTest {
        val baseBytes = encodeManifest(mapOf("notes/old.md" to md5("v1")))
        val localRepo = MockProjectRepository(
            mapOf(manifestPath to baseBytes),
        )
        val syncRepo = MockSyncRepository(
            mapOf(
                "vault/notes/old.md" to "v1".encodeToByteArray(),
                remoteManifestPath to baseBytes,
            ),
        )

        createUseCase(localRepo, syncRepo).invoke(SyncProjectInput(project, enabledSettings))

        assertNull(syncRepo.getFileText("vault/notes/old.md"))
        assertEquals(listOf("vault/notes/old.md"), syncRepo.deletedPaths)
        assertTrue(readLocalManifest(localRepo).files.isEmpty())
        assertTrue(readRemoteManifest(syncRepo).files.isEmpty())
    }

    @Test
    fun editVsDelete_localEditedAndRemoteDeleted_preservesLocalByUploading() = runTest {
        val baseBytes = encodeManifest(mapOf("notes/important.md" to md5("v1")))
        val remoteManifestBytes = encodeManifest(emptyMap())
        val localRepo = MockProjectRepository(
            mapOf(
                "notes/important.md" to "v2-precious-edit".encodeToByteArray(),
                manifestPath to baseBytes,
            ),
        )
        val syncRepo = MockSyncRepository(
            mapOf(remoteManifestPath to remoteManifestBytes),
        )

        createUseCase(localRepo, syncRepo).invoke(SyncProjectInput(project, enabledSettings))

        assertEquals("v2-precious-edit", localRepo.getFileText("notes/important.md"))
        assertEquals("v2-precious-edit", syncRepo.getFileText("vault/notes/important.md"))
        assertTrue(localRepo.deletedPaths.isEmpty())
        assertEquals(
            md5("v2-precious-edit"),
            readRemoteManifest(syncRepo).files["notes/important.md"]
        )
    }

    @Test
    fun editVsDelete_localDeletedAndRemoteEdited_preservesRemoteByDownloading() = runTest {
        val baseBytes = encodeManifest(mapOf("notes/important.md" to md5("v1")))
        val remoteManifestBytes =
            encodeManifest(mapOf("notes/important.md" to md5("v2-cloud-edit")))
        val localRepo = MockProjectRepository(
            mapOf(manifestPath to baseBytes),
        )
        val syncRepo = MockSyncRepository(
            mapOf(
                "vault/notes/important.md" to "v2-cloud-edit".encodeToByteArray(),
                remoteManifestPath to remoteManifestBytes,
            ),
        )

        createUseCase(localRepo, syncRepo).invoke(SyncProjectInput(project, enabledSettings))

        assertEquals("v2-cloud-edit", localRepo.getFileText("notes/important.md"))
        assertEquals("v2-cloud-edit", syncRepo.getFileText("vault/notes/important.md"))
        assertTrue(syncRepo.deletedPaths.isEmpty())
        assertEquals(md5("v2-cloud-edit"), readLocalManifest(localRepo).files["notes/important.md"])
    }

    @Test
    fun conflict_bothModifiedDifferently_localWinsWithConflictUpload() = runTest {
        val baseBytes = encodeManifest(mapOf("notes/c.md" to md5("v1")))
        val remoteManifestBytes = encodeManifest(mapOf("notes/c.md" to md5("v2-remote")))
        val localRepo = MockProjectRepository(
            mapOf(
                "notes/c.md" to "v2-local".encodeToByteArray(),
                manifestPath to baseBytes,
            ),
        )
        val syncRepo = MockSyncRepository(
            mapOf(
                "vault/notes/c.md" to "v2-remote".encodeToByteArray(),
                remoteManifestPath to remoteManifestBytes,
            ),
        )

        createUseCase(localRepo, syncRepo).invoke(SyncProjectInput(project, enabledSettings))

        assertEquals("v2-local", localRepo.getFileText("notes/c.md"))
        assertEquals("v2-local", syncRepo.getFileText("vault/notes/c.md"))
        assertEquals(md5("v2-local"), readRemoteManifest(syncRepo).files["notes/c.md"])
        assertEquals(md5("v2-local"), readLocalManifest(localRepo).files["notes/c.md"])
    }

    @Test
    fun convergentEdit_bothModifiedIdentically_skipsFileTransferAndUpdatesManifest() = runTest {
        val baseBytes = encodeManifest(mapOf("notes/same.md" to md5("v1")))
        val remoteManifestBytes = encodeManifest(mapOf("notes/same.md" to md5("v2-identical")))
        val localRepo = MockProjectRepository(
            mapOf(
                "notes/same.md" to "v2-identical".encodeToByteArray(),
                manifestPath to baseBytes,
            ),
        )
        val syncRepo = MockSyncRepository(
            mapOf(
                "vault/notes/same.md" to "v2-identical".encodeToByteArray(),
                remoteManifestPath to remoteManifestBytes,
            ),
        )

        createUseCase(localRepo, syncRepo).invoke(SyncProjectInput(project, enabledSettings))

        assertEquals(listOf(remoteManifestPath), syncRepo.uploadedPaths)
        assertEquals(listOf(remoteManifestPath), syncRepo.downloadedPaths)
        assertEquals(md5("v2-identical"), readLocalManifest(localRepo).files["notes/same.md"])
    }

    @Test
    fun bothDeletedSinceBase_removesFromManifestWithoutTransfer() = runTest {
        val baseBytes = encodeManifest(mapOf("notes/gone.md" to md5("v1")))
        val remoteManifestBytes = encodeManifest(emptyMap())
        val localRepo = MockProjectRepository(mapOf(manifestPath to baseBytes))
        val syncRepo = MockSyncRepository(mapOf(remoteManifestPath to remoteManifestBytes))

        createUseCase(localRepo, syncRepo).invoke(SyncProjectInput(project, enabledSettings))

        assertTrue(readLocalManifest(localRepo).files.isEmpty())
        assertTrue(readRemoteManifest(syncRepo).files.isEmpty())
        assertTrue(localRepo.deletedPaths.isEmpty())
        assertTrue(syncRepo.deletedPaths.isEmpty())
    }

    @Test
    fun midSyncLocalEdit_isNotRecordedInManifestAndGetsUploadedOnNextSync() = runTest {
        val hashB1 = md5("b-v1")
        val baseBytes = encodeManifest(mapOf("notes/b.md" to hashB1))
        val localRepo = MockProjectRepository(
            mapOf(
                "notes/a.md" to "a-new".encodeToByteArray(),
                "notes/b.md" to "b-v1".encodeToByteArray(),
                manifestPath to baseBytes,
            ),
        )
        val syncRepo = MockSyncRepository(
            mapOf(
                "vault/notes/b.md" to "b-v1".encodeToByteArray(),
                remoteManifestPath to baseBytes,
            ),
        )

        // Simulate user editing notes/b.md while notes/a.md is being uploaded mid-sync
        syncRepo.onBeforeUpload = { path, _ ->
            if (path.value == "vault/notes/a.md") {
                localRepo.putFile("notes/b.md", "b-v2-mid-sync")
            }
        }

        val useCase = createUseCase(localRepo, syncRepo)
        useCase(SyncProjectInput(project, enabledSettings))

        // First sync must record the hash that was actually synced (b-v1), NOT b-v2-mid-sync
        assertEquals(hashB1, readLocalManifest(localRepo).files["notes/b.md"])
        assertEquals(hashB1, readRemoteManifest(syncRepo).files["notes/b.md"])
        assertEquals("b-v1", syncRepo.getFileText("vault/notes/b.md"))

        // Second sync sees local b-v2-mid-sync != base b-v1 and uploads it
        syncRepo.onBeforeUpload = null
        useCase(SyncProjectInput(project, enabledSettings))

        assertEquals("b-v2-mid-sync", syncRepo.getFileText("vault/notes/b.md"))
        assertEquals(md5("b-v2-mid-sync"), readLocalManifest(localRepo).files["notes/b.md"])
        assertEquals(md5("b-v2-mid-sync"), readRemoteManifest(syncRepo).files["notes/b.md"])
    }

    @Test
    fun midSyncLocalEditToUploadingFile_beforeUploadRead_uploadsLatestAndMatchesManifest() =
        runTest {
            val baseBytes = encodeManifest(mapOf("notes/a.md" to md5("a-v1")))
            val localRepo = MockProjectRepository(
                mapOf(
                    "notes/a.md" to "a-v2".encodeToByteArray(),
                    manifestPath to baseBytes,
                ),
            )
            val syncRepo = MockSyncRepository(
                mapOf(
                    "vault/notes/a.md" to "a-v1".encodeToByteArray(),
                    remoteManifestPath to baseBytes,
                ),
            )

            // Mutate notes/a.md right after snapshotLocal hashes "a-v2", before Upload reads it
            var mutated = false
            localRepo.onAfterRead = { path, _ ->
                if (path.value == "notes/a.md" && !mutated) {
                    mutated = true
                    localRepo.putFile("notes/a.md", "a-v3-before-upload-read")
                }
            }

            val useCase = createUseCase(localRepo, syncRepo)
            useCase(SyncProjectInput(project, enabledSettings))

            // Upload read and uploaded "a-v3-before-upload-read", and manifests must record its hash (not stale a-v2)
            val expectedHash = md5("a-v3-before-upload-read")
            assertEquals("a-v3-before-upload-read", syncRepo.getFileText("vault/notes/a.md"))
            assertEquals(expectedHash, readLocalManifest(localRepo).files["notes/a.md"])
            assertEquals(expectedHash, readRemoteManifest(syncRepo).files["notes/a.md"])

            // A follow-up sync must be a clean no-op (no mismatch between manifests and real files)
            syncRepo.uploadedPaths.clear()
            localRepo.writtenPaths.clear()
            useCase(SyncProjectInput(project, enabledSettings))
            assertTrue(syncRepo.uploadedPaths.isEmpty())
            assertTrue(localRepo.writtenPaths.isEmpty())
        }

    @Test
    fun midSyncLocalEditToUploadingFile_duringUpload_recordsUploadedHashAndUploadsEditNextSync() =
        runTest {
            val baseBytes = encodeManifest(mapOf("notes/a.md" to md5("a-v1")))
            val localRepo = MockProjectRepository(
                mapOf(
                    "notes/a.md" to "a-v2".encodeToByteArray(),
                    manifestPath to baseBytes,
                ),
            )
            val syncRepo = MockSyncRepository(
                mapOf(
                    "vault/notes/a.md" to "a-v1".encodeToByteArray(),
                    remoteManifestPath to baseBytes,
                ),
            )

            // Mutate notes/a.md after Upload has already read "a-v2" and is uploading it
            syncRepo.onBeforeUpload = { path, _ ->
                if (path.value == "vault/notes/a.md") {
                    localRepo.putFile("notes/a.md", "a-v3-during-upload")
                }
            }

            val useCase = createUseCase(localRepo, syncRepo)
            useCase(SyncProjectInput(project, enabledSettings))

            // Remote and both manifests must reflect the exact bytes that were uploaded ("a-v2"), never "a-v3"
            val hashV2 = md5("a-v2")
            assertEquals("a-v2", syncRepo.getFileText("vault/notes/a.md"))
            assertEquals("a-v3-during-upload", localRepo.getFileText("notes/a.md"))
            assertEquals(hashV2, readLocalManifest(localRepo).files["notes/a.md"])
            assertEquals(hashV2, readRemoteManifest(syncRepo).files["notes/a.md"])

            // Next sync detects local "a-v3-during-upload" != base "a-v2" and converges everything
            syncRepo.onBeforeUpload = null
            useCase(SyncProjectInput(project, enabledSettings))

            val hashV3 = md5("a-v3-during-upload")
            assertEquals("a-v3-during-upload", syncRepo.getFileText("vault/notes/a.md"))
            assertEquals(hashV3, readLocalManifest(localRepo).files["notes/a.md"])
            assertEquals(hashV3, readRemoteManifest(syncRepo).files["notes/a.md"])
        }

    @Test
    fun corruptBaseAndRemoteManifests_fallBackToEmptyManifest() = runTest {
        val localRepo = MockProjectRepository(
            mapOf(
                "notes/a.md" to "content".encodeToByteArray(),
                manifestPath to "not valid json {{{".encodeToByteArray(),
            ),
        )
        val syncRepo = MockSyncRepository(
            mapOf(remoteManifestPath to "also broken".encodeToByteArray()),
        )

        createUseCase(localRepo, syncRepo).invoke(SyncProjectInput(project, enabledSettings))

        assertEquals("content", syncRepo.getFileText("vault/notes/a.md"))
        assertEquals(mapOf("notes/a.md" to md5("content")), readLocalManifest(localRepo).files)
        assertEquals(mapOf("notes/a.md" to md5("content")), readRemoteManifest(syncRepo).files)
    }

    @Test
    fun missingRemoteBlobDuringDownload_throwsSyncStateExceptionWithoutUpdatingManifests() =
        runTest {
            val baseBytes = encodeManifest(emptyMap())
            val remoteManifestBytes = encodeManifest(mapOf("notes/missing.md" to md5("ghost")))
            val localRepo = MockProjectRepository(mapOf(manifestPath to baseBytes))
            val syncRepo = MockSyncRepository(
                mapOf(remoteManifestPath to remoteManifestBytes),
                // Note: "vault/notes/missing.md" is deliberately absent
            )

            assertFailsWith<SyncStateException> {
                createUseCase(localRepo, syncRepo).invoke(
                    SyncProjectInput(
                        project,
                        enabledSettings
                    )
                )
            }

            // Manifests were not overwritten with partial/broken state
            assertTrue(readLocalManifest(localRepo).files.isEmpty())
            assertEquals(
                mapOf("notes/missing.md" to md5("ghost")),
                readRemoteManifest(syncRepo).files
            )
        }
}
