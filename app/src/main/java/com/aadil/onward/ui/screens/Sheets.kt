package com.aadil.onward.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.aadil.onward.model.ListPalette
import com.aadil.onward.model.Priority
import com.aadil.onward.model.Space
import com.aadil.onward.model.Task
import com.aadil.onward.model.TaskList
import com.aadil.onward.model.dueDate
import com.aadil.onward.ui.components.ColorSwatches
import com.aadil.onward.ui.components.DueButton
import com.aadil.onward.ui.components.Eyebrow
import com.aadil.onward.ui.components.Field
import com.aadil.onward.ui.components.IconPicker
import com.aadil.onward.ui.components.IconTile
import com.aadil.onward.ui.components.InkButton
import com.aadil.onward.ui.components.ListIcons
import com.aadil.onward.ui.components.ListPickerButton
import com.aadil.onward.ui.components.PriorityPicker
import com.aadil.onward.ui.theme.Onward
import kotlinx.coroutines.launch
import java.time.LocalDate

/** The shared frame for Onward's bottom sheets. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun OnwardSheet(
    title: String,
    onDismiss: () -> Unit,
    content: @Composable ColumnScope.(close: (() -> Unit) -> Unit) -> Unit,
) {
    val c = Onward.colors
    val state = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    val close: (() -> Unit) -> Unit = { then ->
        scope.launch { state.hide() }.invokeOnCompletion { then(); onDismiss() }
    }
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = state,
        containerColor = c.surface,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        dragHandle = { BottomSheetDefaults.DragHandle(color = c.lineStrong) },
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 22.dp)
                .padding(bottom = 18.dp)
                .navigationBarsPadding(),
        ) {
            Text(title, style = MaterialTheme.typography.headlineSmall)
            Spacer(Modifier.height(18.dp))
            content(close)
        }
    }
}

@Composable
private fun Label(text: String) {
    Eyebrow(text, Modifier.padding(top = 18.dp, bottom = 8.dp))
}

@Composable
private fun SheetButtons(onDelete: (() -> Unit)?, saveLabel: String, canSave: Boolean, onSave: () -> Unit) {
    val c = Onward.colors
    Spacer(Modifier.height(26.dp))
    Row(verticalAlignment = Alignment.CenterVertically) {
        if (onDelete != null) {
            TextButton(onClick = onDelete) {
                Icon(Icons.Rounded.DeleteOutline, null, tint = c.high, modifier = Modifier.size(19.dp))
                Spacer(Modifier.width(6.dp))
                Text("Delete", color = c.high, style = MaterialTheme.typography.labelLarge)
            }
        }
        Spacer(Modifier.weight(1f))
        InkButton(saveLabel, onSave, enabled = canSave, modifier = Modifier.width(140.dp))
    }
}

/** Edit a task's title, notes, priority, due date and list. */
@Composable
fun TaskEditorSheet(
    task: Task,
    lists: List<TaskList>,
    onDismiss: () -> Unit,
    onSave: (title: String, notes: String, priority: Priority, due: LocalDate?, listId: String) -> Unit,
    onDelete: () -> Unit,
) {
    var title by rememberSaveable { mutableStateOf(task.title) }
    var notes by rememberSaveable { mutableStateOf(task.notes) }
    var priority by rememberSaveable { mutableStateOf(task.priority) }
    var due by rememberSaveable { mutableStateOf(task.due) }
    var listId by rememberSaveable { mutableStateOf(task.listId) }

    OnwardSheet("Edit task", onDismiss) { close ->
        Field(title, { title = it }, "Task", singleLine = false, minHeight = 56.dp)
        Label("Notes")
        Field(notes, { notes = it }, "Add details, links, acceptance criteria…", singleLine = false, minHeight = 96.dp,
            textStyle = MaterialTheme.typography.bodyMedium)
        Label("Priority")
        PriorityPicker(priority, { priority = it })
        Label("Due & list")
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            ListPickerButton(lists, listId, { listId = it })
            DueButton(due?.let(LocalDate::parse), { due = it?.toString() })
        }
        SheetButtons(
            onDelete = { close(onDelete) },
            saveLabel = "Save",
            canSave = title.isNotBlank(),
            onSave = { close { onSave(title.trim(), notes.trim(), priority, due?.let(LocalDate::parse), listId) } },
        )
    }
}

/** Create or edit a list: name, colour and icon, with a live preview. */
@Composable
fun ListEditorSheet(
    existing: TaskList?,
    space: Space,
    onDismiss: () -> Unit,
    onSave: (name: String, color: Long, icon: String) -> Unit,
    onDelete: (() -> Unit)? = null,
) {
    val c = Onward.colors
    var name by rememberSaveable { mutableStateOf(existing?.name ?: "") }
    var color by rememberSaveable { mutableStateOf(existing?.color ?: ListPalette.BLUE) }
    var icon by rememberSaveable { mutableStateOf(existing?.icon ?: if (space == Space.WORK) "work" else "star") }

    OnwardSheet(if (existing == null) "New ${space.label.lowercase()} list" else "Edit list", onDismiss) { close ->
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconTile(ListIcons.of(icon), color, size = 52.dp)
            Spacer(Modifier.width(14.dp))
            Box(Modifier.weight(1f)) {
                Field(name, { name = it }, if (space == Space.WORK) "e.g. Kafka migration" else "e.g. Groceries")
            }
        }
        Label("Colour")
        ColorSwatches(ListPalette.colors, color) { color = it }
        Label("Icon")
        IconPicker(icon, color) { icon = it }
        SheetButtons(
            onDelete = onDelete?.let { d -> { close(d) } },
            saveLabel = if (existing == null) "Create" else "Save",
            canSave = name.isNotBlank(),
            onSave = { close { onSave(name.trim(), color, icon) } },
        )
        if (onDelete != null) {
            Spacer(Modifier.height(10.dp))
            Text("Deleting a list also deletes its tasks. You can undo straight after.",
                style = MaterialTheme.typography.bodySmall, color = c.muted)
        }
    }
}

/** Create or edit a habit: its name and colour. */
@Composable
fun HabitEditorSheet(
    existingName: String?,
    existingColor: Long?,
    onDismiss: () -> Unit,
    onSave: (name: String, color: Long) -> Unit,
    onDelete: (() -> Unit)? = null,
) {
    val c = Onward.colors
    var name by rememberSaveable { mutableStateOf(existingName ?: "") }
    var color by rememberSaveable { mutableStateOf(existingColor ?: ListPalette.EVERGREEN) }
    OnwardSheet(if (existingName == null) "New habit" else "Edit habit", onDismiss) { close ->
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(14.dp).background(c.tint(color), RoundedCornerShape(50)))
            Spacer(Modifier.width(14.dp))
            Box(Modifier.weight(1f)) { Field(name, { name = it }, "e.g. Read 10 pages") }
        }
        Label("Colour")
        ColorSwatches(ListPalette.colors, color) { color = it }
        SheetButtons(
            onDelete = onDelete?.let { d -> { close(d) } },
            saveLabel = if (existingName == null) "Add habit" else "Save",
            canSave = name.isNotBlank(),
            onSave = { close { onSave(name.trim(), color) } },
        )
    }
}
