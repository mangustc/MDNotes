package com.mangustc.mdnotes.domain.models

data class ProjectFile(
    val domainFile: DomainFile,
    val relativePath: RelativePath,
)