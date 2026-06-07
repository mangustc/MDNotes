package com.mangustc.mdnotes.domain.repositories

import com.mangustc.mdnotes.domain.models.RelativePath

interface SyncRepository {
    val name: String
    suspend fun downloadFile(path: RelativePath): ByteArray?
    suspend fun uploadFile(path: RelativePath, bytes: ByteArray)
    suspend fun deleteFile(path: RelativePath)
}