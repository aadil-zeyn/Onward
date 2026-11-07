import 'dart:math' as math;
import 'dart:ui' show PathMetric;

import 'package:flutter/material.dart';

import '../theme.dart';

/// The app mark: an evergreen tile with a check.
class BrandMark extends StatelessWidget {
  const BrandMark({super.key, this.size = 28});
  final double size;

  @override
  Widget build(BuildContext context) {
    final c = context.colors;
    return SizedBox.square(
      dimension: size,
      child: CustomPaint(painter: _BrandPainter(c.accent, c.accentInk)),
    );
  }
}

class _BrandPainter extends CustomPainter {
  _BrandPainter(this.bg, this.fg);
  final Color bg;
  final Color fg;

  @override
  void paint(Canvas canvas, Size size) {
    final s = size.width / 32;
    canvas.drawRRect(
      RRect.fromRectAndRadius(Offset.zero & size, Radius.circular(9 * s)),
      Paint()..color = bg,
    );
    final tick = Path()
      ..moveTo(9.5 * s, 16.5 * s)
      ..lineTo(14 * s, 21 * s)
      ..lineTo(22.5 * s, 11.5 * s);
    canvas.drawPath(
      tick,
      Paint()
        ..color = fg
        ..style = PaintingStyle.stroke
        ..strokeWidth = 2.6 * s
        ..strokeCap = StrokeCap.round
        ..strokeJoin = StrokeJoin.round,
    );
  }

  @override
  bool shouldRepaint(_BrandPainter old) => old.bg != bg || old.fg != fg;
}

/// Soft coloured glows behind the content.
class Backdrop extends StatelessWidget {
  const Backdrop({super.key});

  @override
  Widget build(BuildContext context) {
    final c = context.colors;
    return IgnorePointer(
      child: Stack(
        children: [
          Positioned(
            left: -220,
            top: -260,
            width: 640,
            height: 520,
            child: DecoratedBox(
              decoration: BoxDecoration(
                gradient: RadialGradient(
                  colors: [c.glow1, c.glow1.withValues(alpha: 0)],
                ),
              ),
            ),
          ),
          Positioned(
            right: -260,
            top: -220,
            width: 560,
            height: 460,
            child: DecoratedBox(
              decoration: BoxDecoration(
                gradient: RadialGradient(
                  colors: [c.glow2, c.glow2.withValues(alpha: 0)],
                ),
              ),
            ),
          ),
        ],
      ),
    );
  }
}

/// A sun rising over the horizon, drawn once with a gentle entrance.
class SunriseArt extends StatelessWidget {
  const SunriseArt({super.key});

  @override
  Widget build(BuildContext context) {
    final c = context.colors;
    return TweenAnimationBuilder<double>(
      tween: Tween(begin: 0, end: 1),
      duration: const Duration(milliseconds: 900),
      curve: Curves.easeOutCubic,
      builder: (context, t, _) => SizedBox(
        width: 150,
        height: 100,
        child: CustomPaint(painter: _SunrisePainter(c, t)),
      ),
    );
  }
}

class _SunrisePainter extends CustomPainter {
  _SunrisePainter(this.c, this.t);
  final OnwardColors c;
  final double t;

  @override
  void paint(Canvas canvas, Size size) {
    final s = size.width / 160;
    canvas.scale(s);
    Paint stroke(Color color) => Paint()
      ..color = color
      ..style = PaintingStyle.stroke
      ..strokeWidth = 3
      ..strokeCap = StrokeCap.round;

    // Horizon lines.
    canvas.drawLine(
      const Offset(16, 79),
      const Offset(144, 79),
      stroke(c.lineStrong),
    );
    final soft = stroke(c.line);
    canvas.drawLine(const Offset(36, 92), const Offset(80, 92), soft);
    canvas.drawLine(const Offset(92, 92), const Offset(114, 92), soft);
    canvas.drawLine(const Offset(56, 103), const Offset(104, 103), soft);

    // The sun rises from below the horizon.
    canvas.save();
    canvas.clipRect(const Rect.fromLTRB(0, 0, 160, 77.5));
    canvas.translate(0, (1 - t) * 34);
    final disc = Rect.fromCircle(center: const Offset(80, 78), radius: 36);
    canvas.drawArc(disc, math.pi, math.pi, true, Paint()..color = c.accentSoft);
    canvas.drawArc(disc, math.pi, math.pi, false, stroke(c.accent));
    canvas.restore();

    // Rays fade in last.
    final rays = stroke(
      c.accent.withValues(alpha: 0.55 * Curves.easeIn.transform(t)),
    );
    for (final (a, b) in const [
      (Offset(80, 26), Offset(80, 14)),
      (Offset(52, 36), Offset(44, 28)),
      (Offset(108, 36), Offset(116, 28)),
      (Offset(36, 60), Offset(25, 56)),
      (Offset(124, 60), Offset(135, 56)),
    ]) {
      canvas.drawLine(a, b, rays);
    }
  }

  @override
  bool shouldRepaint(_SunrisePainter old) => old.t != t || old.c != c;
}

/// Draws the first [fraction] of [path].
Path partialPath(Path path, double fraction) {
  final out = Path();
  if (fraction <= 0) return out;
  for (final PathMetric m in path.computeMetrics()) {
    out.addPath(
      m.extractPath(0, m.length * fraction.clamp(0.0, 1.0)),
      Offset.zero,
    );
  }
  return out;
}
