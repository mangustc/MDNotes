package com.mangustc.mdnotes.domain.repositories

import com.mangustc.mdnotes.domain.models.DomainFile

interface PlatformPathHandler {
    fun takePersistablePathPermission(path: DomainFile)
}