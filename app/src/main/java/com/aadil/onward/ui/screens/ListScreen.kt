package com.aadil.onward.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.MoreHoriz
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aadil.onward.model.AppData
import com.aadil.onward.model.Priority
import com.aadil.onward.model.Task
import com.aadil.onward.model.TaskList
import com.aadil.onward.model.dueDate
import com.aadil.onward.model.isDueBy
import com.aadil.onward.ui.components.Backdrop
import com.aadil.onward.ui.components.Composer
import com.aadil.onward.ui.components.Confetti
import com.aadil.onward.ui.components.EmptyState
import com.aadil.onward.ui.components.Eyebrow
import com.aadil.onward.ui.components.IconTile
import com.aadil.onward.ui.components.ListIcons
import com.aadil.onward.ui.components.ProgressRing
import com.aadil.onward.ui.components.Segmented
import com.aadil.onward.ui.components.SwipeableTask
import com.aadil.onward.ui.components.onwardCard
import com.aadil.onward.ui.theme.Onward
import java.time.LocalDate

enum class TaskFilter(val label: String) {
    ALL("All"), ACTIVE("Active"), DONE("Done");

    fun matches(t: Task) = when (this) {
        ALL -> true
        ACTIVE -> !t.done
        DONE -> t.done
    }
}

@Composable
fun ListScreen(
    list: TaskList,
    data: AppData,
    today: LocalDate,
    onBack: () -> Unit,
    onAddTask: (title: String, Priority, LocalDate?) -> Unit,
    onToggleTask: (Task) -> Unit,
    onDeleteTask: (Task) -> Unit,
    onEditTask: (Task) -> Unit,
    onClearDone: () -> Unit,
    onEditList: () -> Unit,
    onDeleteList: () -> Unit,
) {
    val c = Onward.colors
    var filter by rememberSaveable { mutableStateOf(TaskFilter.ALL) }
    var confetti by remember { mutableStateOf(0) }
    var ringCenter by remember { mutableStateOf(Offset.Zero) }
    val all = data.tasksIn(list.id)
    val done = all.count { it.done }
    val visible = all.filter(filter::matches)
    val dueSoon = all.count { it.isDueBy(today) }

    fun toggle(t: Task) {
        onToggleTask(t)
        // The last open task just got done: celebrate.
        if (!t.done && all.size > 1 && all.count { !it.done } == 1) confetti++
    }

    Box(Modifier.fillMaxSize()) {
        Backdrop()
        LazyColumn(
            Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 32.dp),
        ) {
            item(key = "bar") {
                Row(Modifier.statusBarsPadding().padding(top = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Back", tint = c.ink) }
                    Spacer(Modifier.weight(1f))
                    var menu by remember { mutableStateOf(false) }
                    Box {
                        IconButton(onClick = { menu = true }) { Icon(Icons.Rounded.MoreHoriz, "More", tint = c.ink2) }
                        DropdownMenu(
                            expanded = menu,
                            onDismissRequest = { menu = false },
                            shape = RoundedCornerShape(16.dp),
                            containerColor = c.surface,
                            border = BorderStroke(1.dp, c.line),
                        ) {
                            DropdownMenuItem(
                                text = { Text("Edit list") },
                                leadingIcon = { Icon(Icons.Rounded.Edit, null, tint = c.muted) },
                                onClick = { menu = false; onEditList() },
                            )
                            DropdownMenuItem(
                                text = { Text("Clear completed") },
                                leadingIcon = { Icon(Icons.Rounded.DeleteSweep, null, tint = c.muted) },
                                enabled = done > 0,
                                onClick = { menu = false; onClearDone() },
                            )
                            DropdownMenuItem(
                                text = { Text("Delete list", color = c.high) },
                                leadingIcon = { Icon(Icons.Rounded.DeleteOutline, null, tint = c.high) },
                                onClick = { menu = false; onDeleteList() },
                            )
                        }
                    }
                }
            }
            item(key = "hero") {
                Row(Modifier.padding(start = 4.dp, top = 14.dp, bottom = 24.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconTile(ListIcons.of(list.icon), list.color, size = 30.dp)
                            Spacer(Modifier.width(10.dp))
                            Eyebrow(list.space.label)
                        }
                        Spacer(Modifier.height(10.dp))
                        Text(list.name, style = MaterialTheme.typography.displaySmall)
                        Spacer(Modifier.height(8.dp))
                        val open = all.size - done
                        Text(
                            when {
                                all.isEmpty() -> "A fresh list. Add the first thing."
                                open == 0 -> "Everything's done."
                                dueSoon > 0 -> "$open to go · $dueSoon due"
                                else -> "$open to go"
                            },
                            style = MaterialTheme.typography.bodyLarge,
                            color = c.ink2,
                        )
                    }
                    ProgressRing(
                        if (all.isEmpty()) 0f else done.toFloat() / all.size,
                        size = 70.dp,
                        color = c.tint(list.color),
                        modifier = Modifier.onGloballyPositioned { ringCenter = it.boundsInRoot().center },
                    ) {
                        Text(
                            if (all.isEmpty()) "–" else "${(done * 100f / all.size).toInt()}%",
                            fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = c.ink,
                        )
                    }
                }
            }
            item(key = "composer") {
                Composer(onAdd = { title, p, due, _ ->
                    onAddTask(title, p, due)
                    if (filter == TaskFilter.DONE) filter = TaskFilter.ALL
                })
            }
            item(key = "filter") {
                Row(Modifier.padding(top = 26.dp, bottom = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Segmented(
                        TaskFilter.entries, filter, { it.label }, { filter = it },
                        count = { f -> all.count(f::matches) },
                    )
                    Spacer(Modifier.weight(1f))
                    IconButton(onClick = onClearDone, enabled = done > 0) {
                        Icon(Icons.Rounded.DeleteSweep, "Clear completed", tint = if (done > 0) c.ink2 else c.line)
                    }
                }
            }
            if (visible.isEmpty()) {
                item(key = "empty") {
                    Box(Modifier.fillMaxWidth().onwardCard()) {
                        when {
                            all.isEmpty() -> EmptyState("Nothing here yet", "Add a task above. Give it a priority or a due date if it helps.")
                            filter == TaskFilter.ACTIVE -> EmptyState("All clear", "Every task in ${list.name} is done.")
                            else -> EmptyState("Nothing done yet", "Completed tasks land here.")
                        }
                    }
                }
            } else {
                items(visible, key = { it.id }) { t ->
                    SwipeableTask(
                        task = t,
                        today = today,
                        onToggle = { toggle(t) },
                        onDelete = { onDeleteTask(t) },
                        onClick = { onEditTask(t) },
                        modifier = Modifier.padding(bottom = 8.dp).animateItem(),
                    )
                }
            }
            item(key = "hint") {
                Text(
                    "Swipe right to complete · left to delete · tap to edit",
                    style = MaterialTheme.typography.bodySmall,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(top = 18.dp).navigationBarsPadding(),
                )
            }
        }
        Confetti(confetti, ringCenter, listOf(c.accent, c.high, c.medium, c.low, c.tint(list.color)))
    }
}
