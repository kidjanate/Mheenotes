package com.mheenotes.ui.design

import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp

@Immutable
data class Palette(
    val isDark: Boolean,
    /** App canvas behind everything. */
    val background: Color,
    /** Cards, toolbars, sheets. */
    val surface: Color,
    /** Raised floating chrome (toolbar pill, popovers). */
    val elevated: Color,
    /** Grouped rows, inputs, pressed fills. */
    val fill: Color,
    val fillStrong: Color,
    val separator: Color,
    val text: Color,
    val textSecondary: Color,
    val textTertiary: Color,
    val accent: Color,
    val accentSoft: Color,
    val onAccent: Color,
    val danger: Color,
    val dangerSoft: Color,
    val success: Color,
    val warning: Color,
    val scrim: Color,
    val shadow: Color,
    /** Area around pages in the editor. */
    val desk: Color,
)

val LightPalette = Palette(
    isDark = false,
    background = Color(0xFFF4F4F6),
    surface = Color(0xFFFFFFFF),
    elevated = Color(0xFFFFFFFF),
    fill = Color(0x0F1C1C24),
    fillStrong = Color(0x1C1C1C24),
    separator = Color(0x1A3C3C43),
    text = Color(0xFF1C1C1F),
    textSecondary = Color(0xFF6B6B72),
    textTertiary = Color(0xFFA3A3AA),
    accent = Color(0xFF3A63F0),
    accentSoft = Color(0x1F3A63F0),
    onAccent = Color(0xFFFFFFFF),
    danger = Color(0xFFE5484D),
    dangerSoft = Color(0x1AE5484D),
    success = Color(0xFF2FA36B),
    warning = Color(0xFFE79A2C),
    scrim = Color(0x47101018),
    shadow = Color(0x2E1C1C30),
    desk = Color(0xFFE9E9EE),
)

val DarkPalette = Palette(
    isDark = true,
    background = Color(0xFF0F0F12),
    surface = Color(0xFF1B1B1F),
    elevated = Color(0xFF26262B),
    fill = Color(0x14FFFFFF),
    fillStrong = Color(0x24FFFFFF),
    separator = Color(0x1FFFFFFF),
    text = Color(0xFFF4F4F6),
    textSecondary = Color(0xFFA1A1A8),
    textTertiary = Color(0xFF6C6C74),
    accent = Color(0xFF7090FF),
    accentSoft = Color(0x297090FF),
    onAccent = Color(0xFF0B1030),
    danger = Color(0xFFFF6369),
    dangerSoft = Color(0x24FF6369),
    success = Color(0xFF4CC38A),
    warning = Color(0xFFF1A10D),
    scrim = Color(0x8C000000),
    shadow = Color(0x66000000),
    desk = Color(0xFF16161A),
)

/** One family per weight: Android's custom fallback chain is built per weight. */
@Immutable
class AppFonts(
    val regular: FontFamily,
    val medium: FontFamily,
    val semibold: FontFamily,
    val bold: FontFamily,
) {
    fun forWeight(w: FontWeight): FontFamily = when {
        w.weight >= 700 -> bold
        w.weight >= 600 -> semibold
        w.weight >= 500 -> medium
        else -> regular
    }
}

@Immutable
class Typography(f: AppFonts) {
    val display = TextStyle(fontFamily = f.bold, fontWeight = FontWeight.Bold, fontSize = 32.sp, lineHeight = 38.sp, letterSpacing = (-0.02).em)
    val title = TextStyle(fontFamily = f.semibold, fontWeight = FontWeight.SemiBold, fontSize = 22.sp, lineHeight = 28.sp, letterSpacing = (-0.015).em)
    val headline = TextStyle(fontFamily = f.semibold, fontWeight = FontWeight.SemiBold, fontSize = 17.sp, lineHeight = 22.sp, letterSpacing = (-0.01).em)
    val body = TextStyle(fontFamily = f.regular, fontWeight = FontWeight.Normal, fontSize = 15.sp, lineHeight = 21.sp, letterSpacing = (-0.005).em)
    val bodyStrong = body.copy(fontFamily = f.medium, fontWeight = FontWeight.Medium)
    val callout = TextStyle(fontFamily = f.regular, fontWeight = FontWeight.Normal, fontSize = 13.5.sp, lineHeight = 18.sp)
    val calloutStrong = callout.copy(fontFamily = f.medium, fontWeight = FontWeight.Medium)
    val caption = TextStyle(fontFamily = f.medium, fontWeight = FontWeight.Medium, fontSize = 12.sp, lineHeight = 16.sp)
    val overline = TextStyle(fontFamily = f.semibold, fontWeight = FontWeight.SemiBold, fontSize = 11.sp, lineHeight = 14.sp, letterSpacing = 0.06.em)
}

object Radii {
    val xs: Dp = 6.dp
    val sm: Dp = 10.dp
    val md: Dp = 14.dp
    val lg: Dp = 20.dp
    val xl: Dp = 28.dp
}

object Space {
    val xxs: Dp = 2.dp
    val xs: Dp = 4.dp
    val sm: Dp = 8.dp
    val md: Dp = 12.dp
    val lg: Dp = 16.dp
    val xl: Dp = 24.dp
    val xxl: Dp = 32.dp
    val xxxl: Dp = 48.dp
}

/** Motion tokens. Springs everywhere so interrupted animations never jump. */
object Motion {
    fun <T> standard(): FiniteAnimationSpec<T> = spring(dampingRatio = 0.86f, stiffness = 520f)
    fun <T> snappy(): FiniteAnimationSpec<T> = spring(dampingRatio = 0.9f, stiffness = 900f)
    fun <T> gentle(): FiniteAnimationSpec<T> = spring(dampingRatio = 1f, stiffness = Spring.StiffnessLow)
    fun <T> bouncy(): FiniteAnimationSpec<T> = spring(dampingRatio = 0.68f, stiffness = 480f)
    fun <T> fade(): FiniteAnimationSpec<T> = tween(durationMillis = 170)
}

@Immutable
class Theme(val palette: Palette, val type: Typography, val fonts: AppFonts)

private val LocalTheme = staticCompositionLocalOf<Theme> { error("MheenotesTheme missing") }

object M {
    val colors: Palette @Composable @ReadOnlyComposable get() = LocalTheme.current.palette
    val type: Typography @Composable @ReadOnlyComposable get() = LocalTheme.current.type
    val fonts: AppFonts @Composable @ReadOnlyComposable get() = LocalTheme.current.fonts
}

enum class ThemeMode { System, Light, Dark }

@Composable
fun MheenotesTheme(mode: ThemeMode = ThemeMode.System, content: @Composable () -> Unit) {
    val dark = when (mode) {
        ThemeMode.System -> isSystemInDarkTheme()
        ThemeMode.Light -> false
        ThemeMode.Dark -> true
    }
    val fonts = rememberAppFonts()
    val theme = remember(dark, fonts) { Theme(if (dark) DarkPalette else LightPalette, Typography(fonts), fonts) }
    CompositionLocalProvider(LocalTheme provides theme, content = content)
}

/** Inter with IBM Plex Sans Thai as per-glyph fallback, provided by each platform. */
@Composable
expect fun rememberAppFonts(): AppFonts
