import 'package:flutter/material.dart';

import '../data/habit_store.dart';
import '../data/task_store.dart';
import '../data/theme_controller.dart';
import 'home_screen.dart';
import 'tasks_screen.dart';

/// The app frame: Tasks and Habits tabs with a bottom navigation bar.
class Shell extends StatefulWidget {
  const Shell({
    super.key,
    required this.tasks,
    required this.habits,
    required this.theme,
  });

  final TaskStore tasks;
  final HabitStore habits;
  final ThemeController theme;

  @override
  State<Shell> createState() => _ShellState();
}

class _ShellState extends State<Shell> {
  int _index = 0;

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      body: IndexedStack(
        index: _index,
        children: [
          TasksScreen(store: widget.tasks, theme: widget.theme),
          HomeScreen(store: widget.habits),
        ],
      ),
      bottomNavigationBar: DecoratedBox(
        decoration: BoxDecoration(
          border: Border(
            top: BorderSide(
              color: Theme.of(context).colorScheme.outlineVariant,
            ),
          ),
        ),
        child: NavigationBar(
          selectedIndex: _index,
          onDestinationSelected: (i) {
            FocusManager.instance.primaryFocus?.unfocus();
            setState(() => _index = i);
          },
          destinations: const [
            NavigationDestination(
              icon: Icon(Icons.check_circle_outline_rounded),
              selectedIcon: Icon(Icons.check_circle_rounded),
              label: 'Tasks',
            ),
            NavigationDestination(
              icon: Icon(Icons.local_fire_department_outlined),
              selectedIcon: Icon(Icons.local_fire_department_rounded),
              label: 'Habits',
            ),
          ],
        ),
      ),
    );
  }
}
