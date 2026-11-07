import 'package:flutter/material.dart';

import '../data/habit_store.dart';
import '../models/habit.dart';

const habitColors = <int>[
  0xFF3F7D58,
  0xFF2F6FB0,
  0xFFB5532E,
  0xFF8A4FB0,
  0xFFC2902A,
  0xFFC0436B,
];

class HomeScreen extends StatelessWidget {
  const HomeScreen({super.key, required this.store});

  final HabitStore store;

  @override
  Widget build(BuildContext context) {
    return ListenableBuilder(
      listenable: store,
      builder: (context, _) {
        final habits = store.habits;
        final today = DateTime.now();
        final done = habits.where((h) => h.isDoneOn(today)).length;

        return Scaffold(
          appBar: AppBar(title: const Text('Habits')),
          floatingActionButton: FloatingActionButton.extended(
            onPressed: () => _openEditor(context),
            icon: const Icon(Icons.add),
            label: const Text('New habit'),
          ),
          body: habits.isEmpty
              ? const _EmptyState()
              : ListView(
                  padding: const EdgeInsets.fromLTRB(16, 8, 16, 96),
                  children: [
                    _TodaySummary(done: done, total: habits.length),
                    const SizedBox(height: 16),
                    for (final h in habits)
                      _HabitCard(
                        habit: h,
                        onToggle: (day) => store.toggle(h, day),
                        onEdit: () => _openEditor(context, habit: h),
                      ),
                  ],
                ),
        );
      },
    );
  }

  Future<void> _openEditor(BuildContext context, {Habit? habit}) async {
    final result = await showModalBottomSheet<_EditorResult>(
      context: context,
      isScrollControlled: true,
      showDragHandle: true,
      builder: (_) => _HabitEditor(habit: habit),
    );
    if (result == null) return;
    if (result.delete && habit != null) {
      store.remove(habit);
    } else if (habit == null) {
      store.add(result.name, result.colorValue);
    } else {
      store.update(habit, name: result.name, colorValue: result.colorValue);
    }
  }
}

class _EmptyState extends StatelessWidget {
  const _EmptyState();

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    return Center(
      child: Padding(
        padding: const EdgeInsets.all(32),
        child: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            Icon(
              Icons.flag_outlined,
              size: 64,
              color: theme.colorScheme.primary,
            ),
            const SizedBox(height: 16),
            Text('Start something small', style: theme.textTheme.titleLarge),
            const SizedBox(height: 8),
            Text(
              'Add a habit you want to keep, then check it off each day.',
              textAlign: TextAlign.center,
              style: theme.textTheme.bodyMedium,
            ),
          ],
        ),
      ),
    );
  }
}

class _TodaySummary extends StatelessWidget {
  const _TodaySummary({required this.done, required this.total});

  final int done;
  final int total;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    return Card(
      elevation: 0,
      color: theme.colorScheme.primaryContainer,
      child: Padding(
        padding: const EdgeInsets.all(20),
        child: Row(
          children: [
            Expanded(
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text('Today', style: theme.textTheme.labelLarge),
                  const SizedBox(height: 4),
                  Text(
                    '$done of $total done',
                    style: theme.textTheme.headlineSmall,
                  ),
                ],
              ),
            ),
            SizedBox(
              width: 56,
              height: 56,
              child: CircularProgressIndicator(
                value: total == 0 ? 0 : done / total,
                strokeWidth: 6,
                backgroundColor: theme.colorScheme.surface,
              ),
            ),
          ],
        ),
      ),
    );
  }
}

class _HabitCard extends StatelessWidget {
  const _HabitCard({
    required this.habit,
    required this.onToggle,
    required this.onEdit,
  });

  final Habit habit;
  final ValueChanged<DateTime> onToggle;
  final VoidCallback onEdit;

  static const _weekdays = ['M', 'T', 'W', 'T', 'F', 'S', 'S'];

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final color = Color(habit.colorValue);
    final now = DateTime.now();
    final today = DateTime(now.year, now.month, now.day);
    final days = [
      for (var i = 6; i >= 0; i--) today.subtract(Duration(days: i)),
    ];
    final doneToday = habit.isDoneOn(today);
    final streak = habit.currentStreak(now);

    return Card(
      margin: const EdgeInsets.only(bottom: 12),
      child: InkWell(
        borderRadius: BorderRadius.circular(20),
        onLongPress: onEdit,
        child: Padding(
          padding: const EdgeInsets.all(16),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Row(
                children: [
                  Expanded(
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        Text(habit.name, style: theme.textTheme.titleMedium),
                        const SizedBox(height: 2),
                        Text(
                          streak == 0
                              ? 'No streak yet'
                              : '🔥 $streak-day streak',
                          style: theme.textTheme.bodySmall,
                        ),
                      ],
                    ),
                  ),
                  IconButton(
                    tooltip: 'Edit',
                    onPressed: onEdit,
                    icon: const Icon(Icons.more_horiz),
                  ),
                  IconButton.filled(
                    tooltip: doneToday ? 'Mark not done' : 'Mark done',
                    style: IconButton.styleFrom(
                      backgroundColor: doneToday
                          ? color
                          : theme.colorScheme.surfaceContainerHighest,
                      foregroundColor: doneToday
                          ? Colors.white
                          : theme.colorScheme.onSurfaceVariant,
                    ),
                    onPressed: () => onToggle(today),
                    icon: Icon(doneToday ? Icons.check : Icons.check_outlined),
                  ),
                ],
              ),
              const SizedBox(height: 12),
              Row(
                mainAxisAlignment: MainAxisAlignment.spaceBetween,
                children: [
                  for (final d in days)
                    _DayDot(
                      label: _weekdays[d.weekday - 1],
                      done: habit.isDoneOn(d),
                      isToday: d == today,
                      color: color,
                      onTap: () => onToggle(d),
                    ),
                ],
              ),
            ],
          ),
        ),
      ),
    );
  }
}

class _DayDot extends StatelessWidget {
  const _DayDot({
    required this.label,
    required this.done,
    required this.isToday,
    required this.color,
    required this.onTap,
  });

  final String label;
  final bool done;
  final bool isToday;
  final Color color;
  final VoidCallback onTap;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    return GestureDetector(
      onTap: onTap,
      child: Column(
        children: [
          Text(
            label,
            style: theme.textTheme.labelSmall?.copyWith(
              fontWeight: isToday ? FontWeight.bold : null,
            ),
          ),
          const SizedBox(height: 4),
          AnimatedContainer(
            duration: const Duration(milliseconds: 150),
            width: 28,
            height: 28,
            decoration: BoxDecoration(
              shape: BoxShape.circle,
              color: done ? color : Colors.transparent,
              border: Border.all(
                color: done ? color : theme.colorScheme.outlineVariant,
                width: isToday ? 2 : 1,
              ),
            ),
          ),
        ],
      ),
    );
  }
}

class _EditorResult {
  const _EditorResult(this.name, this.colorValue, {this.delete = false});
  final String name;
  final int colorValue;
  final bool delete;
}

class _HabitEditor extends StatefulWidget {
  const _HabitEditor({this.habit});

  final Habit? habit;

  @override
  State<_HabitEditor> createState() => _HabitEditorState();
}

class _HabitEditorState extends State<_HabitEditor> {
  late final _controller = TextEditingController(text: widget.habit?.name);
  late int _color = widget.habit?.colorValue ?? habitColors.first;

  @override
  void dispose() {
    _controller.dispose();
    super.dispose();
  }

  void _submit() {
    final name = _controller.text.trim();
    if (name.isEmpty) return;
    Navigator.pop(context, _EditorResult(name, _color));
  }

  @override
  Widget build(BuildContext context) {
    final editing = widget.habit != null;
    return Padding(
      padding: EdgeInsets.fromLTRB(
        24,
        0,
        24,
        MediaQuery.viewInsetsOf(context).bottom + 24,
      ),
      child: Column(
        mainAxisSize: MainAxisSize.min,
        crossAxisAlignment: CrossAxisAlignment.stretch,
        children: [
          Text(
            editing ? 'Edit habit' : 'New habit',
            style: Theme.of(context).textTheme.titleLarge,
          ),
          const SizedBox(height: 16),
          TextField(
            controller: _controller,
            autofocus: true,
            textCapitalization: TextCapitalization.sentences,
            decoration: const InputDecoration(
              labelText: 'Name',
              hintText: 'e.g. Read 10 pages',
              border: OutlineInputBorder(),
            ),
            onSubmitted: (_) => _submit(),
          ),
          const SizedBox(height: 16),
          Wrap(
            spacing: 12,
            children: [
              for (final c in habitColors)
                GestureDetector(
                  onTap: () => setState(() => _color = c),
                  child: CircleAvatar(
                    radius: 18,
                    backgroundColor: Color(c),
                    child: c == _color
                        ? const Icon(Icons.check, color: Colors.white, size: 18)
                        : null,
                  ),
                ),
            ],
          ),
          const SizedBox(height: 24),
          Row(
            children: [
              if (editing)
                TextButton.icon(
                  onPressed: () => Navigator.pop(
                    context,
                    _EditorResult('', 0, delete: true),
                  ),
                  icon: const Icon(Icons.delete_outline),
                  label: const Text('Delete'),
                  style: TextButton.styleFrom(
                    foregroundColor: Theme.of(context).colorScheme.error,
                  ),
                ),
              const Spacer(),
              FilledButton(
                onPressed: _submit,
                child: Text(editing ? 'Save' : 'Add'),
              ),
            ],
          ),
        ],
      ),
    );
  }
}
