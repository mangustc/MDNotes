package com.mangustc.mdnotes.domain.repositories

import com.mangustc.mdnotes.domain.models.LinkPreview

interface LinkPreviewRepository {
    suspend fun getLinkPreview(url: String): LinkPreview?
}