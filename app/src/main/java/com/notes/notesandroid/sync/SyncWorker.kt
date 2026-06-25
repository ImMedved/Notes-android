package com.notes.notesandroid.sync

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.notes.notesandroid.data.NotesRepository

class SyncWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params) {
    override suspend fun doWork(): Result {
        return if (NotesRepository.from(applicationContext).syncNow().isSuccess) {
            Result.success()
        } else {
            Result.retry()
        }
    }
}
