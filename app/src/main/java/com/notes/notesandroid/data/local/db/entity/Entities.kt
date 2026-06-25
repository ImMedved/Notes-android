package com.notes.notesandroid.data.local.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.notes.notesandroid.data.model.PendingSyncAction
import com.notes.notesandroid.data.model.TimerMode

@Entity(tableName = "notes")
data class NoteEntity(
    @PrimaryKey val id: String,
    val title: String,
    val content: String,
    val pinned: Boolean,
    val createdAt: Long,
    val updatedAt: Long,
    val isDeleted: Boolean,
    val pendingSyncAction: PendingSyncAction,
    val remoteExists: Boolean,
)

@Entity(tableName = "timers")
data class TimerEntity(
    @PrimaryKey val id: String,
    val name: String,
    val mode: TimerMode,
    val durationMillis: Long,
    val startedAt: Long?,
    val accumulatedMillis: Long,
    val running: Boolean,
    val createdAt: Long,
    val updatedAt: Long,
    val isDeleted: Boolean,
    val pendingSyncAction: PendingSyncAction,
    val remoteExists: Boolean,
)

@Entity(tableName = "sync_metadata")
data class SyncMetadataEntity(
    @PrimaryKey val id: Int = 0,
    val revision: Long = 0L,
    val serverTimeEpochMillis: Long = 0L,
    val isRunning: Boolean = false,
    val lastMessage: String = "Local mode is ready. Add server settings to enable sync.",
    val lastAttemptAt: Long? = null,
    val lastSuccessAt: Long? = null,
)
