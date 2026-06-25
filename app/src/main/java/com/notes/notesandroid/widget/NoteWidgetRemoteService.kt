package com.notes.notesandroid.widget

import android.content.Intent
import android.widget.RemoteViews
import android.widget.RemoteViewsService
import com.notes.notesandroid.R
import com.notes.notesandroid.data.NotesRepository
import com.notes.notesandroid.data.model.Note
import com.notes.notesandroid.data.model.previewText
import com.notes.notesandroid.util.formatTimestamp
import kotlinx.coroutines.runBlocking

class NoteWidgetRemoteService : RemoteViewsService() {
    override fun onGetViewFactory(intent: Intent): RemoteViewsFactory {
        return NoteWidgetRemoteViewsFactory(applicationContext)
    }
}

private class NoteWidgetRemoteViewsFactory(
    private val context: android.content.Context,
) : RemoteViewsService.RemoteViewsFactory {
    private var notes: List<Note> = emptyList()

    override fun onCreate() = Unit

    override fun onDataSetChanged() {
        notes = runBlocking { NotesRepository.from(context).currentVisibleNotes() }
    }

    override fun onDestroy() {
        notes = emptyList()
    }

    override fun getCount(): Int = notes.size

    override fun getViewAt(position: Int): RemoteViews? {
        val note = notes.getOrNull(position) ?: return null
        return RemoteViews(context.packageName, R.layout.widget_note_item).apply {
            setTextViewText(R.id.noteWidgetItemTitle, note.title)
            setTextViewText(R.id.noteWidgetItemBody, note.previewText())
            setTextViewText(
                R.id.noteWidgetItemMeta,
                buildString {
                    if (note.pinned) append("Pinned | ")
                    append("Updated ${formatTimestamp(note.updatedAt)}")
                }
            )

            val fillInIntent = Intent().apply {
                putExtra(WidgetActionReceiver.EXTRA_TAB, WidgetActionReceiver.TAB_NOTES)
                putExtra(WidgetActionReceiver.EXTRA_NOTE_ID, note.id)
            }
            setOnClickFillInIntent(R.id.noteWidgetItemContainer, fillInIntent)
        }
    }

    override fun getLoadingView(): RemoteViews? = null

    override fun getViewTypeCount(): Int = 1

    override fun getItemId(position: Int): Long = notes.getOrNull(position)?.id?.hashCode()?.toLong() ?: position.toLong()

    override fun hasStableIds(): Boolean = true
}
