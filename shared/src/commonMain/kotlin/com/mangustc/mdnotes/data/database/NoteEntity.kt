package com.mangustc.mdnotes.data.database

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Fts4
import androidx.room.FtsOptions
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.Relation

@Entity(
    tableName = "notes",
    indices = [
        Index(value = ["uri"], unique = true),
        Index(value = ["projectId"]),
    ],
    foreignKeys = [
        ForeignKey(
            entity = ProjectEntity::class,
            parentColumns = ["id"],
            childColumns = ["projectId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
)
data class NoteEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val projectId: Long,
    val uri: String,
    val name: String,
    val lastModified: Long,
    val createdAt: Long? = null,
    val body: String,
) {
    companion object {
        fun normalizeTags(tags: List<String>): List<String> =
            tags.asSequence()
                .map { it.trim() }
                .filter { it.isNotEmpty() }
                .distinct()
                .toList()
    }
}

@Entity(
    tableName = "note_tags",
    primaryKeys = ["noteId", "tag"],
    indices = [
        Index(value = ["tag", "noteId"]),
        Index(value = ["noteId"]),
    ],
    foreignKeys = [
        ForeignKey(
            entity = NoteEntity::class,
            parentColumns = ["id"],
            childColumns = ["noteId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
)
data class NoteTagEntity(
    val noteId: Long,
    val tag: String,
)

data class NoteWithTags(
    @Embedded val note: NoteEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "noteId",
        entity = NoteTagEntity::class,
        projection = ["tag"],
    )
    val tags: List<String>,
)

@Fts4(
    contentEntity = NoteEntity::class,
    tokenizer = FtsOptions.TOKENIZER_UNICODE61,
)
@Entity(tableName = "notesFts")
data class NoteEntityFts(
    val id: Long,
    val name: String,
    val body: String,
)