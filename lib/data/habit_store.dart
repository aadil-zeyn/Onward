import 'dart:convert';

import 'package:flutter/foundation.dart';
import 'package:shared_preferences/shared_preferences.dart';

import '../models/habit.dart';

/// Holds the habit list and saves it to local storage on every change.
class HabitStore extends ChangeNotifier {
  HabitStore(this._prefs) {
    final raw = _prefs.getString(_key);
    if (raw != null) {
      final list = jsonDecode(raw) as List;
      _habits.addAll(
        list.map((e) => Habit.fromJson(e as Map<String, dynamic>)),
      );
    }
  }

  static const _key = 'onward.habits';
  final SharedPreferences _prefs;
  final List<Habit> _habits = [];

  List<Habit> get habits => List.unmodifiable(_habits);

  void add(String name, int colorValue) {
    _habits.add(
      Habit(
        id: DateTime.now().microsecondsSinceEpoch.toString(),
        name: name,
        colorValue: colorValue,
      ),
    );
    _save();
  }

  void update(Habit habit, {required String name, required int colorValue}) {
    habit
      ..name = name
      ..colorValue = colorValue;
    _save();
  }

  void remove(Habit habit) {
    _habits.removeWhere((h) => h.id == habit.id);
    _save();
  }

  void toggle(Habit habit, DateTime day) {
    habit.toggle(day);
    _save();
  }

  void _save() {
    _prefs.setString(_key, jsonEncode(_habits.map((h) => h.toJson()).toList()));
    notifyListeners();
  }
}
