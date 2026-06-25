package com.notes.notesandroid.feature.notes

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.notes.notesandroid.data.NotesRepository
import com.notes.notesandroid.data.model.Note
import com.notes.notesandroid.data.model.previewText
import com.notes.notesandroid.ui.components.EmptyStateCard
import com.notes.notesandroid.ui.components.PullToRevealSyncContainer
import com.notes.notesandroid.ui.components.SectionCard
import com.notes.notesandroid.util.formatTimestamp

@Composable
fun NotesRoute(
    repository: NotesRepository,
    onCreate: () -> Unit,
    onOpen: (String) -> Unit,
) {
    val viewModel: NotesViewModel = viewModel(factory = NotesViewModel.factory(repository))
    val state = viewModel.uiState.collectAsStateWithLifecycle()

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
                    Text("New note")
                }
            }

            item {
                androidx.compose.animation.AnimatedVisibility(visible = state.value.notes.isEmpty()) {
                    EmptyStateCard(
                        title = "No notes yet",
                        body = "Create a markdown note with headings, lists or code blocks. It saves locally first and syncs when the server is reachable.",
                    )
                }
            }

            items(state.value.notes, key = { it.id }) { note ->
                NoteCard(
                    note = note,
                    onOpen = { onOpen(note.id) },
                    onDelete = { viewModel.deleteNote(note.id) },
                )
            }
        }
    }
}

@Composable
private fun NoteCard(
    note: Note,
    onOpen: () -> Unit,
    onDelete: () -> Unit,
) {
    SectionCard {
        Text(note.title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Text(
            text = note.previewText(),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 8.dp),
        )
        Text(
            text = buildString {
                if (note.pinned) append("Pinned | ")
                append("Updated ${formatTimestamp(note.updatedAt)}")
            },
            color = MaterialTheme.colorScheme.primary,
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.padding(top = 12.dp),
        )
        androidx.compose.foundation.layout.Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.padding(top = 14.dp),
        ) {
            Button(onClick = onOpen) { Text("Open") }
            OutlinedButton(onClick = onDelete) { Text("Delete") }
        }
    }
}
