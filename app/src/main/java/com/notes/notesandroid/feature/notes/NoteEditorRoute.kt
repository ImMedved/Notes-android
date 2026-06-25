package com.notes.notesandroid.feature.notes

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.view.WindowManager
import android.webkit.JavascriptInterface
import android.webkit.WebView
import androidx.compose.animation.AnimatedContent
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
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.Alignment
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.notes.notesandroid.data.NotesRepository
import com.notes.notesandroid.data.model.Note
import com.notes.notesandroid.data.model.SyncStatus
import com.notes.notesandroid.ui.components.PullToRevealSyncContainer
import com.notes.notesandroid.util.MarkdownRenderer
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.UUID

@Composable
fun NoteEditorRoute(
    repository: NotesRepository,
    noteId: String?,
    onBack: () -> Unit,
) {
    val existingNoteFlow = remember(noteId) { noteId?.let(repository::observeNote) }
    val syncStatus by repository.syncStatus.collectAsStateWithLifecycle(initialValue = SyncStatus())
    var title by remember(noteId) { mutableStateOf("") }
    var contentField by remember(noteId) { mutableStateOf(TextFieldValue("")) }
    var pinned by remember(noteId) { mutableStateOf(false) }
    var createdAt by remember(noteId) { mutableStateOf(System.currentTimeMillis()) }
    var previewMode by remember(noteId) { mutableStateOf(noteId != null) }
    var titleFocused by remember(noteId) { mutableStateOf(false) }
    var markdownFocused by remember(noteId) { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val renderedMarkdown = remember(contentField.text) {
        MarkdownRenderer.render(contentField.text.ifBlank { "_Nothing to preview yet._" })
    }

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

    LaunchedEffect(existingNoteFlow) {
        val note = existingNoteFlow?.first()
        if (note != null) {
            title = note.title
            contentField = TextFieldValue(note.content)
            pinned = note.pinned
            createdAt = note.createdAt
        }
    }

    fun persistNote(updatedContent: String = contentField.text, exitAfterSave: Boolean = false) {
        scope.launch {
            repository.upsertNote(
                Note(
                    id = noteId ?: UUID.randomUUID().toString(),
                    title = title,
                    content = updatedContent,
                    pinned = pinned,
                    createdAt = createdAt,
                    updatedAt = System.currentTimeMillis(),
                )
            )
            if (exitAfterSave) {
                onBack()
            }
        }
    }

    fun toggleTask(lineIndex: Int) {
        val updatedText = MarkdownRenderer.toggleCheckbox(contentField.text, lineIndex)
        if (updatedText == contentField.text) return

        val newCursor = contentField.selection.end.coerceAtMost(updatedText.length)
        contentField = contentField.copy(
            text = updatedText,
            selection = TextRange(newCursor),
        )

        if (noteId != null) {
            persistNote(updatedContent = updatedText)
        }
    }

    PullToRevealSyncContainer(
        syncStatus = syncStatus,
        modifier = Modifier.fillMaxSize(),
        enabled = previewMode,
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .navigationBarsPadding()
                .padding(horizontal = 18.dp, vertical = 20.dp),
        ) {
            if (!titleFocused) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                ) {
                    NoteEditorField(
                        previewMode = previewMode,
                        noteId = noteId,
                        title = title,
                        pinned = pinned,
                        createdAt = createdAt,
                        contentField = contentField,
                        renderedHtml = renderedMarkdown.html,
                        onContentChange = { contentField = it },
                        onTaskToggle = ::toggleTask,
                        onEditorFocusChange = {
                            markdownFocused = it
                            if (it) {
                                titleFocused = false
                            }
                        },
                    )
                }
            }

            if (!previewMode) {
                MarkdownActionBar(
                    value = contentField,
                    onValueChange = { contentField = it },
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth(),
                )
            }

            if (!previewMode && !markdownFocused) {
                Button(
                    onClick = { persistNote(exitAfterSave = true) },
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .padding(bottom = 90.dp),
                ) {
                    Text("Save note")
                }
            }

            if (!markdownFocused) {
                Column(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        TextButton(onClick = onBack) { Text("Back") }

                        FilterChip(
                            selected = !previewMode,
                            onClick = {
                                markdownFocused = false
                                titleFocused = false
                                previewMode = false
                            },
                            label = { Text("Edit") },
                        )

                        FilterChip(
                            selected = previewMode,
                            onClick = {
                                markdownFocused = false
                                titleFocused = false
                                previewMode = true
                            },
                            label = { Text("Preview") },
                        )

                        FilterChip(
                            selected = pinned,
                            onClick = { pinned = !pinned },
                            label = { Text("Pinned") },
                        )
                    }

                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .onFocusChanged {
                                titleFocused = it.isFocused
                                if (it.isFocused) {
                                    markdownFocused = false
                                }
                            },
                        label = { Text("Title") },
                        singleLine = true,
                    )
                }
            }
        }
    }
}

@Composable
private fun NoteEditorField(
    previewMode: Boolean,
    noteId: String?,
    title: String,
    pinned: Boolean,
    createdAt: Long,
    contentField: TextFieldValue,
    renderedHtml: String,
    onContentChange: (TextFieldValue) -> Unit,
    onTaskToggle: (Int) -> Unit,
    onEditorFocusChange: (Boolean) -> Unit,
) {
    AnimatedContent(
        targetState = previewMode,
        label = "note-preview-mode",
        modifier = Modifier.fillMaxSize(),
    ) { isPreview ->
        if (isPreview) {
            val bridge = remember(noteId, title, pinned, createdAt, contentField.text) {
                TaskToggleBridge { lineIndex -> onTaskToggle(lineIndex) }
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
                        renderedHtml,
                        "text/html",
                        "utf-8",
                        null,
                    )
                },
            )
        } else {
            OutlinedTextField(
                value = contentField,
                onValueChange = onContentChange,
                modifier = Modifier
                    .fillMaxSize()
                    .onFocusChanged { onEditorFocusChange(it.isFocused) },
                label = { Text("Markdown") },
                placeholder = { Text("Write markdown, tasks, links, and code here.") },
                textStyle = MaterialTheme.typography.bodyLarge,
            )
        }
    }
}

@Composable
private fun MarkdownActionBar(
    value: TextFieldValue,
    onValueChange: (TextFieldValue) -> Unit,
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

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
