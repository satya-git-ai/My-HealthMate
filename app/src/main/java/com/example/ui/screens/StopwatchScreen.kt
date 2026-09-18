package com.example.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.GlassCard
import com.example.ui.components.GlowPill
import com.example.ui.theme.GlowCyan
import com.example.ui.theme.GlowEmerald
import com.example.ui.theme.GlowOrange
import com.example.ui.theme.GlowPurple
import com.example.ui.theme.HealthRose
import com.example.ui.theme.isAppInDarkTheme
import kotlinx.coroutines.delay

data class StopwatchLap(
    val lapIndex: Int,
    val lapDurationMillis: Long,
    val overallMillis: Long
)

enum class StopwatchState {
    IDLE,
    RUNNING,
    PAUSED
}

@Composable
fun StopwatchScreen(
    onBack: () -> Unit,
    onHome: () -> Unit = onBack,
    modifier: Modifier = Modifier
) {
    var state by remember { mutableStateOf(StopwatchState.IDLE) }
    var elapsedMillis by remember { mutableLongStateOf(0L) }
    val laps = remember { mutableStateListOf<StopwatchLap>() }
    var lastLapMarkMillis by remember { mutableLongStateOf(0L) }

    val isDark = isAppInDarkTheme()

    // Smooth Coroutine Ticker (~16ms for 60fps millisecond precision)
    LaunchedEffect(state) {
        if (state == StopwatchState.RUNNING) {
            val startTime = System.currentTimeMillis() - elapsedMillis
            while (state == StopwatchState.RUNNING) {
                elapsedMillis = System.currentTimeMillis() - startTime
                delay(16)
            }
        }
    }

    // Time calculations
    val totalSeconds = elapsedMillis / 1000
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    val millis = (elapsedMillis % 1000) / 10 // Two digits (00-99)

    val currentLapDuration = elapsedMillis - lastLapMarkMillis

    // Identify fastest and slowest lap when multiple exist
    val minLapTime = if (laps.size > 1) laps.minOf { it.lapDurationMillis } else -1L
    val maxLapTime = if (laps.size > 1) laps.maxOf { it.lapDurationMillis } else -1L

    // Pulsing Glow Animation for Running State
    val infiniteTransition = rememberInfiniteTransition(label = "stopwatchGlow")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.35f,
        targetValue = 0.9f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlpha"
    )

    val activeGlowColor by animateColorAsState(
        targetValue = when (state) {
            StopwatchState.RUNNING -> GlowCyan
            StopwatchState.PAUSED -> GlowOrange
            StopwatchState.IDLE -> GlowPurple
        },
        label = "activeGlowColor"
    )

    val backgroundBrush = if (isDark) {
        Brush.verticalGradient(
            listOf(
                Color(0xFF070C18),
                Color(0xFF0C152B),
                Color(0xFF060A14)
            )
        )
    } else {
        Brush.verticalGradient(
            listOf(
                Color(0xFFF1F5F9),
                Color(0xFFE2E8F0),
                Color(0xFFF8FAFC)
            )
        )
    }

    val textPrimary = if (isDark) Color(0xFFF8FAFC) else Color(0xFF0F172A)
    val textSecondary = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(backgroundBrush)
            .statusBarsPadding()
            .padding(horizontal = 20.dp)
            .testTag("stopwatch_screen")
    ) {
        Spacer(modifier = Modifier.height(12.dp))

        // Top Header Bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier.testTag("stopwatch_back_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = textPrimary
                    )
                }

                Spacer(modifier = Modifier.width(6.dp))

                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Stop Watch",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.ExtraBold,
                                letterSpacing = 0.5.sp
                            ),
                            color = textPrimary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        GlowPill(
                            glowColor = activeGlowColor,
                            modifier = Modifier.padding(top = 2.dp)
                        ) {
                            Text(
                                text = when (state) {
                                    StopwatchState.RUNNING -> "RUNNING"
                                    StopwatchState.PAUSED -> "PAUSED"
                                    StopwatchState.IDLE -> "READY"
                                },
                                fontSize = 10.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = activeGlowColor,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                            )
                        }
                    }
                    Text(
                        text = "High-precision timer with lap splitting",
                        style = MaterialTheme.typography.labelSmall,
                        color = textSecondary
                    )
                }
            }

            IconButton(
                onClick = onHome,
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(if (isDark) Color(0x3338BDF8) else Color(0x1A0284C7))
                    .testTag("stopwatch_home_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Home,
                    contentDescription = "Home",
                    tint = textPrimary,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Hero Glossy Stopwatch Dial Card
        GlassCard(
            glowColor = activeGlowColor.copy(alpha = if (state == StopwatchState.RUNNING) pulseAlpha * 0.4f else 0.15f),
            glassAlpha = 0.85f,
            shape = RoundedCornerShape(28.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 20.dp, horizontal = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Circular Glowing Progress Ring & Timer Readout
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.size(230.dp)
                ) {
                    Canvas(modifier = Modifier.size(230.dp)) {
                        val strokePx = 12.dp.toPx()
                        val arcSize = Size(size.width - strokePx, size.height - strokePx)
                        val topLeft = Offset(strokePx / 2f, strokePx / 2f)

                        // Ambient Track
                        drawArc(
                            color = if (isDark) Color(0x2638BDF8) else Color(0x1F0F172A),
                            startAngle = 0f,
                            sweepAngle = 360f,
                            useCenter = false,
                            topLeft = topLeft,
                            size = arcSize,
                            style = Stroke(width = strokePx, cap = StrokeCap.Round)
                        )

                        // Seconds sweep arc (0-60s full circle)
                        val secondsSweep = ((seconds + (millis / 100f)) / 60f) * 360f
                        drawArc(
                            brush = Brush.sweepGradient(
                                listOf(
                                    activeGlowColor.copy(alpha = 0.3f),
                                    activeGlowColor,
                                    GlowEmerald
                                )
                            ),
                            startAngle = -90f,
                            sweepAngle = secondsSweep,
                            useCenter = false,
                            topLeft = topLeft,
                            size = arcSize,
                            style = Stroke(width = strokePx, cap = StrokeCap.Round)
                        )

                        // Millisecond Inner Glow Ring Indicator
                        if (state == StopwatchState.RUNNING) {
                            val msSweep = (millis / 100f) * 360f
                            val innerStrokePx = 3.dp.toPx()
                            val innerSize = Size(size.width - 44.dp.toPx(), size.height - 44.dp.toPx())
                            val innerTopLeft = Offset(22.dp.toPx(), 22.dp.toPx())

                            drawArc(
                                color = GlowEmerald.copy(alpha = pulseAlpha),
                                startAngle = -90f,
                                sweepAngle = msSweep,
                                useCenter = false,
                                topLeft = innerTopLeft,
                                size = innerSize,
                                style = Stroke(width = innerStrokePx, cap = StrokeCap.Round)
                            )
                        }
                    }

                    // Center High-Precision Digital Time
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(horizontal = 8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Timer,
                            contentDescription = null,
                            tint = activeGlowColor,
                            modifier = Modifier.size(24.dp)
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        // Digital Readout
                        Row(
                            verticalAlignment = Alignment.Bottom,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            if (hours > 0) {
                                Text(
                                    text = "%02d:".format(hours),
                                    fontSize = 32.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontFamily = FontFamily.Monospace,
                                    color = textPrimary,
                                    letterSpacing = (-0.5).sp
                                )
                            }
                            Text(
                                text = "%02d:%02d".format(minutes, seconds),
                                fontSize = if (hours > 0) 32.sp else 38.sp,
                                fontWeight = FontWeight.ExtraBold,
                                fontFamily = FontFamily.Monospace,
                                color = textPrimary,
                                letterSpacing = (-0.5).sp
                            )
                            Text(
                                text = ".%02d".format(millis),
                                fontSize = if (hours > 0) 20.sp else 24.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                color = activeGlowColor,
                                modifier = Modifier.padding(bottom = 3.dp, start = 2.dp)
                            )
                        }

                        // Current Lap split indicator
                        if (laps.isNotEmpty()) {
                            val lapSec = (currentLapDuration / 1000) % 60
                            val lapMin = (currentLapDuration / 1000) / 60
                            val lapMs = (currentLapDuration % 1000) / 10
                            Text(
                                text = "Lap ${laps.size + 1}: %02d:%02d.%02d".format(lapMin, lapSec, lapMs),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = textSecondary
                            )
                        } else {
                            Text(
                                text = "PRECISION 0.01s",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp,
                                color = textSecondary
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Primary Action Controls (Start / Stop & Pause / Continue & Lap / Reset)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Left Secondary Button: Lap (when running) or Reset (when paused/idle)
                    when (state) {
                        StopwatchState.RUNNING -> {
                            Button(
                                onClick = {
                                    val lapDuration = elapsedMillis - lastLapMarkMillis
                                    laps.add(
                                        0,
                                        StopwatchLap(
                                            lapIndex = laps.size + 1,
                                            lapDurationMillis = lapDuration,
                                            overallMillis = elapsedMillis
                                        )
                                    )
                                    lastLapMarkMillis = elapsedMillis
                                },
                                shape = RoundedCornerShape(16.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isDark) Color(0x3338BDF8) else Color(0x260284C7)
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(52.dp)
                                    .testTag("stopwatch_lap_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Flag,
                                    contentDescription = "Lap",
                                    tint = GlowCyan,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Lap",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isDark) Color.White else Color(0xFF0284C7)
                                )
                            }
                        }
                        StopwatchState.PAUSED -> {
                            OutlinedButton(
                                onClick = {
                                    state = StopwatchState.IDLE
                                    elapsedMillis = 0L
                                    laps.clear()
                                    lastLapMarkMillis = 0L
                                },
                                shape = RoundedCornerShape(16.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(52.dp)
                                    .testTag("stopwatch_reset_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = "Reset",
                                    tint = textSecondary,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Reset",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = textPrimary
                                )
                            }
                        }
                        StopwatchState.IDLE -> {
                            OutlinedButton(
                                onClick = {
                                    elapsedMillis = 0L
                                    laps.clear()
                                    lastLapMarkMillis = 0L
                                },
                                enabled = elapsedMillis > 0 || laps.isNotEmpty(),
                                shape = RoundedCornerShape(16.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(52.dp)
                                    .testTag("stopwatch_reset_button_idle")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = "Reset",
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Reset",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    // Right Primary Button: Start / Stop & Pause / Continue
                    when (state) {
                        StopwatchState.IDLE -> {
                            Button(
                                onClick = { state = StopwatchState.RUNNING },
                                shape = RoundedCornerShape(16.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = GlowEmerald
                                ),
                                modifier = Modifier
                                    .weight(1.3f)
                                    .height(52.dp)
                                    .testTag("stopwatch_start_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PlayArrow,
                                    contentDescription = "Start",
                                    tint = Color.Black,
                                    modifier = Modifier.size(22.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Start",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color.Black
                                )
                            }
                        }
                        StopwatchState.RUNNING -> {
                            // Pause button
                            Button(
                                onClick = { state = StopwatchState.PAUSED },
                                shape = RoundedCornerShape(16.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = GlowOrange
                                ),
                                modifier = Modifier
                                    .weight(1.3f)
                                    .height(52.dp)
                                    .testTag("stopwatch_pause_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Pause,
                                    contentDescription = "Pause",
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Pause",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color.White
                                )
                            }
                        }
                        StopwatchState.PAUSED -> {
                            // Continue / Resume button
                            Button(
                                onClick = { state = StopwatchState.RUNNING },
                                shape = RoundedCornerShape(16.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = GlowEmerald
                                ),
                                modifier = Modifier
                                    .weight(1.3f)
                                    .height(52.dp)
                                    .testTag("stopwatch_continue_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PlayArrow,
                                    contentDescription = "Continue",
                                    tint = Color.Black,
                                    modifier = Modifier.size(22.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Continue",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color.Black
                                )
                            }
                        }
                    }
                }

                // Stop & Finish workout action when paused or running
                if (state != StopwatchState.IDLE) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Button(
                        onClick = {
                            state = StopwatchState.PAUSED
                        },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = HealthRose.copy(alpha = 0.15f)
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(38.dp)
                            .testTag("stopwatch_stop_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Stop,
                            contentDescription = "Stop",
                            tint = HealthRose,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Stop Stopwatch",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = HealthRose
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // Laps Section
        Text(
            text = "Lap Splits (${laps.size})",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = textPrimary
        )

        Spacer(modifier = Modifier.height(10.dp))

        if (laps.isEmpty()) {
            GlassCard(
                glowColor = Color.Transparent,
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.Flag,
                        contentDescription = null,
                        tint = textSecondary.copy(alpha = 0.5f),
                        modifier = Modifier.size(36.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "No laps recorded yet",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                        color = textSecondary
                    )
                    Text(
                        text = "Press 'Lap' while the stopwatch is running to record splits",
                        style = MaterialTheme.typography.bodySmall,
                        color = textSecondary.copy(alpha = 0.7f),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            }
        } else {
            GlassCard(
                glowColor = GlowCyan.copy(alpha = 0.1f),
                shape = RoundedCornerShape(20.dp),
                contentPadding = PaddingValues(0.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(12.dp)
                ) {
                    itemsIndexed(laps) { _, lap ->
                        val isFastest = laps.size > 1 && lap.lapDurationMillis == minLapTime
                        val isSlowest = laps.size > 1 && lap.lapDurationMillis == maxLapTime

                        val lapSec = (lap.lapDurationMillis / 1000) % 60
                        val lapMin = (lap.lapDurationMillis / 1000) / 60
                        val lapMs = (lap.lapDurationMillis % 1000) / 10

                        val overallSec = (lap.overallMillis / 1000) % 60
                        val overallMin = (lap.overallMillis / 1000) / 60
                        val overallMs = (lap.overallMillis % 1000) / 10

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp, horizontal = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "Lap %02d".format(lap.lapIndex),
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                    color = textPrimary
                                )

                                if (isFastest) {
                                    Spacer(modifier = Modifier.width(6.dp))
                                    GlowPill(
                                        glowColor = GlowEmerald,
                                        backgroundColor = GlowEmerald.copy(alpha = 0.15f)
                                    ) {
                                        Text(
                                            text = "FASTEST 🔥",
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = GlowEmerald,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                } else if (isSlowest) {
                                    Spacer(modifier = Modifier.width(6.dp))
                                    GlowPill(
                                        glowColor = GlowOrange,
                                        backgroundColor = GlowOrange.copy(alpha = 0.15f)
                                    ) {
                                        Text(
                                            text = "SLOWEST",
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = GlowOrange,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        text = "+%02d:%02d.%02d".format(lapMin, lapSec, lapMs),
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            fontFamily = FontFamily.Monospace
                                        ),
                                        color = when {
                                            isFastest -> GlowEmerald
                                            isSlowest -> GlowOrange
                                            else -> textPrimary
                                        }
                                    )
                                    Text(
                                        text = "Total %02d:%02d.%02d".format(overallMin, overallSec, overallMs),
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontFamily = FontFamily.Monospace
                                        ),
                                        color = textSecondary
                                    )
                                }
                            }
                        }
                        HorizontalDivider(
                            color = if (isDark) Color(0x1A38BDF8) else Color(0x1F0F172A),
                            thickness = 0.8.dp
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}
