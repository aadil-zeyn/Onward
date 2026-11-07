import 'package:flutter/material.dart';

import 'models/task.dart';

const serifFamily = 'Fraunces';
const sansFamily = 'DMSans';

/// Onward's design tokens: a warm paper palette with an evergreen accent.
@immutable
class OnwardColors extends ThemeExtension<OnwardColors> {
  const OnwardColors({
    required this.bg,
    required this.surface,
    required this.surface2,
    required this.ink,
    required this.ink2,
    required this.muted,
    required this.line,
    required this.lineStrong,
    required this.accent,
    required this.accentInk,
    required this.accentSoft,
    required this.high,
    required this.medium,
    required this.low,
    required this.none,
    required this.dangerSoft,
    required this.glow1,
    required this.glow2,
    required this.shadow,
  });

  final Color bg;
  final Color surface;
  final Color surface2;
  final Color ink;
  final Color ink2;
  final Color muted;
  final Color line;
  final Color lineStrong;
  final Color accent;
  final Color accentInk;
  final Color accentSoft;
  final Color high;
  final Color medium;
  final Color low;
  final Color none;
  final Color dangerSoft;
  final Color glow1;
  final Color glow2;
  final Color shadow;

  static const light = OnwardColors(
    bg: Color(0xFFF4F0E8),
    surface: Color(0xFFFFFDF9),
    surface2: Color(0xFFF6F2EA),
    ink: Color(0xFF1D1B17),
    ink2: Color(0xFF4F4A42),
    muted: Color(0xFF8E877B),
    line: Color(0x173C2D14),
    lineStrong: Color(0x293C2D14),
    accent: Color(0xFF2F7A5F),
    accentInk: Color(0xFFFFFFFF),
    accentSoft: Color(0x1F2F7A5F),
    high: Color(0xFFD4483F),
    medium: Color(0xFFC98712),
    low: Color(0xFF3A78C2),
    none: Color(0xFFB5AD9F),
    dangerSoft: Color(0x1AC4413A),
    glow1: Color(0x212F7A5F),
    glow2: Color(0x24DE9646),
    shadow: Color(0x2E32230A),
  );

  static const dark = OnwardColors(
    bg: Color(0xFF12110F),
    surface: Color(0xFF1B1A17),
    surface2: Color(0xFF22201C),
    ink: Color(0xFFF2EEE6),
    ink2: Color(0xFFC8C1B5),
    muted: Color(0xFF8A8377),
    line: Color(0x13FFF5E1),
    lineStrong: Color(0x26FFF5E1),
    accent: Color(0xFF5FCB9C),
    accentInk: Color(0xFF0B1F16),
    accentSoft: Color(0x245FCB9C),
    high: Color(0xFFF2766D),
    medium: Color(0xFFEEB44F),
    low: Color(0xFF6EAAF0),
    none: Color(0xFF5F5A51),
    dangerSoft: Color(0x1FF2766D),
    glow1: Color(0x175FCB9C),
    glow2: Color(0x0FEEB44F),
    shadow: Color(0x99000000),
  );

  Color priority(Priority p) => switch (p) {
    Priority.none => none,
    Priority.low => low,
    Priority.medium => medium,
    Priority.high => high,
  };

  List<BoxShadow> get cardShadow => [
    BoxShadow(
      color: shadow.withValues(alpha: shadow.a * 0.25),
      blurRadius: 2,
      offset: const Offset(0, 1),
    ),
    BoxShadow(
      color: shadow.withValues(alpha: shadow.a * 0.55),
      blurRadius: 30,
      spreadRadius: -12,
      offset: const Offset(0, 10),
    ),
  ];

  List<BoxShadow> get liftShadow => [
    BoxShadow(
      color: shadow.withValues(alpha: shadow.a * 0.3),
      blurRadius: 6,
      offset: const Offset(0, 2),
    ),
    BoxShadow(
      color: shadow,
      blurRadius: 48,
      spreadRadius: -14,
      offset: const Offset(0, 24),
    ),
  ];

  @override
  OnwardColors copyWith() => this;

  @override
  OnwardColors lerp(OnwardColors? other, double t) {
    if (other == null) return this;
    Color l(Color a, Color b) => Color.lerp(a, b, t)!;
    return OnwardColors(
      bg: l(bg, other.bg),
      surface: l(surface, other.surface),
      surface2: l(surface2, other.surface2),
      ink: l(ink, other.ink),
      ink2: l(ink2, other.ink2),
      muted: l(muted, other.muted),
      line: l(line, other.line),
      lineStrong: l(lineStrong, other.lineStrong),
      accent: l(accent, other.accent),
      accentInk: l(accentInk, other.accentInk),
      accentSoft: l(accentSoft, other.accentSoft),
      high: l(high, other.high),
      medium: l(medium, other.medium),
      low: l(low, other.low),
      none: l(none, other.none),
      dangerSoft: l(dangerSoft, other.dangerSoft),
      glow1: l(glow1, other.glow1),
      glow2: l(glow2, other.glow2),
      shadow: l(shadow, other.shadow),
    );
  }
}

extension OnwardThemeContext on BuildContext {
  OnwardColors get colors => Theme.of(this).extension<OnwardColors>()!;
}

ThemeData buildTheme(Brightness brightness) {
  final c = brightness == Brightness.dark
      ? OnwardColors.dark
      : OnwardColors.light;

  final scheme =
      ColorScheme.fromSeed(
        seedColor: c.accent,
        brightness: brightness,
      ).copyWith(
        primary: c.accent,
        onPrimary: c.accentInk,
        primaryContainer: c.accentSoft,
        onPrimaryContainer: c.ink,
        secondaryContainer: c.accentSoft,
        onSecondaryContainer: c.ink,
        surface: c.surface,
        onSurface: c.ink,
        onSurfaceVariant: c.muted,
        surfaceContainerLowest: c.surface,
        surfaceContainerLow: c.surface,
        surfaceContainer: c.surface,
        surfaceContainerHigh: c.surface2,
        surfaceContainerHighest: c.surface2,
        outline: c.lineStrong,
        outlineVariant: c.line,
        error: c.high,
        inverseSurface: c.ink,
        onInverseSurface: c.bg,
        inversePrimary: brightness == Brightness.dark
            ? OnwardColors.light.accent
            : OnwardColors.dark.accent,
      );

  final base = ThemeData(
    useMaterial3: true,
    brightness: brightness,
    colorScheme: scheme,
    fontFamily: sansFamily,
    scaffoldBackgroundColor: c.bg,
    splashFactory: InkSparkle.splashFactory,
  );

  final t = base.textTheme.apply(bodyColor: c.ink, displayColor: c.ink);
  TextStyle? serif(TextStyle? s, double spacing) => s?.copyWith(
    fontFamily: serifFamily,
    fontWeight: FontWeight.w500,
    letterSpacing: spacing,
  );

  final buttonShape = RoundedRectangleBorder(
    borderRadius: BorderRadius.circular(12),
  );
  const buttonText = TextStyle(
    fontFamily: sansFamily,
    fontWeight: FontWeight.w600,
    fontSize: 14.5,
  );

  return base.copyWith(
    extensions: [c],
    textTheme: t.copyWith(
      displaySmall: serif(t.displaySmall, -1.2),
      headlineLarge: serif(t.headlineLarge, -1),
      headlineMedium: serif(t.headlineMedium, -0.8),
      headlineSmall: serif(t.headlineSmall, -0.5),
      titleLarge: serif(t.titleLarge, -0.3),
    ),
    appBarTheme: AppBarTheme(
      backgroundColor: c.bg,
      surfaceTintColor: Colors.transparent,
      scrolledUnderElevation: 0,
      centerTitle: false,
      titleTextStyle: TextStyle(
        fontFamily: serifFamily,
        fontWeight: FontWeight.w600,
        fontSize: 24,
        letterSpacing: -0.4,
        color: c.ink,
      ),
    ),
    cardTheme: CardThemeData(
      color: c.surface,
      surfaceTintColor: Colors.transparent,
      elevation: 0,
      shape: RoundedRectangleBorder(
        borderRadius: BorderRadius.circular(20),
        side: BorderSide(color: c.line),
      ),
    ),
    filledButtonTheme: FilledButtonThemeData(
      style: FilledButton.styleFrom(
        backgroundColor: c.ink,
        foregroundColor: c.bg,
        minimumSize: const Size(64, 44),
        padding: const EdgeInsets.symmetric(horizontal: 20),
        shape: buttonShape,
        textStyle: buttonText,
      ),
    ),
    textButtonTheme: TextButtonThemeData(
      style: TextButton.styleFrom(
        foregroundColor: c.ink2,
        minimumSize: const Size(48, 44),
        shape: buttonShape,
        textStyle: buttonText,
      ),
    ),
    floatingActionButtonTheme: FloatingActionButtonThemeData(
      backgroundColor: c.ink,
      foregroundColor: c.bg,
      elevation: 2,
      highlightElevation: 4,
      shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(18)),
      extendedTextStyle: buttonText,
    ),
    navigationBarTheme: NavigationBarThemeData(
      backgroundColor: c.surface,
      surfaceTintColor: Colors.transparent,
      indicatorColor: c.accentSoft,
      elevation: 0,
      height: 68,
      labelTextStyle: WidgetStateProperty.resolveWith(
        (s) => TextStyle(
          fontFamily: sansFamily,
          fontSize: 12.5,
          fontWeight: s.contains(WidgetState.selected)
              ? FontWeight.w600
              : FontWeight.w500,
          color: s.contains(WidgetState.selected) ? c.ink : c.muted,
        ),
      ),
      iconTheme: WidgetStateProperty.resolveWith(
        (s) => IconThemeData(
          color: s.contains(WidgetState.selected) ? c.accent : c.muted,
        ),
      ),
    ),
    bottomSheetTheme: BottomSheetThemeData(
      backgroundColor: c.surface,
      surfaceTintColor: Colors.transparent,
      dragHandleColor: c.lineStrong,
      shape: const RoundedRectangleBorder(
        borderRadius: BorderRadius.vertical(top: Radius.circular(28)),
      ),
    ),
    snackBarTheme: SnackBarThemeData(
      behavior: SnackBarBehavior.floating,
      backgroundColor: c.ink,
      contentTextStyle: TextStyle(
        fontFamily: sansFamily,
        fontSize: 14.5,
        color: c.bg,
      ),
      actionTextColor: scheme.inversePrimary,
      shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(14)),
      elevation: 6,
    ),
    inputDecorationTheme: InputDecorationTheme(
      filled: true,
      fillColor: c.surface2,
      hintStyle: TextStyle(color: c.muted),
      border: OutlineInputBorder(
        borderRadius: BorderRadius.circular(14),
        borderSide: BorderSide(color: c.line),
      ),
      enabledBorder: OutlineInputBorder(
        borderRadius: BorderRadius.circular(14),
        borderSide: BorderSide(color: c.line),
      ),
      focusedBorder: OutlineInputBorder(
        borderRadius: BorderRadius.circular(14),
        borderSide: BorderSide(color: c.accent, width: 1.5),
      ),
    ),
    menuTheme: MenuThemeData(
      style: MenuStyle(
        backgroundColor: WidgetStatePropertyAll(c.surface),
        surfaceTintColor: const WidgetStatePropertyAll(Colors.transparent),
        elevation: const WidgetStatePropertyAll(8),
        shadowColor: WidgetStatePropertyAll(c.shadow),
        padding: const WidgetStatePropertyAll(EdgeInsets.all(6)),
        shape: WidgetStatePropertyAll(
          RoundedRectangleBorder(
            borderRadius: BorderRadius.circular(14),
            side: BorderSide(color: c.line),
          ),
        ),
      ),
    ),
    menuButtonTheme: MenuButtonThemeData(
      style: MenuItemButton.styleFrom(
        minimumSize: const Size(220, 44),
        shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(9)),
        textStyle: const TextStyle(fontFamily: sansFamily, fontSize: 14.5),
        iconColor: c.muted,
        foregroundColor: c.ink,
      ),
    ),
    dividerTheme: DividerThemeData(color: c.line, thickness: 1, space: 1),
    datePickerTheme: DatePickerThemeData(
      backgroundColor: c.surface,
      surfaceTintColor: Colors.transparent,
      headerHeadlineStyle: TextStyle(
        fontFamily: serifFamily,
        fontWeight: FontWeight.w500,
        fontSize: 30,
        color: c.ink,
      ),
    ),
  );
}
