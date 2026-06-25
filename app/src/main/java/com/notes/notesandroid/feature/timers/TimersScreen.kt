package com.notes.notesandroid.feature.timers

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.notes.notesandroid.data.NotesRepository
import com.notes.notesandroid.data.model.TimerEntry
import com.notes.notesandroid.ui.components.EmptyStateCard
import com.notes.notesandroid.ui.components.PullToRevealSyncContainer
import com.notes.notesandroid.ui.components.SectionCard
import com.notes.notesandroid.util.displayDuration
import com.notes.notesandroid.util.displayModeLabel
import kotlinx.coroutines.delay

@Composable
fun TimersRoute(
    repository: NotesRepository,
    onCreate: () -> Unit,
    onOpen: (String) -> Unit,
) {
    val viewModel: TimersViewModel = viewModel(factory = TimersViewModel.factory(repository))
    val state = viewModel.uiState.collectAsStateWithLifecycle()
    val now by produceState(initialValue = System.currentTimeMillis()) {
        while (true) {
            delay(1_000L)
            value = System.currentTimeMillis()
        }
    }

    PullToRevealSyncContainer(
        syncStatus = state.value.syncStatus,
        modifier = Modifier.fillMaxSize(),
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 18.dp, vertical = 22.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            item {
                Button(
                    onClick = onCreate,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("New timer")
                }
            }

            item {
                AnimatedVisibility(visible = state.value.timers.isEmpty()) {
                    EmptyStateCard(
                        title = "No timers yet",
                        body = "Build a pomodoro countdown or a long-running stopwatch. Room keeps the latest state instantly available.",
                    )
                }
            }

            items(state.value.timers, key = { it.id }) { timer ->
                TimerCard(
                    timer = timer,
                    now = now,
                    onOpen = { onOpen(timer.id) },
                    onToggle = { viewModel.toggle(timer.id) },
                    onReset = { viewModel.reset(timer.id) },
                    onDelete = { viewModel.delete(timer.id) },
                )
            }
        }
    }
}

@Composable
private fun TimerCard(
    timer: TimerEntry,
    now: Long,
    onOpen: () -> Unit,
    onToggle: () -> Unit,
    onReset: () -> Unit,
    onDelete: () -> Unit,
) {
    SectionCard {
        Text(timer.name, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Text(timer.displayModeLabel(now), color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(
            text = timer.displayDuration(now),
            style = MaterialTheme.typography.displaySmall,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(top = 12.dp),
        )
        Row(
            modifier = Modifier.padding(top = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Button(onClick = onToggle) { Text(if (timer.running) "Pause" else "Start") }
            OutlinedButton(onClick = onReset) { Text("Reset") }
        }
        Row(
            modifier = Modifier.padding(top = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            OutlinedButton(onClick = onOpen) { Text("Edit") }
            OutlinedButton(onClick = onDelete) { Text("Delete") }
        }
    }
}
