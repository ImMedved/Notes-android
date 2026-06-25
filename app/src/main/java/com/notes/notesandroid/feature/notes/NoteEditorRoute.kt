package com.notes.notesandroid.feature.notes

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.view.WindowManager
import android.webkit.JavascriptInterface
import android.webkit.WebView
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.notes.notesandroid.data.NotesRepository
import com.notes.notesandroid.data.model.Note
import com.notes.notesandroid.data.model.SyncStatus
import com.notes.notesandroid.data.model.previewText
import com.notes.notesandroid.ui.components.PullToRevealSyncContainer
import com.notes.notesandroid.ui.components.SectionCard
import com.notes.notesandroid.util.MarkdownRenderer
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

@Composable
fun NoteEditorRoute(
    repository: NotesRepository,
    noteId: String?,
    onBack: () -> Unit,
    onEditTitle: (String) -> Unit,
    onEditMarkdown: (String) -> Unit,
) {
    val note = rememberNote(repository, noteId)
    val syncStatus by repository.syncStatus.collectAsStateWithLifecycle(initialValue = SyncStatus())
    val currentNote = note ?: return
    val renderedMarkdown = remember(currentNote.content) {
        MarkdownRenderer.render(currentNote.content.ifBlank { "_Nothing to preview yet._" })
    }
    val scope = rememberCoroutineScope()

    fun toggleTask(lineIndex: Int) {
        val updatedText = MarkdownRenderer.toggleCheckbox(currentNote.content, lineIndex)
        if (updatedText == currentNote.content) return
        scope.launch {
            repository.upsertNote(
                currentNote.copy(
                    content = updatedText,
                    updatedAt = System.currentTimeMillis(),
                )
            )
        }
    }

    PullToRevealSyncContainer(
        syncStatus = syncStatus,
        modifier = Modifier.fillMaxSize(),
        enabled = true,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .navigationBarsPadding()
                .padding(horizontal = 18.dp, vertical = 20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                TextButton(onClick = onBack) { Text("Back") }
                FilterChip(
                    selected = currentNote.pinned,
                    onClick = {
                        scope.launch {
                            repository.upsertNote(
                                currentNote.copy(
                                    pinned = !currentNote.pinned,
                                    updatedAt = System.currentTimeMillis(),
                                )
                            )
                        }
                    },
                    label = { Text("Pinned") },
                )
            }

            SectionCard(
                modifier = Modifier.clickable { onEditTitle(currentNote.id) },
            ) {
                Text(
                    text = if (currentNote.title.isBlank()) "Untitled note" else currentNote.title,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = "Edit title",
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }

            SectionCard(
                modifier = Modifier.clickable { onEditMarkdown(currentNote.id) },
            ) {
                Text(
                    text = currentNote.previewText(),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = "Open markdown editor",
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
            ) {
                val bridge = remember(currentNote.id, currentNote.content) {
                    TaskToggleBridge { lineIndex -> toggleTask(lineIndex) }
                }
                AndroidView(
                    factory = { androidContext ->
                        WebView(androidContext).apply {
                            setBackgroundColor(android.graphics.Color.TRANSPARENT)
                            settings.javaScriptEnabled = true
                            addJavascriptInterface(bridge, "TaskBridge")
                        }
                    },
                    modifier = Modifier.fillMaxSize(),
                    update = { webView ->
                        webView.loadDataWithBaseURL(
                            null,
                            renderedMarkdown.html,
                            "text/html",
                            "utf-8",
                            null,
                        )
                    },
                )
            }
        }
    }
}

@Composable
fun NoteTitleEditorRoute(
    repository: NotesRepository,
    noteId: String?,
    onBack: () -> Unit,
) {
    val note = rememberNote(repository, noteId)
    val currentNote = note ?: return
    var title by remember(currentNote.id, currentNote.title) { mutableStateOf(currentNote.title) }
    val scope = rememberCoroutineScope()

    SoftInputAdjustNothingEffect()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .navigationBarsPadding()
            .padding(horizontal = 18.dp, vertical = 20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        TextButton(onClick = onBack) { Text("Back") }

        OutlinedTextField(
            value = title,
            onValueChange = { title = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Title") },
            singleLine = true,
        )

        Button(
            onClick = {
                scope.launch {
                    repository.upsertNote(
                        currentNote.copy(
                            title = title,
                            updatedAt = System.currentTimeMillis(),
                        )
                    )
                    onBack()
                }
            },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Save title")
        }
    }
}

@Composable
fun NoteMarkdownEditorRoute(
    repository: NotesRepository,
    noteId: String?,
    onBack: () -> Unit,
) {
    val note = rememberNote(repository, noteId)
    val currentNote = note ?: return
    var contentField by remember(currentNote.id, currentNote.content) { mutableStateOf(TextFieldValue(currentNote.content)) }
    val scope = rememberCoroutineScope()

    SoftInputAdjustNothingEffect()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .navigationBarsPadding()
            .padding(horizontal = 18.dp, vertical = 20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        TextButton(onClick = onBack) { Text("Back") }

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
        ) {
            OutlinedTextField(
                value = contentField,
                onValueChange = { contentField = it },
                modifier = Modifier.fillMaxSize(),
                label = { Text("Markdown") },
                placeholder = { Text("Write markdown, tasks, links, and code here.") },
                textStyle = MaterialTheme.typography.bodyLarge,
            )
        }

        MarkdownActionBar(
            value = contentField,
            onValueChange = { contentField = it },
            onSave = {
                scope.launch {
                    repository.upsertNote(
                        currentNote.copy(
                            content = contentField.text,
                            updatedAt = System.currentTimeMillis(),
                        )
                    )
                    onBack()
                }
            },
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun rememberNote(
    repository: NotesRepository,
    noteId: String?,
): Note? {
    val existingNoteFlow = remember(noteId) { noteId?.let(repository::observeNote) }
    var note by remember(noteId) { mutableStateOf<Note?>(null) }

    LaunchedEffect(existingNoteFlow) {
        note = existingNoteFlow?.first()
    }

    return note
}

@Composable
private fun MarkdownActionBar(
    value: TextFieldValue,
    onValueChange: (TextFieldValue) -> Unit,
    onSave: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.extraLarge,
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f),
        tonalElevation = 4.dp,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 10.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            if (onSave != null) {
                Button(onClick = onSave) {
                    Text("Save")
                }
            }
            MarkdownActionChip("Check") {
                onValueChange(insertLinePrefix(value, "- [ ] "))
            }
            MarkdownActionChip("H2") {
                onValueChange(insertLinePrefix(value, "## "))
            }
            MarkdownActionChip("Bold") {
                onValueChange(insertWrapped(value, "**", "**", "bold"))
            }
            MarkdownActionChip("Code") {
                onValueChange(insertWrapped(value, "`", "`", "code"))
            }
            MarkdownActionChip("Block") {
                onValueChange(insertWrapped(value, "\n```kotlin\n", "\n```\n", "println(\"Hello\")"))
            }
            MarkdownActionChip("Link") {
                onValueChange(insertWrapped(value, "[", "](https://example.com)", "label"))
            }
            MarkdownActionChip("Quote") {
                onValueChange(insertLinePrefix(value, "> "))
            }
        }
    }
}

@Composable
private fun MarkdownActionChip(
    label: String,
    onClick: () -> Unit,
) {
    SuggestionChip(
        onClick = onClick,
        label = { Text(label) },
    )
}

private fun insertWrapped(
    value: TextFieldValue,
    prefix: String,
    suffix: String,
    placeholder: String,
): TextFieldValue {
    val start = value.selection.min
    val end = value.selection.max
    val selectedText = value.text.substring(start, end)
    val replacementCore = if (selectedText.isNotEmpty()) selectedText else placeholder
    val replacement = prefix + replacementCore + suffix
    val updatedText = value.text.replaceRange(start, end, replacement)
    val selectionStart = start + prefix.length
    val selectionEnd = selectionStart + replacementCore.length
    return value.copy(
        text = updatedText,
        selection = TextRange(selectionStart, selectionEnd),
    )
}

private fun insertLinePrefix(
    value: TextFieldValue,
    prefix: String,
): TextFieldValue {
    val cursor = value.selection.min
    val lineStart = value.text.lastIndexOf('\n', startIndex = (cursor - 1).coerceAtLeast(0))
        .let { if (it == -1) 0 else it + 1 }
    val updatedText = value.text.replaceRange(lineStart, lineStart, prefix)
    val newCursor = cursor + prefix.length
    return value.copy(
        text = updatedText,
        selection = TextRange(newCursor),
    )
}

private class TaskToggleBridge(
    private val onToggle: (Int) -> Unit,
) {
    @JavascriptInterface
    fun onTaskToggled(lineIndex: Int) {
        onToggle(lineIndex)
    }
}

@Composable
private fun SoftInputAdjustNothingEffect() {
    val context = LocalContext.current
    DisposableEffect(context) {
        val activity = context.findActivity()
        val window = activity?.window
        val previousMode = window?.attributes?.softInputMode
        window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_NOTHING)
        onDispose {
            if (previousMode != null) {
                window.setSoftInputMode(previousMode)
            }
        }
    }
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
