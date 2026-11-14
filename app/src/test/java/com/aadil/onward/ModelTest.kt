package com.aadil.onward

import com.aadil.onward.data.Actions
import com.aadil.onward.data.LegacyPrefs
import com.aadil.onward.data.OnwardStore
import com.aadil.onward.data.Removed
import com.aadil.onward.model.AppData
import com.aadil.onward.model.DueState
import com.aadil.onward.model.Habit
import com.aadil.onward.model.Priority
import com.aadil.onward.model.Space
import com.aadil.onward.model.Task
import com.aadil.onward.model.ThemePref
import com.aadil.onward.model.bestStreak
import com.aadil.onward.model.currentStreak
import com.aadil.onward.model.describeDue
import com.aadil.onward.model.isDueBy
import com.aadil.onward.model.nextMonday
import com.aadil.onward.model.toggled
import com.aadil.onward.ui.screens.TodayAgenda
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.time.LocalDate

class ModelTest {
    @get:Rule val tmp = TemporaryFolder()

    private val today = LocalDate.of(2026, 11, 14) // a Saturday

    @Test fun dueLabels() {
        assertEquals("Today", describeDue(today, today).label)
        assertEquals(DueState.TODAY, describeDue(today, today).state)
        assertEquals("Tomorrow", describeDue(today.plusDays(1), today).label)
        assertEquals("Yesterday", describeDue(today.minusDays(1), today).label)
        assertEquals(DueState.OVERDUE, describeDue(today.minusDays(3), today).state)
        assertEquals("Tuesday", describeDue(today.plusDays(3), today).label)
        assertEquals("Jan 5, 2027", describeDue(LocalDate.of(2027, 1, 5), today).label)
    }

    @Test fun nextMondayIsAfterToday() {
        assertEquals(LocalDate.of(2026, 11, 16), nextMonday(today))
        assertEquals(LocalDate.of(2026, 11, 23), nextMonday(LocalDate.of(2026, 11, 16)))
    }

    @Test fun streakCountsBackFromYesterdayUntilTodayIsDone() {
        var h = Habit("h", "Read", 0)
        listOf(1L, 2L, 3L, 5L).forEach { h = h.toggled(today.minusDays(it)) }
        assertEquals(3, h.currentStreak(today))
        h = h.toggled(today)
        assertEquals(4, h.currentStreak(today))
        assertEquals(4, h.bestStreak())
        h = h.toggled(today)
        assertEquals(3, h.currentStreak(today))
    }

    @Test fun restorePutsTasksBackInPlace() {
        val tasks = (0..4).map { Task("t$it", "l", "Task $it") }
        val d = AppData(tasks = tasks)
        val removed = listOf(Removed(tasks[1], 1), Removed(tasks[3], 3))
        val after = d.copy(tasks = tasks.filterNot { it.id == "t1" || it.id == "t3" })
        assertEquals(tasks, Actions.restore(after, removed).tasks)
    }

    @Test fun addAndToggle() {
        var d = AppData(lists = OnwardStore.defaultLists())
        d = Actions.addTask(d, "graphql", "  Add resolvers ", Priority.HIGH, today)
        val t = d.tasks.single()
        assertEquals("Add resolvers", t.title)
        assertTrue(t.isDueBy(today))
        d = Actions.toggleTask(d, t.id)
        assertTrue(d.tasks.single().done)
        assertNotNull(d.tasks.single().completedAt)
        assertFalse(d.tasks.single().isDueBy(today))
    }

    @Test fun defaultListsCoverPersonalAndWork() {
        val lists = OnwardStore.defaultLists()
        assertEquals(listOf("General", "Personal"), lists.filter { it.space == Space.PERSONAL }.map { it.name })
        assertEquals(
            listOf("WBS", "GraphQL App", "MongoDB", "Neo4j", "Misc"),
            lists.filter { it.space == Space.WORK }.map { it.name },
        )
    }

    @Test fun importsFlutterData() {
        val legacy = LegacyPrefs(
            tasksJson = """[{"id":"1","title":"Old task","done":false,"priority":"high","due":"2026-11-14","created":1700000000000,"completed":null},
                            {"id":"2","title":"Finished","done":true,"priority":"none","due":null,"created":1700000000000,"completed":1700000100000}]""",
            habitsJson = """[{"id":"h","name":"Walk","color":4282350936,"days":["2026-11-13","2026-11-12"]}]""",
            themeMode = "dark",
        )
        val d = OnwardStore.importFlutter(legacy)!!
        assertEquals(ThemePref.DARK, d.theme)
        assertEquals(listOf("Old task", "Finished"), d.tasks.map { it.title })
        assertTrue(d.tasks.all { it.listId == OnwardStore.GENERAL })
        assertEquals(Priority.HIGH, d.tasks[0].priority)
        assertEquals("2026-11-14", d.tasks[0].due)
        assertTrue(d.tasks[1].done)
        assertEquals(0xFF3F7D58, d.habits.single().color)
        assertEquals(2, d.habits.single().currentStreak(today))
    }

    @Test fun storeRoundTripsAndSeedsFirstRun() {
        val file = File(tmp.root, "onward.json")
        val store = OnwardStore(file)
        val first = store.load(legacy = null, today = today)
        assertTrue(file.exists())
        assertEquals(7, first.lists.size)
        assertTrue(first.tasks.isNotEmpty())
        val changed = Actions.addTask(first, "neo4j", "Model the graph", Priority.LOW, null)
        store.save(changed)
        assertEquals(changed, OnwardStore(file).load(today = today))
    }

    @Test fun firstRunPrefersFlutterData() {
        val store = OnwardStore(File(tmp.root, "onward.json"))
        val d = store.load(LegacyPrefs("""[{"id":"1","title":"Keep me"}]""", null, null), today)
        assertEquals(listOf("Keep me"), d.tasks.map { it.title })
    }

    @Test fun todayAgendaCountsTasksAndHabits() {
        val d = AppData(
            lists = OnwardStore.defaultLists(),
            tasks = listOf(
                Task("a", "general", "Due today", due = today.toString()),
                Task("b", "wbs", "Overdue", due = today.minusDays(2).toString()),
                Task("c", "misc", "Later", due = today.plusDays(2).toString()),
                Task("d", "misc", "Undated"),
            ),
            habits = listOf(Habit("h1", "Walk", 0, setOf(today.toString())), Habit("h2", "Read", 0)),
        )
        val a = TodayAgenda.of(d, today)
        assertEquals(listOf("b", "a"), a.tasks.map { it.id })
        assertEquals(4, a.total)
        assertEquals(1, a.done)
    }
}
