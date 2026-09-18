package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.HealthBlue
import com.example.ui.theme.HealthCyan

@Composable
fun WaterWaveIndicator(
    currentMl: Int,
    goalMl: Int,
    modifier: Modifier = Modifier
) {
    val progressRatio = if (goalMl > 0) (currentMl.toFloat() / goalMl.toFloat()).coerceIn(0f, 1.2f) else 0f
    val animatedRatio by animateFloatAsState(
        targetValue = progressRatio.coerceAtMost(1f),
        animationSpec = tween(durationMillis = 800, easing = FastOutSlowInEasing),
        label = "waterRatio"
    )

    val remainingMl = maxOf(0, goalMl - currentMl)
    val percentage = (progressRatio * 100).toInt()

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .fillMaxWidth()
            .testTag("water_wave_indicator")
    ) {
        // Visual Cup/Bottle Fill Container
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(width = 160.dp, height = 210.dp)
                .clip(RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp, bottomStart = 38.dp, bottomEnd = 38.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Canvas(modifier = Modifier.matchParentSize()) {
                val fillHeight = size.height * animatedRatio
                val topY = size.height - fillHeight

                // Draw Water Fill Gradient
                drawRoundRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(HealthCyan, HealthBlue)
                    ),
                    topLeft = Offset(0f, topY),
                    size = Size(size.width, fillHeight),
                    cornerRadius = CornerRadius(0f, 0f)
                )

                // Subtitle measurement markers (25%, 50%, 75%)
                val markerColor = Color(0x33FFFFFF)
                val quarter = size.height / 4f
                for (i in 1..3) {
                    val y = size.height - (quarter * i)
                    drawLine(
                        color = markerColor,
                        start = Offset(16f, y),
                        end = Offset(44f, y),
                        strokeWidth = 2.5f
                    )
                }
            }

            // Center Stat Text Overlay
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.WaterDrop,
                    contentDescription = "Water",
                    tint = if (animatedRatio > 0.45f) Color.White else HealthBlue,
                    modifier = Modifier.size(32.dp)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "%,d".format(currentMl),
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 28.sp
                    ),
                    color = if (animatedRatio > 0.5f) Color.White else MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = "ml",
                    style = MaterialTheme.typography.labelMedium,
                    color = if (animatedRatio > 0.5f) Color.White.copy(alpha = 0.9f) else MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "$percentage%",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = if (animatedRatio > 0.6f) Color.White else HealthBlue
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = when {
                currentMl >= 2500 -> "Target achieved"
                remainingMl <= 0 -> "Target achieved"
                else -> "%,d ml to reach daily goal".format(remainingMl)
            },
            style = MaterialTheme.typography.bodyMedium.copy(
                fontWeight = FontWeight.SemiBold
            ),
            color = if (currentMl >= 2500 || remainingMl <= 0) HealthBlue else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
