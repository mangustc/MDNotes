package com.mangustc.mdnotes.domain.usecases.linkPreview

import com.mangustc.mdnotes.domain.models.LinkPreview
import com.mangustc.mdnotes.domain.repositories.LinkPreviewRepository
import com.mangustc.mdnotes.domain.usecases.UseCase

data class GetLinkPreviewInput(
    val url: String,
)

class GetLinkPreviewUseCase(
    private val linkPreviewRepository: LinkPreviewRepository,
) : UseCase<GetLinkPreviewInput, LinkPreview?> {
    override suspend fun invoke(input: GetLinkPreviewInput): LinkPreview? {
        return linkPreviewRepository.getLinkPreview(input.url)
    }
}
