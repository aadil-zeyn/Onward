import 'package:flutter/foundation.dart';
import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:shared_preferences/shared_preferences.dart';

import 'data/habit_store.dart';
import 'data/task_store.dart';
import 'data/theme_controller.dart';
import 'screens/shell.dart';
import 'theme.dart';

Future<void> main() async {
  WidgetsFlutterBinding.ensureInitialized();
  LicenseRegistry.addLicense(() async* {
    for (final (name, file) in const [
      ('DM Sans', 'OFL-DMSans.txt'),
      ('Fraunces', 'OFL-Fraunces.txt'),
    ]) {
      yield LicenseEntryWithLineBreaks([
        name,
      ], await rootBundle.loadString('assets/fonts/$file'));
    }
  });
  SystemChrome.setEnabledSystemUIMode(SystemUiMode.edgeToEdge);

  final prefs = await SharedPreferences.getInstance();
  runApp(
    OnwardApp(
      habits: HabitStore(prefs),
      tasks: TaskStore(prefs),
      theme: ThemeController(prefs),
    ),
  );
}

class OnwardApp extends StatelessWidget {
  const OnwardApp({
    super.key,
    required this.habits,
    required this.tasks,
    required this.theme,
  });

  final HabitStore habits;
  final TaskStore tasks;
  final ThemeController theme;

  @override
  Widget build(BuildContext context) {
    return ListenableBuilder(
      listenable: theme,
      builder: (context, _) => MaterialApp(
        title: 'Onward',
        debugShowCheckedModeBanner: false,
        theme: buildTheme(Brightness.light),
        darkTheme: buildTheme(Brightness.dark),
        themeMode: theme.mode,
        home: Shell(tasks: tasks, habits: habits, theme: theme),
      ),
    );
  }
}
