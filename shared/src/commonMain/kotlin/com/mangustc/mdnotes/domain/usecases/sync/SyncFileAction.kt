package com.mangustc.mdnotes.domain.usecases.sync

import com.mangustc.mdnotes.domain.models.RelativePath

sealed interface SyncFileAction {
    val relativePath: RelativePath

    data class Upload(override val relativePath: RelativePath) : SyncFileAction
    data class Download(override val relativePath: RelativePath) : SyncFileAction
    data class DeleteLocal(override val relativePath: RelativePath) : SyncFileAction
    data class DeleteRemote(override val relativePath: RelativePath) : SyncFileAction
    data class ConflictUpload(override val relativePath: RelativePath) : SyncFileAction
    data class NoOp(override val relativePath: RelativePath) : SyncFileAction
}