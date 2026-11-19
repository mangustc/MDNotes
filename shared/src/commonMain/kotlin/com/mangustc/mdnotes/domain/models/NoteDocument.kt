package com.mangustc.mdnotes.domain.models

data class NoteDocument(
    val frontMatter: FrontMatter = FrontMatter.Empty,
    val body: String = "",
) {
    fun serialize(): String =
        if (frontMatter.fields.isNotEmpty()) {
            "$frontMatter\n$body"
        } else {
            body
        }

    companion object {
        fun parse(content: String): NoteDocument {
            val trimmed = content.trimStart()
            if (!trimmed.startsWith("---")) return NoteDocument(FrontMatter.Empty, content)

            val lines = trimmed.lines()
            if (lines.first().trim() != "---") return NoteDocument(FrontMatter.Empty, content)

            val closeIdx = lines.drop(1).indexOfFirst { it.trim() == "---" }
            if (closeIdx < 0) return NoteDocument(FrontMatter.Empty, content)

            val fmText = lines.drop(1).take(closeIdx).joinToString("\n")
            val body = lines.drop(closeIdx + 2).joinToString("\n")
            val frontMatter = try {
                FrontMatter.parse(fmText)
            } catch (_: IllegalArgumentException) {
                return NoteDocument(FrontMatter.Empty, content)
            }
            return NoteDocument(frontMatter, body)
        }
    }
}
