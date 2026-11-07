package com.respotfck.spotifck.ui

import android.os.Build
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.respotfck.spotifck.ui.theme.Saira
import kotlin.math.PI
import kotlin.math.sin

private val BrandRed = Color(0xFFD90218)
private const val WORDMARK = "ReSpotifuck"

/**
 * Animated launch screen. On Android 12+ the system splash has already drawn the logo
 * (see splash_icon_animated.xml), so this continues from the finished logo; older
 * versions draw it here.
 */
@Composable
fun LaunchSplash(status: String?, modifier: Modifier = Modifier) {
    val drawLogoIn = Build.VERSION.SDK_INT < Build.VERSION_CODES.S
    val logoIn = remember { Animatable(if (drawLogoIn) 0f else 1f) }
    val pulse = remember { Animatable(0f) }
    val wordmark = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        logoIn.animateTo(1f, tween(650, easing = FastOutSlowInEasing))
        pulse.animateTo(1f, tween(420, easing = FastOutSlowInEasing))
    }
    LaunchedEffect(Unit) {
        wordmark.animateTo(1f, tween(750, delayMillis = 150, easing = FastOutSlowInEasing))
    }

    val loop = rememberInfiniteTransition(label = "splash")
    val glow by loop.animateFloat(
        0.16f, 0.40f,
        infiniteRepeatable(tween(1400, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "glow"
    )
    val orbit by loop.animateFloat(
        0f, 360f, infiniteRepeatable(tween(2400, easing = LinearEasing)), label = "orbit"
    )
    val wavePhase by loop.animateFloat(
        0f, (2 * PI).toFloat(), infiniteRepeatable(tween(1500, easing = LinearEasing)), label = "waves"
    )
    val shimmer by loop.animateFloat(
        -0.4f, 1.6f, infiniteRepeatable(tween(1900, easing = LinearEasing)), label = "shimmer"
    )

    Box(
        modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .drawBehind {
                drawRect(
                    Brush.radialGradient(
                        listOf(BrandRed.copy(alpha = glow * wordmark.value), Color.Transparent),
                        center = Offset(size.width / 2f, size.height / 2f - 60.dp.toPx()),
                        radius = size.minDimension * 0.8f
                    )
                )
            },
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(Modifier.size(260.dp), contentAlignment = Alignment.Center) {
                Canvas(Modifier.fillMaxSize()) { drawOrbits(orbit, wordmark.value) }
                Canvas(Modifier.size(178.dp)) {
                    val beat = 1f + 0.07f * sin(PI.toFloat() * pulse.value)
                    scale(beat) { drawLogo(logoIn.value, wavePhase, pulse.value) }
                }
            }

            Spacer(Modifier.height(8.dp))
            Wordmark(wordmark.value, shimmer)
            Spacer(Modifier.height(22.dp))
            Equalizer(wordmark.value)
            Spacer(Modifier.height(18.dp))
            Text(
                text = status.orEmpty(),
                color = Color.White.copy(alpha = 0.55f * wordmark.value),
                fontSize = 13.sp,
                letterSpacing = 1.5.sp
            )
        }
    }
}

private fun DrawScope.drawOrbits(angle: Float, alpha: Float) {
    if (alpha <= 0f) return
    val stroke = 2.5.dp.toPx()
    val outer = size.minDimension / 2f - stroke
    val inner = outer - 14.dp.toPx()
    rotate(angle) {
        drawArc(
            brush = Brush.sweepGradient(listOf(Color.Transparent, BrandRed, Color.Transparent)),
            startAngle = 0f, sweepAngle = 280f, useCenter = false,
            topLeft = Offset(center.x - outer, center.y - outer), size = Size(outer * 2, outer * 2),
            style = Stroke(stroke, cap = StrokeCap.Round), alpha = alpha
        )
    }
    rotate(-angle * 1.5f) {
        drawArc(
            brush = Brush.sweepGradient(listOf(Color.Transparent, Color.White.copy(alpha = 0.5f), Color.Transparent)),
            startAngle = 0f, sweepAngle = 200f, useCenter = false,
            topLeft = Offset(center.x - inner, center.y - inner), size = Size(inner * 2, inner * 2),
            style = Stroke(stroke * 0.6f, cap = StrokeCap.Round), alpha = alpha
        )
    }
}

// Logo geometry in a 178-unit box: ring radius 84 around the centre, three waves.
private val WAVES = listOf(
    floatArrayOf(-50f, -20f, 0f, -44f, 50f, -20f, 15f),
    floatArrayOf(-40f, 7f, 0f, -13f, 40f, 7f, 13f),
    floatArrayOf(-30f, 32f, 0f, 18f, 30f, 32f, 11f),
)

private fun DrawScope.drawLogo(progress: Float, wavePhase: Float, pulse: Float) {
    val u = size.minDimension / 178f
    val c = center
    val radius = 84f * u
    drawCircle(Color.Black, radius, c)
    val ringP = (progress / 0.6f).coerceIn(0f, 1f)
    drawArc(
        BrandRed, -90f, 360f * ringP, useCenter = false,
        topLeft = Offset(c.x - radius, c.y - radius), size = Size(radius * 2, radius * 2),
        style = Stroke(9f * u, cap = StrokeCap.Round)
    )
    val measure = PathMeasure()
    WAVES.forEachIndexed { i, w ->
        val waveP = ((progress - 0.35f - i * 0.1f) / 0.3f).coerceIn(0f, 1f)
        if (waveP <= 0f) return@forEachIndexed
        val path = Path().apply {
            moveTo(c.x + w[0] * u, c.y + w[1] * u)
            quadraticTo(c.x + w[2] * u, c.y + w[3] * u, c.x + w[4] * u, c.y + w[5] * u)
        }
        val segment = Path()
        measure.setPath(path, false)
        measure.getSegment(0f, measure.length * waveP, segment, true)
        // Once the intro pulse finishes, the waves shimmer outward like a broadcast signal.
        val signal = 0.7f + 0.3f * sin(wavePhase - i * 0.9f)
        val alpha = 1f - pulse + pulse * signal
        drawPath(segment, BrandRed.copy(alpha = alpha), style = Stroke(w[6] * u, cap = StrokeCap.Round))
    }
}

@Composable
private fun Wordmark(progress: Float, shimmer: Float) {
    Row(
        Modifier
            .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
            .drawWithContent {
                drawContent()
                val x = size.width * shimmer
                drawRect(
                    Brush.linearGradient(
                        listOf(Color.Transparent, Color.White.copy(alpha = 0.6f), Color.Transparent),
                        start = Offset(x - size.width * 0.25f, 0f),
                        end = Offset(x + size.width * 0.25f, size.height)
                    ),
                    blendMode = BlendMode.SrcAtop
                )
            }
    ) {
        val spread = 4f
        WORDMARK.forEachIndexed { i, ch ->
            val p = ((progress * (WORDMARK.length + spread) - i) / spread).coerceIn(0f, 1f)
            Text(
                text = ch.toString(),
                color = if (i < 2) BrandRed else Color.White,
                fontFamily = Saira,
                fontWeight = FontWeight.Black,
                fontSize = 40.sp,
                modifier = Modifier.graphicsLayer {
                    alpha = p
                    translationY = (1f - p) * 18.dp.toPx()
                    scaleX = 0.85f + 0.15f * p
                    scaleY = 0.85f + 0.15f * p
                }
            )
        }
    }
}

@Composable
private fun Equalizer(alpha: Float) {
    val loop = rememberInfiniteTransition(label = "eq")
    val durations = listOf(520, 680, 460, 740, 600)
    val heights = durations.mapIndexed { i, d ->
        loop.animateFloat(
            0.25f, 1f,
            infiniteRepeatable(tween(d, delayMillis = i * 40, easing = FastOutSlowInEasing), RepeatMode.Reverse),
            label = "bar$i"
        )
    }
    Row(
        Modifier.height(26.dp).graphicsLayer { this.alpha = alpha },
        horizontalArrangement = Arrangement.spacedBy(5.dp),
        verticalAlignment = Alignment.Bottom
    ) {
        heights.forEach { h ->
            Canvas(Modifier.width(5.dp).height(26.dp)) {
                val barH = size.height * h.value
                drawRoundRect(
                    Brush.verticalGradient(listOf(BrandRed, BrandRed.copy(alpha = 0.35f))),
                    topLeft = Offset(0f, size.height - barH),
                    size = Size(size.width, barH),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(size.width / 2f)
                )
            }
        }
    }
}
