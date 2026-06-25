package com.notes.notesandroid.feature.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil3.compose.AsyncImage
import com.notes.notesandroid.R
import com.notes.notesandroid.data.NotesRepository
import com.notes.notesandroid.data.model.AppThemeMode
import com.notes.notesandroid.ui.components.HeroCard
import com.notes.notesandroid.ui.components.PullToRevealSyncContainer
import com.notes.notesandroid.ui.components.SectionCard

@Composable
fun SettingsRoute(
    repository: NotesRepository,
) {
    val viewModel: SettingsViewModel = viewModel(factory = SettingsViewModel.factory(repository))
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var baseUrl by remember(state.preferences.baseUrl) { mutableStateOf(state.preferences.baseUrl) }
    var apiKey by remember(state.preferences.apiKey) { mutableStateOf(state.preferences.apiKey) }
    var autoSync by remember(state.preferences.autoSync) { mutableStateOf(state.preferences.autoSync) }

    PullToRevealSyncContainer(
        syncStatus = state.syncStatus,
        modifier = Modifier.fillMaxSize(),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 18.dp, vertical = 22.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            HeroCard(
                title = "Offline-first sync",
                subtitle = "Room is the local source of truth, WorkManager pushes changes later, and Retrofit refreshes the server snapshot.",
                accent = "",
                action = { Button(onClick = viewModel::syncNow) { Text("Sync now") } },
                artwork = {
                    AsyncImage(
                        model = R.drawable.art_sync,
                        contentDescription = null,
                        modifier = Modifier.fillMaxWidth(),
                    )
                },
            )

            SectionCard {
                Text("Server", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                OutlinedTextField(
                    value = baseUrl,
                    onValueChange = { baseUrl = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp),
                    label = { Text("Base URL") },
                    supportingText = { Text("Example: http://127.0.0.1:8080 or your Tailscale host") },
                    singleLine = true,
                )
                OutlinedTextField(
                    value = apiKey,
                    onValueChange = { apiKey = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp),
                    label = { Text("API key") },
                    supportingText = { Text("Sent as X-Notes-Api-Key when the backend expects it") },
                    singleLine = true,
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Auto sync", fontWeight = FontWeight.SemiBold)
                        Text(
                            "Schedules periodic background sync every 15 minutes while a server URL is configured.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Switch(checked = autoSync, onCheckedChange = { autoSync = it })
                }
                Button(
                    onClick = { viewModel.saveSettings(baseUrl, apiKey, autoSync) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 14.dp),
                ) {
                    Text("Save sync settings")
                }
            }

            SectionCard {
                Text("Theme", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text(
                    text = "Light mode fades from white through warm beige into soft violet. Dark mode drops into black, wine red and gold accents.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 10.dp),
                )
                Row(
                    modifier = Modifier.padding(top = 14.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    AppThemeMode.entries.forEach { mode ->
                        FilterChip(
                            selected = state.preferences.themeMode == mode,
                            onClick = { viewModel.setThemeMode(mode) },
                            label = { Text(mode.name.lowercase().replaceFirstChar(Char::uppercase)) },
                        )
                    }
                }
            }
        }
    }
}
