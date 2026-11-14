package com.aadil.onward.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aadil.onward.model.AppData
import com.aadil.onward.model.Space
import com.aadil.onward.model.TaskList
import com.aadil.onward.model.bestStreak
import com.aadil.onward.model.dueDate
import com.aadil.onward.model.isDoneOn
import com.aadil.onward.model.isDueBy
import com.aadil.onward.ui.components.Backdrop
import com.aadil.onward.ui.components.DashedOutline
import com.aadil.onward.ui.components.Eyebrow
import com.aadil.onward.ui.components.IconTile
import com.aadil.onward.ui.components.ListIcons
import com.aadil.onward.ui.components.ProgressRing
import com.aadil.onward.ui.components.SectionHeader
import com.aadil.onward.ui.components.onwardCard
import com.aadil.onward.ui.theme.Onward
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale

/** A space (Personal or Work): its lists as cards, and on Personal, habits. */
@Composable
fun SpaceScreen(
    space: Space,
    data: AppData,
    today: LocalDate,
    onOpenList: (TaskList) -> Unit,
    onEditList: (TaskList) -> Unit,
    onNewList: () -> Unit,
    onOpenHabits: () -> Unit,
) {
    val c = Onward.colors
    val lists = data.lists(space)
    val ids = lists.map { it.id }.toSet()
    val tasks = data.tasks.filter { it.listId in ids }
    val open = tasks.count { !it.done }
    val due = tasks.count { it.isDueBy(today) }

    Box(Modifier.fillMaxSize()) {
        Backdrop()
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 32.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item(span = { GridItemSpan(maxLineSpan) }, key = "header") {
                Column(Modifier.statusBarsPadding().padding(top = 28.dp, start = 4.dp, bottom = 8.dp)) {
                    Eyebrow(if (space == Space.WORK) "Workspace" else "Life outside work")
                    Spacer(Modifier.height(8.dp))
                    Text(space.label, style = MaterialTheme.typography.displaySmall)
                    Spacer(Modifier.height(8.dp))
                    Text(
                        buildList {
                            add("$open open")
                            if (due > 0) add("$due due")
                            add("${lists.size} ${if (lists.size == 1) "list" else "lists"}")
                        }.joinToString(" · "),
                        style = MaterialTheme.typography.bodyLarge,
                        color = c.ink2,
                    )
                }
            }
            if (space == Space.PERSONAL) {
                item(span = { GridItemSpan(maxLineSpan) }, key = "habits") {
                    HabitsCard(data, today, onOpenHabits)
                }
            }
            item(span = { GridItemSpan(maxLineSpan) }, key = "listsHeader") {
                SectionHeader(
                    if (space == Space.WORK) "Projects" else "To-dos",
                    Modifier.padding(start = 4.dp, top = 8.dp),
                    action = "New list",
                    onAction = onNewList,
                )
            }
            items(lists, key = { it.id }) { list ->
                ListCard(list, data, today, { onOpenList(list) }, { onEditList(list) }, Modifier.animateItem())
            }
            item(key = "new") {
                Box(
                    Modifier
                        .height(156.dp)
                        .clip(RoundedCornerShape(22.dp))
                        .combinedClickable(onClick = onNewList),
                    contentAlignment = Alignment.Center,
                ) {
                    DashedOutline(Modifier.fillMaxSize())
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(Modifier.size(40.dp).background(c.surface2, CircleShape), contentAlignment = Alignment.Center) {
                            Icon(Icons.Rounded.Add, null, tint = c.ink2)
                        }
                        Spacer(Modifier.height(8.dp))
                        Text("New list", style = MaterialTheme.typography.labelLarge, color = c.ink2)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ListCard(
    list: TaskList,
    data: AppData,
    today: LocalDate,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val c = Onward.colors
    val haptics = LocalHapticFeedback.current
    val tasks = data.tasksIn(list.id)
    val open = tasks.count { !it.done }
    val done = tasks.size - open
    val overdue = tasks.count { !it.done && it.dueDate?.isBefore(today) == true }
    val dueToday = tasks.count { !it.done && it.dueDate == today }
    Column(
        modifier
            .height(156.dp)
            .onwardCard()
            .combinedClickable(
                onClick = onClick,
                onLongClick = { haptics.performHapticFeedback(HapticFeedbackType.LongPress); onLongClick() },
                onLongClickLabel = "Edit list",
            )
            .padding(16.dp),
    ) {
        Row(verticalAlignment = Alignment.Top) {
            IconTile(ListIcons.of(list.icon), list.color)
            Spacer(Modifier.weight(1f))
            when {
                overdue > 0 -> Badge("$overdue late", c.high, c.dangerSoft)
                dueToday > 0 -> Badge("$dueToday today", c.accent, c.accentSoft)
            }
        }
        Spacer(Modifier.weight(1f))
        Text(
            list.name,
            style = MaterialTheme.typography.titleLarge.copy(fontSize = 19.sp),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Spacer(Modifier.height(2.dp))
        Text(
            when {
                tasks.isEmpty() -> "No tasks yet"
                open == 0 -> "All ${tasks.size} done"
                else -> "$open open"
            },
            style = MaterialTheme.typography.bodySmall,
        )
        Spacer(Modifier.height(10.dp))
        val frac = if (tasks.isEmpty()) 0f else done.toFloat() / tasks.size
        Box(Modifier.fillMaxWidth().height(5.dp).clip(CircleShape).background(c.line)) {
            Box(Modifier.fillMaxHeight().fillMaxWidth(frac).clip(CircleShape).background(c.tint(list.color)))
        }
    }
}

@Composable
private fun Badge(text: String, fg: androidx.compose.ui.graphics.Color, bg: androidx.compose.ui.graphics.Color) {
    Text(
        text,
        fontSize = 11.5.sp,
        fontWeight = FontWeight.SemiBold,
        color = fg,
        modifier = Modifier.background(bg, CircleShape).padding(horizontal = 8.dp, vertical = 3.dp),
    )
}

/** The Habits entry on Personal: today's progress and the past week. */
@Composable
private fun HabitsCard(data: AppData, today: LocalDate, onClick: () -> Unit) {
    val c = Onward.colors
    val habits = data.habits
    val doneToday = habits.count { it.isDoneOn(today) }
    val best = habits.maxOfOrNull { it.bestStreak() } ?: 0
    Column(
        Modifier
            .fillMaxWidth()
            .onwardCard()
            .combinedClickable(onClick = onClick)
            .padding(18.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(44.dp).background(c.accentSoft, RoundedCornerShape(14.dp)), contentAlignment = Alignment.Center) {
                Icon(Icons.Rounded.LocalFireDepartment, null, tint = c.accent)
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text("Habit tracker", style = MaterialTheme.typography.titleLarge)
                Text(
                    if (habits.isEmpty()) "Build something that sticks"
                    else "$doneToday of ${habits.size} today" + if (best > 1) " · best streak $best" else "",
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            ProgressRing(
                if (habits.isEmpty()) 0f else doneToday.toFloat() / habits.size,
                size = 44.dp,
                stroke = 5.dp,
            )
        }
        Spacer(Modifier.height(16.dp))
        // The last seven days, as bars showing how many habits were kept.
        Row(Modifier.fillMaxWidth().height(64.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            for (i in 6 downTo 0) {
                val day = today.minusDays(i.toLong())
                val frac = if (habits.isEmpty()) 0f else habits.count { it.isDoneOn(day) }.toFloat() / habits.size
                Column(Modifier.weight(1f).fillMaxHeight(), horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(Modifier.weight(1f).width(22.dp).clip(RoundedCornerShape(7.dp)).background(c.surface2),
                        contentAlignment = Alignment.BottomCenter) {
                        if (frac > 0f) {
                            Box(Modifier.fillMaxWidth().fillMaxHeight(frac).clip(RoundedCornerShape(7.dp))
                                .background(if (i == 0) c.accent else c.accent.copy(alpha = 0.45f)))
                        }
                    }
                    Spacer(Modifier.height(5.dp))
                    Text(
                        day.dayOfWeek.getDisplayName(TextStyle.NARROW, Locale.getDefault()),
                        fontSize = 11.sp,
                        fontWeight = if (i == 0) FontWeight.SemiBold else FontWeight.Medium,
                        color = if (i == 0) c.ink else c.muted,
                    )
                }
            }
        }
    }
}
