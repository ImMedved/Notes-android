package com.notes.notesandroid.data

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.notes.notesandroid.data.model.AppPreferences
import com.notes.notesandroid.sync.SyncWorker
import java.util.concurrent.TimeUnit

object SyncScheduler {
    private const val PERIODIC_SYNC = "paper-notes-periodic-sync"
    private const val IMMEDIATE_SYNC = "paper-notes-immediate-sync"

    fun applySettings(context: Context, preferences: AppPreferences) {
        val manager = WorkManager.getInstance(context)
        if (!preferences.autoSync || preferences.baseUrl.isBlank()) {
            manager.cancelUniqueWork(PERIODIC_SYNC)
            return
        }

        val request = PeriodicWorkRequestBuilder<SyncWorker>(15, TimeUnit.MINUTES)
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()
            )
            .build()

        manager.enqueueUniquePeriodicWork(
            PERIODIC_SYNC,
            ExistingPeriodicWorkPolicy.UPDATE,
            request,
        )
    }

    fun enqueueImmediate(context: Context) {
        val request = OneTimeWorkRequestBuilder<SyncWorker>()
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()
            )
            .build()

        WorkManager.getInstance(context).enqueueUniqueWork(
            IMMEDIATE_SYNC,
            ExistingWorkPolicy.APPEND_OR_REPLACE,
            request,
        )
    }
}
