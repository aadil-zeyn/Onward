package com.aadil.onward.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aadil.onward.R
import com.aadil.onward.model.Priority

val Serif = FontFamily(
    Font(R.font.fraunces_medium, FontWeight.Medium),
    Font(R.font.fraunces_semibold, FontWeight.SemiBold),
)

val Sans = FontFamily(
    Font(R.font.dmsans_regular, FontWeight.Normal),
    Font(R.font.dmsans_medium, FontWeight.Medium),
    Font(R.font.dmsans_semibold, FontWeight.SemiBold),
)

/** Onward's design tokens: a warm paper palette with an evergreen accent. */
@Immutable
data class OnwardColors(
    val isDark: Boolean,
    val bg: Color,
    val surface: Color,
    val surface2: Color,
    val ink: Color,
    val ink2: Color,
    val muted: Color,
    val line: Color,
    val lineStrong: Color,
    val accent: Color,
    val accentInk: Color,
    val accentSoft: Color,
    val high: Color,
    val medium: Color,
    val low: Color,
    val none: Color,
    val dangerSoft: Color,
    val glow1: Color,
    val glow2: Color,
    val shadow: Color,
) {
    fun priority(p: Priority): Color = when (p) {
        Priority.NONE -> none
        Priority.LOW -> low
        Priority.MEDIUM -> medium
        Priority.HIGH -> high
    }

    /** A list or habit colour, lifted a little so it glows on dark paper. */
    fun tint(argb: Long): Color {
        val c = Color(argb)
        return if (isDark) lerp(c, Color.White, 0.28f) else c
    }

    /** A soft wash of a list colour, for icon tiles and chips. */
    fun wash(argb: Long): Color = tint(argb).copy(alpha = if (isDark) 0.18f else 0.13f)

    companion object {
        val Light = OnwardColors(
            isDark = false,
            bg = Color(0xFFF4F0E8),
            surface = Color(0xFFFFFDF9),
            surface2 = Color(0xFFF6F2EA),
            ink = Color(0xFF1D1B17),
            ink2 = Color(0xFF4F4A42),
            muted = Color(0xFF8E877B),
            line = Color(0x173C2D14),
            lineStrong = Color(0x293C2D14),
            accent = Color(0xFF2F7A5F),
            accentInk = Color(0xFFFFFFFF),
            accentSoft = Color(0x1F2F7A5F),
            high = Color(0xFFD4483F),
            medium = Color(0xFFC98712),
            low = Color(0xFF3A78C2),
            none = Color(0xFFB5AD9F),
            dangerSoft = Color(0x1AC4413A),
            glow1 = Color(0x212F7A5F),
            glow2 = Color(0x24DE9646),
            shadow = Color(0x2E32230A),
        )

        val Dark = OnwardColors(
            isDark = true,
            bg = Color(0xFF12110F),
            surface = Color(0xFF1B1A17),
            surface2 = Color(0xFF22201C),
            ink = Color(0xFFF2EEE6),
            ink2 = Color(0xFFC8C1B5),
            muted = Color(0xFF8A8377),
            line = Color(0x13FFF5E1),
            lineStrong = Color(0x26FFF5E1),
            accent = Color(0xFF5FCB9C),
            accentInk = Color(0xFF0B1F16),
            accentSoft = Color(0x245FCB9C),
            high = Color(0xFFF2766D),
            medium = Color(0xFFEEB44F),
            low = Color(0xFF6EAAF0),
            none = Color(0xFF5F5A51),
            dangerSoft = Color(0x1FF2766D),
            glow1 = Color(0x175FCB9C),
            glow2 = Color(0x0FEEB44F),
            shadow = Color(0x99000000),
        )
    }
}

val LocalOnwardColors = staticCompositionLocalOf { OnwardColors.Light }

object Onward {
    val colors: OnwardColors
        @Composable get() = LocalOnwardColors.current
}

private fun typography(c: OnwardColors): Typography {
    val base = Typography()
    fun TextStyle.sans() = copy(fontFamily = Sans, color = c.ink)
    fun serif(size: Int, spacing: Double, weight: FontWeight = FontWeight.Medium, line: Int = (size * 1.15).toInt()) =
        TextStyle(fontFamily = Serif, fontWeight = weight, fontSize = size.sp, lineHeight = line.sp,
            letterSpacing = spacing.sp, color = c.ink)
    return Typography(
        displayLarge = serif(52, -1.6),
        displayMedium = serif(44, -1.4),
        displaySmall = serif(38, -1.2),
        headlineLarge = serif(32, -1.0),
        headlineMedium = serif(28, -0.8),
        headlineSmall = serif(24, -0.5),
        titleLarge = serif(21, -0.3, FontWeight.SemiBold),
        titleMedium = base.titleMedium.sans().copy(fontWeight = FontWeight.SemiBold, fontSize = 16.5.sp),
        titleSmall = base.titleSmall.sans().copy(fontWeight = FontWeight.SemiBold),
        bodyLarge = base.bodyLarge.sans().copy(fontSize = 16.5.sp, lineHeight = 23.sp, letterSpacing = 0.sp),
        bodyMedium = base.bodyMedium.sans().copy(fontSize = 14.5.sp, letterSpacing = 0.sp, color = c.ink2),
        bodySmall = base.bodySmall.sans().copy(fontSize = 13.sp, letterSpacing = 0.sp, color = c.muted),
        labelLarge = base.labelLarge.sans().copy(fontWeight = FontWeight.SemiBold, fontSize = 14.5.sp, letterSpacing = 0.sp),
        labelMedium = base.labelMedium.sans().copy(fontWeight = FontWeight.Medium, fontSize = 13.sp, letterSpacing = 0.sp),
        labelSmall = base.labelSmall.sans().copy(fontWeight = FontWeight.SemiBold, fontSize = 11.5.sp, letterSpacing = 1.4.sp,
            color = c.muted),
    )
}

@Composable
fun OnwardTheme(dark: Boolean, content: @Composable () -> Unit) {
    val c = if (dark) OnwardColors.Dark else OnwardColors.Light
    val base = if (dark) darkColorScheme() else lightColorScheme()
    val scheme = base.copy(
        primary = c.accent,
        onPrimary = c.accentInk,
        primaryContainer = c.accentSoft,
        onPrimaryContainer = c.ink,
        secondary = c.ink2,
        secondaryContainer = c.accentSoft,
        onSecondaryContainer = c.ink,
        tertiary = c.medium,
        background = c.bg,
        onBackground = c.ink,
        surface = c.surface,
        onSurface = c.ink,
        onSurfaceVariant = c.muted,
        surfaceVariant = c.surface2,
        surfaceContainerLowest = c.surface,
        surfaceContainerLow = c.surface,
        surfaceContainer = c.surface,
        surfaceContainerHigh = c.surface,
        surfaceContainerHighest = c.surface2,
        surfaceTint = Color.Transparent,
        outline = c.lineStrong,
        outlineVariant = c.line,
        error = c.high,
        inverseSurface = c.ink,
        inverseOnSurface = c.bg,
        inversePrimary = if (dark) OnwardColors.Light.accent else OnwardColors.Dark.accent,
        scrim = Color.Black.copy(alpha = 0.4f),
    )
    CompositionLocalProvider(LocalOnwardColors provides c) {
        MaterialTheme(
            colorScheme = scheme,
            typography = typography(c),
            shapes = Shapes(
                extraSmall = RoundedCornerShape(8.dp),
                small = RoundedCornerShape(12.dp),
                medium = RoundedCornerShape(16.dp),
                large = RoundedCornerShape(22.dp),
                extraLarge = RoundedCornerShape(28.dp),
            ),
            content = content,
        )
    }
}
