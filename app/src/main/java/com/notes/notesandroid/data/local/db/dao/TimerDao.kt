package com.notes.notesandroid.data.local.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.notes.notesandroid.data.local.db.entity.TimerEntity
import com.notes.notesandroid.data.model.PendingSyncAction
import kotlinx.coroutines.flow.Flow

@Dao
interface TimerDao {
    @Query(
        """
        SELECT * FROM timers
        WHERE isDeleted = 0
        ORDER BY running DESC, updatedAt DESC
        """
    )
    fun observeVisibleTimers(): Flow<List<TimerEntity>>

    @Query("SELECT * FROM timers WHERE id = :id LIMIT 1")
    fun observeTimerById(id: String): Flow<TimerEntity?>

    @Query("SELECT * FROM timers WHERE id = :id LIMIT 1")
    suspend fun getTimerById(id: String): TimerEntity?

    @Query(
        """
        SELECT * FROM timers
        WHERE isDeleted = 0
        ORDER BY running DESC, updatedAt DESC
        LIMIT 1
        """
    )
    suspend fun getTopVisibleTimer(): TimerEntity?

    @Query(
        """
        SELECT * FROM timers
        WHERE isDeleted = 0
        ORDER BY running DESC, updatedAt DESC
        """
    )
    suspend fun getVisibleTimers(): List<TimerEntity>

    @Query(
        """
        SELECT * FROM timers
        WHERE pendingSyncAction != :none
        ORDER BY updatedAt ASC
        """
    )
    suspend fun getPendingSyncTimers(none: PendingSyncAction = PendingSyncAction.NONE): List<TimerEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: TimerEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(entities: List<TimerEntity>)

    @Query("DELETE FROM timers WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM timers")
    suspend fun deleteAll()

    @Transaction
    suspend fun replaceSnapshot(entities: List<TimerEntity>) {
        deleteAll()
        if (entities.isNotEmpty()) {
            upsertAll(entities)
        }
    }
}
