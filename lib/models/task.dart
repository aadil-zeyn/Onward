/// How important a task is. Drives the colour of its checkbox and flag.
enum Priority {
  none('None'),
  low('Low'),
  medium('Medium'),
  high('High');

  const Priority(this.label);
  final String label;
}

/// A to-do item. Due dates are calendar days with no time component.
class Task {
  Task({
    required this.id,
    required this.title,
    this.done = false,
    this.priority = Priority.none,
    this.due,
    DateTime? createdAt,
    this.completedAt,
  }) : createdAt = createdAt ?? DateTime.now();

  final String id;
  String title;
  bool done;
  Priority priority;
  DateTime? due;
  final DateTime createdAt;
  DateTime? completedAt;

  Map<String, dynamic> toJson() => {
    'id': id,
    'title': title,
    'done': done,
    'priority': priority.name,
    'due': due == null ? null : dateKey(due!),
    'created': createdAt.millisecondsSinceEpoch,
    'completed': completedAt?.millisecondsSinceEpoch,
  };

  factory Task.fromJson(Map<String, dynamic> json) => Task(
    id: json['id'] as String,
    title: json['title'] as String,
    done: json['done'] as bool? ?? false,
    priority: Priority.values.asNameMap()[json['priority']] ?? Priority.none,
    due: parseDateKey(json['due'] as String?),
    createdAt: json['created'] is int
        ? DateTime.fromMillisecondsSinceEpoch(json['created'] as int)
        : null,
    completedAt: json['completed'] is int
        ? DateTime.fromMillisecondsSinceEpoch(json['completed'] as int)
        : null,
  );
}

/* ---------------------------------------------------------------------------
   Date helpers
   ------------------------------------------------------------------------- */

DateTime dateOnly(DateTime d) => DateTime(d.year, d.month, d.day);

String dateKey(DateTime d) =>
    '${d.year.toString().padLeft(4, '0')}-'
    '${d.month.toString().padLeft(2, '0')}-'
    '${d.day.toString().padLeft(2, '0')}';

DateTime? parseDateKey(String? s) {
  if (s == null) return null;
  final m = RegExp(r'^(\d{4})-(\d{2})-(\d{2})$').firstMatch(s);
  if (m == null) return null;
  return DateTime(int.parse(m[1]!), int.parse(m[2]!), int.parse(m[3]!));
}

/// Whole calendar days from [from] to [to], immune to DST shifts.
int daysBetween(DateTime from, DateTime to) => DateTime.utc(
  to.year,
  to.month,
  to.day,
).difference(DateTime.utc(from.year, from.month, from.day)).inDays;

/// The Monday after today (or the one a week out, if today is Monday).
DateTime nextMonday(DateTime now) {
  final today = dateOnly(now);
  final add = (8 - today.weekday) % 7;
  return today.add(Duration(days: add == 0 ? 7 : add));
}

const monthsShort = [
  'Jan',
  'Feb',
  'Mar',
  'Apr',
  'May',
  'Jun',
  'Jul',
  'Aug',
  'Sep',
  'Oct',
  'Nov',
  'Dec',
];
const monthsLong = [
  'January',
  'February',
  'March',
  'April',
  'May',
  'June',
  'July',
  'August',
  'September',
  'October',
  'November',
  'December',
];
const weekdaysLong = [
  'Monday',
  'Tuesday',
  'Wednesday',
  'Thursday',
  'Friday',
  'Saturday',
  'Sunday',
];
const weekdaysShort = ['Mon', 'Tue', 'Wed', 'Thu', 'Fri', 'Sat', 'Sun'];

String shortDate(DateTime d, {bool weekday = false}) =>
    '${weekday ? '${weekdaysShort[d.weekday - 1]}, ' : ''}${monthsShort[d.month - 1]} ${d.day}';

enum DueState { overdue, today, upcoming }

class DueInfo {
  const DueInfo(this.label, this.state);
  final String label;
  final DueState state;
}

/// A friendly label for a due date: Today, Tomorrow, Thursday, Nov 12…
DueInfo describeDue(DateTime due, [DateTime? now]) {
  now ??= DateTime.now();
  final diff = daysBetween(now, due);
  final String label;
  if (diff == 0) {
    label = 'Today';
  } else if (diff == 1) {
    label = 'Tomorrow';
  } else if (diff == -1) {
    label = 'Yesterday';
  } else if (diff > 1 && diff < 7) {
    label = weekdaysLong[due.weekday - 1];
  } else {
    label = due.year == now.year
        ? shortDate(due)
        : '${shortDate(due)}, ${due.year}';
  }
  final state = diff < 0
      ? DueState.overdue
      : diff == 0
      ? DueState.today
      : DueState.upcoming;
  return DueInfo(label, state);
}
