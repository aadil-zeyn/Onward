import 'dart:math' as math;

import 'package:flutter/material.dart';

import '../theme.dart';

/// Today's progress as an animated ring with a percentage in the middle.
/// Gives a little pop when it reaches 100%.
class ProgressRing extends StatefulWidget {
  const ProgressRing({
    super.key,
    required this.done,
    required this.total,
    this.size = 66,
  });

  final int done;
  final int total;
  final double size;

  double get fraction => total == 0 ? 0 : done / total;

  @override
  State<ProgressRing> createState() => _ProgressRingState();
}

class _ProgressRingState extends State<ProgressRing>
    with SingleTickerProviderStateMixin {
  late final _pop = AnimationController(
    vsync: this,
    duration: const Duration(milliseconds: 600),
  );
  late final _scale = TweenSequence<double>([
    TweenSequenceItem(
      tween: Tween<double>(
        begin: 1,
        end: 1.14,
      ).chain(CurveTween(curve: Curves.easeOut)),
      weight: 35,
    ),
    TweenSequenceItem(
      tween: Tween<double>(
        begin: 1.14,
        end: 1,
      ).chain(CurveTween(curve: Curves.elasticOut)),
      weight: 65,
    ),
  ]).animate(_pop);

  @override
  void didUpdateWidget(ProgressRing old) {
    super.didUpdateWidget(old);
    if (widget.total > 0 && widget.fraction == 1 && old.fraction < 1) {
      _pop.forward(from: 0);
    }
  }

  @override
  void dispose() {
    _pop.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    final c = context.colors;
    final pct = (widget.fraction * 100).round();
    return Semantics(
      label: widget.total == 0
          ? 'No tasks yet'
          : '${widget.done} of ${widget.total} tasks done',
      excludeSemantics: true,
      child: ScaleTransition(
        scale: _scale,
        child: SizedBox.square(
          dimension: widget.size,
          child: TweenAnimationBuilder<double>(
            tween: Tween(end: widget.fraction),
            duration: const Duration(milliseconds: 800),
            curve: Curves.easeInOutCubic,
            builder: (context, value, _) => CustomPaint(
              painter: _RingPainter(value, c.lineStrong, c.accent),
              child: Center(
                child: Text(
                  '$pct%',
                  style: TextStyle(
                    fontSize: widget.size < 62 ? 12 : 13,
                    fontWeight: FontWeight.w600,
                    color: c.ink,
                    fontFeatures: const [FontFeature.tabularFigures()],
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

class _RingPainter extends CustomPainter {
  _RingPainter(this.value, this.track, this.bar);
  final double value;
  final Color track;
  final Color bar;

  @override
  void paint(Canvas canvas, Size size) {
    const stroke = 5.0;
    final rect = (Offset.zero & size).deflate(stroke / 2 + 0.5);
    canvas.drawArc(
      rect,
      0,
      math.pi * 2,
      false,
      Paint()
        ..color = track
        ..style = PaintingStyle.stroke
        ..strokeWidth = stroke,
    );
    if (value <= 0) return;
    canvas.drawArc(
      rect,
      -math.pi / 2,
      math.pi * 2 * value.clamp(0.0, 1.0),
      false,
      Paint()
        ..color = bar
        ..style = PaintingStyle.stroke
        ..strokeWidth = stroke
        ..strokeCap = StrokeCap.round,
    );
  }

  @override
  bool shouldRepaint(_RingPainter old) =>
      old.value != value || old.track != track || old.bar != bar;
}
