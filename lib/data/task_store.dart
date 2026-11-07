import 'dart:convert';

import 'package:flutter/foundation.dart';
import 'package:shared_preferences/shared_preferences.dart';

import '../models/task.dart';

/// A task that was removed, and where it was, so it can be put back.
class RemovedTask {
  const RemovedTask(this.task, this.index);
  final Task task;
  final int index;
}

/// Holds the ordered task list and saves it to local storage on every change.
class TaskStore extends ChangeNotifier {
  TaskStore(this._prefs, {bool seedIfNew = true}) {
    final raw = _prefs.getString(_key);
    if (raw != null) {
      try {
        final list = jsonDecode(raw) as List;
        _tasks.addAll(
          list.map((e) => Task.fromJson(e as Map<String, dynamic>)),
        );
      } on Object catch (e) {
        debugPrint('Onward: could not read saved tasks ($e)');
      }
    } else if (seedIfNew) {
      _tasks.addAll(_welcomeTasks());
      _persist();
    }
  }

  static const _key = 'onward.tasks';
  final SharedPreferences _prefs;
  final List<Task> _tasks = [];
  int _counter = 0;

  List<Task> get tasks => List.unmodifiable(_tasks);
  int get doneCount => _tasks.where((t) => t.done).length;
  int get activeCount => _tasks.length - doneCount;
  bool get allDone => _tasks.isNotEmpty && _tasks.every((t) => t.done);

  String _newId() => '${DateTime.now().microsecondsSinceEpoch}-${_counter++}';

  Task add(String title, {Priority priority = Priority.none, DateTime? due}) {
    final task = Task(
      id: _newId(),
      title: title,
      priority: priority,
      due: due == null ? null : dateOnly(due),
    );
    _tasks.insert(0, task);
    _save();
    return task;
  }

  void toggle(Task task) {
    task.done = !task.done;
    task.completedAt = task.done ? DateTime.now() : null;
    _save();
  }

  void update(
    Task task, {
    required String title,
    required Priority priority,
    required DateTime? due,
  }) {
    task
      ..title = title
      ..priority = priority
      ..due = due == null ? null : dateOnly(due);
    _save();
  }

  List<RemovedTask> remove(Task task) => _removeWhere((t) => t.id == task.id);

  List<RemovedTask> clearDone() => _removeWhere((t) => t.done);

  List<RemovedTask> _removeWhere(bool Function(Task) test) {
    final removed = <RemovedTask>[
      for (var i = 0; i < _tasks.length; i++)
        if (test(_tasks[i])) RemovedTask(_tasks[i], i),
    ];
    if (removed.isEmpty) return removed;
    _tasks.removeWhere(test);
    _save();
    return removed;
  }

  /// Puts removed tasks back where they were (used by "Undo").
  void restore(List<RemovedTask> removed) {
    final sorted = [...removed]..sort((a, b) => a.index.compareTo(b.index));
    for (final r in sorted) {
      if (_tasks.any((t) => t.id == r.task.id)) continue;
      _tasks.insert(r.index.clamp(0, _tasks.length), r.task);
    }
    _save();
  }

  /// Applies a new order to a subset of tasks (e.g. only the ones visible
  /// under a filter), leaving every other task in its slot.
  void reorderSubset(List<String> orderedIds) {
    final wanted = orderedIds.toSet();
    final byId = {for (final t in _tasks) t.id: t};
    var k = 0;
    for (var i = 0; i < _tasks.length; i++) {
      if (wanted.contains(_tasks[i].id)) _tasks[i] = byId[orderedIds[k++]]!;
    }
    _save();
  }

  void _save() {
    _persist();
    notifyListeners();
  }

  void _persist() {
    _prefs.setString(_key, jsonEncode(_tasks.map((t) => t.toJson()).toList()));
  }

  List<Task> _welcomeTasks() {
    final today = dateOnly(DateTime.now());
    return [
      Task(
        id: _newId(),
        title: 'Welcome to Onward — tap the circle to complete a task',
      ),
      Task(
        id: _newId(),
        title: 'Tap a task to edit it',
        priority: Priority.medium,
        due: today,
      ),
      Task(
        id: _newId(),
        title: 'Hold the dotted handle and drag to reorder',
        priority: Priority.low,
        due: today.add(const Duration(days: 1)),
      ),
      Task(
        id: _newId(),
        title: 'Swipe left to delete, right to complete',
        priority: Priority.high,
      ),
      Task(
        id: _newId(),
        title: 'Open Onward for the first time',
        done: true,
        completedAt: DateTime.now(),
      ),
    ];
  }
}
