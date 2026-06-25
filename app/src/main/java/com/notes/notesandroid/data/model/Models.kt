package com.notes.notesandroid.data.model

data class Note(
    val id: String,
    val title: String,
    val content: String,
    val pinned: Boolean,
    val createdAt: Long,
    val updatedAt: Long,
)

enum class TimerMode {
    COUNTDOWN,
    STOPWATCH,
}

data class TimerEntry(
    val id: String,
    val name: String,
    val mode: TimerMode,
    val durationMillis: Long,
    val startedAt: Long?,
    val accumulatedMillis: Long,
    val running: Boolean,
    val createdAt: Long,
    val updatedAt: Long,
)

enum class PendingSyncAction {
    NONE,
    UPSERT,
    DELETE,
}

enum class AppThemeMode {
    SYSTEM,
    LIGHT,
    DARK,
}

data class ServerSnapshot(
    val revision: Long,
    val serverTimeEpochMillis: Long,
    val notes: List<Note>,
    val timers: List<TimerEntry>,
)

data class ApiErrorEnvelope(
    val error: ApiErrorBody? = null,
)

data class ApiErrorBody(
    val code: String = "unknown",
    val message: String = "Unknown server error.",
)

data class DeleteResponse(
    val ok: Boolean,
    val id: String,
)

data class AppPreferences(
    val baseUrl: String = "",
    val apiKey: String = "",
    val clientId: String = "",
    val autoSync: Boolean = true,
    val themeMode: AppThemeMode = AppThemeMode.SYSTEM,
)

data class SyncStatus(
    val revision: Long = 0L,
    val serverTimeEpochMillis: Long = 0L,
    val isRunning: Boolean = false,
    val message: String = "Local mode is ready. Add server settings to enable sync.",
    val lastAttemptAt: Long? = null,
    val lastSyncSuccessAt: Long? = null,
)

data class DashboardSnapshot(
    val notes: List<Note> = emptyList(),
    val timers: List<TimerEntry> = emptyList(),
    val preferences: AppPreferences = AppPreferences(),
    val syncStatus: SyncStatus = SyncStatus(),
)

fun TimerEntry.elapsedAt(now: Long): Long {
    val live = if (running && startedAt != null) now - startedAt else 0L
    return (accumulatedMillis + live).coerceAtLeast(0L)
}

fun TimerEntry.remainingAt(now: Long): Long =
    (durationMillis - elapsedAt(now)).coerceAtLeast(0L)

fun TimerEntry.toggleRunning(now: Long): TimerEntry {
    return if (running) {
        copy(
            startedAt = null,
            accumulatedMillis = elapsedAt(now),
            running = false,
            updatedAt = now,
        )
    } else {
        val resetAccumulated = if (mode == TimerMode.COUNTDOWN && remainingAt(now) == 0L) 0L else accumulatedMillis
        copy(
            startedAt = now,
            accumulatedMillis = resetAccumulated,
            running = true,
            updatedAt = now,
        )
    }
}

fun TimerEntry.reset(now: Long): TimerEntry = copy(
    startedAt = null,
    accumulatedMillis = 0L,
    running = false,
    updatedAt = now,
)

fun Note.previewText(): String {
    return content
        .replace("\r", "")
        .lines()
        .map { it.trim() }
        .firstOrNull { it.isNotBlank() }
        ?.take(180)
        ?: "Empty markdown note"
}
