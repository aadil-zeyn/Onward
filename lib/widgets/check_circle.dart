import 'dart:math' as math;

import 'package:flutter/material.dart';

import '../theme.dart';
import 'decorations.dart';

/// A round checkbox that fills with the accent colour and draws its tick.
/// The empty ring takes the task's priority colour.
class CheckCircle extends StatefulWidget {
  const CheckCircle({
    super.key,
    required this.checked,
    required this.ringColor,
    required this.onTap,
    required this.semanticLabel,
    this.emphasis = false,
  });

  final bool checked;
  final Color ringColor;
  final VoidCallback onTap;
  final String semanticLabel;

  /// Thicker ring for tasks that have a priority.
  final bool emphasis;

  @override
  State<CheckCircle> createState() => _CheckCircleState();
}

class _CheckCircleState extends State<CheckCircle>
    with SingleTickerProviderStateMixin {
  late final _c = AnimationController(
    vsync: this,
    duration: const Duration(milliseconds: 420),
    value: widget.checked ? 1 : 0,
  );

  @override
  void didUpdateWidget(CheckCircle old) {
    super.didUpdateWidget(old);
    if (old.checked != widget.checked) {
      widget.checked ? _c.forward() : _c.reverse();
    }
  }

  @override
  void dispose() {
    _c.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    final colors = context.colors;
    return Semantics(
      checked: widget.checked,
      button: true,
      label: widget.semanticLabel,
      excludeSemantics: true,
      child: GestureDetector(
        behavior: HitTestBehavior.opaque,
        onTap: widget.onTap,
        child: SizedBox.square(
          dimension: 44,
          child: Center(
            child: AnimatedBuilder(
              animation: _c,
              builder: (context, _) => CustomPaint(
                size: const Size.square(24),
                painter: _CheckPainter(
                  t: _c.value,
                  forward: _c.status == AnimationStatus.forward,
                  ring: widget.ringColor,
                  fill: colors.accent,
                  tick: colors.accentInk,
                  ringWidth: widget.emphasis ? 2 : 1.75,
                ),
              ),
            ),
          ),
        ),
      ),
    );
  }
}

class _CheckPainter extends CustomPainter {
  _CheckPainter({
    required this.t,
    required this.forward,
    required this.ring,
    required this.fill,
    required this.tick,
    required this.ringWidth,
  });

  final double t;
  final bool forward;
  final Color ring;
  final Color fill;
  final Color tick;
  final double ringWidth;

  @override
  void paint(Canvas canvas, Size size) {
    final center = size.center(Offset.zero);
    final fillT = Curves.easeOut.transform((t / 0.55).clamp(0.0, 1.0));
    final tickT = Curves.easeInOutCubic.transform(
      ((t - 0.35) / 0.65).clamp(0.0, 1.0),
    );
    // A little squash-and-pop while checking.
    final pop = forward
        ? 1 +
              0.14 * math.sin(math.pi * t) -
              0.1 * math.sin(math.pi * (t * 2).clamp(0, 1))
        : 1.0;

    canvas.save();
    canvas.translate(center.dx, center.dy);
    canvas.scale(pop);
    canvas.translate(-center.dx, -center.dy);

    const r = 10.0;
    if (fillT > 0) {
      canvas.drawCircle(
        center,
        r + ringWidth / 2,
        Paint()..color = fill.withValues(alpha: fill.a * fillT),
      );
    }
    canvas.drawCircle(
      center,
      r,
      Paint()
        ..color = Color.lerp(ring, fill, fillT)!
        ..style = PaintingStyle.stroke
        ..strokeWidth = ringWidth,
    );

    if (tickT > 0) {
      final s = size.width / 24;
      final path = Path()
        ..moveTo(7.5 * s, 12.5 * s)
        ..lineTo(10.5 * s, 15.5 * s)
        ..lineTo(16.5 * s, 9 * s);
      canvas.drawPath(
        partialPath(path, tickT),
        Paint()
          ..color = tick
          ..style = PaintingStyle.stroke
          ..strokeWidth = 2.25
          ..strokeCap = StrokeCap.round
          ..strokeJoin = StrokeJoin.round,
      );
    }
    canvas.restore();
  }

  @override
  bool shouldRepaint(_CheckPainter old) =>
      old.t != t ||
      old.ring != ring ||
      old.fill != fill ||
      old.tick != tick ||
      old.ringWidth != ringWidth;
}
