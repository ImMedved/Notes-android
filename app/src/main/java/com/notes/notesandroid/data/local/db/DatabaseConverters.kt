package com.notes.notesandroid.data.local.db

import androidx.room.TypeConverter
import com.notes.notesandroid.data.model.AppThemeMode
import com.notes.notesandroid.data.model.PendingSyncAction
import com.notes.notesandroid.data.model.TimerMode

class DatabaseConverters {
    @TypeConverter
    fun fromTimerMode(value: TimerMode): String = value.name

    @TypeConverter
    fun toTimerMode(value: String): TimerMode = TimerMode.valueOf(value)

    @TypeConverter
    fun fromPendingSyncAction(value: PendingSyncAction): String = value.name

    @TypeConverter
    fun toPendingSyncAction(value: String): PendingSyncAction = PendingSyncAction.valueOf(value)

    @TypeConverter
    fun fromThemeMode(value: AppThemeMode): String = value.name

    @TypeConverter
    fun toThemeMode(value: String): AppThemeMode = AppThemeMode.valueOf(value)
}
