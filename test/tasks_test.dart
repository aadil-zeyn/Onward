import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:shared_preferences/shared_preferences.dart';

import 'package:onward/data/habit_store.dart';
import 'package:onward/data/task_store.dart';
import 'package:onward/data/theme_controller.dart';
import 'package:onward/main.dart';
import 'package:onward/models/task.dart';
import 'package:onward/widgets/check_circle.dart';

Finder checkFor(String title) =>
    find.byWidgetPredicate((w) => w is CheckCircle && w.semanticLabel == title);

void main() {
  setUp(() => SharedPreferences.setMockInitialValues({}));

  group('describeDue', () {
    final now = DateTime(2026, 11, 7, 9); // a Saturday

    test('uses relative words close to today', () {
      expect(describeDue(DateTime(2026, 11, 7), now).label, 'Today');
      expect(describeDue(DateTime(2026, 11, 8), now).label, 'Tomorrow');
      expect(describeDue(DateTime(2026, 11, 6), now).label, 'Yesterday');
      expect(describeDue(DateTime(2026, 11, 11), now).label, 'Wednesday');
    });

    test('falls back to a short date, with the year when it differs', () {
      expect(describeDue(DateTime(2026, 11, 20), now).label, 'Nov 20');
      expect(describeDue(DateTime(2027, 1, 3), now).label, 'Jan 3, 2027');
    });

    test('reports overdue, today and upcoming states', () {
      expect(describeDue(DateTime(2026, 11, 1), now).state, DueState.overdue);
      expect(describeDue(DateTime(2026, 11, 7), now).state, DueState.today);
      expect(describeDue(DateTime(2026, 12, 1), now).state, DueState.upcoming);
    });

    test('next week means the coming Monday', () {
      expect(nextMonday(now), DateTime(2026, 11, 9));
      expect(nextMonday(DateTime(2026, 11, 9, 10)), DateTime(2026, 11, 16));
    });
  });

  group('TaskStore', () {
    test('seeds welcome tasks on first run only', () async {
      final prefs = await SharedPreferences.getInstance();
      final store = TaskStore(prefs);
      expect(store.tasks, isNotEmpty);
      store.clearDone();
      for (final t in store.tasks) {
        store.remove(t);
      }
      expect(TaskStore(prefs).tasks, isEmpty);
    });

    test('adds to the top and persists every field', () async {
      final prefs = await SharedPreferences.getInstance();
      final store = TaskStore(prefs, seedIfNew: false);
      store.add('First');
      store.add(
        'Second',
        priority: Priority.high,
        due: DateTime(2026, 11, 9, 18, 30),
      );
      store.toggle(store.tasks.last);

      final reloaded = TaskStore(prefs).tasks;
      expect(reloaded.map((t) => t.title), ['Second', 'First']);
      expect(reloaded.first.priority, Priority.high);
      expect(reloaded.first.due, DateTime(2026, 11, 9));
      expect(reloaded.last.done, isTrue);
      expect(reloaded.last.completedAt, isNotNull);
    });

    test('restores deleted tasks to their original positions', () async {
      final prefs = await SharedPreferences.getInstance();
      final store = TaskStore(prefs, seedIfNew: false);
      for (final t in ['d', 'c', 'b', 'a']) {
        store.add(t);
      }
      store.toggle(store.tasks[1]); // b
      store.toggle(store.tasks[3]); // d
      final removed = store.clearDone();
      expect(store.tasks.map((t) => t.title), ['a', 'c']);
      store.restore(removed);
      expect(store.tasks.map((t) => t.title), ['a', 'b', 'c', 'd']);
    });

    test('reorders a filtered subset without moving the others', () async {
      final prefs = await SharedPreferences.getInstance();
      final store = TaskStore(prefs, seedIfNew: false);
      for (final t in ['e', 'd', 'c', 'b', 'a']) {
        store.add(t);
      }
      store.toggle(store.tasks[1]); // b is done, so not "active"
      store.toggle(store.tasks[3]); // d is done
      final active = store.tasks
          .where((t) => !t.done)
          .map((t) => t.id)
          .toList(); // a c e
      store.reorderSubset([active[2], active[0], active[1]]); // e a c
      expect(store.tasks.map((t) => t.title), ['e', 'b', 'a', 'd', 'c']);
    });
  });

  group('Tasks screen', () {
    Future<TaskStore> pumpApp(WidgetTester tester) async {
      final prefs = await SharedPreferences.getInstance();
      final tasks = TaskStore(prefs, seedIfNew: false);
      await tester.pumpWidget(
        OnwardApp(
          habits: HabitStore(prefs),
          tasks: tasks,
          theme: ThemeController(prefs),
        ),
      );
      await tester.pumpAndSettle();
      return tasks;
    }

    Future<void> addTask(WidgetTester tester, String title) async {
      await tester.enterText(find.byType(TextField).first, title);
      await tester.testTextInput.receiveAction(TextInputAction.done);
      await tester.pumpAndSettle();
    }

    testWidgets('adding and completing a task updates the progress', (
      tester,
    ) async {
      final store = await pumpApp(tester);
      expect(find.text('All clear'), findsOneWidget);
      expect(find.text('0%'), findsOneWidget);

      await addTask(tester, 'Buy oat milk');
      await addTask(tester, 'Call mum');
      expect(store.tasks.map((t) => t.title), ['Call mum', 'Buy oat milk']);
      expect(find.text('2 tasks to go'), findsOneWidget);

      await tester.tap(checkFor('Call mum'));
      await tester.pumpAndSettle();
      expect(store.tasks.first.done, isTrue);
      expect(find.text('50%'), findsOneWidget);
      expect(find.text('1 task to go'), findsOneWidget);

      await tester.tap(checkFor('Buy oat milk'));
      await tester.pumpAndSettle();
      expect(find.text('100%'), findsOneWidget);
      expect(find.text('Everything’s done. Onward.'), findsOneWidget);
      expect(find.text('All done. Nicely played.'), findsOneWidget);
    });

    testWidgets('filters show only matching tasks', (tester) async {
      final store = await pumpApp(tester);
      await addTask(tester, 'Open item');
      await addTask(tester, 'Finished item');
      store.toggle(store.tasks.first);
      await tester.pumpAndSettle();

      await tester.tap(find.text('Active'));
      await tester.pumpAndSettle();
      expect(find.text('Open item'), findsOneWidget);
      expect(find.text('Finished item'), findsNothing);

      await tester.tap(find.text('Done'));
      await tester.pumpAndSettle();
      expect(find.text('Open item'), findsNothing);
      expect(find.text('Finished item'), findsOneWidget);
    });

    testWidgets('swipe to delete, then undo', (tester) async {
      final store = await pumpApp(tester);
      await addTask(tester, 'Temporary');

      await tester.drag(find.text('Temporary'), const Offset(-500, 0));
      await tester.pumpAndSettle();
      expect(store.tasks, isEmpty);
      expect(find.text('Task deleted'), findsOneWidget);

      await tester.tap(find.text('Undo'));
      await tester.pumpAndSettle();
      expect(store.tasks.single.title, 'Temporary');
      expect(find.text('Temporary'), findsOneWidget);
    });

    testWidgets('tap a task to edit its title and priority', (tester) async {
      final store = await pumpApp(tester);
      await addTask(tester, 'Draft');

      await tester.tap(find.text('Draft'));
      await tester.pumpAndSettle();
      expect(find.text('Edit task'), findsOneWidget);

      await tester.enterText(
        find.widgetWithText(TextField, 'Draft'),
        'Final copy',
      );
      await tester.tap(find.bySemanticsLabel('High priority').last);
      await tester.tap(find.widgetWithText(FilledButton, 'Save'));
      await tester.pumpAndSettle();

      expect(store.tasks.single.title, 'Final copy');
      expect(store.tasks.single.priority, Priority.high);
      expect(find.text('Final copy'), findsOneWidget);
      expect(find.text('High'), findsWidgets);
    });
  });
}
