package com.notes.notesandroid.navigation

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Sync
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.notes.notesandroid.data.NotesRepository
import com.notes.notesandroid.data.model.AppThemeMode
import com.notes.notesandroid.data.model.Note
import com.notes.notesandroid.feature.notes.NoteEditorRoute
import com.notes.notesandroid.feature.notes.NoteMarkdownEditorRoute
import com.notes.notesandroid.feature.notes.NoteTitleEditorRoute
import com.notes.notesandroid.feature.notes.NotesRoute
import com.notes.notesandroid.feature.settings.SettingsRoute
import com.notes.notesandroid.feature.timers.TimerEditorRoute
import com.notes.notesandroid.feature.timers.TimersRoute
import com.notes.notesandroid.ui.theme.NotesAndroidTheme
import kotlinx.coroutines.launch
import java.util.UUID

enum class TopLevelDestination(
    val route: String,
    val label: String,
) {
    NOTES("notes", "Notes"),
    TIMERS("timers", "Timers"),
    SETTINGS("settings", "Sync"),
}

private object AppRoute {
    const val NOTE_NEW = "note/new"
    const val NOTE_EDIT = "note/edit/{noteId}"
    const val NOTE_EDIT_TITLE = "note/title/{noteId}"
    const val NOTE_EDIT_MARKDOWN = "note/markdown/{noteId}"
    const val TIMER_NEW = "timer/new"
    const val TIMER_EDIT = "timer/edit/{timerId}"

    fun noteEdit(noteId: String): String = "note/edit/$noteId"
    fun noteEditTitle(noteId: String): String = "note/title/$noteId"
    fun noteEditMarkdown(noteId: String): String = "note/markdown/$noteId"
    fun timerEdit(timerId: String): String = "timer/edit/$timerId"
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PaperNotesApp(
    repository: NotesRepository,
    startDestination: TopLevelDestination,
    requestedNoteId: String? = null,
    requestedTimerId: String? = null,
    launchRequestKey: Long = 0L,
) {
    val dashboard by repository.dashboard.collectAsStateWithLifecycle()
    val navController = rememberNavController()
    val scope = rememberCoroutineScope()
    val visibleDestinations = remember { listOf(TopLevelDestination.NOTES, TopLevelDestination.TIMERS) }

    NotesAndroidTheme(themeMode = dashboard.preferences.themeMode) {
        Scaffold(
            contentWindowInsets = WindowInsets.safeDrawing,
            topBar = {
                TopAppBar(
                    title = {
                        AnimatedContent(targetState = navController.currentBackStackEntryAsState().value?.destination?.route) { route ->
                            Text(
                                text = when {
                                    route == TopLevelDestination.TIMERS.route -> "Chronicle Timers"
                                    route == TopLevelDestination.SETTINGS.route -> "Sync Atelier"
                                    route?.startsWith("note/") == true -> "Markdown Studio"
                                    route?.startsWith("timer/") == true -> "Timer Studio"
                                    else -> "Paper Notes"
                                }
                            )
                        }
                    },
                    actions = {
                        IconButton(
                            onClick = {
                                val nextMode = when (dashboard.preferences.themeMode) {
                                    AppThemeMode.SYSTEM -> AppThemeMode.DARK
                                    AppThemeMode.DARK -> AppThemeMode.LIGHT
                                    AppThemeMode.LIGHT -> AppThemeMode.SYSTEM
                                }
                                scope.launch {
                                    repository.saveThemeMode(nextMode)
                                }
                            },
                        ) {
                            Icon(
                                imageVector = if (MaterialTheme.colorScheme.primary.luminance() < 0.5f) {
                                    Icons.Rounded.DarkMode
                                } else {
                                    Icons.Rounded.AutoAwesome
                                },
                                contentDescription = "Change theme",
                                tint = MaterialTheme.colorScheme.primary,
                            )
                        }
                        IconButton(
                            onClick = {
                                navController.navigate(TopLevelDestination.SETTINGS.route) {
                                    launchSingleTop = true
                                }
                            },
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Sync,
                                contentDescription = "Sync settings",
                                tint = MaterialTheme.colorScheme.primary,
                            )
                        }
                    },
                )
            },
            bottomBar = {
                val backStackEntry by navController.currentBackStackEntryAsState()
                val currentRoute = backStackEntry?.destination?.route
                if (currentRoute in visibleDestinations.map { it.route }) {
                    BottomAppBar(
                        modifier = Modifier.navigationBarsPadding(),
                        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f),
                    ) {
                        visibleDestinations.forEach { destination ->
                            NavigationBarItem(
                                selected = currentRoute == destination.route,
                                onClick = {
                                    navController.navigate(destination.route) {
                                        popUpTo(navController.graph.findStartDestination().id) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                },
                                icon = {
                                    Icon(
                                        imageVector = when (destination) {
                                            TopLevelDestination.NOTES -> Icons.Rounded.Description
                                            TopLevelDestination.TIMERS -> Icons.Rounded.Schedule
                                            TopLevelDestination.SETTINGS -> Icons.Rounded.Sync
                                        },
                                        contentDescription = destination.label,
                                    )
                                },
                                label = { Text(destination.label) },
                            )
                        }
                    }
                }
            },
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = if (MaterialTheme.colorScheme.background.luminance() > 0.5f) {
                                listOf(
                                    Color(0xFFFFFFFF),
                                    Color(0xFFF1E4D2),
                                    Color(0xFFE9DDFE),
                                )
                            } else {
                                listOf(
                                    Color(0xFF050505),
                                    Color(0xFF2C0E18),
                                    Color(0xFF12070A),
                                )
                            }
                        )
                    )
                    .padding(innerPadding),
            ) {
                val startRoute = remember(startDestination) { startDestination.route }
                NavHost(
                    navController = navController,
                    startDestination = startRoute,
                    modifier = Modifier.fillMaxSize(),
                ) {
                    composable(TopLevelDestination.NOTES.route) {
                        NotesRoute(
                            repository = repository,
                            onCreate = { navController.navigate(AppRoute.NOTE_NEW) },
                            onOpen = { noteId -> navController.navigate(AppRoute.noteEdit(noteId)) },
                        )
                    }
                    composable(TopLevelDestination.TIMERS.route) {
                        TimersRoute(
                            repository = repository,
                            onCreate = { navController.navigate(AppRoute.TIMER_NEW) },
                            onOpen = { timerId -> navController.navigate(AppRoute.timerEdit(timerId)) },
                        )
                    }
                    composable(TopLevelDestination.SETTINGS.route) {
                        SettingsRoute(repository = repository)
                    }
                    composable(AppRoute.NOTE_NEW) {
                        LaunchedEffect(Unit) {
                            val noteId = UUID.randomUUID().toString()
                            val now = System.currentTimeMillis()
                            repository.upsertNote(
                                Note(
                                    id = noteId,
                                    title = "",
                                    content = "",
                                    pinned = false,
                                    createdAt = now,
                                    updatedAt = now,
                                )
                            )
                            navController.navigate(AppRoute.noteEdit(noteId)) {
                                popUpTo(AppRoute.NOTE_NEW) { inclusive = true }
                                launchSingleTop = true
                            }
                        }
                    }
                    composable(AppRoute.NOTE_EDIT) { backStackEntry ->
                        NoteEditorRoute(
                            repository = repository,
                            noteId = backStackEntry.arguments?.getString("noteId"),
                            onBack = { navController.popBackStack() },
                            onEditTitle = { noteId -> navController.navigate(AppRoute.noteEditTitle(noteId)) },
                            onEditMarkdown = { noteId -> navController.navigate(AppRoute.noteEditMarkdown(noteId)) },
                        )
                    }
                    composable(AppRoute.NOTE_EDIT_TITLE) { backStackEntry ->
                        NoteTitleEditorRoute(
                            repository = repository,
                            noteId = backStackEntry.arguments?.getString("noteId"),
                            onBack = { navController.popBackStack() },
                        )
                    }
                    composable(AppRoute.NOTE_EDIT_MARKDOWN) { backStackEntry ->
                        NoteMarkdownEditorRoute(
                            repository = repository,
                            noteId = backStackEntry.arguments?.getString("noteId"),
                            onBack = { navController.popBackStack() },
                        )
                    }
                    composable(AppRoute.TIMER_NEW) {
                        TimerEditorRoute(
                            repository = repository,
                            timerId = null,
                            onBack = { navController.popBackStack() },
                        )
                    }
                    composable(AppRoute.TIMER_EDIT) { backStackEntry ->
                        TimerEditorRoute(
                            repository = repository,
                            timerId = backStackEntry.arguments?.getString("timerId"),
                            onBack = { navController.popBackStack() },
                        )
                    }
                }

                LaunchedEffect(launchRequestKey) {
                    when {
                        requestedNoteId != null -> {
                            navController.navigate(TopLevelDestination.NOTES.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                            navController.navigate(AppRoute.noteEdit(requestedNoteId)) {
                                launchSingleTop = true
                            }
                        }

                        requestedTimerId != null -> {
                            navController.navigate(TopLevelDestination.TIMERS.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                            navController.navigate(AppRoute.timerEdit(requestedTimerId)) {
                                launchSingleTop = true
                            }
                        }

                        else -> {
                            navController.navigate(startDestination.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    }
                }
            }
        }
    }
}
