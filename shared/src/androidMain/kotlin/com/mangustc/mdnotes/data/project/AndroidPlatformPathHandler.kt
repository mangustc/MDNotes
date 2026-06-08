package com.mangustc.mdnotes.data.project

import android.content.Context
import android.content.Intent
import com.mangustc.mdnotes.domain.models.DomainFile
import com.mangustc.mdnotes.domain.repositories.PlatformPathHandler
import io.github.vinceglb.filekit.dialogs.toAndroidUri

class AndroidPlatformPathHandler(
    private val context: Context,
) : PlatformPathHandler {
    override fun takePersistablePathPermission(path: DomainFile) {
        val projectUri = path.file.toAndroidUri()
        context.contentResolver.takePersistableUriPermission(
            projectUri,
            Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION,
        )
    }
}