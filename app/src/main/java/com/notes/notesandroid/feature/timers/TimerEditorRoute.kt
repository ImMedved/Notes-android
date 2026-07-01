package com.notes.notesandroid.feature.timers

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.notes.notesandroid.data.NotesRepository
import com.notes.notesandroid.data.model.TimerEntry
import com.notes.notesandroid.data.model.TimerMode
import com.notes.notesandroid.data.model.elapsedAt
import com.notes.notesandroid.util.durationParts
import com.notes.notesandroid.util.epochMillisToLocalDateTime
import com.notes.notesandroid.util.formatDateTimeDetailed
import com.notes.notesandroid.util.formatDurationCompact
import com.notes.notesandroid.util.formatWheelDayLabel
import com.notes.notesandroid.util.localDateTimeToEpochMillis
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.UUID
import kotlin.math.roundToInt

private const val STOPWATCH_DURATION_PLACEHOLDER = 365L * 24L * 60L * 60L * 1000L

/**
 * Edits timers and stopwatch offsets. Stopwatch mode can either be adjusted as
 * elapsed duration or mapped onto a real start date/time through the wheel dialog.
 */
@Composable
fun TimerEditorRoute(
    repository: NotesRepository,
    timerId: String?,
    onBack: () -> Unit,
) {
    val existingTimerFlow = remember(timerId) { timerId?.let(repository::observeTimer) }
    var existingTimer by remember(timerId) { mutableStateOf<TimerEntry?>(null) }
    var name by remember(timerId) { mutableStateOf("") }
    var mode by remember(timerId) { mutableStateOf(TimerMode.COUNTDOWN) }
    var minutes by remember(timerId) { mutableStateOf("25") }
    var seconds by remember(timerId) { mutableStateOf("00") }
    var stopwatchHours by remember(timerId) { mutableStateOf("0") }
    var stopwatchMinutes by remember(timerId) { mutableStateOf("00") }
    var stopwatchSeconds by remember(timerId) { mutableStateOf("00") }
    var createdAt by remember(timerId) { mutableStateOf(System.currentTimeMillis()) }
    var showStartTimeDialog by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    fun readStopwatchOffsetMillis(): Long {
        val hours = stopwatchHours.toLongOrNull()?.coerceAtLeast(0L) ?: 0L
        val mins = stopwatchMinutes.toLongOrNull()?.coerceIn(0L, 59L) ?: 0L
        val secs = stopwatchSeconds.toLongOrNull()?.coerceIn(0L, 59L) ?: 0L
        return ((hours * 3600L) + (mins * 60L) + secs) * 1000L
    }

    fun applyStopwatchOffsetMillis(durationMillis: Long) {
        val (hours, mins, secs) = durationParts(durationMillis.coerceAtLeast(0L))
        stopwatchHours = hours.toString()
        stopwatchMinutes = mins.toString().padStart(2, '0')
        stopwatchSeconds = secs.toString().padStart(2, '0')
    }

    LaunchedEffect(existingTimerFlow) {
        val timer = existingTimerFlow?.filterNotNull()?.first()
        existingTimer = timer
        if (timer != null) {
            val now = System.currentTimeMillis()
            name = timer.name
            mode = timer.mode
            minutes = ((timer.durationMillis / 1000L) / 60L).toString()
            seconds = (((timer.durationMillis / 1000L) % 60L)).toString().padStart(2, '0')
            applyStopwatchOffsetMillis(timer.elapsedAt(now))
            createdAt = timer.createdAt
        }
    }

    val currentOffsetMillis = readStopwatchOffsetMillis()
    val derivedStartTime = epochMillisToLocalDateTime(
        (System.currentTimeMillis() - currentOffsetMillis).coerceAtLeast(0L)
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 18.dp, vertical = 20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            TextButton(onClick = onBack) { Text("Back") }
            FilterChip(
                selected = mode == TimerMode.COUNTDOWN,
                onClick = { mode = TimerMode.COUNTDOWN },
                label = { Text("Countdown") },
            )
            FilterChip(
                selected = mode == TimerMode.STOPWATCH,
                onClick = { mode = TimerMode.STOPWATCH },
                label = { Text("Stopwatch") },
            )
        }

        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Name") },
            singleLine = true,
        )

        if (mode == TimerMode.COUNTDOWN) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = minutes,
                    onValueChange = { minutes = it.filter(Char::isDigit) },
                    modifier = Modifier.weight(1f),
                    label = { Text("Minutes") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                )
                OutlinedTextField(
                    value = seconds,
                    onValueChange = { seconds = it.filter(Char::isDigit).take(2) },
                    modifier = Modifier.weight(1f),
                    label = { Text("Seconds") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                )
            }

            Text(
                text = "Duration preview: ${formatDurationCompact((((minutes.toLongOrNull() ?: 0L) * 60L) + (seconds.toLongOrNull() ?: 0L)) * 1000L)}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            Text(
                text = "Choose a custom stopwatch offset or map it to a real start date and time.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = stopwatchHours,
                    onValueChange = { stopwatchHours = it.filter(Char::isDigit) },
                    modifier = Modifier.weight(1f),
                    label = { Text("Hours") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                )
                OutlinedTextField(
                    value = stopwatchMinutes,
                    onValueChange = { stopwatchMinutes = it.filter(Char::isDigit).take(2) },
                    modifier = Modifier.weight(1f),
                    label = { Text("Minutes") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                )
                OutlinedTextField(
                    value = stopwatchSeconds,
                    onValueChange = { stopwatchSeconds = it.filter(Char::isDigit).take(2) },
                    modifier = Modifier.weight(1f),
                    label = { Text("Seconds") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                OutlinedButton(
                    onClick = { applyStopwatchOffsetMillis(currentOffsetMillis + 10L * 60L * 60L * 1000L) },
                    modifier = Modifier.weight(1f),
                ) {
                    Text("+10 hours")
                }
                OutlinedButton(
                    onClick = { applyStopwatchOffsetMillis(currentOffsetMillis + 10L * 60L * 1000L) },
                    modifier = Modifier.weight(1f),
                ) {
                    Text("+10 min")
                }
                OutlinedButton(
                    onClick = { applyStopwatchOffsetMillis(currentOffsetMillis + 10_000L) },
                    modifier = Modifier.weight(1f),
                ) {
                    Text("+10 sec")
                }
            }

            OutlinedButton(
                onClick = { showStartTimeDialog = true },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Edit start time")
            }

            Surface(
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
                tonalElevation = 4.dp,
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Text(
                        text = "Stopwatch preview",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        text = "Starts from ${formatDurationCompact(currentOffsetMillis)}",
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Text(
                        text = "Derived start time: ${formatDateTimeDetailed(derivedStartTime)}",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        Button(
            onClick = {
                scope.launch {
                    val now = System.currentTimeMillis()
                    val previousTimer = existingTimer
                    val updatedTimer = if (mode == TimerMode.COUNTDOWN) {
                        val durationMillis = ((((minutes.toLongOrNull() ?: 0L) * 60L) + (seconds.toLongOrNull() ?: 0L))
                            .coerceAtLeast(1L)) * 1000L
                        val canResumePrevious = previousTimer?.mode == TimerMode.COUNTDOWN && previousTimer.running
                        val previousElapsed = if (previousTimer?.mode == TimerMode.COUNTDOWN) {
                            previousTimer.elapsedAt(now).coerceAtMost(durationMillis)
                        } else {
                            0L
                        }
                        TimerEntry(
                            id = timerId ?: UUID.randomUUID().toString(),
                            name = name,
                            mode = TimerMode.COUNTDOWN,
                            durationMillis = durationMillis,
                            startedAt = if (canResumePrevious && previousElapsed < durationMillis) now else null,
                            accumulatedMillis = if (canResumePrevious) previousElapsed else 0L,
                            running = canResumePrevious && previousElapsed < durationMillis,
                            createdAt = createdAt,
                            updatedAt = now,
                        )
                    } else {
                        val keepRunning = previousTimer?.mode == TimerMode.STOPWATCH && previousTimer.running
                        TimerEntry(
                            id = timerId ?: UUID.randomUUID().toString(),
                            name = name,
                            mode = TimerMode.STOPWATCH,
                            durationMillis = previousTimer?.durationMillis ?: STOPWATCH_DURATION_PLACEHOLDER,
                            startedAt = if (keepRunning) now else null,
                            accumulatedMillis = currentOffsetMillis,
                            running = keepRunning,
                            createdAt = createdAt,
                            updatedAt = now,
                        )
                    }

                    repository.upsertTimer(updatedTimer)
                    onBack()
                }
            },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Save timer")
        }
    }

    if (showStartTimeDialog) {
        StopwatchStartTimeDialog(
            initialDateTime = derivedStartTime,
            onDismiss = { showStartTimeDialog = false },
            onConfirm = { selectedDateTime ->
                val startTimeMillis = localDateTimeToEpochMillis(selectedDateTime)
                applyStopwatchOffsetMillis((System.currentTimeMillis() - startTimeMillis).coerceAtLeast(0L))
                showStartTimeDialog = false
            },
        )
    }
}

/**
 * Wheel-style dialog for mapping the current stopwatch offset onto a concrete
 * calendar day and time. The confirmed value is converted back into elapsed time.
 */
@Composable
private fun StopwatchStartTimeDialog(
    initialDateTime: LocalDateTime,
    onDismiss: () -> Unit,
    onConfirm: (LocalDateTime) -> Unit,
) {
    val today = remember { LocalDate.now() }
    val dayOptions = remember(today) { (-365L..30L).map { today.plusDays(it) } }
    val initialDayIndex = remember(dayOptions, initialDateTime) {
        dayOptions.indexOf(initialDateTime.toLocalDate()).takeIf { it >= 0 } ?: dayOptions.indexOf(today)
    }

    var selectedDayIndex by remember(initialDayIndex) { mutableStateOf(initialDayIndex.coerceAtLeast(0)) }
    var selectedHour by remember(initialDateTime) { mutableStateOf(initialDateTime.hour) }
    var selectedMinute by remember(initialDateTime) { mutableStateOf(initialDateTime.minute) }
    var selectedSecond by remember(initialDateTime) { mutableStateOf(initialDateTime.second) }

    val selectedDateTime = remember(selectedDayIndex, selectedHour, selectedMinute, selectedSecond, dayOptions) {
        LocalDateTime.of(
            dayOptions[selectedDayIndex],
            LocalTime.of(selectedHour, selectedMinute, selectedSecond),
        )
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(30.dp),
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.98f),
            tonalElevation = 8.dp,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Text(
                    text = "Edit start time",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = "Spin the wheels to align the stopwatch with a real moment in time.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                // Every wheel shares the same centering logic so day/hour/minute/second
                // all resolve to the same selected row in the highlighted center slot.
                WheelDateTimePicker(
                    dayOptions = dayOptions,
                    selectedDayIndex = selectedDayIndex,
                    onDaySelected = { selectedDayIndex = it },
                    selectedHour = selectedHour,
                    onHourSelected = { selectedHour = it },
                    selectedMinute = selectedMinute,
                    onMinuteSelected = { selectedMinute = it },
                    selectedSecond = selectedSecond,
                    onSecondSelected = { selectedSecond = it },
                )
                Surface(
                    shape = RoundedCornerShape(22.dp),
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                ) {
                    Text(
                        text = formatDateTimeDetailed(selectedDateTime),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        color = MaterialTheme.colorScheme.primary,
                        textAlign = TextAlign.Center,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                    ) {
                        Text("Cancel")
                    }
                    Button(
                        onClick = { onConfirm(selectedDateTime) },
                        modifier = Modifier.weight(1f),
                    ) {
                        Text("Apply")
                    }
                }
            }
        }
    }
}

@Composable
private fun WheelDateTimePicker(
    dayOptions: List<LocalDate>,
    selectedDayIndex: Int,
    onDaySelected: (Int) -> Unit,
    selectedHour: Int,
    onHourSelected: (Int) -> Unit,
    selectedMinute: Int,
    onMinuteSelected: (Int) -> Unit,
    selectedSecond: Int,
    onSecondSelected: (Int) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        WheelPickerColumn(
            title = "Day",
            values = dayOptions.map { formatWheelDayLabel(it) },
            selectedIndex = selectedDayIndex,
            onSelectedIndexChange = onDaySelected,
            modifier = Modifier.weight(1.55f),
        )
        WheelPickerColumn(
            title = "Hour",
            values = (0..23).map { it.toString().padStart(2, '0') },
            selectedIndex = selectedHour,
            onSelectedIndexChange = onHourSelected,
            modifier = Modifier.weight(0.72f),
        )
        WheelPickerColumn(
            title = "Minute",
            values = (0..59).map { it.toString().padStart(2, '0') },
            selectedIndex = selectedMinute,
            onSelectedIndexChange = onMinuteSelected,
            modifier = Modifier.weight(0.82f),
        )
        WheelPickerColumn(
            title = "Second",
            values = (0..59).map { it.toString().padStart(2, '0') },
            selectedIndex = selectedSecond,
            onSelectedIndexChange = onSecondSelected,
            modifier = Modifier.weight(0.82f),
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun WheelPickerColumn(
    title: String,
    values: List<String>,
    selectedIndex: Int,
    onSelectedIndexChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val itemHeight = 44.dp
    val itemHeightPx = with(LocalDensity.current) { itemHeight.toPx() }
    val wheelTextColors = rememberWheelTextColors()
    val centerPadding = itemHeight * 2
    val listState = rememberLazyListState(initialFirstVisibleItemIndex = selectedIndex.coerceIn(0, values.lastIndex))
    val flingBehavior = rememberSnapFlingBehavior(lazyListState = listState)

    LaunchedEffect(selectedIndex, values.size) {
        val coerced = selectedIndex.coerceIn(0, values.lastIndex)
        if (coerced != listState.firstVisibleItemIndex || listState.firstVisibleItemScrollOffset != 0) {
            listState.animateScrollToItem(coerced)
        }
    }

    LaunchedEffect(listState.isScrollInProgress) {
        if (!listState.isScrollInProgress) {
            val viewportCenter = (listState.layoutInfo.viewportStartOffset + listState.layoutInfo.viewportEndOffset) / 2
            val nearestIndex = listState.layoutInfo.visibleItemsInfo
                .minByOrNull { item ->
                    val itemCenter = item.offset + (item.size / 2)
                    kotlin.math.abs(itemCenter - viewportCenter)
                }
                ?.index
                ?.coerceIn(0, values.lastIndex)
                ?: selectedIndex.coerceIn(0, values.lastIndex)

            if (nearestIndex != selectedIndex) {
                onSelectedIndexChange(nearestIndex)
            }
            listState.animateScrollToItem(nearestIndex)
        }
    }

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(itemHeight * 5)
        ) {
            Surface(
                modifier = Modifier
                    .align(Alignment.Center)
                    .fillMaxWidth()
                    .height(itemHeight + 4.dp),
                shape = RoundedCornerShape(18.dp),
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
            ) {}

            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                flingBehavior = flingBehavior,
                contentPadding = PaddingValues(vertical = centerPadding),
            ) {
                items(values.size) { index ->
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(itemHeight),
                        contentAlignment = Alignment.Center,
                    ) {
                        val isSelected = index == selectedIndex
                        Text(
                            text = values[index],
                            color = if (isSelected) {
                                wheelTextColors.selected
                            } else {
                                wheelTextColors.unselected
                            },
                            modifier = Modifier.alpha(if (isSelected) 1f else 0.48f),
                            style = if (isSelected) {
                                MaterialTheme.typography.titleLarge
                            } else {
                                MaterialTheme.typography.titleMedium
                            },
                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                            textAlign = TextAlign.Center,
                        )
                    }
                }
            }

            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth()
                    .height(itemHeight * 2)
                    .background(
                        MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)
                    ),
            )
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .height(itemHeight * 2)
                    .background(
                        MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)
                    ),
            )
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .fillMaxWidth()
                    .height(itemHeight + 4.dp)
            )
        }
    }
}

/**
 * Keeps the wheel legible in both app themes. The timer editor uses one dialog
 * surface, but the selected and idle rows need stronger contrast than default M3.
 */
@Composable
private fun rememberWheelTextColors(): WheelTextColors {
    val darkTheme = MaterialTheme.colorScheme.surface.luminance() < 0.5f
    return remember(darkTheme) {
        if (darkTheme) {
            WheelTextColors(
                selected = Color(0xFFF6EBD8),
                unselected = Color(0xFFD1BEA1),
            )
        } else {
            WheelTextColors(
                selected = Color(0xFF3F3A3B),
                unselected = Color(0xFF6A6366),
            )
        }
    }
}

private data class WheelTextColors(
    val selected: Color,
    val unselected: Color,
)
