package com.aadil.onward.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Spa
import androidx.compose.material.icons.outlined.WbSunny
import androidx.compose.material.icons.outlined.WorkOutline
import androidx.compose.material.icons.rounded.Spa
import androidx.compose.material.icons.rounded.WbSunny
import androidx.compose.material.icons.rounded.Work
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.aadil.onward.data.OnwardViewModel
import com.aadil.onward.model.AppData
import com.aadil.onward.model.Space
import com.aadil.onward.model.Task
import com.aadil.onward.model.ThemePref
import com.aadil.onward.ui.screens.HabitEditorSheet
import com.aadil.onward.ui.screens.HabitsScreen
import com.aadil.onward.ui.screens.ListEditorSheet
import com.aadil.onward.ui.screens.ListScreen
import com.aadil.onward.ui.screens.SpaceScreen
import com.aadil.onward.ui.screens.TaskEditorSheet
import com.aadil.onward.ui.screens.TodayScreen
import com.aadil.onward.ui.theme.Onward
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.LocalDate

private enum class Tab(val label: String, val icon: ImageVector, val selectedIcon: ImageVector) {
    TODAY("Today", Icons.Outlined.WbSunny, Icons.Rounded.WbSunny),
    PERSONAL("Personal", Icons.Outlined.Spa, Icons.Rounded.Spa),
    WORK("Work", Icons.Outlined.WorkOutline, Icons.Rounded.Work),
}

/** Which bottom sheet is open, if any. */
private sealed interface Sheet {
    data class EditTask(val id: String) : Sheet
    data class NewList(val space: Space) : Sheet
    data class EditList(val id: String) : Sheet
    data object NewHabit : Sheet
    data class EditHabit(val id: String) : Sheet
}

/** The current calendar day, refreshed every minute so "Today" rolls over. */
@Composable
fun rememberToday(): LocalDate = produceState(LocalDate.now()) {
    while (true) {
        delay(60_000)
        value = LocalDate.now()
    }
}.value

@Composable
fun OnwardApp(vm: OnwardViewModel, data: AppData) {
    val c = Onward.colors
    val today = rememberToday()
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    var tab by rememberSaveable { mutableIntStateOf(0) }
    // "habits", "list:<id>" or null for the tab itself.
    var detail by rememberSaveable { mutableStateOf<String?>(null) }
    var sheet by remember { mutableStateOf<Sheet?>(null) }

    fun undoable(message: String, undo: () -> Unit) {
        scope.launch {
            snackbar.currentSnackbarData?.dismiss()
            val r = snackbar.showSnackbar(message, actionLabel = "Undo", duration = SnackbarDuration.Long)
            if (r == SnackbarResult.ActionPerformed) undo()
        }
    }

    fun deleteTask(t: Task) {
        val removed = vm.deleteTask(t.id)
        undoable("Task deleted") { vm.restoreTasks(removed) }
    }

    fun clearDone(listId: String) {
        val removed = vm.clearDone(listId)
        if (removed.isNotEmpty()) {
            undoable("Cleared ${removed.size} completed ${if (removed.size == 1) "task" else "tasks"}") { vm.restoreTasks(removed) }
        }
    }

    fun deleteList(id: String) {
        val name = data.list(id)?.name ?: "List"
        val removed = vm.deleteList(id) ?: return
        if (detail == "list:$id") detail = null
        undoable("$name deleted") { vm.restoreList(removed) }
    }

    BackHandler(enabled = detail != null) { detail = null }

    // A list that disappeared (deleted elsewhere) closes its screen.
    LaunchedEffect(detail, data.lists) {
        val d = detail
        if (d != null && d.startsWith("list:") && data.list(d.removePrefix("list:")) == null) detail = null
    }

    val dark = c.isDark
    val toggleTheme = { vm.setTheme(if (dark) ThemePref.LIGHT else ThemePref.DARK) }

    Scaffold(
        containerColor = c.bg,
        contentWindowInsets = WindowInsets(0),
        snackbarHost = {
            SnackbarHost(snackbar) { d ->
                Snackbar(
                    d,
                    shape = RoundedCornerShape(14.dp),
                    containerColor = c.ink,
                    contentColor = c.bg,
                    actionColor = if (dark) com.aadil.onward.ui.theme.OnwardColors.Light.accent
                    else com.aadil.onward.ui.theme.OnwardColors.Dark.accent,
                )
            }
        },
        bottomBar = {
            AnimatedVisibility(
                detail == null,
                enter = slideInVertically { it } + fadeIn(),
                exit = slideOutVertically { it } + fadeOut(),
            ) {
                Column {
                    HorizontalDivider(color = c.line)
                    NavigationBar(containerColor = c.surface, tonalElevation = 0.dp) {
                        Tab.entries.forEachIndexed { i, t ->
                            val on = tab == i
                            NavigationBarItem(
                                selected = on,
                                onClick = { tab = i },
                                icon = { Icon(if (on) t.selectedIcon else t.icon, null) },
                                label = { Text(t.label) },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = c.accent,
                                    selectedTextColor = c.ink,
                                    indicatorColor = c.accentSoft,
                                    unselectedIconColor = c.muted,
                                    unselectedTextColor = c.muted,
                                ),
                            )
                        }
                    }
                }
            }
        },
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding).imePadding()) {
            AnimatedContent(
                targetState = detail,
                transitionSpec = {
                    if (targetState != null) {
                        (slideInHorizontally(tween(320)) { it / 3 } + fadeIn(tween(260))) togetherWith fadeOut(tween(180))
                    } else {
                        fadeIn(tween(260)) togetherWith (slideOutHorizontally(tween(280)) { it / 3 } + fadeOut(tween(220)))
                    }
                },
                label = "detail",
            ) { route ->
                when {
                    route == "habits" -> HabitsScreen(
                        habits = data.habits,
                        today = today,
                        onBack = { detail = null },
                        onToggle = { h, day -> vm.toggleHabit(h.id, day) },
                        onEdit = { sheet = Sheet.EditHabit(it.id) },
                        onNew = { sheet = Sheet.NewHabit },
                    )

                    route != null && route.startsWith("list:") -> {
                        val list = data.list(route.removePrefix("list:"))
                        if (list != null) {
                            ListScreen(
                                list = list,
                                data = data,
                                today = today,
                                onBack = { detail = null },
                                onAddTask = { title, p, due -> vm.addTask(list.id, title, p, due) },
                                onToggleTask = { vm.toggleTask(it.id) },
                                onDeleteTask = ::deleteTask,
                                onEditTask = { sheet = Sheet.EditTask(it.id) },
                                onClearDone = { clearDone(list.id) },
                                onEditList = { sheet = Sheet.EditList(list.id) },
                                onDeleteList = { deleteList(list.id) },
                            )
                        }
                    }

                    else -> Crossfade(tab, animationSpec = tween(220), label = "tab") { t ->
                        when (Tab.entries[t]) {
                            Tab.TODAY -> TodayScreen(
                                data = data,
                                today = today,
                                onAddTask = { listId, title, p, due -> vm.addTask(listId, title, p, due) },
                                onToggleTask = { vm.toggleTask(it.id) },
                                onDeleteTask = ::deleteTask,
                                onEditTask = { sheet = Sheet.EditTask(it.id) },
                                onToggleHabit = { vm.toggleHabit(it.id, today) },
                                onOpenHabits = { detail = "habits" },
                                onNewHabit = { sheet = Sheet.NewHabit },
                                onOpenSpace = { tab = if (it == Space.WORK) Tab.WORK.ordinal else Tab.PERSONAL.ordinal },
                                onToggleTheme = toggleTheme,
                            )

                            Tab.PERSONAL, Tab.WORK -> {
                                val space = if (Tab.entries[t] == Tab.WORK) Space.WORK else Space.PERSONAL
                                SpaceScreen(
                                    space = space,
                                    data = data,
                                    today = today,
                                    onOpenList = { detail = "list:${it.id}" },
                                    onEditList = { sheet = Sheet.EditList(it.id) },
                                    onNewList = { sheet = Sheet.NewList(space) },
                                    onOpenHabits = { detail = "habits" },
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Sheets
    when (val s = sheet) {
        null -> {}
        is Sheet.EditTask -> {
            val task = data.tasks.firstOrNull { it.id == s.id }
            if (task == null) sheet = null else key(s.id) {
                TaskEditorSheet(
                    task = task,
                    lists = data.lists,
                    onDismiss = { sheet = null },
                    onSave = { title, notes, p, due, listId -> vm.updateTask(task.id, title, notes, p, due, listId) },
                    onDelete = { deleteTask(task) },
                )
            }
        }

        is Sheet.NewList -> ListEditorSheet(
            existing = null,
            space = s.space,
            onDismiss = { sheet = null },
            onSave = { name, color, icon ->
                val id = vm.addList(name, s.space, color, icon)
                detail = "list:$id"
            },
        )

        is Sheet.EditList -> {
            val list = data.list(s.id)
            if (list == null) sheet = null else ListEditorSheet(
                existing = list,
                space = list.space,
                onDismiss = { sheet = null },
                onSave = { name, color, icon -> vm.updateList(list.id, name, color, icon) },
                onDelete = { deleteList(list.id) },
            )
        }

        Sheet.NewHabit -> HabitEditorSheet(
            existingName = null,
            existingColor = null,
            onDismiss = { sheet = null },
            onSave = { name, color -> vm.addHabit(name, color) },
        )

        is Sheet.EditHabit -> {
            val habit = data.habits.firstOrNull { it.id == s.id }
            if (habit == null) sheet = null else HabitEditorSheet(
                existingName = habit.name,
                existingColor = habit.color,
                onDismiss = { sheet = null },
                onSave = { name, color -> vm.updateHabit(habit.id, name, color) },
                onDelete = {
                    val removed = vm.deleteHabit(habit.id)
                    if (removed != null) undoable("${habit.name} deleted") { vm.restoreHabit(removed) }
                },
            )
        }
    }
}
