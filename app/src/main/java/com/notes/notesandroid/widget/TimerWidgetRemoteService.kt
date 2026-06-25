package com.notes.notesandroid.widget

import android.content.Intent
import android.widget.RemoteViews
import android.widget.RemoteViewsService
import com.notes.notesandroid.R
import com.notes.notesandroid.data.NotesRepository
import com.notes.notesandroid.data.model.TimerEntry
import com.notes.notesandroid.util.displayDuration
import com.notes.notesandroid.util.displayModeLabel
import kotlinx.coroutines.runBlocking

class TimerWidgetRemoteService : RemoteViewsService() {
    override fun onGetViewFactory(intent: Intent): RemoteViewsFactory {
        return TimerWidgetRemoteViewsFactory(applicationContext)
    }
}

private class TimerWidgetRemoteViewsFactory(
    private val context: android.content.Context,
) : RemoteViewsService.RemoteViewsFactory {
    private var timers: List<TimerEntry> = emptyList()

    override fun onCreate() = Unit

    override fun onDataSetChanged() {
        timers = runBlocking { NotesRepository.from(context).currentVisibleTimers() }
    }

    override fun onDestroy() {
        timers = emptyList()
    }

    override fun getCount(): Int = timers.size

    override fun getViewAt(position: Int): RemoteViews? {
        val timer = timers.getOrNull(position) ?: return null
        val now = System.currentTimeMillis()
        return RemoteViews(context.packageName, R.layout.widget_timer_item).apply {
            setTextViewText(R.id.timerWidgetItemName, timer.name)
            setTextViewText(R.id.timerWidgetItemMode, timer.displayModeLabel(now))
            setTextViewText(R.id.timerWidgetItemValue, timer.displayDuration(now))

            val fillInIntent = Intent().apply {
                putExtra(WidgetActionReceiver.EXTRA_TAB, WidgetActionReceiver.TAB_TIMERS)
                putExtra(WidgetActionReceiver.EXTRA_TIMER_ID, timer.id)
            }
            setOnClickFillInIntent(R.id.timerWidgetItemContainer, fillInIntent)
        }
    }

    override fun getLoadingView(): RemoteViews? = null

    override fun getViewTypeCount(): Int = 1

    override fun getItemId(position: Int): Long = timers.getOrNull(position)?.id?.hashCode()?.toLong() ?: position.toLong()

    override fun hasStableIds(): Boolean = true
}
