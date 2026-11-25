package com.etoile.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import com.etoile.app.data.Role

/** The one colour that differs between HIM and HER. */
@Immutable
data class Accent(val main: Color, val star: Color) {
    val line: Color get() = main.copy(alpha = 0.45f)
    val soft: Color get() = main.copy(alpha = 0.08f)
}

fun accentFor(role: Role?): Accent = when (role) {
    Role.HER -> Accent(Rose, RoseStar)
    else -> Accent(Gold, GoldStar)
}

val LocalAccent = staticCompositionLocalOf { accentFor(null) }

@Composable
fun EtoileTheme(role: Role? = null, content: @Composable () -> Unit) {
    val accent = accentFor(role)
    val colors = darkColorScheme(
        primary = accent.main,
        onPrimary = Navy,
        secondary = accent.star,
        background = Navy,
        onBackground = Cream,
        surface = NavyRaised,
        onSurface = Cream,
        surfaceVariant = NavyRaised,
        onSurfaceVariant = Muted,
        outline = Hairline,
        error = Danger,
    )
    CompositionLocalProvider(LocalAccent provides accent) {
        MaterialTheme(colorScheme = colors, typography = Typography, content = content)
    }
}

/** Shortcut: `Etoile.accent` inside any composable. */
object Etoile {
    val accent: Accent
        @Composable get() = LocalAccent.current
}
