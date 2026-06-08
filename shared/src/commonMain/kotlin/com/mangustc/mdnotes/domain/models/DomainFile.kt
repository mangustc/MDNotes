package com.mangustc.mdnotes.domain.models

import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.extension
import io.github.vinceglb.filekit.name
import io.github.vinceglb.filekit.nameWithoutExtension
import io.github.vinceglb.filekit.path
import io.github.vinceglb.filekit.resolve

@JvmInline
value class DomainFile(val file: PlatformFile) {
    val name: String
        get() = file.name
    val nameWithoutExtension: String
        get() = file.nameWithoutExtension
    val extension: String
        get() = file.extension
    val path: String
        get() = file.path

    fun resolve(relativePath: RelativePath): DomainFile =
        DomainFile(file.resolve(relativePath.value))

    operator fun div(child: RelativePath): DomainFile = resolve(child)

    override fun toString(): String {
        return file.path
    }
}