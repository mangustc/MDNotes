package com.mangustc.mdnotes.data.project

import androidx.room.RoomRawQuery
import com.mangustc.mdnotes.domain.models.FrontMatter
import com.mangustc.mdnotes.domain.models.Project
import com.mangustc.mdnotes.domain.models.SearchQuery

private fun escapeLike(value: String): String =
    value
        .replace("\\", "\\\\")
        .replace("%", "\\%")
        .replace("_", "\\_")

fun SearchQuery.buildRoomRawQuery(project: Project): RoomRawQuery {
    val args = mutableListOf<Any>()
    val sb = StringBuilder()
    val ftsMatchExpr = buildFtsMatchQuery()

    sb.append("SELECT notes.* FROM notes")
    if (ftsMatchExpr != null) {
        sb.append("\nJOIN notesFts ON notes.rowid = notesFts.rowid")
        sb.append("\n  AND notesFts MATCH ?")
        args.add(ftsMatchExpr)
    }

    val conditions = mutableListOf<String>()
    conditions.add("notes.projectId = (SELECT id FROM projects WHERE rootPath = ? LIMIT 1)")
    args.add(project.rootDomainFile.path)
    for (term in negatedBodyTerms) {
        conditions.add("(notes.name NOT LIKE ? ESCAPE '\\' AND notes.body NOT LIKE ? ESCAPE '\\')")
        val pattern = "%${escapeLike(term)}%"
        args.add(pattern)
        args.add(pattern)
    }
    for (tag in positiveTagLikes()) {
        conditions.add(
            "EXISTS (SELECT 1 FROM note_tags WHERE note_tags.noteId = notes.id AND note_tags.tag = ?)",
        )
        args.add(tag)
    }
    for (tag in negatedTagLikes()) {
        conditions.add(
            "NOT EXISTS (SELECT 1 FROM note_tags WHERE note_tags.noteId = notes.id AND note_tags.tag = ?)",
        )
        args.add(tag)
    }
    nameFilter?.let {
        conditions.add("notes.name LIKE ? ESCAPE '\\'")
        args.add("%${escapeLike(it)}%")
    }
    negatedNameFilter?.let {
        conditions.add("notes.name NOT LIKE ? ESCAPE '\\'")
        args.add("%${escapeLike(it)}%")
    }
    if (conditions.isNotEmpty()) {
        sb.append("\nWHERE ")
        sb.append(conditions.joinToString("\n  AND "))
    }

    val pinnedClause = if (pinnedFirst) {
        args.add(FrontMatter.PINNED_TAG)
        "CASE WHEN EXISTS (SELECT 1 FROM note_tags WHERE note_tags.noteId = notes.id AND note_tags.tag = ?) THEN 0 ELSE 1 END ASC,\n  "
    } else {
        ""
    }
    val sortClause = when (sortBy) {
        SearchQuery.SortBy.LAST_MODIFIED -> "notes.lastModified DESC"
        SearchQuery.SortBy.CREATED_AT ->
            "notes.createdAt DESC, notes.lastModified DESC"
    }
    sb.append("\nORDER BY $pinnedClause$sortClause")

    return RoomRawQuery(
        sql = sb.toString(),
        onBindStatement = { statement ->
            args.forEachIndexed { index, arg ->
                when (arg) {
                    is String -> statement.bindText(index + 1, arg)
                    is Long -> statement.bindLong(index + 1, arg)
                    is Int -> statement.bindLong(index + 1, arg.toLong())
                    is Double -> statement.bindDouble(index + 1, arg)
                    is Boolean -> statement.bindBoolean(index + 1, arg)
                    else -> throw IllegalArgumentException("Unknown argument type")
                }
            }
        },
    )
}
