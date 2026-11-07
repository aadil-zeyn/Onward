import 'package:flutter/material.dart';

import '../models/task.dart';
import '../theme.dart';

/// A segmented None / Low / Medium / High picker with coloured dots.
/// In [compact] mode only the selected option shows its label.
class PriorityPicker extends StatelessWidget {
  const PriorityPicker({
    super.key,
    required this.value,
    required this.onChanged,
    this.compact = false,
  });

  final Priority value;
  final ValueChanged<Priority> onChanged;
  final bool compact;

  @override
  Widget build(BuildContext context) {
    final c = context.colors;
    return Container(
      padding: const EdgeInsets.all(3),
      decoration: BoxDecoration(
        color: c.surface2,
        borderRadius: BorderRadius.circular(12),
        border: Border.all(color: c.line),
      ),
      child: Row(
        mainAxisSize: MainAxisSize.min,
        children: [
          for (final p in Priority.values)
            _PriorityOption(
              priority: p,
              selected: p == value,
              showLabel: !compact || p == value,
              onTap: () => onChanged(p),
            ),
        ],
      ),
    );
  }
}

class _PriorityOption extends StatelessWidget {
  const _PriorityOption({
    required this.priority,
    required this.selected,
    required this.showLabel,
    required this.onTap,
  });

  final Priority priority;
  final bool selected;
  final bool showLabel;
  final VoidCallback onTap;

  @override
  Widget build(BuildContext context) {
    final c = context.colors;
    final color = c.priority(priority);
    return Semantics(
      inMutuallyExclusiveGroup: true,
      selected: selected,
      button: true,
      label: '${priority.label} priority',
      excludeSemantics: true,
      child: GestureDetector(
        behavior: HitTestBehavior.opaque,
        onTap: onTap,
        child: AnimatedContainer(
          duration: const Duration(milliseconds: 220),
          curve: Curves.easeOutCubic,
          height: 32,
          padding: const EdgeInsets.symmetric(horizontal: 11),
          decoration: BoxDecoration(
            color: selected ? c.surface : c.surface.withValues(alpha: 0),
            borderRadius: BorderRadius.circular(9),
            border: Border.all(
              color: selected ? c.line : c.line.withValues(alpha: 0),
            ),
            boxShadow: selected
                ? [
                    BoxShadow(
                      color: c.shadow.withValues(alpha: c.shadow.a * 0.3),
                      blurRadius: 3,
                      offset: const Offset(0, 1),
                    ),
                  ]
                : null,
          ),
          child: AnimatedSize(
            duration: const Duration(milliseconds: 220),
            curve: Curves.easeOutCubic,
            child: Row(
              mainAxisSize: MainAxisSize.min,
              children: [
                AnimatedScale(
                  scale: selected ? 1.2 : 1,
                  duration: const Duration(milliseconds: 220),
                  child: Container(
                    width: 8,
                    height: 8,
                    decoration: BoxDecoration(
                      shape: BoxShape.circle,
                      color: priority == Priority.none ? null : color,
                      border: priority == Priority.none
                          ? Border.all(color: color, width: 1.5)
                          : null,
                    ),
                  ),
                ),
                if (showLabel) ...[
                  const SizedBox(width: 7),
                  Text(
                    priority.label,
                    style: TextStyle(
                      fontSize: 13,
                      fontWeight: FontWeight.w500,
                      color: selected ? c.ink : c.muted,
                    ),
                  ),
                ],
              ],
            ),
          ),
        ),
      ),
    );
  }
}

/// A pill that shows the due date and opens a quick-pick menu.
class DueChip extends StatelessWidget {
  const DueChip({super.key, required this.value, required this.onChanged});

  final DateTime? value;
  final ValueChanged<DateTime?> onChanged;

  @override
  Widget build(BuildContext context) {
    final c = context.colors;
    final now = DateTime.now();
    final today = dateOnly(now);
    final tomorrow = today.add(const Duration(days: 1));
    final monday = nextMonday(now);
    final info = value == null ? null : describeDue(value!, now);
    final overdue = info?.state == DueState.overdue;
    final fg = info == null ? c.muted : (overdue ? c.high : c.accent);
    final bg = info == null
        ? Colors.transparent
        : (overdue ? c.dangerSoft : c.accentSoft);

    Widget hint(String s) =>
        Text(s, style: TextStyle(fontSize: 12.5, color: c.muted));

    return MenuAnchor(
      alignmentOffset: const Offset(0, 6),
      menuChildren: [
        MenuItemButton(
          leadingIcon: const Icon(Icons.wb_sunny_outlined, size: 19),
          trailingIcon: hint(weekdaysShort[today.weekday - 1]),
          onPressed: () => onChanged(today),
          child: const Text('Today'),
        ),
        MenuItemButton(
          leadingIcon: const Icon(Icons.wb_twilight_outlined, size: 19),
          trailingIcon: hint(weekdaysShort[tomorrow.weekday - 1]),
          onPressed: () => onChanged(tomorrow),
          child: const Text('Tomorrow'),
        ),
        MenuItemButton(
          leadingIcon: const Icon(Icons.arrow_forward_rounded, size: 19),
          trailingIcon: hint(shortDate(monday, weekday: true)),
          onPressed: () => onChanged(monday),
          child: const Text('Next week'),
        ),
        MenuItemButton(
          leadingIcon: const Icon(Icons.calendar_month_outlined, size: 19),
          onPressed: () async {
            final picked = await showDatePicker(
              context: context,
              initialDate: value ?? today,
              firstDate: DateTime(today.year - 5),
              lastDate: DateTime(today.year + 10),
            );
            if (picked != null) onChanged(picked);
          },
          child: const Text('Pick a date…'),
        ),
        if (value != null) ...[
          const Divider(height: 9, indent: 6, endIndent: 6),
          MenuItemButton(
            leadingIcon: Icon(Icons.close_rounded, size: 19, color: c.high),
            style: MenuItemButton.styleFrom(foregroundColor: c.high),
            onPressed: () => onChanged(null),
            child: const Text('No date'),
          ),
        ],
      ],
      builder: (context, menu, _) => Semantics(
        button: true,
        label: info == null
            ? 'Set a due date'
            : 'Due ${info.label}. Change due date',
        excludeSemantics: true,
        child: Material(
          color: bg,
          borderRadius: BorderRadius.circular(11),
          child: InkWell(
            borderRadius: BorderRadius.circular(11),
            onTap: () => menu.isOpen ? menu.close() : menu.open(),
            child: AnimatedSize(
              duration: const Duration(milliseconds: 200),
              curve: Curves.easeOutCubic,
              child: Padding(
                padding: EdgeInsets.fromLTRB(12, 0, info == null ? 12 : 6, 0),
                child: SizedBox(
                  height: 36,
                  child: Row(
                    mainAxisSize: MainAxisSize.min,
                    children: [
                      Icon(Icons.event_outlined, size: 17, color: fg),
                      const SizedBox(width: 7),
                      Text(
                        info?.label ?? 'Due date',
                        style: TextStyle(
                          fontSize: 13,
                          fontWeight: FontWeight.w500,
                          color: fg,
                        ),
                      ),
                      if (info != null)
                        GestureDetector(
                          behavior: HitTestBehavior.opaque,
                          onTap: () => onChanged(null),
                          child: Padding(
                            padding: const EdgeInsets.fromLTRB(6, 8, 2, 8),
                            child: Icon(
                              Icons.close_rounded,
                              size: 15,
                              color: fg,
                            ),
                          ),
                        ),
                    ],
                  ),
                ),
              ),
            ),
          ),
        ),
      ),
    );
  }
}
