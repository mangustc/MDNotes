package com.mangustc.mdnotes.mocks

import com.mangustc.mdnotes.domain.models.RelativePath
import com.mangustc.mdnotes.domain.repositories.SyncRepository
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class MockSyncRepository(
    initialFiles: Map<String, ByteArray> = emptyMap(),
    override val name: String = "MockSync",
) : SyncRepository {

    private val mutex = Mutex()
    private val files = initialFiles.mapValues { it.value.copyOf() }.toMutableMap()

    val uploadedPaths = mutableListOf<String>()
    val downloadedPaths = mutableListOf<String>()
    val deletedPaths = mutableListOf<String>()

    var onBeforeUpload: (suspend (RelativePath, ByteArray) -> Unit)? = null

    suspend fun putFile(path: String, content: String) = putBytes(path, content.encodeToByteArray())

    suspend fun putBytes(path: String, bytes: ByteArray) = mutex.withLock {
        files[path] = bytes.copyOf()
    }

    suspend fun getFileText(path: String): String? = mutex.withLock {
        files[path]?.decodeToString()
    }

    suspend fun allFilePaths(): Set<String> = mutex.withLock {
        files.keys.toSet()
    }

    override suspend fun downloadFile(path: RelativePath): ByteArray? = mutex.withLock {
        downloadedPaths += path.value
        files[path.value]?.copyOf()
    }

    override suspend fun uploadFile(path: RelativePath, bytes: ByteArray) {
        onBeforeUpload?.invoke(path, bytes)
        mutex.withLock {
            files[path.value] = bytes.copyOf()
            uploadedPaths += path.value
        }
    }

    override suspend fun deleteFile(path: RelativePath): Unit = mutex.withLock {
        files.remove(path.value)
        deletedPaths += path.value
    }
}
