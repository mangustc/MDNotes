package com.mangustc.mdnotes.data.database

import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.RawQuery
import androidx.room.RoomRawQuery
import androidx.room.Transaction

@Dao
interface NoteDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNote(note: NoteEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNotes(notes: List<NoteEntity>)

    @Query("DELETE FROM notes WHERE uri = :uri")
    suspend fun deleteByUri(uri: String)

    @Transaction
    suspend fun applySync(
        toUpsert: List<NoteEntity>,
        toDeleteUris: List<String>,
    ) {
        toDeleteUris.forEach { deleteByUri(it) }
        if (toUpsert.isNotEmpty()) {
            insertNotes(toUpsert)
        }
    }

    @Query("SELECT * FROM notes WHERE uri = :uri LIMIT 1")
    suspend fun getNoteByUri(uri: String): NoteEntity?

    @RawQuery
    suspend fun searchNotes(query: RoomRawQuery): List<NoteEntity>

    @RawQuery(observedEntities = [NoteEntity::class, NoteEntityFts::class])
    fun searchNotesPaged(query: RoomRawQuery): PagingSource<Int, NoteEntity>

    @Query("SELECT tags FROM notes WHERE projectId = :projectId")
    suspend fun getAllTags(projectId: Long): List<String>
}