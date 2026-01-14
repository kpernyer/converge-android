// Copyright 2024-2025 Aprio One AB, Sweden
// Author: Kenneth Pernyer, kenneth@aprio.one
// SPDX-License-Identifier: MIT

package zone.converge.android.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat

// =============================================================================
// CONVERGE DESIGN SYSTEM - Ported from converge-www/src/styles/tokens.css
// =============================================================================

// -----------------------------------------------------------------------------
// Colors - Light Mode (from converge-www)
// -----------------------------------------------------------------------------
object ConvergeColors {
    // Core palette
    val Paper = Color(0xFFF5F4F0) // --paper: background/lightest
    val Ink = Color(0xFF111111) // --ink: primary text
    val InkSecondary = Color(0xFF3A3A3A) // --ink-secondary
    val InkMuted = Color(0xFF666666) // --ink-muted

    // Structural colors
    val Rule = Color(0xFFC8C6C0) // --rule: borders, dividers
    val RuleLight = Color(0xFFDBD9D3) // --rule-light
    val Surface = Color(0xFFEAE9E4) // --surface: cards, hover
    val SurfaceHover = Color(0xFFE0DFDA) // --surface-hover

    // Accent - Converge Green
    val Accent = Color(0xFF2D5A3D) // --accent: primary brand color
    val AccentLight = Color(0xFF3D7A5D) // --accent-light
    val AccentDark = Color(0xFF244A31) // button hover

    // Status colors
    val Success = Color(0xFF2D5A3D) // --status-success (same as accent)
    val Warning = Color(0xFF6B5B3D) // --status-warning (brown/tan)
    val Error = Color(0xFF5A3D3D) // --status-error (dark red)

    // Pack colors (for domain areas)
    val PackMoney = Color(0xFF2E7D32)
    val PackCustomers = Color(0xFF1565C0)
    val PackDelivery = Color(0xFFE65100)
    val PackPeople = Color(0xFF6A1B9A)
    val PackTrust = Color(0xFF00695C)
}

// Dark mode colors (derived from light mode)
object ConvergeColorsDark {
    val Paper = Color(0xFF1A1918)
    val Ink = Color(0xFFF5F4F0)
    val InkSecondary = Color(0xFFCCCCC8)
    val InkMuted = Color(0xFF999996)
    val Rule = Color(0xFF3A3938)
    val RuleLight = Color(0xFF2A2928)
    val Surface = Color(0xFF252423)
    val SurfaceHover = Color(0xFF2F2E2D)
    val Accent = Color(0xFF4CAF50)
    val AccentLight = Color(0xFF81C784)
    val AccentDark = Color(0xFF388E3C)
}

// -----------------------------------------------------------------------------
// Spacing - 4dp base unit (from converge-www)
// -----------------------------------------------------------------------------
object ConvergeSpacing {
    val Space1 = 4.dp // --space-1: 0.25rem
    val Space2 = 8.dp // --space-2: 0.5rem
    val Space3 = 12.dp // --space-3: 0.75rem
    val Space4 = 16.dp // --space-4: 1rem
    val Space5 = 20.dp // --space-5: 1.25rem
    val Space6 = 24.dp // --space-6: 1.5rem
    val Space8 = 32.dp // --space-8: 2rem
    val Space10 = 40.dp // --space-10: 2.5rem
    val Space12 = 48.dp // --space-12: 3rem
    val Space16 = 64.dp // --space-16: 4rem
    val Space20 = 80.dp // --space-20: 5rem

    val HeaderHeight = 56.dp // --header-height: 3.5rem
}

// -----------------------------------------------------------------------------
// Typography - Inter for body, IBM Plex Mono for headings
// -----------------------------------------------------------------------------
// Note: Add google fonts to dependencies for actual font loading
// For now, using system fallbacks that match the spirit

val ConvergeTypography = Typography(
    // Display styles - IBM Plex Mono style (monospace)
    displayLarge = TextStyle(
        fontFamily = FontFamily.Monospace,
        fontWeight = FontWeight.SemiBold,
        fontSize = 40.sp, // --text-4xl
        lineHeight = 52.sp,
    ),
    displayMedium = TextStyle(
        fontFamily = FontFamily.Monospace,
        fontWeight = FontWeight.SemiBold,
        fontSize = 32.sp, // --text-3xl
        lineHeight = 42.sp,
    ),
    displaySmall = TextStyle(
        fontFamily = FontFamily.Monospace,
        fontWeight = FontWeight.SemiBold,
        fontSize = 24.sp, // --text-2xl
        lineHeight = 32.sp,
    ),

    // Headline styles
    headlineLarge = TextStyle(
        fontFamily = FontFamily.Monospace,
        fontWeight = FontWeight.SemiBold,
        fontSize = 20.sp, // --text-xl
        lineHeight = 26.sp,
    ),
    headlineMedium = TextStyle(
        fontFamily = FontFamily.Monospace,
        fontWeight = FontWeight.SemiBold,
        fontSize = 18.sp, // --text-lg
        lineHeight = 24.sp,
    ),
    headlineSmall = TextStyle(
        fontFamily = FontFamily.Monospace,
        fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp, // --text-md
        lineHeight = 22.sp,
    ),

    // Title styles
    titleLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium,
        fontSize = 18.sp,
        lineHeight = 27.sp,
    ),
    titleMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium,
        fontSize = 16.sp,
        lineHeight = 24.sp,
    ),
    titleSmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp, // --text-base
        lineHeight = 21.sp,
    ),

    // Body styles - Inter style (sans-serif)
    bodyLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp, // --text-md
        lineHeight = 24.sp,
    ),
    bodyMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp, // --text-base
        lineHeight = 21.sp,
    ),
    bodySmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp, // --text-sm
        lineHeight = 18.sp,
    ),

    // Label styles
    labelLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 20.sp,
    ),
    labelMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 16.sp,
    ),
    labelSmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium,
        fontSize = 10.sp, // --text-xs
        lineHeight = 14.sp,
    ),
)

// -----------------------------------------------------------------------------
// Material3 Color Schemes
// -----------------------------------------------------------------------------
private val LightColorScheme = lightColorScheme(
    primary = ConvergeColors.Accent,
    onPrimary = Color.White,
    primaryContainer = ConvergeColors.AccentLight,
    onPrimaryContainer = Color.White,

    secondary = ConvergeColors.InkSecondary,
    onSecondary = ConvergeColors.Paper,
    secondaryContainer = ConvergeColors.Surface,
    onSecondaryContainer = ConvergeColors.Ink,

    tertiary = ConvergeColors.PackDelivery,
    onTertiary = Color.White,

    background = ConvergeColors.Paper,
    onBackground = ConvergeColors.Ink,

    surface = Color.White,
    onSurface = ConvergeColors.Ink,
    surfaceVariant = ConvergeColors.Surface,
    onSurfaceVariant = ConvergeColors.InkSecondary,

    outline = ConvergeColors.Rule,
    outlineVariant = ConvergeColors.RuleLight,

    error = ConvergeColors.Error,
    onError = Color.White,
)

private val DarkColorScheme = darkColorScheme(
    primary = ConvergeColorsDark.Accent,
    onPrimary = Color.Black,
    primaryContainer = ConvergeColorsDark.AccentDark,
    onPrimaryContainer = Color.White,

    secondary = ConvergeColorsDark.InkSecondary,
    onSecondary = ConvergeColorsDark.Paper,
    secondaryContainer = ConvergeColorsDark.Surface,
    onSecondaryContainer = ConvergeColorsDark.Ink,

    tertiary = Color(0xFFFF9800),
    onTertiary = Color.Black,

    background = ConvergeColorsDark.Paper,
    onBackground = ConvergeColorsDark.Ink,

    surface = ConvergeColorsDark.Surface,
    onSurface = ConvergeColorsDark.Ink,
    surfaceVariant = ConvergeColorsDark.SurfaceHover,
    onSurfaceVariant = ConvergeColorsDark.InkSecondary,

    outline = ConvergeColorsDark.Rule,
    outlineVariant = ConvergeColorsDark.RuleLight,

    error = Color(0xFFCF6679),
    onError = Color.Black,
)

// -----------------------------------------------------------------------------
// Theme Composable
// -----------------------------------------------------------------------------
@Composable
fun ConvergeTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic color disabled by default to preserve brand colors
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) {
                dynamicDarkColorScheme(context)
            } else {
                dynamicLightColorScheme(context)
            }
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = Color.Transparent.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = ConvergeTypography,
        content = content,
    )
}

// -----------------------------------------------------------------------------
// Custom Theme Extensions
// -----------------------------------------------------------------------------

/**
 * Extended colors for Converge-specific use cases.
 */
data class ConvergeExtendedColors(
    val ink: Color,
    val inkSecondary: Color,
    val inkMuted: Color,
    val paper: Color,
    val surface: Color,
    val surfaceHover: Color,
    val rule: Color,
    val ruleLight: Color,
    val accent: Color,
    val accentLight: Color,
    val success: Color,
    val warning: Color,
    val packMoney: Color,
    val packCustomers: Color,
    val packDelivery: Color,
    val packPeople: Color,
    val packTrust: Color,
)

val LocalConvergeColors = staticCompositionLocalOf {
    ConvergeExtendedColors(
        ink = ConvergeColors.Ink,
        inkSecondary = ConvergeColors.InkSecondary,
        inkMuted = ConvergeColors.InkMuted,
        paper = ConvergeColors.Paper,
        surface = ConvergeColors.Surface,
        surfaceHover = ConvergeColors.SurfaceHover,
        rule = ConvergeColors.Rule,
        ruleLight = ConvergeColors.RuleLight,
        accent = ConvergeColors.Accent,
        accentLight = ConvergeColors.AccentLight,
        success = ConvergeColors.Success,
        warning = ConvergeColors.Warning,
        packMoney = ConvergeColors.PackMoney,
        packCustomers = ConvergeColors.PackCustomers,
        packDelivery = ConvergeColors.PackDelivery,
        packPeople = ConvergeColors.PackPeople,
        packTrust = ConvergeColors.PackTrust,
    )
}
