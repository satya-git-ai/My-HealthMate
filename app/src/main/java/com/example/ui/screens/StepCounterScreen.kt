package com.example.ui.screens

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DirectionsWalk
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.sensor.SensorTypeUsed
import com.example.ui.components.StreakCelebrationOverlay
import com.example.ui.theme.HealthBlue
import com.example.ui.theme.HealthCyan
import com.example.ui.theme.HealthEmerald
import com.example.ui.theme.HealthIndigo
import com.example.ui.theme.HealthOrange
import com.example.ui.theme.HealthRose
import com.example.ui.viewmodel.HealthViewModel
import java.util.Calendar

val StepsFireOrange = Color(0xFFFF6D00)
val StepsPrimaryGreen = Color(0xFF059669)

@Composable
fun StepCounterScreen(
    viewModel: HealthViewModel,
    onBack: () -> Unit,
    onHome: () -> Unit = onBack,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val liveSteps by viewModel.liveTodaySteps.collectAsState()
    val todayRecord by viewModel.todayRecord.collectAsState()
    val userSettings by viewModel.userSettings.collectAsState()
    val sensorState by viewModel.stepSensorState.collectAsState()

    var selectedChartTimeframe by remember { mutableStateOf("Day") }
    var showCustomStepDialog by remember { mutableStateOf(false) }
    var showResetConfirmDialog by remember { mutableStateOf(false) }

    val lifecycleOwner = LocalLifecycleOwner.current

    fun checkActivityPermissionGranted(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACTIVITY_RECOGNITION
            ) == PackageManager.PERMISSION_GRANTED
        } else true
    }

    var hasActivityPermission by remember {
        mutableStateOf(checkActivityPermissionGranted())
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                val granted = checkActivityPermissionGranted()
                hasActivityPermission = granted
                if (!granted) {
                    viewModel.toggleStepTracking(false)
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    fun openAppInfo() {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.fromParts("package", context.packageName, null)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasActivityPermission = granted
        if (granted) {
            viewModel.toggleStepTracking(true)
            Toast.makeText(context, "Step sensor permission granted", Toast.LENGTH_SHORT).show()
        } else {
            viewModel.toggleStepTracking(false)
        }
    }

    val totalSteps = liveSteps ?: (todayRecord?.steps ?: 0)
    val stepGoal = userSettings.stepGoal
    val remainingSteps = maxOf(0, stepGoal - totalSteps)
    val distanceKm = (todayRecord?.distanceMeters ?: (totalSteps * 0.762f)) / 1000f
    val caloriesBurned = (totalSteps * 0.04f).toInt()
    val activeMinutes = maxOf(1, totalSteps / 100)
    val isStreakAchieved = totalSteps >= stepGoal && stepGoal > 0
    var showStreakCelebration by remember { mutableStateOf(false) }

    // Trigger celebration when streak is newly achieved
    androidx.compose.runtime.LaunchedEffect(isStreakAchieved) {
        if (isStreakAchieved && totalSteps > 0) {
            showStreakCelebration = true
        }
    }

    // Clean, crisp Light Theme Palette by default
    val backgroundColor = MaterialTheme.colorScheme.background
    val cardBackground = MaterialTheme.colorScheme.surface
    val cardBorder = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f)
    val textPrimary = MaterialTheme.colorScheme.onBackground
    val textSecondary = MaterialTheme.colorScheme.onSurfaceVariant
    val accentColor = if (isStreakAchieved) StepsFireOrange else StepsPrimaryGreen

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(backgroundColor)
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 18.dp)
            .testTag("step_counter_screen")
    ) {
        Spacer(modifier = Modifier.height(14.dp))

        // Steps Header Bar (Modified to 'Steps', light theme, no color switcher)
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier.testTag("step_counter_back_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = textPrimary
                    )
                }

                Spacer(modifier = Modifier.width(6.dp))

                Column {
                    Text(
                        text = "Steps",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 0.3.sp
                        ),
                        color = textPrimary
                    )
                    Text(
                        text = "Real-time Pedometer & Activity Ring",
                        style = MaterialTheme.typography.labelSmall,
                        color = textSecondary
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = { showResetConfirmDialog = true },
                    modifier = Modifier
                        .size(36.dp)
                        .testTag("step_counter_reset_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Reset steps to 0",
                        tint = textSecondary,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Spacer(modifier = Modifier.width(4.dp))

                IconButton(
                    onClick = onHome,
                    modifier = Modifier
                        .size(36.dp)
                        .testTag("step_counter_home_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Home,
                        contentDescription = "Home",
                        tint = textPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Activity Permission Warning Banner (Android 10+) - Transparent Glossy
        if (!hasActivityPermission && Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0x1F00E676)),
                border = androidx.compose.foundation.BorderStroke(
                    1.5.dp,
                    androidx.compose.ui.graphics.Brush.verticalGradient(
                        listOf(
                            Color(0x8000E676),
                            Color(0x3000E5FF),
                            Color(0x15FFFFFF)
                        )
                    )
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("glossy_activity_permission_card")
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color(0x3300E676))
                        ) {
                            Icon(
                                imageVector = Icons.Default.DirectionsWalk,
                                contentDescription = "Pedometer Sensor",
                                tint = Color(0xFF00E676),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Physical Activity Access",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                                color = textPrimary
                            )
                            Text(
                                text = "Hardware step sensor & live cadence",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFF00E676)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "Android requires Activity Recognition permission to connect directly to your device's built-in step sensor and count daily steps in the background with zero battery drain.",
                        style = MaterialTheme.typography.bodySmall,
                        color = textSecondary,
                        lineHeight = 18.sp
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = { permissionLauncher.launch(Manifest.permission.ACTIVITY_RECOGNITION) },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E676)),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(46.dp)
                                .testTag("grant_activity_permission_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.DirectionsWalk,
                                contentDescription = null,
                                tint = Color(0xFF07111E),
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Prompt Sensor",
                                color = Color(0xFF07111E),
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 13.sp
                            )
                        }

                        OutlinedButton(
                            onClick = { openAppInfo() },
                            shape = RoundedCornerShape(14.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x6000E676)),
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = Color(0x1000E676)
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .height(46.dp)
                                .testTag("open_activity_app_info_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = null,
                                tint = Color(0xFF00E676),
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "App Info",
                                color = Color(0xFF00E676),
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(14.dp))
        }

        // Hero Circular Progress Ring Card (Light theme)
        Card(
            shape = RoundedCornerShape(26.dp),
            colors = CardDefaults.cardColors(containerColor = cardBackground),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, cardBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 24.dp, horizontal = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                StepsRing(
                    current = totalSteps,
                    goal = stepGoal,
                    accentColor = accentColor,
                    size = 230.dp,
                    strokeWidth = 18.dp,
                    isStreakAchieved = isStreakAchieved,
                    textPrimary = textPrimary,
                    textSecondary = textSecondary
                )

                Spacer(modifier = Modifier.height(18.dp))

                // Streak Achieved Announcement or Progress Banner
                if (isStreakAchieved) {
                    Surface(
                        color = Color(0xFFFFF7ED),
                        shape = RoundedCornerShape(14.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, StepsFireOrange.copy(alpha = 0.5f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.LocalFireDepartment,
                                contentDescription = null,
                                tint = StepsFireOrange,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Streak achieved 🔥",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.ExtraBold,
                                    letterSpacing = 0.5.sp
                                ),
                                color = StepsFireOrange
                            )
                        }
                    }
                } else {
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.LocalFireDepartment,
                                    contentDescription = null,
                                    tint = StepsFireOrange,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "${String.format("%,d", stepGoal)} Streak Goal",
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                    color = textPrimary
                                )
                            }
                            if (totalSteps > 0 && remainingSteps > 0) {
                                Text(
                                    text = "%,d to streak 🔥".format(remainingSteps),
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                                    color = StepsFireOrange
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Live Sensor Status Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .background(if (sensorState.isTracking) HealthEmerald else Color.Gray, CircleShape)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = when (sensorState.sensorTypeUsed) {
                                SensorTypeUsed.HARDWARE_STEP_COUNTER -> "Hardware Step Counter Active"
                                SensorTypeUsed.HARDWARE_STEP_DETECTOR -> "Hardware Step Detector Active"
                                SensorTypeUsed.SIMULATOR_TEST -> "Walk Simulator Active"
                                else -> "Hardware Pedometer Active"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = textSecondary
                        )
                    }

                    if (sensorState.currentSpm > 0) {
                        Text(
                            text = "${sensorState.currentSpm} SPM",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = accentColor
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 4-Stat Metric Row (Distance, Calories, Active Time, Cadence)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            StepsMetricBox(
                title = "Distance",
                value = "%.2f".format(distanceKm),
                unit = "km",
                icon = Icons.Default.LocationOn,
                accentColor = HealthBlue,
                cardBackground = cardBackground,
                cardBorder = cardBorder,
                textPrimary = textPrimary,
                textSecondary = textSecondary,
                modifier = Modifier.weight(1f)
            )

            StepsMetricBox(
                title = "Calories",
                value = "%,d".format(caloriesBurned),
                unit = "kcal",
                icon = Icons.Default.LocalFireDepartment,
                accentColor = StepsFireOrange,
                cardBackground = cardBackground,
                cardBorder = cardBorder,
                textPrimary = textPrimary,
                textSecondary = textSecondary,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            StepsMetricBox(
                title = "Active Time",
                value = "$activeMinutes",
                unit = "min",
                icon = Icons.Default.Timer,
                accentColor = HealthEmerald,
                cardBackground = cardBackground,
                cardBorder = cardBorder,
                textPrimary = textPrimary,
                textSecondary = textSecondary,
                modifier = Modifier.weight(1f)
            )

            StepsMetricBox(
                title = "Cadence",
                value = if (sensorState.currentSpm > 0) "${sensorState.currentSpm}" else "--",
                unit = "spm",
                icon = Icons.Default.Speed,
                accentColor = HealthIndigo,
                cardBackground = cardBackground,
                cardBorder = cardBorder,
                textPrimary = textPrimary,
                textSecondary = textSecondary,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(18.dp))

        // 24-Hour Activity Bar Chart Card (Light theme)
        Card(
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(containerColor = cardBackground),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, cardBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Activity Timeline",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = textPrimary
                        )
                        Text(
                            text = "Hourly step distribution today",
                            style = MaterialTheme.typography.bodySmall,
                            color = textSecondary
                        )
                    }

                    // Day / Week / Month selector
                    Row(
                        modifier = Modifier
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f), RoundedCornerShape(10.dp))
                            .padding(2.dp)
                    ) {
                        listOf("Day", "Week", "Month").forEach { tf ->
                            val isSelected = selectedChartTimeframe == tf
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isSelected) cardBackground else Color.Transparent)
                                    .clickable { selectedChartTimeframe = tf }
                                    .padding(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = tf,
                                    fontSize = 11.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) accentColor else textSecondary
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // 24-Hour Bar Chart Visualization
                StepsHourlyChart(
                    totalSteps = totalSteps,
                    accentColor = accentColor,
                    barTrackColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    labelColor = textSecondary
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 7-Day Streak Habit Tracker Card (Light theme)
        Card(
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(containerColor = cardBackground),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, cardBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.LocalFireDepartment,
                            contentDescription = null,
                            tint = StepsFireOrange,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Weekly Streak Tracker",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = textPrimary
                        )
                    }

                    Text(
                        text = if (isStreakAchieved) "Streak active 🔥" else "${String.format("%,d", stepGoal)} steps = 🔥",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = StepsFireOrange
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // 7 Days Circles
                val daysOfWeek = listOf("M", "T", "W", "T", "F", "S", "S")
                val todayDayIndex = (Calendar.getInstance().get(Calendar.DAY_OF_WEEK) + 5) % 7 // Monday = 0

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    daysOfWeek.forEachIndexed { index, dayInitial ->
                        val isToday = index == todayDayIndex
                        val isPastStreak = isToday && isStreakAchieved

                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = dayInitial,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isToday) textPrimary else textSecondary
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(
                                        when {
                                            isPastStreak -> StepsFireOrange
                                            isToday -> accentColor.copy(alpha = 0.15f)
                                            else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                        }
                                    )
                                    .border(
                                        width = if (isToday) 2.dp else 1.dp,
                                        color = when {
                                            isPastStreak -> StepsFireOrange
                                            isToday -> accentColor
                                            else -> cardBorder
                                        },
                                        shape = CircleShape
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                if (isPastStreak) {
                                    Text("🔥", fontSize = 16.sp)
                                } else if (isToday) {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .background(accentColor, CircleShape)
                                    )
                                } else {
                                    Text(
                                        text = "${index + 10}",
                                        fontSize = 11.sp,
                                        color = textSecondary
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Background Tracking Toggle Button
        Button(
            onClick = {
                viewModel.toggleStepTracking(!sensorState.isTracking)
            },
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = if (sensorState.isTracking) MaterialTheme.colorScheme.surfaceVariant else accentColor
            ),
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
                .testTag("step_sensor_tracking_toggle_button")
        ) {
            Icon(
                imageVector = if (sensorState.isTracking) Icons.Default.Pause else Icons.Default.PlayArrow,
                contentDescription = null,
                tint = if (sensorState.isTracking) textPrimary else Color.White,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = if (sensorState.isTracking) "Pause Background Pedometer" else "Resume Background Pedometer",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = if (sensorState.isTracking) textPrimary else Color.White
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Quick Step Adjustment Card
        Card(
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(containerColor = cardBackground),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, cardBorder),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("custom_steps_management_section")
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Quick Step Adjustment",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = textPrimary
                        )
                        Text(
                            text = "Add or adjust steps to today's record",
                            style = MaterialTheme.typography.bodySmall,
                            color = textSecondary
                        )
                    }

                    OutlinedButton(
                        onClick = { showCustomStepDialog = true },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.testTag("custom_step_dialog_button")
                    ) {
                        Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(14.dp), tint = accentColor)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Custom", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = accentColor)
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Quick Add Steps Row
                Text(
                    text = "Quick Add (+)",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = HealthEmerald
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(50, 100, 500, 1000).forEach { amt ->
                        Button(
                            onClick = {
                                viewModel.addManualStepCount(amt)
                                Toast.makeText(context, "+$amt steps added", Toast.LENGTH_SHORT).show()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = HealthEmerald.copy(alpha = 0.12f)),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(38.dp)
                                .testTag("btn_quick_add_$amt")
                        ) {
                            Text(
                                text = "+$amt",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                color = HealthEmerald
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Milestone test button to instantly test streak
                val isAchieveDisabled = isStreakAchieved || totalSteps >= stepGoal
                Button(
                    onClick = {
                        if (!isAchieveDisabled) {
                            val toAdd = maxOf(1, stepGoal - totalSteps)
                            viewModel.addManualStepCount(toAdd)
                            Toast.makeText(context, "Streak achieved 🔥", Toast.LENGTH_SHORT).show()
                        }
                    },
                    enabled = !isAchieveDisabled,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = StepsFireOrange.copy(alpha = 0.15f),
                        contentColor = StepsFireOrange,
                        disabledContainerColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f),
                        disabledContentColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                    ),
                    shape = RoundedCornerShape(10.dp),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        if (isAchieveDisabled) MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                        else StepsFireOrange.copy(alpha = 0.4f)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(38.dp)
                        .testTag("btn_achieve_step_streak")
                ) {
                    Text(
                        text = if (isAchieveDisabled) {
                            "Streak Goal Achieved 🔥 (Completed)"
                        } else {
                            "Achieve ${String.format("%,d", stepGoal)} Step Streak 🔥"
                        },
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = if (isAchieveDisabled) {
                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                        } else {
                            StepsFireOrange
                        }
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Quick Remove Steps Row
                Text(
                    text = "Quick Remove (-)",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = HealthRose
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(50, 100, 500, 1000).forEach { amt ->
                        Button(
                            onClick = {
                                viewModel.removeManualStepCount(amt)
                                Toast.makeText(context, "-$amt steps removed", Toast.LENGTH_SHORT).show()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = HealthRose.copy(alpha = 0.12f)),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(38.dp)
                                .testTag("btn_quick_remove_$amt")
                        ) {
                            Text(
                                text = "-$amt",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                color = HealthRose
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))
    }

    // Custom Add / Remove Steps Dialog
    if (showCustomStepDialog) {
        var isAddMode by remember { mutableStateOf(true) }
        var stepInputText by remember { mutableStateOf("") }
        var errorMessage by remember { mutableStateOf<String?>(null) }

        AlertDialog(
            onDismissRequest = { showCustomStepDialog = false },
            title = {
                Text(
                    text = if (isAddMode) "Add Custom Steps" else "Remove Custom Steps",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .padding(2.dp)
                    ) {
                        Surface(
                            color = if (isAddMode) HealthEmerald else Color.Transparent,
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .weight(1f)
                                .clickable { isAddMode = true }
                        ) {
                            Text(
                                text = "Add (+)",
                                textAlign = TextAlign.Center,
                                fontWeight = FontWeight.Bold,
                                color = if (isAddMode) Color.White else textSecondary,
                                modifier = Modifier.padding(vertical = 8.dp)
                            )
                        }

                        Surface(
                            color = if (!isAddMode) HealthRose else Color.Transparent,
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .weight(1f)
                                .clickable { isAddMode = false }
                        ) {
                            Text(
                                text = "Remove (-)",
                                textAlign = TextAlign.Center,
                                fontWeight = FontWeight.Bold,
                                color = if (!isAddMode) Color.White else textSecondary,
                                modifier = Modifier.padding(vertical = 8.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    OutlinedTextField(
                        value = stepInputText,
                        onValueChange = {
                            stepInputText = it.filter { ch -> ch.isDigit() }
                            errorMessage = null
                        },
                        label = { Text("Number of steps") },
                        placeholder = { Text("e.g., 250") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        isError = errorMessage != null,
                        supportingText = errorMessage?.let { { Text(it, color = HealthRose) } },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("custom_step_input_field")
                    )

                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Current steps: %,d".format(totalSteps),
                        style = MaterialTheme.typography.bodySmall,
                        color = textSecondary
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val count = stepInputText.toIntOrNull()
                        if (count == null || count <= 0) {
                            errorMessage = "Please enter a valid step count (> 0)"
                            return@Button
                        }
                        if (count > 50000) {
                            errorMessage = "Max 50,000 steps per adjustment"
                            return@Button
                        }
                        if (isAddMode) {
                            viewModel.addManualStepCount(count)
                            Toast.makeText(context, "+%,d steps added".format(count), Toast.LENGTH_SHORT).show()
                        } else {
                            viewModel.removeManualStepCount(count)
                            Toast.makeText(context, "-%,d steps removed".format(count), Toast.LENGTH_SHORT).show()
                        }
                        showCustomStepDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isAddMode) HealthEmerald else HealthRose
                    ),
                    modifier = Modifier.testTag("custom_step_apply_button")
                ) {
                    Text(if (isAddMode) "Add Steps" else "Remove Steps")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCustomStepDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Reset Confirm Dialog
    if (showResetConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showResetConfirmDialog = false },
            title = { Text("Reset Today's Steps?") },
            text = { Text("This will reset today's step count, distance, and calories back to zero.") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.resetTodaySteps()
                        showResetConfirmDialog = false
                        Toast.makeText(context, "Today's steps reset to 0", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = HealthRose),
                    modifier = Modifier.testTag("confirm_reset_steps_button")
                ) {
                    Text("Reset to 0")
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetConfirmDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Temporary streak celebration animation overlay (only if streak achieved)
    if (showStreakCelebration && isStreakAchieved) {
        StreakCelebrationOverlay(
            stepGoal = stepGoal,
            onDismiss = { showStreakCelebration = false }
        )
    }
}

// Steps Circular Progress Ring in Light Theme
@Composable
fun StepsRing(
    current: Int,
    goal: Int,
    accentColor: Color,
    modifier: Modifier = Modifier,
    size: Dp = 230.dp,
    strokeWidth: Dp = 18.dp,
    isStreakAchieved: Boolean = false,
    textPrimary: Color = Color(0xFF0F172A),
    textSecondary: Color = Color(0xFF64748B)
) {
    val progressRatio = if (goal > 0) (current.toFloat() / goal.toFloat()).coerceIn(0f, 1.5f) else 0f
    val animatedProgress by animateFloatAsState(
        targetValue = progressRatio,
        animationSpec = tween(durationMillis = 1000, easing = FastOutSlowInEasing),
        label = "stepsRingProgress"
    )

    val percentage = (progressRatio * 100).toInt()
    val trackColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(size)
            .testTag("circular_goal_progress")
    ) {
        Canvas(modifier = Modifier.size(size)) {
            val strokePx = strokeWidth.toPx()
            val arcSize = Size(this.size.width - strokePx, this.size.height - strokePx)
            val topLeft = Offset(strokePx / 2f, strokePx / 2f)

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

            // Progress Arc
            val sweep = 270f * animatedProgress.coerceAtMost(1f)
            if (sweep > 0f) {
                val gradientColors = if (isStreakAchieved) {
                    listOf(Color(0xFFFF9100), Color(0xFFFF3D00))
                } else {
                    listOf(accentColor, HealthCyan)
                }
                drawArc(
                    brush = Brush.linearGradient(colors = gradientColors),
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
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(horizontal = 24.dp)
        ) {
            Icon(
                imageVector = if (isStreakAchieved) Icons.Default.LocalFireDepartment else Icons.Default.DirectionsWalk,
                contentDescription = null,
                tint = if (isStreakAchieved) StepsFireOrange else accentColor,
                modifier = Modifier.size(26.dp)
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "%,d".format(current),
                fontSize = 38.sp,
                fontWeight = FontWeight.ExtraBold,
                color = textPrimary,
                letterSpacing = (-0.5).sp
            )

            Text(
                text = "STEPS",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = textSecondary,
                letterSpacing = 2.sp
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = if (isStreakAchieved) "$percentage% of ${"%,d".format(goal)}" else "$percentage% of ${"%,d".format(goal)}",
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = accentColor
            )
        }
    }
}

// 4-Stat Metric Box
@Composable
private fun StepsMetricBox(
    title: String,
    value: String,
    unit: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    accentColor: Color,
    cardBackground: Color,
    cardBorder: Color,
    textPrimary: Color,
    textSecondary: Color,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = cardBackground),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, cardBorder),
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = title,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = textSecondary
                )
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .background(accentColor.copy(alpha = 0.12f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = value,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = textPrimary
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = unit,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = textSecondary,
                    modifier = Modifier.padding(bottom = 2.dp)
                )
            }
        }
    }
}

// 24-Hour Bar Chart Composable in Light Theme
@Composable
private fun StepsHourlyChart(
    totalSteps: Int,
    accentColor: Color,
    barTrackColor: Color,
    labelColor: Color
) {
    val currentHour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)

    val hourlyWeights = listOf(
        0.01f, 0.00f, 0.00f, 0.00f, 0.00f, 0.02f, // 0-5
        0.05f, 0.08f, 0.12f, 0.09f, 0.07f, 0.08f, // 6-11
        0.11f, 0.08f, 0.06f, 0.07f, 0.09f, 0.10f, // 12-17
        0.12f, 0.08f, 0.05f, 0.03f, 0.02f, 0.01f  // 18-23
    )

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(110.dp),
            horizontalArrangement = Arrangement.spacedBy(3.dp),
            verticalAlignment = Alignment.Bottom
        ) {
            for (hour in 0..23) {
                val isPastOrCurrent = hour <= currentHour
                val stepFrac = if (isPastOrCurrent && totalSteps > 0) hourlyWeights[hour] else 0f
                val barHeightFrac = if (totalSteps > 0) (stepFrac / 0.15f).coerceIn(0.08f, 1f) else 0.05f
                val isCurrent = hour == currentHour

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .height(110.dp),
                    verticalArrangement = Arrangement.Bottom,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height((barHeightFrac * 100).dp)
                            .clip(RoundedCornerShape(topStart = 3.dp, topEnd = 3.dp))
                            .background(
                                when {
                                    isCurrent -> accentColor
                                    isPastOrCurrent && totalSteps > 0 -> accentColor.copy(alpha = 0.65f)
                                    else -> barTrackColor
                                }
                            )
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Time Axis Labels
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            listOf("00:00", "04:00", "08:00", "12:00", "16:00", "20:00", "24:00").forEach { timeLabel ->
                Text(
                    text = timeLabel,
                    fontSize = 10.sp,
                    color = labelColor
                )
            }
        }
    }
}
