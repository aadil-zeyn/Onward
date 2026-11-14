package com.aadil.onward.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aadil.onward.model.Habit
import com.aadil.onward.model.bestStreak
import com.aadil.onward.model.currentStreak
import com.aadil.onward.model.isDoneOn
import com.aadil.onward.ui.components.Backdrop
import com.aadil.onward.ui.components.CheckCircle
import com.aadil.onward.ui.components.EmptyState
import com.aadil.onward.ui.components.Eyebrow
import com.aadil.onward.ui.components.InkButton
import com.aadil.onward.ui.components.ProgressRing
import com.aadil.onward.ui.components.onwardCard
import com.aadil.onward.ui.theme.Onward
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale

@Composable
fun HabitsScreen(
    habits: List<Habit>,
    today: LocalDate,
    onBack: () -> Unit,
    onToggle: (Habit, LocalDate) -> Unit,
    onEdit: (Habit) -> Unit,
    onNew: () -> Unit,
) {
    val c = Onward.colors
    val doneToday = habits.count { it.isDoneOn(today) }
    Box(Modifier.fillMaxSize()) {
        Backdrop()
        LazyColumn(
            Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item(key = "bar") {
                Row(Modifier.statusBarsPadding().padding(top = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Back", tint = c.ink) }
                    Spacer(Modifier.weight(1f))
                    IconButton(onClick = onNew) { Icon(Icons.Rounded.Add, "New habit", tint = c.ink) }
                }
            }
            item(key = "hero") {
                Row(Modifier.padding(start = 4.dp, top = 6.dp, bottom = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Eyebrow("Personal")
                        Spacer(Modifier.height(8.dp))
                        Text("Habits", style = MaterialTheme.typography.displaySmall)
                        Spacer(Modifier.height(8.dp))
                        Text(
                            when {
                                habits.isEmpty() -> "Small things, every day."
                                doneToday == habits.size -> "All kept today. Lovely."
                                else -> "$doneToday of ${habits.size} kept today"
                            },
                            style = MaterialTheme.typography.bodyLarge,
                            color = c.ink2,
                        )
                    }
                    ProgressRing(if (habits.isEmpty()) 0f else doneToday.toFloat() / habits.size, size = 70.dp) {
                        Text("$doneToday/${habits.size}", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = c.ink)
                    }
                }
            }
            if (habits.isEmpty()) {
                item(key = "empty") {
                    Column(Modifier.fillMaxWidth().onwardCard(), horizontalAlignment = Alignment.CenterHorizontally) {
                        EmptyState("Start something small", "Add a habit you want to keep, then check it off each day.")
                        InkButton("New habit", onNew, icon = Icons.Rounded.Add, modifier = Modifier.padding(bottom = 24.dp))
                    }
                }
            } else {
                items(habits, key = { it.id }) { h ->
                    HabitCard(h, today, { day -> onToggle(h, day) }, { onEdit(h) }, Modifier.animateItem())
                }
                item(key = "hint") {
                    Text(
                        "Tap a day to mark it · tap a habit's name to edit it",
                        style = MaterialTheme.typography.bodySmall,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        modifier = Modifier.fillMaxWidth().padding(top = 6.dp).navigationBarsPadding(),
                    )
                }
            }
        }
    }
}

@Composable
private fun HabitCard(
    habit: Habit,
    today: LocalDate,
    onToggle: (LocalDate) -> Unit,
    onEdit: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val c = Onward.colors
    val haptics = LocalHapticFeedback.current
    val tint = c.tint(habit.color)
    val streak = habit.currentStreak(today)
    val best = habit.bestStreak()
    val doneToday = habit.isDoneOn(today)
    val week = (6 downTo 0).map { today.minusDays(it.toLong()) }
    val keptThisWeek = week.count { habit.isDoneOn(it) }

    Column(modifier.fillMaxWidth().onwardCard().padding(18.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(
                Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(10.dp))
                    .clickable(onClickLabel = "Edit habit", onClick = onEdit),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(10.dp).background(tint, CircleShape))
                    Spacer(Modifier.width(9.dp))
                    Text(habit.name, style = MaterialTheme.typography.titleLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                Spacer(Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Stat(Icons.Rounded.LocalFireDepartment, if (streak > 0) c.medium else c.none,
                        if (streak == 0) "No streak" else "$streak-day streak")
                    Spacer(Modifier.width(14.dp))
                    Stat(Icons.Rounded.EmojiEvents, c.muted, "Best $best")
                    Spacer(Modifier.width(14.dp))
                    Text("$keptThisWeek/7 this week", style = MaterialTheme.typography.bodySmall)
                }
            }
            Spacer(Modifier.width(12.dp))
            Box(
                Modifier
                    .size(52.dp)
                    .clip(CircleShape)
                    .clickable(role = Role.Checkbox, onClickLabel = "Done today") {
                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        onToggle(today)
                    },
                contentAlignment = Alignment.Center,
            ) {
                CheckCircle(doneToday, tint, fill = tint, tickColor = c.surface, size = 40.dp)
            }
        }
        Spacer(Modifier.height(16.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            week.forEach { day ->
                val done = habit.isDoneOn(day)
                val isToday = day == today
                val bg by animateColorAsState(if (done) tint else Color.Transparent, label = "day")
                Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        day.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.getDefault()).take(2),
                        fontSize = 11.5.sp,
                        fontWeight = if (isToday) FontWeight.SemiBold else FontWeight.Medium,
                        color = if (isToday) c.ink else c.muted,
                    )
                    Spacer(Modifier.height(6.dp))
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .aspectRatio(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (done) bg else c.surface2)
                            .border(
                                if (isToday && !done) 1.5.dp else 1.dp,
                                if (isToday && !done) tint.copy(alpha = 0.7f) else c.line,
                                RoundedCornerShape(12.dp),
                            )
                            .clickable(role = Role.Checkbox) {
                                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                onToggle(day)
                            },
                        contentAlignment = Alignment.Center,
                    ) {
                        if (done) Icon(Icons.Rounded.Check, null, tint = c.surface, modifier = Modifier.size(16.dp))
                        else Text("${day.dayOfMonth}", fontSize = 12.sp, color = c.muted)
                    }
                }
            }
        }
    }
}

@Composable
private fun Stat(icon: androidx.compose.ui.graphics.vector.ImageVector, tint: Color, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, tint = tint, modifier = Modifier.size(14.dp))
        Spacer(Modifier.width(3.dp))
        Text(text, style = MaterialTheme.typography.bodySmall)
    }
}
