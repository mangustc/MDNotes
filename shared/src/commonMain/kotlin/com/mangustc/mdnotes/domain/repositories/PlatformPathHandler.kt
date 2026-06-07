package com.mangustc.mdnotes.domain.repositories

import com.mangustc.mdnotes.domain.models.FileSystemPath

interface PlatformPathHandler {
    fun takePersistablePathPermission(path: FileSystemPath)
}