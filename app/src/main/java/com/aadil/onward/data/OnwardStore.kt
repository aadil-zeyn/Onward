package com.aadil.onward.data

import android.content.Context
import android.util.Log
import com.aadil.onward.model.AppData
import com.aadil.onward.model.Habit
import com.aadil.onward.model.ListPalette
import com.aadil.onward.model.Priority
import com.aadil.onward.model.Space
import com.aadil.onward.model.Task
import com.aadil.onward.model.TaskList
import com.aadil.onward.model.ThemePref
import com.aadil.onward.model.key
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull
import java.io.File
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters
import java.util.UUID

/** Reads and writes everything as one JSON document in the app's files. */
class OnwardStore(private val file: File) {

    constructor(context: Context) : this(File(context.filesDir, FILE_NAME))

    /**
     * Loads saved data. On the very first launch it brings over tasks and
     * habits from the earlier Flutter version of Onward if there are any,
     * and otherwise starts with a few welcome tasks.
     */
    fun load(legacy: LegacyPrefs? = null, today: LocalDate = LocalDate.now()): AppData {
        if (file.exists()) {
            try {
                return json.decodeFromString(AppData.serializer(), file.readText())
            } catch (e: Exception) {
                Log.w(TAG, "Could not read saved data; keeping a copy and starting fresh", e)
                file.copyTo(File(file.parentFile, "$FILE_NAME.broken"), overwrite = true)
            }
        }
        val imported = legacy?.let { importFlutter(it) }
        val data = if (imported != null && (imported.tasks.isNotEmpty() || imported.habits.isNotEmpty())) {
            imported
        } else {
            seed(today).copy(theme = imported?.theme ?: ThemePref.SYSTEM)
        }
        save(data)
        return data
    }

    fun save(data: AppData) {
        val tmp = File(file.parentFile, "$FILE_NAME.tmp")
        tmp.writeText(json.encodeToString(AppData.serializer(), data))
        if (!tmp.renameTo(file)) {
            file.writeText(tmp.readText())
            tmp.delete()
        }
    }

    companion object {
        const val FILE_NAME = "onward.json"
        private const val TAG = "Onward"

        val json = Json {
            ignoreUnknownKeys = true
            encodeDefaults = true
            coerceInputValues = true
        }

        fun newId(): String = UUID.randomUUID().toString()

        const val GENERAL = "general"
        const val PERSONAL = "personal"

        /** The lists every new install starts with. */
        fun defaultLists(): List<TaskList> = listOf(
            TaskList(GENERAL, "General", Space.PERSONAL, ListPalette.EVERGREEN, "inbox"),
            TaskList(PERSONAL, "Personal", Space.PERSONAL, ListPalette.VIOLET, "heart"),
            TaskList("wbs", "WBS", Space.WORK, ListPalette.AMBER, "tree"),
            TaskList("graphql", "GraphQL App", Space.WORK, ListPalette.ROSE, "graphql"),
            TaskList("mongodb", "MongoDB", Space.WORK, ListPalette.TEAL, "database"),
            TaskList("neo4j", "Neo4j", Space.WORK, ListPalette.BLUE, "graph"),
            TaskList("misc", "Misc", Space.WORK, ListPalette.SLATE, "shapes"),
        )

        fun seed(today: LocalDate): AppData {
            val now = System.currentTimeMillis()
            fun t(list: String, title: String, p: Priority = Priority.NONE, due: LocalDate? = null, done: Boolean = false) =
                Task(newId(), list, title, priority = p, due = due?.key(), done = done, createdAt = now,
                    completedAt = if (done) now else null)
            return AppData(
                lists = defaultLists(),
                tasks = listOf(
                    t(GENERAL, "Welcome to Onward — tap the circle to complete a task"),
                    t(GENERAL, "Tap a task to edit it, or move it to another list", Priority.MEDIUM, today),
                    t(GENERAL, "Swipe right to complete, left to delete", Priority.HIGH),
                    t(PERSONAL, "Plan something fun for the weekend", Priority.LOW,
                        today.with(TemporalAdjusters.nextOrSame(java.time.DayOfWeek.SATURDAY))),
                    t("wbs", "Break the next milestone into work packages", Priority.MEDIUM),
                    t(GENERAL, "Open Onward for the first time", done = true),
                ),
            )
        }

        /**
         * Converts what the Flutter app saved (shared_preferences JSON
         * strings) into the new format. Tasks land in General.
         */
        fun importFlutter(prefs: LegacyPrefs): AppData? {
            val tasksRaw = prefs.tasksJson
            val habitsRaw = prefs.habitsJson
            if (tasksRaw == null && habitsRaw == null && prefs.themeMode == null) return null
            val theme = when (prefs.themeMode) {
                "light" -> ThemePref.LIGHT
                "dark" -> ThemePref.DARK
                else -> ThemePref.SYSTEM
            }
            val tasks = parseArray(tasksRaw).mapNotNull { o ->
                val id = o.str("id") ?: return@mapNotNull null
                val title = o.str("title") ?: return@mapNotNull null
                Task(
                    id = id,
                    listId = GENERAL,
                    title = title,
                    done = o["done"]?.jsonPrimitive?.booleanOrNull ?: false,
                    priority = when (o.str("priority")) {
                        "low" -> Priority.LOW
                        "medium" -> Priority.MEDIUM
                        "high" -> Priority.HIGH
                        else -> Priority.NONE
                    },
                    due = o.str("due"),
                    createdAt = o["created"]?.jsonPrimitive?.longOrNull ?: 0,
                    completedAt = o["completed"]?.jsonPrimitive?.longOrNull,
                )
            }
            val habits = parseArray(habitsRaw).mapNotNull { o ->
                val id = o.str("id") ?: return@mapNotNull null
                val name = o.str("name") ?: return@mapNotNull null
                val color = (o["color"]?.jsonPrimitive?.longOrNull ?: ListPalette.EVERGREEN) and 0xFFFFFFFFL
                val days = (o["days"] as? JsonArray)?.mapNotNull { it.jsonPrimitive.contentOrNull }?.toSet()
                    ?: emptySet()
                Habit(id, name, color, days)
            }
            return AppData(lists = defaultLists(), tasks = tasks, habits = habits, theme = theme)
        }

        private fun parseArray(raw: String?): List<JsonObject> = try {
            if (raw == null) emptyList() else json.parseToJsonElement(raw).jsonArray.mapNotNull { it as? JsonObject }
        } catch (e: Exception) {
            emptyList()
        }

        private fun JsonObject.str(key: String): String? = this[key]?.jsonPrimitive?.contentOrNull
    }
}

/** What the Flutter version of Onward left in its shared preferences. */
data class LegacyPrefs(val tasksJson: String?, val habitsJson: String?, val themeMode: String?) {
    companion object {
        fun read(context: Context): LegacyPrefs {
            // shared_preferences on Android stores into this file, with a
            // "flutter." prefix on every key.
            val p = context.getSharedPreferences("FlutterSharedPreferences", Context.MODE_PRIVATE)
            return LegacyPrefs(
                tasksJson = p.getString("flutter.onward.tasks", null),
                habitsJson = p.getString("flutter.onward.habits", null),
                themeMode = p.getString("flutter.onward.themeMode", null),
            )
        }
    }
}
