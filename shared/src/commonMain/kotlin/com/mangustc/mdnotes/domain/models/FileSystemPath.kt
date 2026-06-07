package com.mangustc.mdnotes.domain.models

data class FileSystemPath(val value: String) {
    override fun toString(): String {
        return value
    }
}