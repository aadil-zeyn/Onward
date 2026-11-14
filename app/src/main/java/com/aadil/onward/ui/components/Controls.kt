package com.aadil.onward.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Notes
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.CalendarToday
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Event
import androidx.compose.material.icons.rounded.EventBusy
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.automirrored.rounded.NextWeek
import androidx.compose.material.icons.rounded.OutlinedFlag
import androidx.compose.material.icons.rounded.Today
import androidx.compose.material.icons.rounded.WbSunny
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDefaults
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aadil.onward.model.DueState
import com.aadil.onward.model.Priority
import com.aadil.onward.model.Space
import com.aadil.onward.model.Task
import com.aadil.onward.model.TaskList
import com.aadil.onward.model.describeDue
import com.aadil.onward.model.dueDate
import com.aadil.onward.model.nextMonday
import com.aadil.onward.ui.theme.Onward
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

/** Onward's card: warm surface, hairline border and a soft long shadow. */
fun Modifier.card(
    surface: Color,
    line: Color,
    shadow: Color,
    shape: Shape = RoundedCornerShape(22.dp),
    elevation: Dp = 10.dp,
): Modifier = this
    .shadow(elevation, shape, clip = false, ambientColor = shadow, spotColor = shadow)
    .background(surface, shape)
    .border(1.dp, line, shape)
    .clip(shape)

@Composable
fun Modifier.onwardCard(shape: Shape = RoundedCornerShape(22.dp), elevation: Dp = 10.dp): Modifier {
    val c = Onward.colors
    return card(c.surface, c.line, c.shadow, shape, elevation)
}

/** Small capitals above a heading: "SATURDAY, NOVEMBER 14". */
@Composable
fun Eyebrow(text: String, modifier: Modifier = Modifier, color: Color = Onward.colors.muted) {
    Text(text.uppercase(), style = MaterialTheme.typography.labelSmall, color = color, modifier = modifier)
}

@Composable
fun SectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    action: String? = null,
    onAction: (() -> Unit)? = null,
) {
    Row(modifier.fillMaxWidth().heightIn(min = 40.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(title, style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
        if (action != null && onAction != null) {
            TextButton(onClick = onAction, contentPadding = PaddingValues(horizontal = 10.dp)) {
                Text(action, style = MaterialTheme.typography.labelLarge, color = Onward.colors.accent)
            }
        }
    }
}

/** A pill-shaped segmented control with optional counts. */
@Composable
fun <T> Segmented(
    options: List<T>,
    selected: T,
    label: (T) -> String,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier,
    count: ((T) -> Int)? = null,
) {
    val c = Onward.colors
    Row(
        modifier
            .background(c.surface2, RoundedCornerShape(16.dp))
            .border(1.dp, c.line, RoundedCornerShape(16.dp))
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        options.forEach { o ->
            val on = o == selected
            val bg by animateColorAsState(if (on) c.surface else c.surface2.copy(alpha = 0f), label = "seg")
            Row(
                Modifier
                    .then(if (on) Modifier.shadow(3.dp, RoundedCornerShape(12.dp), ambientColor = c.shadow, spotColor = c.shadow) else Modifier)
                    .clip(RoundedCornerShape(12.dp))
                    .background(bg)
                    .clickable(role = Role.Tab) { onSelect(o) }
                    .padding(horizontal = 14.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    label(o),
                    style = MaterialTheme.typography.labelLarge,
                    color = if (on) c.ink else c.muted,
                )
                if (count != null) {
                    Spacer(Modifier.width(7.dp))
                    Text(
                        count(o).toString(),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (on) c.accent else c.muted,
                        modifier = Modifier
                            .background(if (on) c.accentSoft else c.line, CircleShape)
                            .padding(horizontal = 7.dp, vertical = 1.dp),
                    )
                }
            }
        }
    }
}

/** None / Low / Medium / High. The chosen one shows its name. */
@Composable
fun PriorityPicker(selected: Priority, onSelect: (Priority) -> Unit, modifier: Modifier = Modifier) {
    val c = Onward.colors
    Row(
        modifier
            .background(c.surface2, RoundedCornerShape(14.dp))
            .border(1.dp, c.line, RoundedCornerShape(14.dp))
            .padding(3.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Priority.entries.forEach { p ->
            val on = p == selected
            val bg by animateColorAsState(if (on) c.surface else c.surface2.copy(alpha = 0f), label = "prio")
            Row(
                Modifier
                    .height(34.dp)
                    .clip(RoundedCornerShape(11.dp))
                    .background(bg)
                    .clickable(role = Role.RadioButton) { onSelect(p) }
                    .padding(horizontal = 11.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                val dot = c.priority(p)
                Box(
                    Modifier
                        .size(10.dp)
                        .then(
                            if (p == Priority.NONE) Modifier.border(2.dp, dot, CircleShape)
                            else Modifier.background(dot, CircleShape),
                        ),
                )
                AnimatedVisibility(on, enter = fadeIn() + expandHorizontally(), exit = fadeOut() + shrinkHorizontally()) {
                    Text(p.label, style = MaterialTheme.typography.labelMedium, color = c.ink,
                        modifier = Modifier.padding(start = 7.dp))
                }
            }
        }
    }
}

/** "Due date" button with quick choices and a calendar. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DueButton(
    due: LocalDate?,
    onChange: (LocalDate?) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "Due date",
) {
    val c = Onward.colors
    var menu by remember { mutableStateOf(false) }
    var picker by remember { mutableStateOf(false) }
    val today = LocalDate.now()
    val info = due?.let { describeDue(it, today) }
    val tint = when (info?.state) {
        null -> c.muted
        DueState.OVERDUE -> c.high
        DueState.TODAY -> c.accent
        DueState.UPCOMING -> c.ink
    }
    Box(modifier) {
        Row(
            Modifier
                .height(40.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(if (due != null) c.surface2 else Color.Transparent)
                .clickable { menu = true }
                .padding(horizontal = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Rounded.CalendarToday, null, tint = tint, modifier = Modifier.size(17.dp))
            Spacer(Modifier.width(7.dp))
            Text(info?.label ?: placeholder, style = MaterialTheme.typography.labelLarge, color = tint, maxLines = 1)
        }
        DropdownMenu(
            expanded = menu,
            onDismissRequest = { menu = false },
            shape = RoundedCornerShape(16.dp),
            containerColor = c.surface,
            border = BorderStroke(1.dp, c.line),
        ) {
            @Composable
            fun item(icon: ImageVector, text: String, trailing: String? = null, action: () -> Unit) =
                DropdownMenuItem(
                    text = { Text(text, style = MaterialTheme.typography.bodyLarge) },
                    leadingIcon = { Icon(icon, null, tint = c.muted) },
                    trailingIcon = trailing?.let { { Text(it, style = MaterialTheme.typography.bodySmall) } },
                    onClick = { menu = false; action() },
                )
            item(Icons.Rounded.Today, "Today", today.dayOfWeek.name.take(3).lowercase().replaceFirstChar { it.uppercase() }) { onChange(today) }
            item(Icons.Rounded.WbSunny, "Tomorrow", today.plusDays(1).dayOfWeek.name.take(3).lowercase().replaceFirstChar { it.uppercase() }) { onChange(today.plusDays(1)) }
            item(Icons.AutoMirrored.Rounded.NextWeek, "Next week", describeDue(nextMonday(today), today).label) { onChange(nextMonday(today)) }
            item(Icons.Rounded.Event, "Pick a date…") { picker = true }
            if (due != null) {
                HorizontalDivider(Modifier.padding(vertical = 4.dp), color = c.line)
                item(Icons.Rounded.EventBusy, "No due date") { onChange(null) }
            }
        }
    }
    if (picker) {
        val state = rememberDatePickerState(
            initialSelectedDateMillis = (due ?: today).atStartOfDay().toInstant(ZoneOffset.UTC).toEpochMilli(),
        )
        DatePickerDialog(
            onDismissRequest = { picker = false },
            confirmButton = {
                TextButton(onClick = {
                    state.selectedDateMillis?.let {
                        onChange(Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate())
                    }
                    picker = false
                }) { Text("Set date", color = c.accent) }
            },
            dismissButton = { TextButton(onClick = { picker = false }) { Text("Cancel", color = c.ink2) } },
            colors = DatePickerDefaults.colors(containerColor = c.surface),
        ) {
            DatePicker(
                state = state,
                colors = DatePickerDefaults.colors(
                    containerColor = c.surface,
                    selectedDayContainerColor = c.accent,
                    selectedDayContentColor = c.accentInk,
                    todayDateBorderColor = c.accent,
                    todayContentColor = c.accent,
                ),
            )
        }
    }
}

/** Single-line or multi-line text input in Onward's style, with a hint. */
@Composable
fun Field(
    value: String,
    onValueChange: (String) -> Unit,
    hint: String,
    modifier: Modifier = Modifier,
    singleLine: Boolean = true,
    minHeight: Dp = 52.dp,
    textStyle: androidx.compose.ui.text.TextStyle = MaterialTheme.typography.bodyLarge,
    onDone: (() -> Unit)? = null,
) {
    val c = Onward.colors
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        singleLine = singleLine,
        textStyle = textStyle.copy(color = c.ink),
        cursorBrush = SolidColor(c.accent),
        keyboardOptions = KeyboardOptions(
            capitalization = KeyboardCapitalization.Sentences,
            imeAction = if (singleLine) ImeAction.Done else ImeAction.Default,
        ),
        keyboardActions = KeyboardActions(onDone = { onDone?.invoke() }),
        modifier = modifier,
        decorationBox = { inner ->
            Box(
                Modifier
                    .fillMaxWidth()
                    .heightIn(min = minHeight)
                    .background(c.surface2, RoundedCornerShape(14.dp))
                    .border(1.dp, c.line, RoundedCornerShape(14.dp))
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                contentAlignment = if (singleLine) Alignment.CenterStart else Alignment.TopStart,
            ) {
                if (value.isEmpty()) Text(hint, style = textStyle, color = c.muted)
                inner()
            }
        },
    )
}

/**
 * The quick-capture card: a line to type into, then priority, due date
 * and (on Today) which list it goes to.
 */
@Composable
fun Composer(
    onAdd: (title: String, priority: Priority, due: LocalDate?, listId: String?) -> Unit,
    modifier: Modifier = Modifier,
    hint: String = "What needs doing?",
    lists: List<TaskList> = emptyList(),
    defaultListId: String? = null,
    duePlaceholder: String = "Due date",
) {
    val c = Onward.colors
    val haptics = LocalHapticFeedback.current
    var text by rememberSaveable { mutableStateOf("") }
    var priority by rememberSaveable { mutableStateOf(Priority.NONE) }
    var due by rememberSaveable { mutableStateOf<String?>(null) }
    var listId by rememberSaveable { mutableStateOf(defaultListId) }
    val canAdd = text.isNotBlank()

    fun submit() {
        if (!canAdd) return
        onAdd(text.trim(), priority, due?.let(LocalDate::parse), listId)
        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        text = ""; priority = Priority.NONE; due = null
    }

    Column(modifier.fillMaxWidth().onwardCard()) {
        Row(Modifier.padding(start = 16.dp, end = 8.dp, top = 8.dp, bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(26.dp).border(1.5.dp, c.lineStrong, CircleShape), contentAlignment = Alignment.Center) {
                Icon(Icons.Rounded.Add, null, tint = c.muted, modifier = Modifier.size(17.dp))
            }
            Spacer(Modifier.width(12.dp))
            BasicTextField(
                value = text,
                onValueChange = { text = it },
                singleLine = true,
                textStyle = MaterialTheme.typography.bodyLarge.copy(color = c.ink, fontSize = 17.sp),
                cursorBrush = SolidColor(c.accent),
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences, imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { submit() }),
                modifier = Modifier.weight(1f),
                decorationBox = { inner ->
                    Box(Modifier.heightIn(min = 44.dp), contentAlignment = Alignment.CenterStart) {
                        if (text.isEmpty()) Text(hint, style = MaterialTheme.typography.bodyLarge, color = c.muted, fontSize = 17.sp)
                        inner()
                    }
                },
            )
            val bg by animateColorAsState(if (canAdd) c.ink else c.lineStrong, label = "send")
            Box(
                Modifier
                    .size(46.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(bg)
                    .clickable(enabled = canAdd, role = Role.Button) { submit() },
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Rounded.ArrowUpward, "Add task", tint = if (canAdd) c.bg else c.surface, modifier = Modifier.size(22.dp))
            }
        }
        HorizontalDivider(color = c.line)
        Row(Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            PriorityPicker(priority, { priority = it })
            Spacer(Modifier.weight(1f).width(6.dp))
            DueButton(due?.let(LocalDate::parse), { due = it?.toString() }, placeholder = duePlaceholder)
        }
        if (lists.isNotEmpty()) {
            HorizontalDivider(color = c.line)
            Row(Modifier.fillMaxWidth().padding(start = 16.dp, end = 10.dp, top = 8.dp, bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("Add to", style = MaterialTheme.typography.bodyMedium, color = c.muted)
                Spacer(Modifier.weight(1f))
                ListPickerButton(lists, listId, { listId = it })
            }
        }
    }
}

/** A chip showing which list a new task will go to, with a menu of all lists. */
@Composable
fun ListPickerButton(lists: List<TaskList>, selectedId: String?, onSelect: (String) -> Unit) {
    val c = Onward.colors
    var open by remember { mutableStateOf(false) }
    val sel = lists.firstOrNull { it.id == selectedId } ?: lists.first()
    Box {
        Row(
            Modifier
                .height(40.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(c.wash(sel.color))
                .clickable { open = true }
                .padding(start = 10.dp, end = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(ListIcons.of(sel.icon), null, tint = c.tint(sel.color), modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(6.dp))
            Text(sel.name, style = MaterialTheme.typography.labelLarge, color = c.ink, maxLines = 1)
            Icon(Icons.Rounded.KeyboardArrowDown, null, tint = c.muted, modifier = Modifier.size(18.dp))
        }
        DropdownMenu(
            expanded = open,
            onDismissRequest = { open = false },
            shape = RoundedCornerShape(16.dp),
            containerColor = c.surface,
            border = BorderStroke(1.dp, c.line),
        ) {
            Space.entries.forEach { space ->
                val inSpace = lists.filter { it.space == space }
                if (inSpace.isEmpty()) return@forEach
                Eyebrow(space.label, Modifier.padding(start = 16.dp, top = 10.dp, bottom = 4.dp))
                inSpace.forEach { l ->
                    DropdownMenuItem(
                        text = { Text(l.name, style = MaterialTheme.typography.bodyLarge) },
                        leadingIcon = { IconTile(ListIcons.of(l.icon), l.color, size = 28.dp) },
                        trailingIcon = if (l.id == sel.id) ({ Icon(Icons.Rounded.Check, null, tint = c.accent) }) else null,
                        onClick = { open = false; onSelect(l.id) },
                    )
                }
            }
        }
    }
}

/** One task: checkbox, title, and a line of due date / priority / list. */
@Composable
fun TaskRow(
    task: Task,
    onToggle: () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    list: TaskList? = null,
    today: LocalDate = LocalDate.now(),
) {
    val c = Onward.colors
    val haptics = LocalHapticFeedback.current
    val fade by animateFloatAsState(if (task.done) 0.55f else 1f, label = "fade")
    Row(
        modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(start = 6.dp, end = 16.dp, top = 6.dp, bottom = 6.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Box(
            Modifier
                .size(44.dp)
                .clip(CircleShape)
                .clickable(role = Role.Checkbox, onClickLabel = if (task.done) "Mark not done" else "Complete") {
                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                    onToggle()
                },
            contentAlignment = Alignment.Center,
        ) {
            CheckCircle(task.done, c.priority(task.priority))
        }
        Column(Modifier.weight(1f).padding(top = 10.dp, bottom = 6.dp, start = 4.dp).alpha(fade)) {
            Text(
                task.title,
                style = MaterialTheme.typography.bodyLarge,
                color = if (task.done) c.muted else c.ink,
                textDecoration = if (task.done) TextDecoration.LineThrough else null,
            )
            val due = task.dueDate
            val hasMeta = due != null || task.priority != Priority.NONE || list != null || task.notes.isNotBlank()
            if (hasMeta) {
                Spacer(Modifier.height(5.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    if (list != null) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(Modifier.size(7.dp).background(c.tint(list.color), CircleShape))
                            Spacer(Modifier.width(5.dp))
                            Text(list.name, style = MaterialTheme.typography.labelMedium, color = c.ink2,
                                maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                    if (due != null) {
                        val info = describeDue(due, today)
                        val tint = when {
                            task.done -> c.muted
                            info.state == DueState.OVERDUE -> c.high
                            info.state == DueState.TODAY -> c.accent
                            else -> c.muted
                        }
                        Meta(Icons.Rounded.CalendarToday, info.label, tint)
                    }
                    if (task.priority != Priority.NONE) {
                        Meta(Icons.Rounded.OutlinedFlag, task.priority.label, if (task.done) c.muted else c.priority(task.priority))
                    }
                    if (task.notes.isNotBlank()) {
                        Icon(Icons.AutoMirrored.Rounded.Notes, "Has notes", tint = c.muted, modifier = Modifier.size(15.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun Meta(icon: ImageVector, text: String, tint: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, tint = tint, modifier = Modifier.size(14.dp))
        Spacer(Modifier.width(4.dp))
        Text(text, style = MaterialTheme.typography.labelMedium, color = tint, maxLines = 1)
    }
}

/** A task card you can swipe right to complete or left to delete. */
@Composable
fun SwipeableTask(
    task: Task,
    onToggle: () -> Unit,
    onDelete: () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    list: TaskList? = null,
    today: LocalDate = LocalDate.now(),
) {
    val c = Onward.colors
    val scope = rememberCoroutineScope()
    val haptics = LocalHapticFeedback.current
    val state = rememberSwipeToDismissBoxState()
    val shape = RoundedCornerShape(18.dp)
    SwipeToDismissBox(
        state = state,
        modifier = modifier,
        onDismiss = { value ->
            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
            if (value == SwipeToDismissBoxValue.StartToEnd) {
                onToggle()
                scope.launch { state.reset() }
            } else if (value == SwipeToDismissBoxValue.EndToStart) {
                onDelete()
            }
        },
        backgroundContent = {
            val dir = state.dismissDirection
            val toEnd = dir == SwipeToDismissBoxValue.StartToEnd
            val bg = when (dir) {
                SwipeToDismissBoxValue.StartToEnd -> c.accentSoft
                SwipeToDismissBoxValue.EndToStart -> c.dangerSoft
                else -> Color.Transparent
            }
            Box(
                Modifier
                    .fillMaxSize()
                    .clip(shape)
                    .background(bg)
                    .padding(horizontal = 22.dp),
                contentAlignment = if (toEnd) Alignment.CenterStart else Alignment.CenterEnd,
            ) {
                if (dir != SwipeToDismissBoxValue.Settled) {
                    val reached = state.targetValue != SwipeToDismissBoxValue.Settled
                    val scale by animateFloatAsState(if (reached) 1.15f else 0.9f, label = "swipeIcon")
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.scale(scale)) {
                        if (toEnd) {
                            Icon(if (task.done) Icons.Rounded.Close else Icons.Rounded.Check, null, tint = c.accent)
                            Spacer(Modifier.width(6.dp))
                            Text(if (task.done) "Undo" else "Done", color = c.accent, style = MaterialTheme.typography.labelLarge)
                        } else {
                            Text("Delete", color = c.high, style = MaterialTheme.typography.labelLarge)
                            Spacer(Modifier.width(6.dp))
                            Icon(Icons.Rounded.DeleteOutline, null, tint = c.high)
                        }
                    }
                }
            }
        },
    ) {
        Box(Modifier.onwardCard(shape, elevation = 4.dp)) {
            TaskRow(task, onToggle, onClick, list = list, today = today)
        }
    }
}

/** A row of colour dots to pick from. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ColorSwatches(colors: List<Long>, selected: Long, onSelect: (Long) -> Unit) {
    val c = Onward.colors
    FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        colors.forEach { col ->
            val on = col == selected
            Box(
                Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .border(if (on) 2.dp else 0.dp, if (on) c.ink else Color.Transparent, CircleShape)
                    .padding(4.dp)
                    .background(c.tint(col), CircleShape)
                    .clickable(role = Role.RadioButton) { onSelect(col) },
                contentAlignment = Alignment.Center,
            ) {
                if (on) Icon(Icons.Rounded.Check, null, tint = Color.White, modifier = Modifier.size(18.dp))
            }
        }
    }
}

/** A grid of list icons, tinted with the chosen colour. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun IconPicker(selected: String, color: Long, onSelect: (String) -> Unit) {
    val c = Onward.colors
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        ListIcons.all.forEach { (key, icon) ->
            val on = key == selected
            Box(
                Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(13.dp))
                    .background(if (on) c.wash(color) else c.surface2)
                    .border(1.dp, if (on) c.tint(color) else c.line, RoundedCornerShape(13.dp))
                    .clickable(role = Role.RadioButton) { onSelect(key) },
                contentAlignment = Alignment.Center,
            ) {
                Icon(icon, key, tint = if (on) c.tint(color) else c.muted, modifier = Modifier.size(21.dp))
            }
        }
    }
}

@Composable
fun EmptyState(title: String, body: String, modifier: Modifier = Modifier, art: @Composable () -> Unit = { SunriseArt() }) {
    Column(
        modifier.fillMaxWidth().padding(horizontal = 32.dp, vertical = 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        art()
        Spacer(Modifier.height(14.dp))
        Text(title, style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(6.dp))
        Text(body, style = MaterialTheme.typography.bodyMedium, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
    }
}

/** A dashed rounded rectangle outline that fills its parent. */
@Composable
fun DashedOutline(modifier: Modifier = Modifier, color: Color = Onward.colors.lineStrong) {
    Canvas(modifier) {
        drawRoundRect(
            color,
            cornerRadius = CornerRadius(22.dp.toPx()),
            style = Stroke(1.5.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 8f))),
        )
    }
}

/** A filled ink button, Onward's primary action. */
@Composable
fun InkButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true, icon: ImageVector? = null) {
    val c = Onward.colors
    Row(
        modifier
            .height(50.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(if (enabled) c.ink else c.lineStrong)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick, interactionSource = remember { MutableInteractionSource() }, indication = androidx.compose.material3.ripple())
            .padding(horizontal = 20.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        if (icon != null) {
            Icon(icon, null, tint = c.bg, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(8.dp))
        }
        Text(text, style = MaterialTheme.typography.labelLarge, color = c.bg)
    }
}
