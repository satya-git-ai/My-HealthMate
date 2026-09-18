package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.GlowCyan
import com.example.ui.theme.GlowEmerald
import com.example.ui.theme.GlowOrange
import com.example.ui.theme.HealthCyan
import com.example.ui.theme.HealthEmerald
import com.example.ui.theme.isAppInDarkTheme

@Composable
fun CircularGoalProgress(
    current: Int,
    goal: Int,
    modifier: Modifier = Modifier,
    size: Dp = 200.dp,
    strokeWidth: Dp = 16.dp,
    primaryColor: Color = GlowEmerald,
    secondaryColor: Color = GlowCyan,
    centerTitle: String = "Steps",
    unit: String = "steps",
    showStreakBadge: Boolean = false
) {
    val isDark = isAppInDarkTheme()
    val progressRatio = if (goal > 0) (current.toFloat() / goal.toFloat()).coerceIn(0f, 1.5f) else 0f
    val animatedProgress by animateFloatAsState(
        targetValue = progressRatio,
        animationSpec = tween(durationMillis = 1000, easing = FastOutSlowInEasing),
        label = "goalProgress"
    )

    val percentage = (progressRatio * 100).toInt()
    val isStreak = showStreakBadge || (goal > 0 && current >= goal)

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(size)
            .testTag("circular_goal_progress")
    ) {
        val trackColor = if (isDark) Color(0x2638BDF8) else Color(0x1F0F172A)

        Canvas(modifier = Modifier.size(size)) {
            val strokePx = strokeWidth.toPx()
            val arcSize = Size(this.size.width - strokePx, this.size.height - strokePx)
            val topLeft = Offset(strokePx / 2f, strokePx / 2f)

            // Outer Ambient Glow Halo
            if (isDark) {
                val glowStrokePx = strokePx + 10.dp.toPx()
                val glowArcSize = Size(this.size.width - glowStrokePx, this.size.height - glowStrokePx)
                val glowTopLeft = Offset(glowStrokePx / 2f, glowStrokePx / 2f)
                val sweep = 270f * animatedProgress.coerceAtMost(1f)
                if (sweep > 0f) {
                    drawArc(
                        brush = Brush.linearGradient(
                            colors = if (isStreak) {
                                listOf(GlowOrange.copy(alpha = 0.25f), Color(0xFFFF3D00).copy(alpha = 0.15f))
                            } else {
                                listOf(primaryColor.copy(alpha = 0.25f), secondaryColor.copy(alpha = 0.25f))
                            }
                        ),
                        startAngle = 135f,
                        sweepAngle = sweep,
                        useCenter = false,
                        topLeft = glowTopLeft,
                        size = glowArcSize,
                        style = Stroke(width = glowStrokePx, cap = StrokeCap.Round)
                    )
                }
            }

            // Background Track
            drawArc(
                color = trackColor,
                startAngle = 135f,
                sweepAngle = 270f,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = strokePx, cap = StrokeCap.Round)
            )

            // Foreground Progress Arc with Glowing Gradient
            val sweep = 270f * animatedProgress.coerceAtMost(1f)
            if (sweep > 0f) {
                drawArc(
                    brush = Brush.linearGradient(
                        colors = if (isStreak) {
                            listOf(GlowOrange, Color(0xFFFF3D00))
                        } else {
                            listOf(primaryColor, secondaryColor)
                        }
                    ),
                    startAngle = 135f,
                    sweepAngle = sweep,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = strokePx, cap = StrokeCap.Round)
                )
            }
        }

        // Center Content
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "%,d".format(current),
                style = MaterialTheme.typography.headlineLarge.copy(
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 32.sp
                ),
                color = if (isDark) Color(0xFFF8FAFC) else Color(0xFF0F172A)
            )
            Text(
                text = "of %,d $unit".format(goal),
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)
            )
            Spacer(modifier = Modifier.height(4.dp))
            if (isStreak) {
                Text(
                    text = "Streak achieved 🔥",
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontWeight = FontWeight.ExtraBold,
                        color = GlowOrange
                    )
                )
            } else {
                Text(
                    text = "$percentage%",
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontWeight = FontWeight.ExtraBold,
                        color = primaryColor
                    )
                )
            }
        }
    }
}
