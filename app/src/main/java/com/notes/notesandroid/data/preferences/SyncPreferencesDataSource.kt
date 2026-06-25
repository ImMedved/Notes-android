package com.notes.notesandroid.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.notes.notesandroid.data.model.AppPreferences
import com.notes.notesandroid.data.model.AppThemeMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.io.IOException
import java.util.UUID

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "paper_notes_prefs")

class SyncPreferencesDataSource(
    private val context: Context,
) {
    val preferencesFlow: Flow<AppPreferences> = context.dataStore.data
        .catch { exception ->
            if (exception is IOException) emit(emptyPreferences()) else throw exception
        }
        .map { preferences ->
            val clientId = preferences[CLIENT_ID] ?: UUID.randomUUID().toString()
            AppPreferences(
                baseUrl = preferences[BASE_URL].orEmpty(),
                apiKey = preferences[API_KEY].orEmpty(),
                clientId = clientId,
                autoSync = preferences[AUTO_SYNC] ?: true,
                themeMode = preferences[THEME_MODE]?.let(AppThemeMode::valueOf) ?: AppThemeMode.SYSTEM,
            )
        }

    suspend fun current(): AppPreferences = preferencesFlow.first()

    suspend fun updateSyncSettings(
        baseUrl: String,
        apiKey: String,
        autoSync: Boolean,
    ) {
        context.dataStore.edit { prefs ->
            val clientId = prefs[CLIENT_ID] ?: UUID.randomUUID().toString()
            prefs[CLIENT_ID] = clientId
            prefs[BASE_URL] = baseUrl.trim()
            prefs[API_KEY] = apiKey.trim()
            prefs[AUTO_SYNC] = autoSync
        }
    }

    suspend fun updateThemeMode(themeMode: AppThemeMode) {
        context.dataStore.edit { prefs ->
            val clientId = prefs[CLIENT_ID] ?: UUID.randomUUID().toString()
            prefs[CLIENT_ID] = clientId
            prefs[THEME_MODE] = themeMode.name
        }
    }

    companion object {
        private val BASE_URL = stringPreferencesKey("base_url")
        private val API_KEY = stringPreferencesKey("api_key")
        private val CLIENT_ID = stringPreferencesKey("client_id")
        private val AUTO_SYNC = booleanPreferencesKey("auto_sync")
        private val THEME_MODE = stringPreferencesKey("theme_mode")
    }
}
