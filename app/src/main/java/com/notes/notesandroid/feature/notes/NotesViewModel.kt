package com.notes.notesandroid.feature.notes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.notes.notesandroid.data.NotesRepository
import com.notes.notesandroid.data.model.Note
import com.notes.notesandroid.data.model.SyncStatus
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class NotesUiState(
    val notes: List<Note> = emptyList(),
    val syncStatus: SyncStatus = SyncStatus(),
)

/**
 * Keeps the notes list screen subscribed to both local note data and sync state.
 * The screen itself stays simple: repository flows produce the list, and delete
 * actions are pushed back into the same repository used by the note detail flow.
 */
class NotesViewModel(
    private val repository: NotesRepository,
) : ViewModel() {
    val uiState: StateFlow<NotesUiState> = combine(
        repository.notes,
        repository.syncStatus,
    ) { notes, syncStatus ->
        NotesUiState(notes = notes, syncStatus = syncStatus)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = NotesUiState(),
    )

    fun deleteNote(noteId: String) {
        viewModelScope.launch {
            repository.deleteNote(noteId)
        }
    }

    companion object {
        /**
         * Notes screens resolve the shared repository through this factory so list,
         * preview, and editor routes all work against the same local-first state.
         */
        fun factory(repository: NotesRepository): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                require(modelClass.isAssignableFrom(NotesViewModel::class.java))
                @Suppress("UNCHECKED_CAST")
                return NotesViewModel(repository) as T
            }
        }
    }
}
