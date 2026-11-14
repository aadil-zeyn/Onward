package com.aadil.onward.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.LightMode
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.rounded.Spa
import androidx.compose.material.icons.rounded.WorkOutline
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aadil.onward.data.OnwardStore
import com.aadil.onward.model.AppData
import com.aadil.onward.model.Habit
import com.aadil.onward.model.Priority
import com.aadil.onward.model.Space
import com.aadil.onward.model.Task
import com.aadil.onward.model.currentStreak
import com.aadil.onward.model.dueDate
import com.aadil.onward.model.greeting
import com.aadil.onward.model.isDoneOn
import com.aadil.onward.model.isDueBy
import com.aadil.onward.ui.components.Backdrop
import com.aadil.onward.ui.components.BrandMark
import com.aadil.onward.ui.components.CheckCircle
import com.aadil.onward.ui.components.Composer
import com.aadil.onward.ui.components.EmptyState
import com.aadil.onward.ui.components.Eyebrow
import com.aadil.onward.ui.components.ProgressRing
import com.aadil.onward.ui.components.SectionHeader
import com.aadil.onward.ui.components.SwipeableTask
import com.aadil.onward.ui.components.onwardCard
import com.aadil.onward.ui.theme.Onward
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

/** What today asks of you, across both spaces. */
data class TodayAgenda(
    val tasks: List<Task>,
    val doneTasks: Int,
    val totalTasks: Int,
    val doneHabits: Int,
    val totalHabits: Int,
) {
    val total get() = totalTasks + totalHabits
    val done get() = doneTasks + doneHabits
    val progress get() = if (total == 0) 0f else done.toFloat() / total

    companion object {
        fun of(data: AppData, today: LocalDate, nowMillis: Long = System.currentTimeMillis()): TodayAgenda {
            val startOfDay = today.atStartOfDay(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()
            val open = data.tasks.filter { it.isDueBy(today) }
            // Things due today or earlier that were finished today still count.
            val finishedToday = data.tasks.filter {
                it.done && (it.completedAt ?: 0) >= startOfDay && it.dueDate?.isAfter(today) == false
            }
            val ordered = (open + finishedToday).sortedWith(
                compareBy<Task> { it.done }
                    .thenBy { it.dueDate }
                    .thenByDescending { it.priority.ordinal },
            )
            return TodayAgenda(
                tasks = ordered,
                doneTasks = finishedToday.size,
                totalTasks = open.size + finishedToday.size,
                doneHabits = data.habits.count { it.isDoneOn(today) },
                totalHabits = data.habits.size,
            )
        }
    }
}

@Composable
fun TodayScreen(
    data: AppData,
    today: LocalDate,
    onAddTask: (listId: String, title: String, Priority, LocalDate?) -> Unit,
    onToggleTask: (Task) -> Unit,
    onDeleteTask: (Task) -> Unit,
    onEditTask: (Task) -> Unit,
    onToggleHabit: (Habit) -> Unit,
    onOpenHabits: () -> Unit,
    onNewHabit: () -> Unit,
    onOpenSpace: (Space) -> Unit,
    onToggleTheme: () -> Unit,
) {
    val c = Onward.colors
    val agenda = TodayAgenda.of(data, today)
    val overdue = agenda.tasks.count { !it.done && it.dueDate?.isBefore(today) == true }

    Box(Modifier.fillMaxSize()) {
        Backdrop()
        LazyColumn(
            Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 32.dp),
        ) {
            item(key = "top") {
                Row(Modifier.statusBarsPadding().padding(top = 8.dp, start = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                    BrandMark()
                    Spacer(Modifier.width(11.dp))
                    Text("Onward", style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.SemiBold))
                    Spacer(Modifier.weight(1f))
                    IconButton(onClick = onToggleTheme) {
                        Icon(if (c.isDark) Icons.Rounded.LightMode else Icons.Rounded.DarkMode,
                            "Switch theme", tint = c.ink2)
                    }
                }
            }
            item(key = "hero") {
                Row(Modifier.padding(top = 26.dp, start = 4.dp, bottom = 24.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Eyebrow(today.format(DateTimeFormatter.ofPattern("EEEE, MMMM d")))
                        Spacer(Modifier.height(8.dp))
                        Text(greeting(LocalTime.now().hour), style = MaterialTheme.typography.displaySmall)
                        Spacer(Modifier.height(8.dp))
                        Text(summary(agenda, overdue), style = MaterialTheme.typography.bodyLarge, color = c.ink2)
                    }
                    Spacer(Modifier.width(12.dp))
                    ProgressRing(agenda.progress) {
                        if (agenda.total > 0 && agenda.done == agenda.total) {
                            Icon(Icons.Rounded.Check, "All done", tint = c.accent, modifier = Modifier.size(30.dp))
                        } else {
                            Text("${(agenda.progress * 100).toInt()}%", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = c.ink)
                        }
                    }
                }
            }
            item(key = "composer") {
                val pickable = data.lists
                Composer(
                    onAdd = { title, p, due, listId ->
                        onAddTask(listId ?: OnwardStore.GENERAL, title, p, due ?: today)
                    },
                    hint = "Add something for today",
                    duePlaceholder = "Today",
                    lists = pickable,
                    defaultListId = pickable.firstOrNull { it.id == OnwardStore.GENERAL }?.id ?: pickable.firstOrNull()?.id,
                )
            }

            // Habits
            item(key = "habitsHeader") {
                SectionHeader(
                    "Habits",
                    Modifier.padding(top = 30.dp, start = 4.dp, bottom = 6.dp),
                    action = if (data.habits.isEmpty()) null else "See all",
                    onAction = onOpenHabits,
                )
            }
            item(key = "habits") {
                if (data.habits.isEmpty()) {
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .onwardCard()
                            .clickable(onClick = onNewHabit)
                            .padding(18.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(Modifier.size(42.dp).background(c.accentSoft, RoundedCornerShape(13.dp)), contentAlignment = Alignment.Center) {
                            Icon(Icons.Rounded.LocalFireDepartment, null, tint = c.accent)
                        }
                        Spacer(Modifier.width(14.dp))
                        Column(Modifier.weight(1f)) {
                            Text("Start a habit", style = MaterialTheme.typography.titleMedium)
                            Text("Something small you'll do every day.", style = MaterialTheme.typography.bodySmall)
                        }
                        Icon(Icons.Rounded.Add, null, tint = c.muted)
                    }
                } else {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        contentPadding = PaddingValues(vertical = 8.dp),
                    ) {
                        items(data.habits, key = { it.id }) { h -> HabitChip(h, today, { onToggleHabit(h) }) }
                    }
                }
            }

            // Due
            item(key = "dueHeader") {
                SectionHeader(
                    if (overdue > 0) "Due & overdue" else "Due today",
                    Modifier.padding(top = 24.dp, start = 4.dp, bottom = 6.dp),
                )
            }
            if (agenda.tasks.isEmpty()) {
                item(key = "dueEmpty") {
                    Box(Modifier.fillMaxWidth().onwardCard()) {
                        EmptyState("A clear day", "Nothing is due today. Add something above, or enjoy the calm.")
                    }
                }
            } else {
                items(agenda.tasks, key = { it.id }) { t ->
                    SwipeableTask(
                        task = t,
                        list = data.list(t.listId),
                        today = today,
                        onToggle = { onToggleTask(t) },
                        onDelete = { onDeleteTask(t) },
                        onClick = { onEditTask(t) },
                        modifier = Modifier.padding(bottom = 8.dp).animateItem(),
                    )
                }
            }

            // Spaces
            item(key = "spacesHeader") {
                SectionHeader("Spaces", Modifier.padding(top = 24.dp, start = 4.dp, bottom = 6.dp))
            }
            item(key = "spaces") {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    SpaceCard(Space.PERSONAL, Icons.Rounded.Spa, data, Modifier.weight(1f)) { onOpenSpace(Space.PERSONAL) }
                    SpaceCard(Space.WORK, Icons.Rounded.WorkOutline, data, Modifier.weight(1f)) { onOpenSpace(Space.WORK) }
                }
            }
            item(key = "footer") {
                Text(
                    "Saved on this device",
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.fillMaxWidth().padding(top = 28.dp),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                )
            }
        }
    }
}

private fun summary(a: TodayAgenda, overdue: Int): String {
    val leftTasks = a.totalTasks - a.doneTasks
    val leftHabits = a.totalHabits - a.doneHabits
    if (a.total == 0) return "Nothing scheduled. A blank page."
    if (leftTasks == 0 && leftHabits == 0) return "All done for today. Nicely played."
    val parts = buildList {
        if (leftTasks > 0) add("$leftTasks ${if (leftTasks == 1) "task" else "tasks"} to go")
        if (leftHabits > 0) add("$leftHabits ${if (leftHabits == 1) "habit" else "habits"} left")
        if (overdue > 0) add("$overdue late")
    }
    return parts.joinToString(" · ")
}

@Composable
private fun HabitChip(habit: Habit, today: LocalDate, onToggle: () -> Unit) {
    val c = Onward.colors
    val haptics = LocalHapticFeedback.current
    val done = habit.isDoneOn(today)
    val streak = habit.currentStreak(today)
    val tint = c.tint(habit.color)
    Row(
        Modifier
            .widthIn(min = 150.dp, max = 220.dp)
            .onwardCard(RoundedCornerShape(18.dp), elevation = 4.dp)
            .clickable(role = Role.Checkbox) {
                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                onToggle()
            }
            .padding(start = 12.dp, end = 16.dp, top = 12.dp, bottom = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CheckCircle(done, tint, fill = tint, tickColor = c.surface, size = 30.dp)
        Spacer(Modifier.width(11.dp))
        Column {
            Text(habit.name, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.LocalFireDepartment, null, tint = if (streak > 0) c.medium else c.none,
                    modifier = Modifier.size(13.dp))
                Spacer(Modifier.width(3.dp))
                Text(if (streak == 0) "No streak yet" else "$streak ${if (streak == 1) "day" else "days"}",
                    style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
private fun SpaceCard(space: Space, icon: ImageVector, data: AppData, modifier: Modifier, onClick: () -> Unit) {
    val c = Onward.colors
    val lists = data.lists(space)
    val ids = lists.map { it.id }.toSet()
    val open = data.tasks.count { it.listId in ids && !it.done }
    Column(
        modifier
            .onwardCard()
            .clickable(onClick = onClick)
            .padding(16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(38.dp).clip(RoundedCornerShape(12.dp)).background(c.accentSoft),
                contentAlignment = Alignment.Center,
            ) { Icon(icon, null, tint = c.accent, modifier = Modifier.size(20.dp)) }
            Spacer(Modifier.weight(1f))
            Icon(Icons.AutoMirrored.Rounded.ArrowForward, null, tint = c.muted, modifier = Modifier.size(18.dp))
        }
        Spacer(Modifier.height(16.dp))
        Text(space.label, style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(2.dp))
        Text(
            "${lists.size} ${if (lists.size == 1) "list" else "lists"} · $open open",
            style = MaterialTheme.typography.bodySmall,
        )
    }
}
