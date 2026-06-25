package com.notes.notesandroid.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.notes.notesandroid.data.NotesRepository
import com.notes.notesandroid.data.model.AppPreferences
import com.notes.notesandroid.data.model.AppThemeMode
import com.notes.notesandroid.data.model.SyncStatus
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class SettingsUiState(
    val preferences: AppPreferences = AppPreferences(),
    val syncStatus: SyncStatus = SyncStatus(),
)

class SettingsViewModel(
    private val repository: NotesRepository,
) : ViewModel() {
    val uiState: StateFlow<SettingsUiState> = combine(
        repository.preferences,
        repository.syncStatus,
    ) { preferences, syncStatus ->
        SettingsUiState(preferences = preferences, syncStatus = syncStatus)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = SettingsUiState(),
    )

    fun saveSettings(baseUrl: String, apiKey: String, autoSync: Boolean) {
        viewModelScope.launch { repository.saveSyncSettings(baseUrl, apiKey, autoSync) }
    }

    fun setThemeMode(mode: AppThemeMode) {
        viewModelScope.launch { repository.saveThemeMode(mode) }
    }

    fun syncNow() {
        viewModelScope.launch { repository.syncNow() }
    }

    companion object {
        fun factory(repository: NotesRepository): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                require(modelClass.isAssignableFrom(SettingsViewModel::class.java))
                @Suppress("UNCHECKED_CAST")
                return SettingsViewModel(repository) as T
            }
        }
    }
}
