package com.notes.notesandroid.widget

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.notes.notesandroid.data.NotesRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class WidgetActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val repository = NotesRepository.from(context)
                when (intent.action) {
                    ACTION_REFRESH -> repository.syncNow()
                    ACTION_TIMER_TOGGLE -> {
                        intent.getStringExtra(EXTRA_TIMER_ID)?.let { repository.toggleTimer(it) }
                    }
                    ACTION_TIMER_RESET -> {
                        intent.getStringExtra(EXTRA_TIMER_ID)?.let { repository.resetTimer(it) }
                    }
                }
                WidgetUpdater.updateAll(context)
            } finally {
                pendingResult.finish()
            }
        }
    }

    companion object {
        const val ACTION_REFRESH = "com.notes.notesandroid.widget.REFRESH"
        const val ACTION_TIMER_TOGGLE = "com.notes.notesandroid.widget.TIMER_TOGGLE"
        const val ACTION_TIMER_RESET = "com.notes.notesandroid.widget.TIMER_RESET"
        const val EXTRA_NOTE_ID = "note_id"
        const val EXTRA_TIMER_ID = "timer_id"
        const val EXTRA_TAB = "tab"
        const val TAB_NOTES = "notes"
        const val TAB_TIMERS = "timers"
        const val TAB_SETTINGS = "settings"
    }
}
