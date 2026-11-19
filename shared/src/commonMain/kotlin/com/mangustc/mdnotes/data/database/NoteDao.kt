package com.mangustc.mdnotes.data.database

import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.RawQuery
import androidx.room.RoomRawQuery
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface NoteDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNote(note: NoteEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNotes(notes: List<NoteEntity>): List<Long>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertNoteTags(tags: List<NoteTagEntity>)

    @Query("DELETE FROM note_tags WHERE noteId = :noteId")
    suspend fun deleteTagsForNote(noteId: Long)

    @Query("DELETE FROM notes WHERE uri = :uri")
    suspend fun deleteByUri(uri: String)

    @Transaction
    suspend fun upsertNoteWithTags(note: NoteEntity, tags: List<String>): Long {
        val noteId = insertNote(note)
        deleteTagsForNote(noteId)
        val tagEntities = NoteEntity.normalizeTags(tags)
            .map { NoteTagEntity(noteId = noteId, tag = it) }
        if (tagEntities.isNotEmpty()) {
            insertNoteTags(tagEntities)
        }
        return noteId
    }

    @Transaction
    suspend fun applySync(
        toUpsert: List<NoteWithTags>,
        toDeleteUris: List<String>,
    ) {
        toDeleteUris.forEach { deleteByUri(it) }
        if (toUpsert.isNotEmpty()) {
            val ids = insertNotes(toUpsert.map { it.note })
            val allTagEntities = buildList {
                toUpsert.forEachIndexed { index, item ->
                    val noteId = ids[index]
                    if (item.note.id != 0L) {
                        deleteTagsForNote(noteId)
                    }
                    NoteEntity.normalizeTags(item.tags).forEach { tag ->
                        add(NoteTagEntity(noteId = noteId, tag = tag))
                    }
                }
            }
            if (allTagEntities.isNotEmpty()) {
                insertNoteTags(allTagEntities)
            }
        }
    }

    @Query("SELECT * FROM notes WHERE uri = :uri LIMIT 1")
    suspend fun getNoteByUri(uri: String): NoteEntity?

    @Query("SELECT * FROM notes WHERE projectId = :projectId")
    suspend fun getNotesForProject(projectId: Long): List<NoteEntity>

    @Transaction
    @RawQuery(
        observedEntities = [
            NoteEntity::class,
            NoteEntityFts::class,
            NoteTagEntity::class,
            ProjectEntity::class,
        ],
    )
    fun observeNotes(query: RoomRawQuery): Flow<List<NoteWithTags>>

    @Transaction
    @RawQuery(
        observedEntities = [
            NoteEntity::class,
            NoteEntityFts::class,
            NoteTagEntity::class,
            ProjectEntity::class,
        ],
    )
    fun searchNotesPaged(query: RoomRawQuery): PagingSource<Int, NoteWithTags>

    @Query(
        """
        SELECT DISTINCT nt.tag
        FROM note_tags nt
        INNER JOIN notes n ON n.id = nt.noteId
        INNER JOIN projects p ON p.id = n.projectId
        WHERE p.rootPath = :rootPath
        ORDER BY nt.tag ASC
        """,
    )
    fun observeAllTags(rootPath: String): Flow<List<String>>
}