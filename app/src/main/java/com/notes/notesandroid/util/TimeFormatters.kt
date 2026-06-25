package com.notes.notesandroid.util

import com.notes.notesandroid.data.model.TimerEntry
import com.notes.notesandroid.data.model.TimerMode
import com.notes.notesandroid.data.model.elapsedAt
import com.notes.notesandroid.data.model.remainingAt
import java.text.SimpleDateFormat
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Date
import java.util.Locale

private val dateFormatter = SimpleDateFormat("dd MMM, HH:mm", Locale.getDefault())
private val dateTimeFormatter = DateTimeFormatter.ofPattern("dd MMM yyyy, HH:mm:ss", Locale.getDefault())
private val dayFormatter = DateTimeFormatter.ofPattern("EEE dd MMM", Locale.getDefault())

fun formatTimestamp(epochMillis: Long?): String {
    if (epochMillis == null || epochMillis <= 0L) return "Never"
    return dateFormatter.format(Date(epochMillis))
}

fun formatDurationCompact(durationMillis: Long): String {
    val totalSeconds = (durationMillis / 1000L).coerceAtLeast(0L)
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    return if (hours > 0) {
        String.format(Locale.getDefault(), "%02d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds)
    }
}

fun TimerEntry.displayDuration(now: Long): String {
    val value = if (mode == TimerMode.COUNTDOWN) remainingAt(now) else elapsedAt(now)
    return formatDurationCompact(value)
}

fun TimerEntry.displayModeLabel(now: Long): String {
    return when (mode) {
        TimerMode.COUNTDOWN -> if (remainingAt(now) == 0L) "Finished" else "Countdown"
        TimerMode.STOPWATCH -> "Stopwatch"
    }
}

fun epochMillisToLocalDateTime(epochMillis: Long): LocalDateTime {
    return Instant.ofEpochMilli(epochMillis)
        .atZone(ZoneId.systemDefault())
        .toLocalDateTime()
}

fun localDateTimeToEpochMillis(value: LocalDateTime): Long {
    return value.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
}

fun formatDateTimeDetailed(value: LocalDateTime): String = value.format(dateTimeFormatter)

fun formatWheelDayLabel(date: LocalDate, today: LocalDate = LocalDate.now()): String {
    return when (date) {
        today -> "Today"
        today.minusDays(1) -> "Yesterday"
        today.plusDays(1) -> "Tomorrow"
        else -> date.format(dayFormatter)
    }
}

fun durationParts(durationMillis: Long): Triple<Long, Long, Long> {
    val totalSeconds = (durationMillis / 1000L).coerceAtLeast(0L)
    val hours = totalSeconds / 3600L
    val minutes = (totalSeconds % 3600L) / 60L
    val seconds = totalSeconds % 60L
    return Triple(hours, minutes, seconds)
}
