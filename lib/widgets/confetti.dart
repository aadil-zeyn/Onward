import 'dart:math' as math;

import 'package:flutter/material.dart';

/// Fires confetti bursts on a [ConfettiLayer].
class ConfettiController extends ChangeNotifier {
  Offset? _origin;
  List<Color> _colors = const [];

  /// Bursts from [globalOrigin] (in global coordinates).
  void burst(Offset globalOrigin, List<Color> colors) {
    _origin = globalOrigin;
    _colors = colors;
    notifyListeners();
  }
}

/// A full-size, non-interactive layer that paints confetti bursts.
class ConfettiLayer extends StatefulWidget {
  const ConfettiLayer({super.key, required this.controller});
  final ConfettiController controller;

  @override
  State<ConfettiLayer> createState() => _ConfettiLayerState();
}

class _Particle {
  _Particle(math.Random r, this.color)
    : angle = r.nextDouble() * math.pi * 2,
      distance = 60 + r.nextDouble() * 120,
      spin = (r.nextDouble() - 0.5) * 14,
      width = 6 + r.nextDouble() * 3,
      height = 9 + r.nextDouble() * 5,
      delay = r.nextDouble() * 0.08;

  final double angle;
  final double distance;
  final double spin;
  final double width;
  final double height;
  final double delay;
  final Color color;
}

class _ConfettiLayerState extends State<ConfettiLayer>
    with SingleTickerProviderStateMixin {
  late final _c = AnimationController(
    vsync: this,
    duration: const Duration(milliseconds: 1600),
  );
  final _random = math.Random();
  List<_Particle> _particles = const [];
  Offset _origin = Offset.zero;

  @override
  void initState() {
    super.initState();
    widget.controller.addListener(_onBurst);
  }

  @override
  void didUpdateWidget(ConfettiLayer old) {
    super.didUpdateWidget(old);
    if (old.controller != widget.controller) {
      old.controller.removeListener(_onBurst);
      widget.controller.addListener(_onBurst);
    }
  }

  void _onBurst() {
    final box = context.findRenderObject() as RenderBox?;
    final origin = widget.controller._origin;
    if (box == null ||
        origin == null ||
        MediaQuery.disableAnimationsOf(context)) {
      return;
    }
    final colors = widget.controller._colors;
    setState(() {
      _origin = box.globalToLocal(origin);
      _particles = [
        for (var i = 0; i < 40; i++)
          _Particle(_random, colors[i % colors.length]),
      ];
    });
    _c.forward(from: 0).whenCompleteOrCancel(() {
      if (mounted) setState(() => _particles = const []);
    });
  }

  @override
  void dispose() {
    widget.controller.removeListener(_onBurst);
    _c.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    if (_particles.isEmpty) return const SizedBox.expand();
    return IgnorePointer(
      child: AnimatedBuilder(
        animation: _c,
        builder: (context, _) => CustomPaint(
          size: Size.infinite,
          painter: _ConfettiPainter(_particles, _origin, _c.value),
        ),
      ),
    );
  }
}

class _ConfettiPainter extends CustomPainter {
  _ConfettiPainter(this.particles, this.origin, this.t);
  final List<_Particle> particles;
  final Offset origin;
  final double t;

  @override
  void paint(Canvas canvas, Size size) {
    for (final p in particles) {
      final local = ((t - p.delay) / (1 - p.delay)).clamp(0.0, 1.0);
      if (local <= 0) continue;
      final burst = Curves.easeOutCubic.transform(
        (local / 0.5).clamp(0.0, 1.0),
      );
      final fall = local * local * 140;
      final pos =
          origin +
          Offset(math.cos(p.angle), math.sin(p.angle)) * p.distance * burst +
          Offset(0, fall);
      final opacity = local < 0.6 ? 1.0 : 1 - (local - 0.6) / 0.4;
      canvas.save();
      canvas.translate(pos.dx, pos.dy);
      canvas.rotate(p.spin * local);
      canvas.drawRRect(
        RRect.fromRectAndRadius(
          Rect.fromCenter(
            center: Offset.zero,
            width: p.width,
            height: p.height * (0.6 + 0.4 * math.cos(local * 12).abs()),
          ),
          const Radius.circular(2),
        ),
        Paint()..color = p.color.withValues(alpha: opacity),
      );
      canvas.restore();
    }
  }

  @override
  bool shouldRepaint(_ConfettiPainter old) => true;
}
