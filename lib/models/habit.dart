/// A habit the user wants to keep, plus the days they completed it.
class Habit {
  Habit({
    required this.id,
    required this.name,
    required this.colorValue,
    Set<String>? completedDays,
  }) : completedDays = completedDays ?? <String>{};

  final String id;
  String name;
  int colorValue;

  /// Completed days stored as `yyyy-mm-dd` keys.
  final Set<String> completedDays;

  static String dayKey(DateTime d) =>
      '${d.year.toString().padLeft(4, '0')}-'
      '${d.month.toString().padLeft(2, '0')}-'
      '${d.day.toString().padLeft(2, '0')}';

  bool isDoneOn(DateTime day) => completedDays.contains(dayKey(day));

  void toggle(DateTime day) {
    final key = dayKey(day);
    if (!completedDays.remove(key)) completedDays.add(key);
  }

  /// Consecutive completed days ending today. If today isn't done yet,
  /// the streak counts back from yesterday so it doesn't reset mid-day.
  int currentStreak([DateTime? now]) {
    var day = _dateOnly(now ?? DateTime.now());
    if (!isDoneOn(day)) day = day.subtract(const Duration(days: 1));
    var streak = 0;
    while (isDoneOn(day)) {
      streak++;
      day = day.subtract(const Duration(days: 1));
    }
    return streak;
  }

  static DateTime _dateOnly(DateTime d) => DateTime(d.year, d.month, d.day);

  Map<String, dynamic> toJson() => {
    'id': id,
    'name': name,
    'color': colorValue,
    'days': completedDays.toList(),
  };

  factory Habit.fromJson(Map<String, dynamic> json) => Habit(
    id: json['id'] as String,
    name: json['name'] as String,
    colorValue: json['color'] as int,
    completedDays: (json['days'] as List).cast<String>().toSet(),
  );
}
