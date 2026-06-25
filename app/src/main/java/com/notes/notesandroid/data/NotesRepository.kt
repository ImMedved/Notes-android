package com.notes.notesandroid.data

import android.content.Context
import androidx.room.Room
import androidx.room.withTransaction
import com.notes.notesandroid.NotesApplication
import com.notes.notesandroid.data.local.db.AppDatabase
import com.notes.notesandroid.data.local.db.entity.NoteEntity
import com.notes.notesandroid.data.local.db.entity.SyncMetadataEntity
import com.notes.notesandroid.data.local.db.entity.TimerEntity
import com.notes.notesandroid.data.model.AppPreferences
import com.notes.notesandroid.data.model.AppThemeMode
import com.notes.notesandroid.data.model.DashboardSnapshot
import com.notes.notesandroid.data.model.Note
import com.notes.notesandroid.data.model.PendingSyncAction
import com.notes.notesandroid.data.model.SyncStatus
import com.notes.notesandroid.data.model.TimerEntry
import com.notes.notesandroid.data.model.TimerMode
import com.notes.notesandroid.data.model.reset
import com.notes.notesandroid.data.model.toggleRunning
import com.notes.notesandroid.data.preferences.SyncPreferencesDataSource
import com.notes.notesandroid.data.remote.NotesApi
import com.notes.notesandroid.data.remote.NotesApiFactory
import com.notes.notesandroid.widget.WidgetUpdater
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

class NotesRepository(
    context: Context,
) {
    private val appContext = context.applicationContext
    private val database = Room.databaseBuilder(
        appContext,
        AppDatabase::class.java,
        "paper-notes.db",
    ).fallbackToDestructiveMigration().build()
    private val noteDao = database.noteDao()
    private val timerDao = database.timerDao()
    private val syncMetadataDao = database.syncMetadataDao()
    private val preferencesDataSource = SyncPreferencesDataSource(appContext)
    private val apiFactory = NotesApiFactory()
    private val syncMutex = Mutex()
    private val repositoryScope = NotesApplication.appScope
    private val syncRunningFlow = MutableStateFlow(false)

    val preferences: Flow<AppPreferences> = preferencesDataSource.preferencesFlow

    val notes: Flow<List<Note>> = noteDao.observeVisibleNotes().map { entities ->
        entities.map { it.toDomain() }
    }

    val timers: Flow<List<TimerEntry>> = timerDao.observeVisibleTimers().map { entities ->
        entities.map { it.toDomain() }
    }

    val syncStatus: Flow<SyncStatus> = combine(
        syncMetadataDao.observe().map { it ?: SyncMetadataEntity() },
        syncRunningFlow,
    ) { metadata, isRunning ->
        SyncStatus(
            revision = metadata.revision,
            serverTimeEpochMillis = metadata.serverTimeEpochMillis,
            isRunning = isRunning || metadata.isRunning,
            message = metadata.lastMessage,
            lastAttemptAt = metadata.lastAttemptAt,
            lastSyncSuccessAt = metadata.lastSuccessAt,
        )
    }

    val dashboard: StateFlow<DashboardSnapshot> = combine(
        notes,
        timers,
        preferences,
        syncStatus,
    ) { notes, timers, preferences, syncStatus ->
        DashboardSnapshot(
            notes = notes,
            timers = timers,
            preferences = preferences,
            syncStatus = syncStatus,
        )
    }.stateIn(
        scope = repositoryScope,
        started = SharingStarted.Eagerly,
        initialValue = DashboardSnapshot(),
    )

    fun observeNote(noteId: String): Flow<Note?> = noteDao.observeNoteById(noteId).map { entity ->
        entity?.takeUnless { it.isDeleted }?.toDomain()
    }

    fun observeTimer(timerId: String): Flow<TimerEntry?> = timerDao.observeTimerById(timerId).map { entity ->
        entity?.takeUnless { it.isDeleted }?.toDomain()
    }

    suspend fun saveSyncSettings(baseUrl: String, apiKey: String, autoSync: Boolean) {
        preferencesDataSource.updateSyncSettings(baseUrl, apiKey, autoSync)
        val preferences = preferencesDataSource.current()
        SyncScheduler.applySettings(appContext, preferences)
        if (preferences.baseUrl.isNotBlank()) {
            SyncScheduler.enqueueImmediate(appContext)
        }
    }

    suspend fun saveThemeMode(mode: AppThemeMode) {
        preferencesDataSource.updateThemeMode(mode)
    }

    suspend fun upsertNote(note: Note) = withContext(Dispatchers.IO) {
        val existing = noteDao.getNoteById(note.id)
        noteDao.upsert(
            note.toEntity(
                pendingSyncAction = PendingSyncAction.UPSERT,
                remoteExists = existing?.remoteExists == true,
            )
        )
        WidgetUpdater.updateAll(appContext)
        scheduleSyncIfConfigured()
    }

    suspend fun deleteNote(noteId: String) = withContext(Dispatchers.IO) {
        val existing = noteDao.getNoteById(noteId) ?: return@withContext
        if (!existing.remoteExists) {
            noteDao.deleteById(noteId)
        } else {
            noteDao.upsert(
                existing.copy(
                    isDeleted = true,
                    pendingSyncAction = PendingSyncAction.DELETE,
                    updatedAt = System.currentTimeMillis(),
                )
            )
        }
        WidgetUpdater.updateAll(appContext)
        scheduleSyncIfConfigured()
    }

    suspend fun upsertTimer(timer: TimerEntry) = withContext(Dispatchers.IO) {
        val existing = timerDao.getTimerById(timer.id)
        timerDao.upsert(
            timer.toEntity(
                pendingSyncAction = PendingSyncAction.UPSERT,
                remoteExists = existing?.remoteExists == true,
            )
        )
        WidgetUpdater.updateAll(appContext)
        scheduleSyncIfConfigured()
    }

    suspend fun deleteTimer(timerId: String) = withContext(Dispatchers.IO) {
        val existing = timerDao.getTimerById(timerId) ?: return@withContext
        if (!existing.remoteExists) {
            timerDao.deleteById(timerId)
        } else {
            timerDao.upsert(
                existing.copy(
                    isDeleted = true,
                    pendingSyncAction = PendingSyncAction.DELETE,
                    updatedAt = System.currentTimeMillis(),
                )
            )
        }
        WidgetUpdater.updateAll(appContext)
        scheduleSyncIfConfigured()
    }

    suspend fun toggleTimer(timerId: String) = withContext(Dispatchers.IO) {
        val timer = timerDao.getTimerById(timerId)?.takeUnless { it.isDeleted }?.toDomain() ?: return@withContext
        upsertTimer(timer.toggleRunning(System.currentTimeMillis()))
    }

    suspend fun resetTimer(timerId: String) = withContext(Dispatchers.IO) {
        val timer = timerDao.getTimerById(timerId)?.takeUnless { it.isDeleted }?.toDomain() ?: return@withContext
        upsertTimer(timer.reset(System.currentTimeMillis()))
    }

    suspend fun syncNow(): Result<Unit> = syncMutex.withLock {
        val preferences = preferencesDataSource.current()
        if (preferences.baseUrl.isBlank()) {
            updateSyncMessage(
                "Saved locally. Add a server URL to enable sync.",
                isRunning = false,
                lastAttemptAt = System.currentTimeMillis(),
            )
            return Result.success(Unit)
        }

        val api = apiFactory.create(preferences)
            ?: run {
                updateSyncMessage(
                    "Invalid server URL.",
                    isRunning = false,
                    lastAttemptAt = System.currentTimeMillis(),
                )
                return Result.failure(IllegalArgumentException("Invalid server URL."))
            }

        syncRunningFlow.value = true
        val attemptAt = System.currentTimeMillis()
        updateSyncMessage(
            message = "Sync in progress...",
            isRunning = true,
            lastAttemptAt = attemptAt,
        )

        val result = runCatching {
            pushPendingChanges(api)
            val snapshot = api.snapshot()
            database.withTransaction {
                noteDao.replaceSnapshot(snapshot.notes.map { it.toEntity(remoteExists = true) })
                timerDao.replaceSnapshot(snapshot.timers.map { it.toEntity(remoteExists = true) })
                syncMetadataDao.upsert(
                    SyncMetadataEntity(
                        revision = snapshot.revision,
                        serverTimeEpochMillis = snapshot.serverTimeEpochMillis,
                        isRunning = false,
                        lastMessage = "Synced revision ${snapshot.revision}.",
                        lastAttemptAt = attemptAt,
                        lastSuccessAt = System.currentTimeMillis(),
                    )
                )
            }
            WidgetUpdater.updateAll(appContext)
        }

        syncRunningFlow.value = false
        result.onFailure { throwable ->
            updateSyncMessage(
                message = throwable.message ?: "Sync failed.",
                isRunning = false,
                lastAttemptAt = attemptAt,
            )
        }
        return result.fold(
            onSuccess = { Result.success(Unit) },
            onFailure = { Result.failure(it) },
        )
    }

    fun currentDashboard(): DashboardSnapshot = dashboard.value

    fun currentPreferencesBlocking(): AppPreferences = runBlocking {
        preferencesDataSource.current()
    }

    suspend fun currentTopNote(): Note? = withContext(Dispatchers.IO) {
        noteDao.getTopVisibleNote()?.toDomain()
    }

    suspend fun currentTopTimer(): TimerEntry? = withContext(Dispatchers.IO) {
        timerDao.getTopVisibleTimer()?.toDomain()
    }

    suspend fun currentVisibleNotes(): List<Note> = withContext(Dispatchers.IO) {
        noteDao.getVisibleNotes().map { it.toDomain() }
    }

    suspend fun currentVisibleTimers(): List<TimerEntry> = withContext(Dispatchers.IO) {
        timerDao.getVisibleTimers().map { it.toDomain() }
    }

    private suspend fun pushPendingChanges(api: NotesApi) {
        val pendingNotes = noteDao.getPendingSyncNotes()
        pendingNotes.forEach { entity ->
            if (entity.pendingSyncAction == PendingSyncAction.DELETE) {
                api.deleteNote(entity.id)
                noteDao.deleteById(entity.id)
            } else {
                api.putNote(entity.id, entity.toDomain())
                noteDao.upsert(
                    entity.copy(
                        pendingSyncAction = PendingSyncAction.NONE,
                        remoteExists = true,
                        isDeleted = false,
                    )
                )
            }
        }

        val pendingTimers = timerDao.getPendingSyncTimers()
        pendingTimers.forEach { entity ->
            if (entity.pendingSyncAction == PendingSyncAction.DELETE) {
                api.deleteTimer(entity.id)
                timerDao.deleteById(entity.id)
            } else {
                api.putTimer(entity.id, entity.toDomain())
                timerDao.upsert(
                    entity.copy(
                        pendingSyncAction = PendingSyncAction.NONE,
                        remoteExists = true,
                        isDeleted = false,
                    )
                )
            }
        }
    }

    private suspend fun scheduleSyncIfConfigured() {
        val preferences = preferencesDataSource.current()
        SyncScheduler.applySettings(appContext, preferences)
        if (preferences.baseUrl.isNotBlank()) {
            SyncScheduler.enqueueImmediate(appContext)
        } else {
            updateSyncMessage("Saved locally. Configure sync when you are ready.", isRunning = false)
        }
    }

    private suspend fun updateSyncMessage(
        message: String,
        isRunning: Boolean,
        lastAttemptAt: Long? = null,
    ) {
        val current = syncMetadataDao.get() ?: SyncMetadataEntity()
        syncMetadataDao.upsert(
            current.copy(
                isRunning = isRunning,
                lastMessage = message,
                lastAttemptAt = lastAttemptAt ?: current.lastAttemptAt,
            )
        )
    }

    companion object {
        fun from(context: Context): NotesRepository = (context.applicationContext as NotesApplication).repository
    }
}

private fun NoteEntity.toDomain(): Note = Note(
    id = id,
    title = title,
    content = content,
    pinned = pinned,
    createdAt = createdAt,
    updatedAt = updatedAt,
)

private fun TimerEntity.toDomain(): TimerEntry = TimerEntry(
    id = id,
    name = name,
    mode = mode,
    durationMillis = durationMillis,
    startedAt = startedAt,
    accumulatedMillis = accumulatedMillis,
    running = running,
    createdAt = createdAt,
    updatedAt = updatedAt,
)

private fun Note.toEntity(
    pendingSyncAction: PendingSyncAction = PendingSyncAction.NONE,
    remoteExists: Boolean = true,
): NoteEntity = NoteEntity(
    id = id,
    title = title.ifBlank { "Untitled note" },
    content = content,
    pinned = pinned,
    createdAt = createdAt,
    updatedAt = updatedAt,
    isDeleted = false,
    pendingSyncAction = pendingSyncAction,
    remoteExists = remoteExists,
)

private fun TimerEntry.toEntity(
    pendingSyncAction: PendingSyncAction = PendingSyncAction.NONE,
    remoteExists: Boolean = true,
): TimerEntity = TimerEntity(
    id = id,
    name = name.ifBlank { "New timer" },
    mode = mode,
    durationMillis = durationMillis,
    startedAt = startedAt,
    accumulatedMillis = accumulatedMillis,
    running = running,
    createdAt = createdAt,
    updatedAt = updatedAt,
    isDeleted = false,
    pendingSyncAction = pendingSyncAction,
    remoteExists = remoteExists,
)
