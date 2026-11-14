package com.aadil.onward.model

import kotlinx.serialization.Serializable

/** The two halves of life Onward keeps apart. */
@Serializable
enum class Space(val label: String) {
    PERSONAL("Personal"),
    WORK("Work"),
}

/** How important a task is. Drives the colour of its checkbox and flag. */
@Serializable
enum class Priority(val label: String) {
    NONE("None"),
    LOW("Low"),
    MEDIUM("Medium"),
    HIGH("High"),
}

@Serializable
enum class ThemePref { SYSTEM, LIGHT, DARK }

/**
 * A named list of to-dos inside a [Space]: "General", "Personal", or a
 * work stream such as "GraphQL App".
 */
@Serializable
data class TaskList(
    val id: String,
    val name: String,
    val space: Space,
    /** ARGB, one of [ListPalette.colors]. */
    val color: Long,
    /** A key from the icon set in the UI layer, e.g. "database". */
    val icon: String,
)

/** A to-do item. Due dates are calendar days (`yyyy-mm-dd`), no time. */
@Serializable
data class Task(
    val id: String,
    val listId: String,
    val title: String,
    val notes: String = "",
    val done: Boolean = false,
    val priority: Priority = Priority.NONE,
    val due: String? = null,
    val createdAt: Long = 0,
    val completedAt: Long? = null,
)

/** A habit the user wants to keep, plus the days they completed it. */
@Serializable
data class Habit(
    val id: String,
    val name: String,
    val color: Long,
    /** Completed days as `yyyy-mm-dd` keys. */
    val days: Set<String> = emptySet(),
    val createdAt: Long = 0,
)

/** Everything Onward saves, in one document. */
@Serializable
data class AppData(
    val version: Int = 1,
    val lists: List<TaskList> = emptyList(),
    val tasks: List<Task> = emptyList(),
    val habits: List<Habit> = emptyList(),
    val theme: ThemePref = ThemePref.SYSTEM,
) {
    fun list(id: String): TaskList? = lists.firstOrNull { it.id == id }
    fun lists(space: Space): List<TaskList> = lists.filter { it.space == space }
    fun tasksIn(listId: String): List<Task> = tasks.filter { it.listId == listId }
}

/** The colours lists and habits can wear. Tuned to sit on the warm paper. */
object ListPalette {
    const val EVERGREEN = 0xFF2F7A5F
    const val BLUE = 0xFF2F6FB0
    const val RUST = 0xFFB5532E
    const val VIOLET = 0xFF8A4FB0
    const val AMBER = 0xFFC2902A
    const val ROSE = 0xFFC0436B
    const val TEAL = 0xFF2E8A8A
    const val SLATE = 0xFF5B6B7A

    val colors = listOf(EVERGREEN, BLUE, RUST, VIOLET, AMBER, ROSE, TEAL, SLATE)
}
