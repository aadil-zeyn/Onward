import 'package:flutter/material.dart';

import '../models/task.dart';
import '../theme.dart';
import '../widgets/task_controls.dart';

class TaskEditResult {
  const TaskEditResult({
    required this.title,
    required this.priority,
    required this.due,
    this.delete = false,
  });
  const TaskEditResult.delete()
    : this(title: '', priority: Priority.none, due: null, delete: true);

  final String title;
  final Priority priority;
  final DateTime? due;
  final bool delete;
}

/// Bottom sheet for changing a task's title, priority and due date.
class TaskEditor extends StatefulWidget {
  const TaskEditor({super.key, required this.task});
  final Task task;

  @override
  State<TaskEditor> createState() => _TaskEditorState();
}

class _TaskEditorState extends State<TaskEditor> {
  late final _title = TextEditingController(text: widget.task.title);
  late Priority _priority = widget.task.priority;
  late DateTime? _due = widget.task.due;

  @override
  void dispose() {
    _title.dispose();
    super.dispose();
  }

  void _save() {
    Navigator.pop(
      context,
      TaskEditResult(title: _title.text.trim(), priority: _priority, due: _due),
    );
  }

  @override
  Widget build(BuildContext context) {
    final c = context.colors;
    final theme = Theme.of(context);
    return Padding(
      padding: EdgeInsets.fromLTRB(
        20,
        0,
        20,
        MediaQuery.viewInsetsOf(context).bottom + 20,
      ),
      child: Column(
        mainAxisSize: MainAxisSize.min,
        crossAxisAlignment: CrossAxisAlignment.stretch,
        children: [
          Text('Edit task', style: theme.textTheme.titleLarge),
          const SizedBox(height: 16),
          TextField(
            controller: _title,
            minLines: 1,
            maxLines: 4,
            textCapitalization: TextCapitalization.sentences,
            textInputAction: TextInputAction.done,
            style: const TextStyle(fontSize: 16.5, height: 1.4),
            decoration: const InputDecoration(hintText: 'Task title'),
            onSubmitted: (_) => _save(),
          ),
          const SizedBox(height: 14),
          Wrap(
            spacing: 8,
            runSpacing: 10,
            crossAxisAlignment: WrapCrossAlignment.center,
            children: [
              PriorityPicker(
                value: _priority,
                onChanged: (p) => setState(() => _priority = p),
              ),
              DueChip(value: _due, onChanged: (d) => setState(() => _due = d)),
            ],
          ),
          const SizedBox(height: 22),
          Row(
            children: [
              TextButton.icon(
                onPressed: () =>
                    Navigator.pop(context, const TaskEditResult.delete()),
                icon: const Icon(Icons.delete_outline_rounded, size: 20),
                label: const Text('Delete'),
                style: TextButton.styleFrom(foregroundColor: c.high),
              ),
              const Spacer(),
              TextButton(
                onPressed: () => Navigator.pop(context),
                child: const Text('Cancel'),
              ),
              const SizedBox(width: 6),
              FilledButton(onPressed: _save, child: const Text('Save')),
            ],
          ),
        ],
      ),
    );
  }
}
