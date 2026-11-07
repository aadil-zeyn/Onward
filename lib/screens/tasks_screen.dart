import 'dart:async';

import 'package:flutter/material.dart';
import 'package:flutter/services.dart';

import '../data/task_store.dart';
import '../data/theme_controller.dart';
import '../models/task.dart';
import '../theme.dart';
import '../widgets/check_circle.dart';
import '../widgets/confetti.dart';
import '../widgets/decorations.dart';
import '../widgets/progress_ring.dart';
import '../widgets/task_controls.dart';
import 'task_editor.dart';

enum TaskFilter {
  all('All'),
  active('Active'),
  done('Done');

  const TaskFilter(this.label);
  final String label;

  bool matches(Task t) => switch (this) {
    TaskFilter.all => true,
    TaskFilter.active => !t.done,
    TaskFilter.done => t.done,
  };
}

class TasksScreen extends StatefulWidget {
  const TasksScreen({super.key, required this.store, required this.theme});

  final TaskStore store;
  final ThemeController theme;

  @override
  State<TasksScreen> createState() => _TasksScreenState();
}

class _TasksScreenState extends State<TasksScreen> {
  final _input = TextEditingController();
  final _focus = FocusNode();
  final _ringKey = GlobalKey();
  final _confetti = ConfettiController();

  Priority _draftPriority = Priority.none;
  DateTime? _draftDue;
  TaskFilter _filter = TaskFilter.all;

  /// Tasks that just stopped matching the filter stay a moment so the
  /// check animation can finish before they leave.
  final _lingering = <String, Timer>{};
  Timer? _clock;

  TaskStore get store => widget.store;

  @override
  void initState() {
    super.initState();
    _input.addListener(() => setState(() {}));
    _focus.addListener(() => setState(() {}));
    // Keep "Today", greetings and overdue states fresh.
    _clock = Timer.periodic(const Duration(minutes: 1), (_) => setState(() {}));
  }

  @override
  void dispose() {
    _clock?.cancel();
    for (final t in _lingering.values) {
      t.cancel();
    }
    _input.dispose();
    _focus.dispose();
    _confetti.dispose();
    super.dispose();
  }

  /* -------------------------------------------------------------------------
     Actions
     ----------------------------------------------------------------------- */

  void _add() {
    final title = _input.text.trim();
    if (title.isEmpty) return;
    store.add(title, priority: _draftPriority, due: _draftDue);
    HapticFeedback.selectionClick();
    setState(() {
      if (_filter == TaskFilter.done) _filter = TaskFilter.all;
      _draftPriority = Priority.none;
      _draftDue = null;
    });
    _input.clear();
  }

  void _toggle(Task task) {
    store.toggle(task);
    HapticFeedback.lightImpact();
    if (!_filter.matches(task)) {
      _lingering[task.id]?.cancel();
      _lingering[task.id] = Timer(const Duration(milliseconds: 650), () {
        if (mounted) setState(() => _lingering.remove(task.id));
      });
    }
    if (task.done && store.allDone) _celebrate();
  }

  void _delete(Task task) {
    _lingering.remove(task.id)?.cancel();
    _offerUndo('Task deleted', store.remove(task));
  }

  void _clearDone() {
    final removed = store.clearDone();
    if (removed.isEmpty) return;
    HapticFeedback.mediumImpact();
    _offerUndo(
      'Cleared ${removed.length} completed ${removed.length == 1 ? 'task' : 'tasks'}',
      removed,
    );
  }

  void _offerUndo(String message, List<RemovedTask> removed) {
    if (removed.isEmpty) return;
    ScaffoldMessenger.of(context)
      ..hideCurrentSnackBar()
      ..showSnackBar(
        SnackBar(
          content: Text(message),
          persist: false,
          duration: const Duration(seconds: 5),
          action: SnackBarAction(
            label: 'Undo',
            onPressed: () => store.restore(removed),
          ),
        ),
      );
  }

  void _celebrate() {
    HapticFeedback.mediumImpact();
    final box = _ringKey.currentContext?.findRenderObject() as RenderBox?;
    if (box != null) {
      final c = context.colors;
      _confetti.burst(box.localToGlobal(box.size.center(Offset.zero)), [
        c.accent,
        c.high,
        c.medium,
        c.low,
      ]);
    }
    ScaffoldMessenger.of(context)
      ..hideCurrentSnackBar()
      ..showSnackBar(
        const SnackBar(
          content: Text('All done. Nicely played.'),
          duration: Duration(seconds: 3),
        ),
      );
  }

  Future<void> _edit(Task task) async {
    final result = await showModalBottomSheet<TaskEditResult>(
      context: context,
      isScrollControlled: true,
      showDragHandle: true,
      useSafeArea: true,
      builder: (_) => TaskEditor(task: task),
    );
    if (result == null || !mounted) return;
    if (result.delete || result.title.isEmpty) {
      _delete(task);
    } else {
      store.update(
        task,
        title: result.title,
        priority: result.priority,
        due: result.due,
      );
    }
  }

  /// [newIndex] is already adjusted for the removed item (onReorderItem).
  void _reorder(List<Task> visible, int oldIndex, int newIndex) {
    final ids = visible.map((t) => t.id).toList();
    ids.insert(newIndex, ids.removeAt(oldIndex));
    store.reorderSubset(ids);
  }

  void _toggleTheme() {
    final dark = Theme.of(context).brightness == Brightness.dark;
    widget.theme.mode = dark ? ThemeMode.light : ThemeMode.dark;
  }

  /* -------------------------------------------------------------------------
     Build
     ----------------------------------------------------------------------- */

  @override
  Widget build(BuildContext context) {
    final c = context.colors;
    final dark = Theme.of(context).brightness == Brightness.dark;

    return AnnotatedRegion<SystemUiOverlayStyle>(
      value: (dark ? SystemUiOverlayStyle.light : SystemUiOverlayStyle.dark)
          .copyWith(statusBarColor: Colors.transparent),
      child: ListenableBuilder(
        listenable: store,
        builder: (context, _) {
          final all = store.tasks;
          final visible = all
              .where((t) => _filter.matches(t) || _lingering.containsKey(t.id))
              .toList();
          final done = store.doneCount;

          return Stack(
            children: [
              const Positioned.fill(child: Backdrop()),
              CustomScrollView(
                keyboardDismissBehavior:
                    ScrollViewKeyboardDismissBehavior.onDrag,
                slivers: [
                  SliverSafeArea(
                    bottom: false,
                    sliver: SliverPadding(
                      padding: const EdgeInsets.fromLTRB(16, 8, 16, 0),
                      sliver: SliverList.list(
                        children: [
                          _TopBar(dark: dark, onToggleTheme: _toggleTheme),
                          const SizedBox(height: 26),
                          _Hero(tasks: all, ringKey: _ringKey),
                          const SizedBox(height: 26),
                          _Composer(
                            controller: _input,
                            focusNode: _focus,
                            priority: _draftPriority,
                            due: _draftDue,
                            onPriority: (p) =>
                                setState(() => _draftPriority = p),
                            onDue: (d) => setState(() => _draftDue = d),
                            onSubmit: _add,
                          ),
                          const SizedBox(height: 28),
                          _Toolbar(
                            filter: _filter,
                            counts: {
                              TaskFilter.all: all.length,
                              TaskFilter.active: all.length - done,
                              TaskFilter.done: done,
                            },
                            onFilter: (f) => setState(() {
                              _filter = f;
                              for (final t in _lingering.values) {
                                t.cancel();
                              }
                              _lingering.clear();
                            }),
                            onClearDone: done == 0 ? null : _clearDone,
                          ),
                          const SizedBox(height: 12),
                        ],
                      ),
                    ),
                  ),
                  SliverPadding(
                    padding: const EdgeInsets.fromLTRB(16, 0, 16, 28),
                    sliver: DecoratedSliver(
                      decoration: BoxDecoration(
                        color: c.surface,
                        borderRadius: BorderRadius.circular(20),
                        border: Border.all(color: c.line),
                        boxShadow: c.cardShadow,
                      ),
                      sliver: visible.isEmpty
                          ? SliverToBoxAdapter(
                              child: _EmptyState(
                                filter: _filter,
                                hasTasks: all.isNotEmpty,
                              ),
                            )
                          : SliverPadding(
                              padding: const EdgeInsets.all(6),
                              sliver: SliverReorderableList(
                                itemCount: visible.length,
                                onReorderItem: (a, b) =>
                                    _reorder(visible, a, b),
                                onReorderStart: (_) =>
                                    HapticFeedback.selectionClick(),
                                proxyDecorator: (child, index, animation) =>
                                    _DragProxy(
                                      animation: animation,
                                      child: child,
                                    ),
                                itemBuilder: (context, i) {
                                  final task = visible[i];
                                  return _TaskTile(
                                    key: ValueKey(task.id),
                                    task: task,
                                    index: i,
                                    showDivider: i > 0,
                                    onToggle: () => _toggle(task),
                                    onEdit: () => _edit(task),
                                    onDelete: () => _delete(task),
                                  );
                                },
                              ),
                            ),
                    ),
                  ),
                  SliverToBoxAdapter(
                    child: Padding(
                      padding: const EdgeInsets.only(bottom: 24),
                      child: Text(
                        'Saved on this device',
                        textAlign: TextAlign.center,
                        style: TextStyle(fontSize: 12.5, color: c.muted),
                      ),
                    ),
                  ),
                ],
              ),
              Positioned.fill(child: ConfettiLayer(controller: _confetti)),
            ],
          );
        },
      ),
    );
  }
}

/* ---------------------------------------------------------------------------
   Header
   ------------------------------------------------------------------------- */

class _TopBar extends StatelessWidget {
  const _TopBar({required this.dark, required this.onToggleTheme});
  final bool dark;
  final VoidCallback onToggleTheme;

  @override
  Widget build(BuildContext context) {
    final c = context.colors;
    return Row(
      children: [
        const BrandMark(),
        const SizedBox(width: 10),
        Text(
          'Onward',
          style: TextStyle(
            fontFamily: serifFamily,
            fontSize: 21,
            fontWeight: FontWeight.w600,
            letterSpacing: -0.2,
            color: c.ink,
          ),
        ),
        const Spacer(),
        IconButton(
          tooltip: dark ? 'Switch to light theme' : 'Switch to dark theme',
          onPressed: onToggleTheme,
          color: c.ink2,
          icon: AnimatedSwitcher(
            duration: const Duration(milliseconds: 300),
            transitionBuilder: (child, a) => RotationTransition(
              turns: Tween(begin: 0.75, end: 1.0).animate(a),
              child: FadeTransition(opacity: a, child: child),
            ),
            child: Icon(
              dark ? Icons.light_mode_outlined : Icons.dark_mode_outlined,
              key: ValueKey(dark),
              size: 22,
            ),
          ),
        ),
      ],
    );
  }
}

class _Hero extends StatelessWidget {
  const _Hero({required this.tasks, required this.ringKey});
  final List<Task> tasks;
  final GlobalKey ringKey;

  static String _greeting(int hour) {
    if (hour < 5) return 'Still up?';
    if (hour < 12) return 'Good morning';
    if (hour < 17) return 'Good afternoon';
    if (hour < 22) return 'Good evening';
    return 'Good night';
  }

  @override
  Widget build(BuildContext context) {
    final c = context.colors;
    final now = DateTime.now();
    final narrow = MediaQuery.sizeOf(context).width < 380;
    final total = tasks.length;
    final done = tasks.where((t) => t.done).length;
    final active = total - done;
    final open = tasks.where((t) => !t.done && t.due != null);
    final dueToday = open.where((t) => daysBetween(now, t.due!) == 0).length;
    final overdue = open.where((t) => daysBetween(now, t.due!) < 0).length;

    final summaryStyle = TextStyle(fontSize: 15, color: c.ink2, height: 1.4);
    final InlineSpan summary;
    if (total == 0) {
      summary = const TextSpan(text: 'A clean slate. What’s first?');
    } else if (active == 0) {
      summary = const TextSpan(text: 'Everything’s done. Onward.');
    } else {
      summary = TextSpan(
        children: [
          TextSpan(text: '$active ${active == 1 ? 'task' : 'tasks'} to go'),
          if (dueToday > 0) TextSpan(text: ' · $dueToday due today'),
          if (overdue > 0) ...[
            const TextSpan(text: ' · '),
            TextSpan(
              text: '$overdue overdue',
              style: TextStyle(color: c.high),
            ),
          ],
        ],
      );
    }

    return Row(
      crossAxisAlignment: CrossAxisAlignment.end,
      children: [
        Expanded(
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Text(
                '${weekdaysLong[now.weekday - 1]}, ${monthsLong[now.month - 1]} ${now.day}'
                    .toUpperCase(),
                style: TextStyle(
                  fontSize: 12,
                  fontWeight: FontWeight.w600,
                  letterSpacing: 1.4,
                  color: c.muted,
                ),
              ),
              const SizedBox(height: 8),
              Text(
                _greeting(now.hour),
                style: TextStyle(
                  fontFamily: serifFamily,
                  fontSize: narrow ? 34 : 40,
                  fontWeight: FontWeight.w500,
                  height: 1.05,
                  letterSpacing: -1,
                  color: c.ink,
                ),
              ),
              const SizedBox(height: 10),
              Text.rich(summary, style: summaryStyle),
            ],
          ),
        ),
        const SizedBox(width: 16),
        ProgressRing(
          key: ringKey,
          done: done,
          total: total,
          size: narrow ? 58 : 66,
        ),
      ],
    );
  }
}

/* ---------------------------------------------------------------------------
   Composer
   ------------------------------------------------------------------------- */

class _Composer extends StatelessWidget {
  const _Composer({
    required this.controller,
    required this.focusNode,
    required this.priority,
    required this.due,
    required this.onPriority,
    required this.onDue,
    required this.onSubmit,
  });

  final TextEditingController controller;
  final FocusNode focusNode;
  final Priority priority;
  final DateTime? due;
  final ValueChanged<Priority> onPriority;
  final ValueChanged<DateTime?> onDue;
  final VoidCallback onSubmit;

  @override
  Widget build(BuildContext context) {
    final c = context.colors;
    final focused = focusNode.hasFocus;
    final canAdd = controller.text.trim().isNotEmpty;
    final wide = MediaQuery.sizeOf(context).width >= 440;

    return AnimatedContainer(
      duration: const Duration(milliseconds: 250),
      curve: Curves.easeOut,
      decoration: BoxDecoration(
        color: c.surface,
        borderRadius: BorderRadius.circular(20),
        border: Border.all(
          color: focused ? c.accent.withValues(alpha: 0.4) : c.line,
        ),
        boxShadow: [
          if (focused) BoxShadow(color: c.accentSoft, spreadRadius: 4),
          ...(focused ? c.liftShadow : c.cardShadow),
        ],
      ),
      child: Column(
        children: [
          Padding(
            padding: const EdgeInsets.fromLTRB(16, 6, 6, 6),
            child: Row(
              children: [
                AnimatedRotation(
                  turns: focused ? 0.25 : 0,
                  duration: const Duration(milliseconds: 300),
                  curve: Curves.easeOutBack,
                  child: AnimatedContainer(
                    duration: const Duration(milliseconds: 250),
                    width: 24,
                    height: 24,
                    decoration: BoxDecoration(
                      shape: BoxShape.circle,
                      border: Border.all(
                        color: focused ? c.accent : c.lineStrong,
                        width: 1.5,
                      ),
                    ),
                    child: Icon(
                      Icons.add_rounded,
                      size: 16,
                      color: focused ? c.accent : c.muted,
                    ),
                  ),
                ),
                const SizedBox(width: 12),
                Expanded(
                  child: TextField(
                    controller: controller,
                    focusNode: focusNode,
                    textCapitalization: TextCapitalization.sentences,
                    textInputAction: TextInputAction.done,
                    maxLength: 300,
                    style: TextStyle(fontSize: 16.5, color: c.ink),
                    cursorColor: c.accent,
                    decoration: InputDecoration(
                      isCollapsed: true,
                      filled: false,
                      border: InputBorder.none,
                      enabledBorder: InputBorder.none,
                      focusedBorder: InputBorder.none,
                      contentPadding: const EdgeInsets.symmetric(vertical: 12),
                      counterText: '',
                      hintText: 'What needs doing?',
                      hintStyle: TextStyle(fontSize: 16.5, color: c.muted),
                    ),
                    onSubmitted: (_) {
                      onSubmit();
                      if (controller.text.isEmpty) focusNode.requestFocus();
                    },
                  ),
                ),
                const SizedBox(width: 8),
                AnimatedOpacity(
                  opacity: canAdd ? 1 : 0.28,
                  duration: const Duration(milliseconds: 200),
                  child: Material(
                    color: c.ink,
                    borderRadius: BorderRadius.circular(13),
                    child: InkWell(
                      borderRadius: BorderRadius.circular(13),
                      onTap: canAdd ? onSubmit : null,
                      child: SizedBox(
                        height: 44,
                        child: Padding(
                          padding: EdgeInsets.symmetric(
                            horizontal: wide ? 18 : 12,
                          ),
                          child: Row(
                            mainAxisSize: MainAxisSize.min,
                            children: [
                              if (wide) ...[
                                Text(
                                  'Add',
                                  style: TextStyle(
                                    color: c.bg,
                                    fontWeight: FontWeight.w600,
                                    fontSize: 14.5,
                                  ),
                                ),
                                const SizedBox(width: 6),
                              ],
                              Icon(
                                Icons.arrow_upward_rounded,
                                color: c.bg,
                                size: 20,
                                semanticLabel: 'Add task',
                              ),
                            ],
                          ),
                        ),
                      ),
                    ),
                  ),
                ),
              ],
            ),
          ),
          Divider(height: 1, color: c.line),
          Padding(
            padding: const EdgeInsets.fromLTRB(10, 8, 10, 8),
            child: Row(
              mainAxisAlignment: MainAxisAlignment.spaceBetween,
              children: [
                Flexible(
                  child: SingleChildScrollView(
                    scrollDirection: Axis.horizontal,
                    child: PriorityPicker(
                      value: priority,
                      onChanged: onPriority,
                      compact: !wide,
                    ),
                  ),
                ),
                const SizedBox(width: 8),
                DueChip(value: due, onChanged: onDue),
              ],
            ),
          ),
        ],
      ),
    );
  }
}

/* ---------------------------------------------------------------------------
   Filters
   ------------------------------------------------------------------------- */

class _Toolbar extends StatelessWidget {
  const _Toolbar({
    required this.filter,
    required this.counts,
    required this.onFilter,
    required this.onClearDone,
  });

  final TaskFilter filter;
  final Map<TaskFilter, int> counts;
  final ValueChanged<TaskFilter> onFilter;
  final VoidCallback? onClearDone;

  @override
  Widget build(BuildContext context) {
    final c = context.colors;
    return Row(
      children: [
        Expanded(
          child: Align(
            alignment: Alignment.centerLeft,
            child: ConstrainedBox(
              constraints: const BoxConstraints(maxWidth: 300),
              child: Container(
                height: 42,
                padding: const EdgeInsets.all(4),
                decoration: BoxDecoration(
                  color: c.surface2,
                  borderRadius: BorderRadius.circular(14),
                  border: Border.all(color: c.line),
                ),
                child: LayoutBuilder(
                  builder: (context, box) {
                    final w = box.maxWidth / TaskFilter.values.length;
                    return Stack(
                      children: [
                        AnimatedPositioned(
                          duration: const Duration(milliseconds: 380),
                          curve: Curves.easeOutCubic,
                          left: w * filter.index,
                          top: 0,
                          bottom: 0,
                          width: w,
                          child: DecoratedBox(
                            decoration: BoxDecoration(
                              color: c.surface,
                              borderRadius: BorderRadius.circular(10),
                              border: Border.all(color: c.line),
                              boxShadow: [
                                BoxShadow(
                                  color: c.shadow.withValues(
                                    alpha: c.shadow.a * 0.3,
                                  ),
                                  blurRadius: 3,
                                  offset: const Offset(0, 1),
                                ),
                              ],
                            ),
                          ),
                        ),
                        Positioned.fill(
                          child: Row(
                            crossAxisAlignment: CrossAxisAlignment.stretch,
                            children: [
                              for (final f in TaskFilter.values)
                                Expanded(
                                  child: Semantics(
                                    selected: f == filter,
                                    button: true,
                                    child: GestureDetector(
                                      behavior: HitTestBehavior.opaque,
                                      onTap: () {
                                        if (f != filter) {
                                          HapticFeedback.selectionClick();
                                          onFilter(f);
                                        }
                                      },
                                      child: _TabLabel(
                                        label: f.label,
                                        count: counts[f] ?? 0,
                                        selected: f == filter,
                                      ),
                                    ),
                                  ),
                                ),
                            ],
                          ),
                        ),
                      ],
                    );
                  },
                ),
              ),
            ),
          ),
        ),
        const SizedBox(width: 8),
        AnimatedOpacity(
          opacity: onClearDone == null ? 0 : 1,
          duration: const Duration(milliseconds: 200),
          child: IconButton(
            tooltip: 'Clear done',
            onPressed: onClearDone,
            color: c.muted,
            icon: const Icon(Icons.delete_sweep_outlined),
          ),
        ),
      ],
    );
  }
}

class _TabLabel extends StatelessWidget {
  const _TabLabel({
    required this.label,
    required this.count,
    required this.selected,
  });
  final String label;
  final int count;
  final bool selected;

  @override
  Widget build(BuildContext context) {
    final c = context.colors;
    return FittedBox(
      fit: BoxFit.scaleDown,
      child: Padding(
        padding: const EdgeInsets.symmetric(horizontal: 4),
        child: Row(
          mainAxisSize: MainAxisSize.min,
          children: [
            AnimatedDefaultTextStyle(
              duration: const Duration(milliseconds: 200),
              style: TextStyle(
                fontFamily: sansFamily,
                fontSize: 13.5,
                fontWeight: FontWeight.w500,
                color: selected ? c.ink : c.muted,
              ),
              child: Text(label),
            ),
            const SizedBox(width: 6),
            AnimatedContainer(
              duration: const Duration(milliseconds: 200),
              constraints: const BoxConstraints(minWidth: 20),
              height: 18,
              padding: const EdgeInsets.symmetric(horizontal: 6),
              alignment: Alignment.center,
              decoration: BoxDecoration(
                color: selected ? c.accentSoft : c.line,
                borderRadius: BorderRadius.circular(9),
              ),
              child: Text(
                '$count',
                style: TextStyle(
                  fontSize: 11.5,
                  fontWeight: FontWeight.w600,
                  color: selected ? c.accent : c.ink2,
                  fontFeatures: const [FontFeature.tabularFigures()],
                ),
              ),
            ),
          ],
        ),
      ),
    );
  }
}

/* ---------------------------------------------------------------------------
   Task rows
   ------------------------------------------------------------------------- */

class _TaskTile extends StatelessWidget {
  const _TaskTile({
    super.key,
    required this.task,
    required this.index,
    required this.showDivider,
    required this.onToggle,
    required this.onEdit,
    required this.onDelete,
  });

  final Task task;
  final int index;
  final bool showDivider;
  final VoidCallback onToggle;
  final VoidCallback onEdit;
  final VoidCallback onDelete;

  @override
  Widget build(BuildContext context) {
    final c = context.colors;
    final due = task.due == null ? null : describeDue(task.due!);
    final hasMeta = due != null || task.priority != Priority.none;

    Widget swipeBg(
      Color bg,
      Color fg,
      IconData icon,
      String label,
      Alignment align,
    ) => Container(
      decoration: BoxDecoration(
        color: bg,
        borderRadius: BorderRadius.circular(14),
      ),
      alignment: align,
      padding: const EdgeInsets.symmetric(horizontal: 20),
      child: Row(
        mainAxisSize: MainAxisSize.min,
        children: [
          Icon(icon, color: fg, size: 20),
          const SizedBox(width: 8),
          Text(
            label,
            style: TextStyle(color: fg, fontWeight: FontWeight.w600),
          ),
        ],
      ),
    );

    return _FadeIn(
      child: Dismissible(
        key: ValueKey('dismiss-${task.id}'),
        background: swipeBg(
          c.accentSoft,
          c.accent,
          task.done ? Icons.undo_rounded : Icons.check_rounded,
          task.done ? 'Undo' : 'Done',
          Alignment.centerLeft,
        ),
        secondaryBackground: swipeBg(
          c.dangerSoft,
          c.high,
          Icons.delete_outline_rounded,
          'Delete',
          Alignment.centerRight,
        ),
        confirmDismiss: (direction) async {
          if (direction == DismissDirection.startToEnd) {
            onToggle();
            return false;
          }
          return true;
        },
        onDismissed: (_) => onDelete(),
        child: Stack(
          children: [
            Material(
              type: MaterialType.transparency,
              child: InkWell(
                borderRadius: BorderRadius.circular(14),
                onTap: onEdit,
                child: Padding(
                  padding: const EdgeInsets.fromLTRB(0, 4, 12, 4),
                  child: Row(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      ReorderableDragStartListener(
                        index: index,
                        child: Semantics(
                          label: 'Drag to reorder',
                          child: SizedBox(
                            width: 26,
                            height: 44,
                            child: Icon(
                              Icons.drag_indicator_rounded,
                              size: 18,
                              color: c.muted.withValues(alpha: 0.55),
                            ),
                          ),
                        ),
                      ),
                      CheckCircle(
                        checked: task.done,
                        ringColor: c.priority(task.priority),
                        emphasis: task.priority != Priority.none,
                        onTap: onToggle,
                        semanticLabel: task.title,
                      ),
                      const SizedBox(width: 4),
                      Expanded(
                        child: Padding(
                          padding: EdgeInsets.only(
                            top: 11,
                            bottom: hasMeta ? 8 : 11,
                          ),
                          child: Column(
                            crossAxisAlignment: CrossAxisAlignment.start,
                            children: [
                              AnimatedDefaultTextStyle(
                                duration: const Duration(milliseconds: 300),
                                style: TextStyle(
                                  fontFamily: sansFamily,
                                  fontSize: 16,
                                  height: 1.4,
                                  color: task.done ? c.muted : c.ink,
                                  decoration: task.done
                                      ? TextDecoration.lineThrough
                                      : TextDecoration.none,
                                  decorationColor: c.muted,
                                  decorationThickness: 1.5,
                                ),
                                child: Text(task.title),
                              ),
                              if (hasMeta) ...[
                                const SizedBox(height: 4),
                                Wrap(
                                  spacing: 14,
                                  runSpacing: 4,
                                  children: [
                                    if (due != null)
                                      _MetaChip(
                                        icon: Icons.event_outlined,
                                        label: due.label,
                                        color: task.done
                                            ? c.muted
                                            : switch (due.state) {
                                                DueState.overdue => c.high,
                                                DueState.today => c.accent,
                                                DueState.upcoming => c.muted,
                                              },
                                      ),
                                    if (task.priority != Priority.none)
                                      _MetaChip(
                                        icon: Icons.flag_outlined,
                                        label: task.priority.label,
                                        color: task.done
                                            ? c.muted
                                            : c.priority(task.priority),
                                      ),
                                  ],
                                ),
                              ],
                            ],
                          ),
                        ),
                      ),
                    ],
                  ),
                ),
              ),
            ),
            if (showDivider)
              Positioned(
                top: 0,
                left: 74,
                right: 12,
                child: IgnorePointer(
                  child: Container(height: 1, color: c.line),
                ),
              ),
          ],
        ),
      ),
    );
  }
}

class _MetaChip extends StatelessWidget {
  const _MetaChip({
    required this.icon,
    required this.label,
    required this.color,
  });
  final IconData icon;
  final String label;
  final Color color;

  @override
  Widget build(BuildContext context) {
    return Row(
      mainAxisSize: MainAxisSize.min,
      children: [
        Icon(icon, size: 14, color: color),
        const SizedBox(width: 5),
        Text(
          label,
          style: TextStyle(
            fontSize: 12.5,
            fontWeight: FontWeight.w500,
            color: color,
          ),
        ),
      ],
    );
  }
}

/// The lifted look of a row while it's being dragged.
class _DragProxy extends StatelessWidget {
  const _DragProxy({required this.animation, required this.child});
  final Animation<double> animation;
  final Widget child;

  @override
  Widget build(BuildContext context) {
    final c = context.colors;
    return AnimatedBuilder(
      animation: animation,
      builder: (context, child) {
        final t = Curves.easeOut.transform(animation.value);
        return Transform.scale(
          scale: 1 + 0.02 * t,
          child: DecoratedBox(
            decoration: BoxDecoration(
              color: c.surface,
              borderRadius: BorderRadius.circular(14),
              border: Border.all(color: c.line.withValues(alpha: c.line.a * t)),
              boxShadow: [
                for (final s in c.liftShadow)
                  BoxShadow(
                    color: s.color.withValues(alpha: s.color.a * t),
                    blurRadius: s.blurRadius,
                    spreadRadius: s.spreadRadius,
                    offset: s.offset,
                  ),
              ],
            ),
            child: child,
          ),
        );
      },
      child: Material(type: MaterialType.transparency, child: child),
    );
  }
}

/// Fades and slides a row in the first time it appears.
class _FadeIn extends StatefulWidget {
  const _FadeIn({required this.child});
  final Widget child;

  @override
  State<_FadeIn> createState() => _FadeInState();
}

class _FadeInState extends State<_FadeIn> with SingleTickerProviderStateMixin {
  late final _c = AnimationController(
    vsync: this,
    duration: const Duration(milliseconds: 380),
  )..forward();
  late final _curve = CurvedAnimation(parent: _c, curve: Curves.easeOutCubic);

  @override
  void dispose() {
    _curve.dispose();
    _c.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    return FadeTransition(
      opacity: _curve,
      child: SlideTransition(
        position: Tween(
          begin: const Offset(0, -0.12),
          end: Offset.zero,
        ).animate(_curve),
        child: widget.child,
      ),
    );
  }
}

/* ---------------------------------------------------------------------------
   Empty state
   ------------------------------------------------------------------------- */

class _EmptyState extends StatelessWidget {
  const _EmptyState({required this.filter, required this.hasTasks});
  final TaskFilter filter;
  final bool hasTasks;

  @override
  Widget build(BuildContext context) {
    final c = context.colors;
    final (title, body) = switch (filter) {
      TaskFilter.all => (
        'All clear',
        'Nothing on your plate. Add a task above to get going.',
      ),
      TaskFilter.active when hasTasks => (
        'Everything’s done',
        'Every task is checked off. Take a breath — you earned it.',
      ),
      TaskFilter.active => (
        'All clear',
        'No active tasks. Add one above when you’re ready.',
      ),
      TaskFilter.done => (
        'Nothing done yet',
        'Completed tasks will gather here. You’ve got this.',
      ),
    };
    return Padding(
      padding: const EdgeInsets.fromLTRB(24, 40, 24, 44),
      child: Column(
        children: [
          const SunriseArt(),
          const SizedBox(height: 16),
          Text(
            title,
            textAlign: TextAlign.center,
            style: TextStyle(
              fontFamily: serifFamily,
              fontSize: 23,
              fontWeight: FontWeight.w500,
              letterSpacing: -0.3,
              color: c.ink,
            ),
          ),
          const SizedBox(height: 6),
          ConstrainedBox(
            constraints: const BoxConstraints(maxWidth: 300),
            child: Text(
              body,
              textAlign: TextAlign.center,
              style: TextStyle(fontSize: 14.5, height: 1.45, color: c.muted),
            ),
          ),
        ],
      ),
    );
  }
}
