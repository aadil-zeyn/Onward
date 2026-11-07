import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:shared_preferences/shared_preferences.dart';

import 'package:onward/data/habit_store.dart';
import 'package:onward/main.dart';
import 'package:onward/models/habit.dart';

void main() {
  group('Habit.currentStreak', () {
    final now = DateTime(2026, 11, 7, 15);

    test('counts consecutive days ending today', () {
      final h = Habit(id: '1', name: 'x', colorValue: 0);
      h.toggle(DateTime(2026, 11, 7));
      h.toggle(DateTime(2026, 11, 6));
      h.toggle(DateTime(2026, 11, 5));
      h.toggle(DateTime(2026, 11, 3)); // gap on the 4th
      expect(h.currentStreak(now), 3);
    });

    test('keeps yesterday\'s streak when today is not done yet', () {
      final h = Habit(id: '1', name: 'x', colorValue: 0);
      h.toggle(DateTime(2026, 11, 6));
      h.toggle(DateTime(2026, 11, 5));
      expect(h.currentStreak(now), 2);
    });

    test('is zero with no recent completions', () {
      final h = Habit(id: '1', name: 'x', colorValue: 0);
      h.toggle(DateTime(2026, 11, 1));
      expect(h.currentStreak(now), 0);
    });
  });

  test('store persists habits across instances', () async {
    SharedPreferences.setMockInitialValues({});
    final prefs = await SharedPreferences.getInstance();
    final store = HabitStore(prefs);
    store.add('Walk', 0xFF000000);
    store.toggle(store.habits.first, DateTime(2026, 11, 7));

    final reloaded = HabitStore(prefs);
    expect(reloaded.habits.single.name, 'Walk');
    expect(reloaded.habits.single.isDoneOn(DateTime(2026, 11, 7)), isTrue);
  });

  testWidgets('add a habit and check it off', (tester) async {
    SharedPreferences.setMockInitialValues({});
    final prefs = await SharedPreferences.getInstance();
    await tester.pumpWidget(OnwardApp(store: HabitStore(prefs)));

    expect(find.text('Start something small'), findsOneWidget);

    await tester.tap(find.text('New habit'));
    await tester.pumpAndSettle();
    await tester.enterText(find.byType(TextField), 'Meditate');
    await tester.tap(find.text('Add'));
    await tester.pumpAndSettle();

    expect(find.text('Meditate'), findsOneWidget);
    expect(find.text('0 of 1 done'), findsOneWidget);

    await tester.tap(find.byTooltip('Mark done'));
    await tester.pumpAndSettle();

    expect(find.text('1 of 1 done'), findsOneWidget);
    expect(find.text('🔥 1-day streak'), findsOneWidget);
  });
}
