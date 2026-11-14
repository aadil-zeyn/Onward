package com.aadil.onward.data

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.aadil.onward.model.AppData
import com.aadil.onward.model.Habit
import com.aadil.onward.model.Priority
import com.aadil.onward.model.Space
import com.aadil.onward.model.Task
import com.aadil.onward.model.TaskList
import com.aadil.onward.model.ThemePref
import com.aadil.onward.model.key
import com.aadil.onward.model.toggled
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.time.LocalDate

/** Something removed, and where it was, so "Undo" can put it back. */
data class Removed<T>(val item: T, val index: Int)

/** A deleted list along with the tasks that went with it. */
data class RemovedList(val list: Removed<TaskList>, val tasks: List<Removed<Task>>)

/**
 * Holds all of Onward's state and saves it after every change. Pure state
 * changes live in [Actions] so they can be tested without Android.
 */
class OnwardViewModel(app: Application) : AndroidViewModel(app) {

    private val store = OnwardStore(app)
    private val saveLock = Mutex()
    private val _data = MutableStateFlow(store.load(LegacyPrefs.read(app)))
    val data: StateFlow<AppData> = _data.asStateFlow()

    private fun change(block: (AppData) -> AppData) {
        _data.update(block)
        val snapshot = _data.value
        viewModelScope.launch(Dispatchers.IO) {
            saveLock.withLock { if (snapshot === _data.value) store.save(snapshot) }
        }
    }

    // Tasks

    fun addTask(listId: String, title: String, priority: Priority, due: LocalDate?) =
        change { Actions.addTask(it, listId, title, priority, due) }

    fun toggleTask(id: String) = change { Actions.toggleTask(it, id) }

    fun updateTask(id: String, title: String, notes: String, priority: Priority, due: LocalDate?, listId: String) =
        change { d ->
            d.copy(tasks = d.tasks.map {
                if (it.id == id) it.copy(title = title, notes = notes, priority = priority, due = due?.key(), listId = listId) else it
            })
        }

    fun deleteTask(id: String): List<Removed<Task>> = removeTasks { it.id == id }

    fun clearDone(listId: String): List<Removed<Task>> = removeTasks { it.listId == listId && it.done }

    private fun removeTasks(test: (Task) -> Boolean): List<Removed<Task>> {
        val removed = _data.value.tasks.withIndex().filter { test(it.value) }.map { Removed(it.value, it.index) }
        if (removed.isNotEmpty()) change { d -> d.copy(tasks = d.tasks.filterNot(test)) }
        return removed
    }

    fun restoreTasks(removed: List<Removed<Task>>) = change { Actions.restore(it, removed) }

    // Lists

    fun addList(name: String, space: Space, color: Long, icon: String): String {
        val id = OnwardStore.newId()
        change { d -> d.copy(lists = d.lists + TaskList(id, name, space, color, icon)) }
        return id
    }

    fun updateList(id: String, name: String, color: Long, icon: String) = change { d ->
        d.copy(lists = d.lists.map { if (it.id == id) it.copy(name = name, color = color, icon = icon) else it })
    }

    fun deleteList(id: String): RemovedList? {
        val d = _data.value
        val index = d.lists.indexOfFirst { it.id == id }
        if (index < 0) return null
        val tasks = d.tasks.withIndex().filter { it.value.listId == id }.map { Removed(it.value, it.index) }
        change { cur -> cur.copy(lists = cur.lists.filterNot { it.id == id }, tasks = cur.tasks.filterNot { it.listId == id }) }
        return RemovedList(Removed(d.lists[index], index), tasks)
    }

    fun restoreList(removed: RemovedList) = change { d ->
        val lists = if (d.lists.any { it.id == removed.list.item.id }) d.lists
        else d.lists.toMutableList().apply { add(removed.list.index.coerceIn(0, size), removed.list.item) }
        Actions.restore(d.copy(lists = lists), removed.tasks)
    }

    // Habits

    fun addHabit(name: String, color: Long) = change { d ->
        d.copy(habits = d.habits + Habit(OnwardStore.newId(), name, color, createdAt = System.currentTimeMillis()))
    }

    fun updateHabit(id: String, name: String, color: Long) = change { d ->
        d.copy(habits = d.habits.map { if (it.id == id) it.copy(name = name, color = color) else it })
    }

    fun toggleHabit(id: String, day: LocalDate) = change { d ->
        d.copy(habits = d.habits.map { if (it.id == id) it.toggled(day) else it })
    }

    fun deleteHabit(id: String): Removed<Habit>? {
        val index = _data.value.habits.indexOfFirst { it.id == id }
        if (index < 0) return null
        val habit = _data.value.habits[index]
        change { d -> d.copy(habits = d.habits.filterNot { it.id == id }) }
        return Removed(habit, index)
    }

    fun restoreHabit(r: Removed<Habit>) = change { d ->
        if (d.habits.any { it.id == r.item.id }) d
        else d.copy(habits = d.habits.toMutableList().apply { add(r.index.coerceIn(0, size), r.item) })
    }

    // Settings

    fun setTheme(pref: ThemePref) = change { it.copy(theme = pref) }
}

/** State transitions shared by the view model and tests. */
object Actions {
    fun addTask(d: AppData, listId: String, title: String, priority: Priority, due: LocalDate?): AppData {
        val task = Task(
            id = OnwardStore.newId(),
            listId = listId,
            title = title.trim(),
            priority = priority,
            due = due?.key(),
            createdAt = System.currentTimeMillis(),
        )
        return d.copy(tasks = listOf(task) + d.tasks)
    }

    fun toggleTask(d: AppData, id: String): AppData = d.copy(tasks = d.tasks.map {
        if (it.id != id) it
        else it.copy(done = !it.done, completedAt = if (!it.done) System.currentTimeMillis() else null)
    })

    /** Puts removed tasks back where they were. */
    fun restore(d: AppData, removed: List<Removed<Task>>): AppData {
        val tasks = d.tasks.toMutableList()
        for (r in removed.sortedBy { it.index }) {
            if (tasks.any { it.id == r.item.id }) continue
            tasks.add(r.index.coerceIn(0, tasks.size), r.item)
        }
        return d.copy(tasks = tasks)
    }
}
