package com.notes.notesandroid.data.local.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.notes.notesandroid.data.local.db.dao.NoteDao
import com.notes.notesandroid.data.local.db.dao.SyncMetadataDao
import com.notes.notesandroid.data.local.db.dao.TimerDao
import com.notes.notesandroid.data.local.db.entity.NoteEntity
import com.notes.notesandroid.data.local.db.entity.SyncMetadataEntity
import com.notes.notesandroid.data.local.db.entity.TimerEntity

@Database(
    entities = [NoteEntity::class, TimerEntity::class, SyncMetadataEntity::class],
    version = 1,
    exportSchema = false,
)
@TypeConverters(DatabaseConverters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun noteDao(): NoteDao
    abstract fun timerDao(): TimerDao
    abstract fun syncMetadataDao(): SyncMetadataDao
}
