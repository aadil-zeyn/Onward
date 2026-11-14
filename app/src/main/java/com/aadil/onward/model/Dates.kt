package com.aadil.onward.model

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeParseException
import java.time.format.TextStyle
import java.time.temporal.ChronoUnit
import java.time.temporal.TemporalAdjusters
import java.util.Locale

fun LocalDate.key(): String = toString() // ISO yyyy-mm-dd

fun parseDayKey(s: String?): LocalDate? =
    if (s == null) null else try {
        LocalDate.parse(s)
    } catch (_: DateTimeParseException) {
        null
    }

val Task.dueDate: LocalDate? get() = parseDayKey(due)

/** The Monday after today (or the one a week out, if today is Monday). */
fun nextMonday(today: LocalDate): LocalDate = today.with(TemporalAdjusters.next(DayOfWeek.MONDAY))

enum class DueState { OVERDUE, TODAY, UPCOMING }

data class DueInfo(val label: String, val state: DueState)

private fun LocalDate.monthDay(): String =
    "${month.getDisplayName(TextStyle.SHORT, Locale.getDefault())} $dayOfMonth"

/** A friendly label for a due date: Today, Tomorrow, Thursday, Nov 12… */
fun describeDue(due: LocalDate, today: LocalDate = LocalDate.now()): DueInfo {
    val diff = ChronoUnit.DAYS.between(today, due)
    val label = when {
        diff == 0L -> "Today"
        diff == 1L -> "Tomorrow"
        diff == -1L -> "Yesterday"
        diff in 2..6 -> due.dayOfWeek.getDisplayName(TextStyle.FULL, Locale.getDefault())
        due.year == today.year -> due.monthDay()
        else -> "${due.monthDay()}, ${due.year}"
    }
    val state = when {
        diff < 0 -> DueState.OVERDUE
        diff == 0L -> DueState.TODAY
        else -> DueState.UPCOMING
    }
    return DueInfo(label, state)
}

/** True when the task still needs doing and is due today or earlier. */
fun Task.isDueBy(today: LocalDate): Boolean {
    val d = dueDate ?: return false
    return !done && !d.isAfter(today)
}

fun Habit.isDoneOn(day: LocalDate): Boolean = day.key() in days

fun Habit.toggled(day: LocalDate): Habit {
    val k = day.key()
    return copy(days = if (k in days) days - k else days + k)
}

/**
 * Consecutive completed days ending today. If today isn't done yet the
 * streak counts back from yesterday, so it doesn't reset mid-day.
 */
fun Habit.currentStreak(today: LocalDate = LocalDate.now()): Int {
    var day = if (isDoneOn(today)) today else today.minusDays(1)
    var streak = 0
    while (isDoneOn(day)) {
        streak++
        day = day.minusDays(1)
    }
    return streak
}

/** The longest run of consecutive completed days, ever. */
fun Habit.bestStreak(): Int {
    val sorted = days.mapNotNull { parseDayKey(it) }.sorted()
    var best = 0
    var run = 0
    var prev: LocalDate? = null
    for (d in sorted) {
        run = if (prev != null && prev.plusDays(1) == d) run + 1 else 1
        if (run > best) best = run
        prev = d
    }
    return best
}

fun greeting(hour: Int): String = when (hour) {
    in 5..11 -> "Good morning"
    in 12..16 -> "Good afternoon"
    in 17..21 -> "Good evening"
    else -> "Still up?"
}
