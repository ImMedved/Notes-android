package com.notes.notesandroid.feature.timers

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.notes.notesandroid.data.NotesRepository
import com.notes.notesandroid.data.model.SyncStatus
import com.notes.notesandroid.data.model.TimerEntry
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class TimersUiState(
    val timers: List<TimerEntry> = emptyList(),
    val syncStatus: SyncStatus = SyncStatus(),
)

class TimersViewModel(
    private val repository: NotesRepository,
) : ViewModel() {
    val uiState: StateFlow<TimersUiState> = combine(
        repository.timers,
        repository.syncStatus,
    ) { timers, syncStatus ->
        TimersUiState(timers = timers, syncStatus = syncStatus)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = TimersUiState(),
    )

    fun toggle(timerId: String) {
        viewModelScope.launch { repository.toggleTimer(timerId) }
    }

    fun reset(timerId: String) {
        viewModelScope.launch { repository.resetTimer(timerId) }
    }

    fun delete(timerId: String) {
        viewModelScope.launch { repository.deleteTimer(timerId) }
    }

    companion object {
        fun factory(repository: NotesRepository): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                require(modelClass.isAssignableFrom(TimersViewModel::class.java))
                @Suppress("UNCHECKED_CAST")
                return TimersViewModel(repository) as T
            }
        }
    }
}
