package com.mangustc.mdnotes.domain.usecases.sync

import com.mangustc.mdnotes.domain.models.RelativePath
import kotlinx.serialization.Serializable

@Serializable
data class SyncManifest(
    val lastSyncTimestamp: Long,
    val files: Map<String, String> = emptyMap(),
) {
    companion object {
        val ProjectRelativePath = RelativePath(".sync_manifest.json")
        val Empty = SyncManifest(lastSyncTimestamp = 0L)
    }
}