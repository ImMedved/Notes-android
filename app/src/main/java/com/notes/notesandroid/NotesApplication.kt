package com.notes.notesandroid

import android.app.Application
import com.notes.notesandroid.data.NotesRepository
import com.notes.notesandroid.data.SyncScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

class NotesApplication : Application() {
    val repository: NotesRepository by lazy { NotesRepository(this) }

    override fun onCreate() {
        super.onCreate()
        appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
        SyncScheduler.applySettings(this, repository.currentPreferencesBlocking())
    }

    companion object {
        lateinit var appScope: CoroutineScope
            private set
    }
}
