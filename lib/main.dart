import 'package:flutter/material.dart';
import 'package:shared_preferences/shared_preferences.dart';

import 'data/habit_store.dart';
import 'screens/home_screen.dart';

Future<void> main() async {
  WidgetsFlutterBinding.ensureInitialized();
  final prefs = await SharedPreferences.getInstance();
  runApp(OnwardApp(store: HabitStore(prefs)));
}

class OnwardApp extends StatelessWidget {
  const OnwardApp({super.key, required this.store});

  final HabitStore store;

  @override
  Widget build(BuildContext context) {
    const seed = Color(0xFF3F7D58);
    return MaterialApp(
      title: 'Onward',
      debugShowCheckedModeBanner: false,
      theme: ThemeData(colorSchemeSeed: seed, useMaterial3: true),
      darkTheme: ThemeData(
        colorSchemeSeed: seed,
        brightness: Brightness.dark,
        useMaterial3: true,
      ),
      home: HomeScreen(store: store),
    );
  }
}
