package com.etoile.app.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import com.etoile.app.ui.theme.Etoile
import com.etoile.app.ui.theme.Navy
import kotlin.math.PI
import kotlin.math.sin
import kotlin.random.Random

/** One background star. Positions are fractions (0..1) of the screen, so it fits any size. */
private data class Star(val x: Float, val y: Float, val radius: Float, val phase: Float, val speed: Float)

/** A small constellation in the top-right corner, drawn in the accent colour. */
private val constellation = listOf(
    Offset(0.62f, 0.05f), Offset(0.73f, 0.09f), Offset(0.86f, 0.075f),
    Offset(0.93f, 0.12f), Offset(0.82f, 0.16f),
)

/**
 * Navy night sky with softly twinkling stars. Put any screen inside it:
 * StarrySky { MyScreen() }
 */
@Composable
fun StarrySky(
    modifier: Modifier = Modifier,
    showConstellation: Boolean = true,
    content: @Composable BoxScope.() -> Unit,
) {
    val accent = Etoile.accent
    val stars = remember {
        val random = Random(26)   // fixed seed: same sky every time
        List(70) {
            Star(
                x = random.nextFloat(),
                y = random.nextFloat(),
                radius = 0.6f + random.nextFloat() * 1.1f,
                phase = random.nextFloat() * 2f * PI.toFloat(),
                speed = 0.6f + random.nextFloat() * 0.8f,
            )
        }
    }
    val time by rememberInfiniteTransition(label = "twinkle").animateFloat(
        initialValue = 0f,
        targetValue = 2f * PI.toFloat(),
        animationSpec = infiniteRepeatable(tween(6000, easing = LinearEasing), RepeatMode.Restart),
        label = "time",
    )

    Box(modifier.fillMaxSize().background(Navy)) {
        Canvas(Modifier.fillMaxSize()) {
            stars.forEach { star ->
                val twinkle = 0.25f + 0.35f * (1f + sin(time * star.speed + star.phase)) / 2f
                drawCircle(
                    color = Color.White.copy(alpha = twinkle),
                    radius = star.radius * density,
                    center = Offset(star.x * size.width, star.y * size.height),
                )
            }
            if (showConstellation) drawConstellation(accent.main, accent.star)
        }
        content()
    }
}

private fun DrawScope.drawConstellation(line: Color, star: Color) {
    val points = constellation.map { Offset(it.x * size.width, it.y * size.height) }
    points.zipWithNext { a, b ->
        drawLine(line.copy(alpha = 0.35f), a, b, strokeWidth = 0.6f * density)
    }
    points.forEachIndexed { i, p ->
        drawCircle(star, radius = (if (i % 2 == 0) 1.6f else 1.1f) * density, center = p)
    }
}
