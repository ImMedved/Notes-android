package com.notes.notesandroid

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.notes.notesandroid.navigation.PaperNotesApp
import com.notes.notesandroid.navigation.TopLevelDestination
import com.notes.notesandroid.widget.WidgetActionReceiver

class MainActivity : ComponentActivity() {
    private var launchSpec by mutableStateOf(IntentLaunchSpec())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        //enableEdgeToEdge()
        launchSpec = intent.toLaunchSpec()
        setContent {
            PaperNotesApp(
                repository = (application as NotesApplication).repository,
                startDestination = launchSpec.startDestination,
                requestedNoteId = launchSpec.noteId,
                requestedTimerId = launchSpec.timerId,
                launchRequestKey = launchSpec.requestKey,
            )
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        launchSpec = intent.toLaunchSpec()
    }
}

private data class IntentLaunchSpec(
    val startDestination: TopLevelDestination = TopLevelDestination.NOTES,
    val noteId: String? = null,
    val timerId: String? = null,
    val requestKey: Long = System.nanoTime(),
)

private fun Intent?.toLaunchSpec(): IntentLaunchSpec {
    val startDestination = when (this?.getStringExtra(WidgetActionReceiver.EXTRA_TAB)) {
        WidgetActionReceiver.TAB_TIMERS -> TopLevelDestination.TIMERS
        WidgetActionReceiver.TAB_SETTINGS -> TopLevelDestination.SETTINGS
        else -> TopLevelDestination.NOTES
    }
    return IntentLaunchSpec(
        startDestination = startDestination,
        noteId = this?.getStringExtra(WidgetActionReceiver.EXTRA_NOTE_ID),
        timerId = this?.getStringExtra(WidgetActionReceiver.EXTRA_TIMER_ID),
    )
}
