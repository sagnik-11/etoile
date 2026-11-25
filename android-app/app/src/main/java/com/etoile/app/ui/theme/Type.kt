package com.etoile.app.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

private val Serif = FontFamily.Serif
private val Sans = FontFamily.SansSerif

val Typography = Typography(
    // The italic "Étoile" wordmark
    displaySmall = TextStyle(fontFamily = Serif, fontStyle = FontStyle.Italic, fontSize = 32.sp, letterSpacing = 0.5.sp, color = Cream),
    // Screen titles, e.g. "What shall we watch?"
    headlineSmall = TextStyle(fontFamily = Serif, fontSize = 22.sp, lineHeight = 28.sp, color = Cream),
    // Card titles, e.g. "Movie night"
    titleMedium = TextStyle(fontFamily = Serif, fontSize = 17.sp, lineHeight = 22.sp, color = Cream),
    // Countdown numbers
    titleSmall = TextStyle(fontFamily = Serif, fontSize = 16.sp, color = Cream),
    bodyLarge = TextStyle(fontFamily = Sans, fontSize = 15.sp, lineHeight = 21.sp, color = Cream),
    bodyMedium = TextStyle(fontFamily = Sans, fontSize = 13.sp, lineHeight = 18.sp, color = Cream),
    bodySmall = TextStyle(fontFamily = Sans, fontSize = 11.sp, lineHeight = 15.sp, color = Muted),
    // Small spaced labels, e.g. "Next under the stars"
    labelSmall = TextStyle(fontFamily = Sans, fontSize = 10.sp, letterSpacing = 1.8.sp, color = Muted),
    labelLarge = TextStyle(fontFamily = Sans, fontWeight = FontWeight.Medium, fontSize = 14.sp, letterSpacing = 0.3.sp),
)
