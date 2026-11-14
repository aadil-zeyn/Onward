package com.aadil.onward

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import com.aadil.onward.data.OnwardStore
import com.aadil.onward.model.AppData
import com.aadil.onward.model.Habit
import com.aadil.onward.model.ListPalette
import com.aadil.onward.model.Priority
import com.aadil.onward.model.Space
import com.aadil.onward.model.Task
import com.aadil.onward.ui.screens.HabitsScreen
import com.aadil.onward.ui.screens.ListScreen
import com.aadil.onward.ui.screens.SpaceScreen
import com.aadil.onward.ui.screens.TodayScreen
import com.aadil.onward.ui.theme.Onward
import com.aadil.onward.ui.theme.OnwardTheme
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.ParameterizedRobolectricTestRunner
import org.robolectric.ParameterizedRobolectricTestRunner.Parameters
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.time.LocalDate

/**
 * Renders each screen with sample data, light and dark. Run
 * `./gradlew recordRoborazziDebug` to write the images to docs/screens.
 */
@RunWith(ParameterizedRobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "w400dp-h870dp-xxhdpi")
class ScreenshotTest(private val dark: Boolean) {
    companion object {
        @JvmStatic
        @Parameters(name = "dark={0}")
        fun themes() = listOf(arrayOf<Any>(false), arrayOf<Any>(true))
    }

    @get:Rule val compose = createComposeRule()

    private val today = LocalDate.now()

    private fun day(offset: Long) = today.plusDays(offset).toString()

    private fun days(vararg offsets: Long) = offsets.map { today.minusDays(it).toString() }.toSet()

    private val sample = AppData(
        lists = OnwardStore.defaultLists(),
        tasks = listOf(
            Task("1", "graphql", "Add pagination to the orders resolver", priority = Priority.HIGH, due = day(0)),
            Task("2", "wbs", "Split Phase 2 into work packages", priority = Priority.MEDIUM, due = day(-1),
                notes = "Align with the Q1 plan"),
            Task("3", "general", "Renew passport", priority = Priority.MEDIUM, due = day(0)),
            Task("4", "mongodb", "Add a compound index on (tenantId, createdAt)", priority = Priority.LOW, due = day(2)),
            Task("5", "neo4j", "Model user → project relationships", due = day(5)),
            Task("6", "graphql", "Write schema docs for Product", done = true, completedAt = 1),
            Task("7", "graphql", "Set up Apollo federation gateway", priority = Priority.MEDIUM),
            Task("8", "graphql", "Rate limit the public endpoint", priority = Priority.LOW, due = day(1)),
            Task("9", "personal", "Call grandma", priority = Priority.HIGH, due = day(0)),
            Task("10", "misc", "Expense report for October"),
            Task("11", "graphql", "Fix N+1 in the reviews field", done = true, completedAt = 1),
        ),
        habits = listOf(
            Habit("h1", "Morning walk", ListPalette.EVERGREEN, days(0, 1, 2, 3, 4, 6, 7, 8)),
            Habit("h2", "Read 20 pages", ListPalette.BLUE, days(1, 2, 3, 5)),
            Habit("h3", "No phone after 10", ListPalette.VIOLET, days(0, 2, 3, 4, 5, 6)),
            Habit("h4", "Drink water", ListPalette.AMBER, days(1, 2)),
        ),
    )

    private fun shoot(name: String, content: @Composable () -> Unit) {
        compose.setContent {
            OnwardTheme(dark) {
                Box(Modifier.fillMaxSize().background(Onward.colors.bg)) { content() }
            }
        }
        compose.onRoot().captureRoboImage("../docs/screens/$name-${if (dark) "dark" else "light"}.png")
    }

    @Test fun today() = shoot("today") {
        TodayScreen(sample, today, { _, _, _, _ -> }, {}, {}, {}, {}, {}, {}, {}, {})
    }

    @Test fun work() = shoot("work") {
        SpaceScreen(Space.WORK, sample, today, {}, {}, {}, {})
    }

    @Test fun personal() = shoot("personal") {
        SpaceScreen(Space.PERSONAL, sample, today, {}, {}, {}, {})
    }

    @Test fun list() = shoot("list") {
        ListScreen(sample.list("graphql")!!, sample, today, {}, { _, _, _ -> }, {}, {}, {}, {}, {}, {})
    }

    @Test fun habits() = shoot("habits") {
        HabitsScreen(sample.habits, today, {}, { _, _ -> }, {}, {})
    }
}
