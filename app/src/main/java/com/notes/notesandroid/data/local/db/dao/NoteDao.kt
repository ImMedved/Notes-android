package com.notes.notesandroid.data.local.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.notes.notesandroid.data.local.db.entity.NoteEntity
import com.notes.notesandroid.data.model.PendingSyncAction
import kotlinx.coroutines.flow.Flow

@Dao
interface NoteDao {
    @Query(
        """
        SELECT * FROM notes
        WHERE isDeleted = 0
        ORDER BY pinned DESC, updatedAt DESC
        """
    )
    fun observeVisibleNotes(): Flow<List<NoteEntity>>

    @Query("SELECT * FROM notes WHERE id = :id LIMIT 1")
    fun observeNoteById(id: String): Flow<NoteEntity?>

    @Query("SELECT * FROM notes WHERE id = :id LIMIT 1")
    suspend fun getNoteById(id: String): NoteEntity?

    @Query(
        """
        SELECT * FROM notes
        WHERE isDeleted = 0
        ORDER BY pinned DESC, updatedAt DESC
        LIMIT 1
        """
    )
    suspend fun getTopVisibleNote(): NoteEntity?

    @Query(
        """
        SELECT * FROM notes
        WHERE isDeleted = 0
        ORDER BY pinned DESC, updatedAt DESC
        """
    )
    suspend fun getVisibleNotes(): List<NoteEntity>

    @Query(
        """
        SELECT * FROM notes
        WHERE pendingSyncAction != :none
        ORDER BY updatedAt ASC
        """
    )
    suspend fun getPendingSyncNotes(none: PendingSyncAction = PendingSyncAction.NONE): List<NoteEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: NoteEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(entities: List<NoteEntity>)

    @Query("DELETE FROM notes WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM notes")
    suspend fun deleteAll()

    @Transaction
    suspend fun replaceSnapshot(entities: List<NoteEntity>) {
        deleteAll()
        if (entities.isNotEmpty()) {
            upsertAll(entities)
        }
    }
}
