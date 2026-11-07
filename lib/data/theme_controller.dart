import 'package:flutter/material.dart';
import 'package:shared_preferences/shared_preferences.dart';

/// Remembers whether the app follows the system theme or a chosen one.
class ThemeController extends ChangeNotifier {
  ThemeController(this._prefs)
    : _mode =
          ThemeMode.values.asNameMap()[_prefs.getString(_key)] ??
          ThemeMode.system;

  static const _key = 'onward.themeMode';
  final SharedPreferences _prefs;
  ThemeMode _mode;

  ThemeMode get mode => _mode;

  set mode(ThemeMode value) {
    if (value == _mode) return;
    _mode = value;
    _prefs.setString(_key, value.name);
    notifyListeners();
  }
}
