package com.example.ui.screens

import android.widget.Toast
import kotlin.math.roundToInt
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.DirectionsRun
import androidx.compose.material.icons.filled.DirectionsWalk
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Medication
import androidx.compose.material.icons.filled.MonitorWeight
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Scale
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.notification.SleepReminderScheduler
import com.example.notification.SleepSoundType
import com.example.ui.components.CircularGoalProgress
import com.example.ui.components.GlassCard
import com.example.ui.components.GlowPill
import com.example.ui.components.MetricCard
import com.example.ui.components.SleepAlertDialog
import com.example.ui.components.SleepReminderDialog
import com.example.ui.theme.GlowBlue
import com.example.ui.theme.GlowCyan
import com.example.ui.theme.GlowEmerald
import com.example.ui.theme.GlowOrange
import com.example.ui.theme.GlowPurple
import com.example.ui.theme.HealthBlue
import com.example.ui.theme.HealthCyan
import com.example.ui.theme.HealthEmerald
import com.example.ui.theme.HealthOrange
import com.example.ui.theme.HealthRose
import com.example.ui.theme.isAppInDarkTheme
import com.example.ui.viewmodel.HealthViewModel
import java.util.Calendar

@Composable
fun HomeScreen(
    viewModel: HealthViewModel,
    onNavigate: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val isDark = isAppInDarkTheme()
    val todayRecord by viewModel.todayRecord.collectAsState()
    val liveSteps by viewModel.liveTodaySteps.collectAsState()
    val userSettings by viewModel.userSettings.collectAsState()
    val activeSleepAlert by viewModel.activeSleepAlert.collectAsState()
    val workoutLiveState by viewModel.workoutLiveState.collectAsState()

    var showCreatorDialog by remember { mutableStateOf(false) }
    var showSleepDialog by remember { mutableStateOf(false) }

    val currentSteps = liveSteps ?: ((todayRecord?.steps ?: 0) + (if (workoutLiveState.isTracking) workoutLiveState.estimatedSteps else 0))
    val stepGoal = userSettings.stepGoal
    val currentWater = todayRecord?.waterMl ?: 0
    val waterGoal = userSettings.waterGoalMl
    val calories = (todayRecord?.caloriesBurned ?: 0) + (if (workoutLiveState.isTracking) workoutLiveState.caloriesBurned else 0)
    val distanceKm = ((todayRecord?.distanceMeters ?: 0f) + (if (workoutLiveState.isTracking) workoutLiveState.distanceMeters else 0f)) / 1000f

    val isStepStreakAchieved = currentSteps >= stepGoal && stepGoal > 0
    val isWaterTargetAchieved = currentWater >= waterGoal && waterGoal > 0

    val greeting = rememberGreeting()
    val motivationalMessage = when {
        isStepStreakAchieved -> "Streak achieved 🔥 • %,d kcal burned".format(calories)
        currentSteps >= stepGoal -> "🎉 Goal crushed! Incredible achievement today!"
        currentSteps >= (stepGoal * 0.75f) -> "You're almost at your daily goal! Push for the finish!"
        currentSteps >= (stepGoal * 0.5f) -> "Great job! Keep moving! You're well over halfway."
        currentSteps >= (stepGoal * 0.25f) -> "Keep the momentum going! Great effort."
        else -> "Every step counts! Start your daily journey."
    }

    val backgroundBrush = if (isDark) {
        Brush.verticalGradient(
            listOf(
                Color(0xFF070B14),
                Color(0xFF0D1627),
                Color(0xFF060911)
            )
        )
    } else {
        Brush.verticalGradient(
            listOf(
                Color(0xFFF4F7FC),
                Color(0xFFEBF1FA),
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
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
            .testTag("home_screen")
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        // Top Header Bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = greeting,
                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.ExtraBold),
                    color = textPrimary
                )
                Text(
                    text = "My HealthMate Dashboard",
                    style = MaterialTheme.typography.bodySmall,
                    color = textSecondary
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                // Theme Toggle Button (Light / Dark mode)
                IconButton(
                    onClick = {
                        val newMode = !userSettings.isDarkMode
                        viewModel.setDarkMode(newMode)
                        Toast.makeText(
                            context,
                            if (newMode) "Dark mode enabled 🌙" else "Light mode enabled ☀️",
                            Toast.LENGTH_SHORT
                        ).show()
                    },
                    modifier = Modifier.testTag("home_theme_toggle_button")
                ) {
                    Icon(
                        imageVector = if (userSettings.isDarkMode) Icons.Default.WbSunny else Icons.Default.DarkMode,
                        contentDescription = if (userSettings.isDarkMode) "Switch to Light Mode" else "Switch to Dark Mode",
                        tint = if (userSettings.isDarkMode) GlowOrange else HealthBlue
                    )
                }

                IconButton(
                    onClick = { onNavigate("statistics") },
                    modifier = Modifier.testTag("home_stats_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.BarChart,
                        contentDescription = "Statistics",
                        tint = textPrimary
                    )
                }

                IconButton(
                    onClick = { onNavigate("history") },
                    modifier = Modifier.testTag("home_history_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.History,
                        contentDescription = "History",
                        tint = textPrimary
                    )
                }

                IconButton(
                    onClick = { onNavigate("settings") },
                    modifier = Modifier.testTag("home_settings_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "Settings",
                        tint = textPrimary
                    )
                }

                // Application info icon
                IconButton(
                    onClick = { showCreatorDialog = true },
                    modifier = Modifier.testTag("home_creator_indicator_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = "Application Info",
                        tint = GlowCyan
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // My HealthMate Rich Colorful Glowing Banner
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(
                    if (isDark) {
                        Brush.linearGradient(
                            colors = listOf(
                                Color(0xFF0F2038),
                                Color(0xFF132A4A),
                                Color(0xFF231B42),
                                Color(0xFF2A1832)
                            )
                        )
                    } else {
                        Brush.linearGradient(
                            colors = listOf(
                                Color(0xFF0284C7),
                                Color(0xFF0D9488),
                                Color(0xFF6366F1),
                                Color(0xFF8B5CF6)
                            )
                        )
                    }
                )
                .border(
                    width = 1.5.dp,
                    brush = Brush.horizontalGradient(
                        colors = listOf(
                            GlowCyan,
                            GlowEmerald,
                            GlowOrange,
                            GlowPurple
                        )
                    ),
                    shape = RoundedCornerShape(24.dp)
                )
                .testTag("healthmate_hero_banner")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(
                                    Brush.linearGradient(
                                        colors = listOf(
                                            Color(0xFF00E5FF),
                                            Color(0xFF00E676),
                                            Color(0xFFFF9100)
                                        )
                                    )
                                )
                                .border(1.dp, Color.White.copy(alpha = 0.8f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = Color.Black,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "MY HEALTHMATE",
                                    style = MaterialTheme.typography.titleSmall.copy(
                                        fontWeight = FontWeight.Black,
                                        letterSpacing = 1.5.sp
                                    ),
                                    color = if (isDark) GlowCyan else Color.White
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(
                                            Brush.horizontalGradient(
                                                colors = listOf(
                                                    Color(0xFFFF9100),
                                                    Color(0xFFFF0055)
                                                )
                                            )
                                        )
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = "PRO SUITE",
                                        fontSize = 8.sp,
                                        fontWeight = FontWeight.Black,
                                        color = Color.White
                                    )
                                }
                            }
                            Text(
                                text = "All-In-One Health & Vitality Hub",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
                                color = if (isDark) Color(0xFFCBD5E1) else Color(0xFFF1F5F9),
                                fontSize = 11.sp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Colorful quick pillar chips
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Steps Chip
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isDark) Color(0x3300E5FF) else Color(0x33FFFFFF))
                            .border(0.8.dp, GlowCyan.copy(alpha = 0.6f), RoundedCornerShape(12.dp))
                            .padding(horizontal = 7.dp, vertical = 4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.DirectionsRun,
                            contentDescription = null,
                            tint = if (isDark) GlowCyan else Color(0xFFE0F7FA),
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            text = "Steps",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isDark) GlowCyan else Color.White
                        )
                    }

                    // Water Chip
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isDark) Color(0x3300E676) else Color(0x33FFFFFF))
                            .border(0.8.dp, GlowEmerald.copy(alpha = 0.6f), RoundedCornerShape(12.dp))
                            .padding(horizontal = 7.dp, vertical = 4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.WaterDrop,
                            contentDescription = null,
                            tint = if (isDark) GlowEmerald else Color(0xFFE8F5E9),
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            text = "Water",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isDark) GlowEmerald else Color.White
                        )
                    }

                    // Stop Watch Chip
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isDark) Color(0x33FF9100) else Color(0x33FFFFFF))
                            .border(0.8.dp, GlowOrange.copy(alpha = 0.6f), RoundedCornerShape(12.dp))
                            .padding(horizontal = 7.dp, vertical = 4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Timer,
                            contentDescription = null,
                            tint = if (isDark) GlowOrange else Color(0xFFFFF3E0),
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            text = "Stop Watch",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isDark) GlowOrange else Color.White
                        )
                    }

                    // Sleep Chip
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isDark) Color(0x33B388FF) else Color(0x33FFFFFF))
                            .border(0.8.dp, GlowPurple.copy(alpha = 0.6f), RoundedCornerShape(12.dp))
                            .padding(horizontal = 7.dp, vertical = 4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Bedtime,
                            contentDescription = null,
                            tint = if (isDark) GlowPurple else Color(0xFFEDE7F6),
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            text = "Sleep",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isDark) GlowPurple else Color.White
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Hero Progress Ring Glass Card
        GlassCard(
            glowColor = if (isStepStreakAchieved) GlowOrange.copy(alpha = 0.35f) else GlowEmerald.copy(alpha = 0.3f),
            glassAlpha = 0.85f,
            shape = RoundedCornerShape(26.dp),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("home_goal_card"),
            onClick = { onNavigate("step_counter") }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                CircularGoalProgress(
                    current = currentSteps,
                    goal = stepGoal,
                    size = 195.dp,
                    strokeWidth = 16.dp,
                    primaryColor = GlowEmerald,
                    secondaryColor = GlowCyan
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = motivationalMessage,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.SemiBold
                    ),
                    color = if (isStepStreakAchieved) GlowOrange else if (isDark) GlowEmerald else HealthEmerald,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )

                Spacer(modifier = Modifier.height(10.dp))
                GlowPill(
                    glowColor = if (isStepStreakAchieved) GlowOrange else GlowEmerald,
                    backgroundColor = if (isStepStreakAchieved) GlowOrange.copy(alpha = 0.15f) else if (isDark) Color(0x2200FF9D) else HealthEmerald.copy(alpha = 0.12f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.LocalFireDepartment,
                            contentDescription = null,
                            tint = if (isStepStreakAchieved) GlowOrange else HealthEmerald,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isStepStreakAchieved) {
                                "Streak achieved 🔥 • %,d kcal burned".format(calories)
                            } else {
                                "${String.format("%,d", stepGoal)} Streak Goal • %,d to streak 🔥".format(maxOf(0, stepGoal - currentSteps))
                            },
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.ExtraBold),
                            color = if (isStepStreakAchieved) GlowOrange else if (isDark) GlowEmerald else HealthEmerald
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Quick Actions Row (Walk, Hydration +250ml, Stopwatch, Sleep Alarm)
        Text(
            text = "Quick Actions",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = textPrimary
        )

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Start Walking Action
            Button(
                onClick = { onNavigate("step_counter") },
                shape = RoundedCornerShape(16.dp),
                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isDark) Color(0x3300FF9D) else HealthEmerald
                ),
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
                    .border(
                        1.dp,
                        if (isDark) GlowEmerald.copy(alpha = 0.6f) else Color.Transparent,
                        RoundedCornerShape(16.dp)
                    )
                    .testTag("quick_action_start_walking")
            ) {
                Icon(
                    imageVector = Icons.Default.DirectionsWalk,
                    contentDescription = null,
                    tint = if (isDark) GlowEmerald else Color.White,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "Walk",
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold, fontSize = 12.sp),
                    color = if (isDark) GlowEmerald else Color.White
                )
            }

            // Add Water Action
            Button(
                onClick = {
                    viewModel.addWater(250)
                    if (currentWater + 250 >= 2500) {
                        Toast.makeText(context, "Target achieved! 2,500 ml reached", Toast.LENGTH_SHORT).show()
                    }
                },
                shape = RoundedCornerShape(16.dp),
                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isDark) Color(0x3300F0FF) else HealthBlue
                ),
                modifier = Modifier
                    .weight(1.1f)
                    .height(48.dp)
                    .border(
                        1.dp,
                        if (isDark) GlowCyan.copy(alpha = 0.6f) else Color.Transparent,
                        RoundedCornerShape(16.dp)
                    )
                    .testTag("quick_action_add_water")
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = null,
                    tint = if (isDark) GlowCyan else Color.White,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(2.dp))
                Text(
                    text = "+250ml",
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold, fontSize = 12.sp),
                    color = if (isDark) GlowCyan else Color.White
                )
            }

            // Medicine Quick Action
            Button(
                onClick = { onNavigate("med_reminder") },
                shape = RoundedCornerShape(16.dp),
                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isDark) Color(0x33F43F5E) else HealthRose
                ),
                modifier = Modifier
                    .weight(1.15f)
                    .height(48.dp)
                    .border(
                        1.dp,
                        if (isDark) HealthRose.copy(alpha = 0.6f) else Color.Transparent,
                        RoundedCornerShape(16.dp)
                    )
                    .testTag("quick_action_medicine")
            ) {
                Icon(
                    imageVector = Icons.Default.Medication,
                    contentDescription = null,
                    tint = if (isDark) HealthRose else Color.White,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(3.dp))
                Text(
                    text = "Medicine",
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold, fontSize = 12.sp),
                    color = if (isDark) HealthRose else Color.White
                )
            }

            // Sleep Alarm Quick Action
            Button(
                onClick = { showSleepDialog = true },
                shape = RoundedCornerShape(16.dp),
                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isDark) Color(0x33818CF8) else Color(0xFF5C6BC0)
                ),
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
                    .border(
                        1.dp,
                        if (isDark) GlowPurple.copy(alpha = 0.6f) else Color.Transparent,
                        RoundedCornerShape(16.dp)
                    )
                    .testTag("quick_action_sleep_alarm")
            ) {
                Icon(
                    imageVector = Icons.Default.Bedtime,
                    contentDescription = null,
                    tint = if (isDark) Color(0xFFA5B4FC) else Color.White,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(2.dp))
                Text(
                    text = "Sleep",
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold, fontSize = 12.sp),
                    color = if (isDark) Color(0xFFA5B4FC) else Color.White
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Dedicated Live Stopwatch Quick Access Glass Card
        GlassCard(
            glowColor = GlowOrange.copy(alpha = 0.25f),
            glassAlpha = 0.82f,
            shape = RoundedCornerShape(22.dp),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("home_stopwatch_tile_card"),
            onClick = { onNavigate("stopwatch") }
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(GlowOrange.copy(alpha = 0.2f))
                            .border(1.2.dp, GlowOrange.copy(alpha = 0.6f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Timer,
                            contentDescription = "Stopwatch",
                            tint = GlowOrange,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Stop Watch",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = textPrimary
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            GlowPill(glowColor = GlowOrange) {
                                Text(
                                    text = "0.01s",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = GlowOrange,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp)
                                )
                            }
                        }
                        Text(
                            text = "Start / Stop • Pause / Continue • Lap splits",
                            style = MaterialTheme.typography.bodySmall,
                            color = textSecondary
                        )
                    }
                }

                Icon(
                    imageVector = Icons.Default.ArrowForward,
                    contentDescription = "Open Stopwatch",
                    tint = GlowOrange,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // GetSteps Weight Loss Walking Calculator Dedicated Card
        GlassCard(
            glowColor = GlowEmerald.copy(alpha = 0.35f),
            glassAlpha = 0.88f,
            shape = RoundedCornerShape(22.dp),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("home_weight_loss_calc_card"),
            onClick = { onNavigate("weight_loss_calculator") }
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.linearGradient(
                                    listOf(
                                        Color(0xFF10B981),
                                        Color(0xFF06B6D4)
                                    )
                                )
                            )
                            .border(1.2.dp, GlowEmerald.copy(alpha = 0.8f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.LocalFireDepartment,
                            contentDescription = "Weight Loss Calculator",
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Weight Loss Calculator",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = textPrimary
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            GlowPill(glowColor = GlowEmerald) {
                                Text(
                                    text = "GetSteps",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = GlowEmerald,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp)
                                )
                            }
                        }
                        Text(
                            text = "Target steps • Calorie deficit • 7,700 kcal / kg rule",
                            style = MaterialTheme.typography.bodySmall,
                            color = textSecondary
                        )
                    }
                }

                Icon(
                    imageVector = Icons.Default.ArrowForward,
                    contentDescription = "Open Calculator",
                    tint = GlowEmerald,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Personalized Biometrics Recommendation Card
        val userRecommendation = remember(userSettings.userWeightKg, userSettings.userHeightCm, userSettings.userAge, userSettings.userGender, userSettings.userPrimaryGoal) {
            com.example.util.HealthCalculations.calculatePersonalizedRecommendation(
                weightKg = userSettings.userWeightKg,
                heightCm = userSettings.userHeightCm,
                age = userSettings.userAge,
                gender = userSettings.userGender,
                primaryGoal = userSettings.userPrimaryGoal
            )
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(
                    if (isDark) {
                        Brush.linearGradient(
                            listOf(
                                Color(0xFF0F2B48).copy(alpha = 0.7f),
                                Color(0xFF0D3B3B).copy(alpha = 0.7f)
                            )
                        )
                    } else {
                        Brush.linearGradient(
                            listOf(
                                Color(0xFFE0F2FE),
                                Color(0xFFE6FFFA)
                            )
                        )
                    }
                )
                .border(1.dp, GlowCyan.copy(alpha = 0.4f), RoundedCornerShape(20.dp))
                .clickable { onNavigate("personalize_plan") }
                .padding(14.dp)
                .testTag("home_biometrics_insight_card")
        ) {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.FitnessCenter,
                            contentDescription = null,
                            tint = GlowCyan,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "CUSTOM PROFILE • ${userSettings.userWeightKg.roundToInt()} KG • ${userSettings.userAge} YRS",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.ExtraBold,
                                letterSpacing = 1.sp
                            ),
                            color = GlowCyan
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(GlowCyan.copy(alpha = 0.15f))
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Customize my profile",
                            tint = GlowCyan,
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Customize my profile",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = GlowCyan
                        )
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = "(Click to modify your data)",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontStyle = FontStyle.Italic,
                        fontWeight = FontWeight.SemiBold
                    ),
                    color = if (isDark) GlowCyan.copy(alpha = 0.85f) else HealthCyan.copy(alpha = 0.95f)
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "BMI: ${userRecommendation.bmi} (${userRecommendation.bmiCategory})",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = Color(userRecommendation.bmiColorHex)
                    )
                    Text(
                        text = "•",
                        style = MaterialTheme.typography.labelSmall,
                        color = textSecondary
                    )
                    Text(
                        text = "Goal: ${String.format("%,d", userSettings.stepGoal)} steps",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = GlowEmerald
                    )
                    Text(
                        text = "•",
                        style = MaterialTheme.typography.labelSmall,
                        color = textSecondary
                    )
                    Text(
                        text = "Water: ${userSettings.waterGoalMl} ml",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = GlowCyan
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Core Metrics Grid
        Text(
            text = "Today's Metrics",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = textPrimary
        )

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            MetricCard(
                title = "Steps",
                value = "%,d".format(currentSteps),
                unit = "/ %,d".format(stepGoal),
                icon = Icons.Default.DirectionsRun,
                iconTint = GlowEmerald,
                iconBgColor = GlowEmerald.copy(alpha = 0.2f),
                badgeText = if (isStepStreakAchieved) "Streak achieved 🔥" else "${String.format("%,d", stepGoal)} Streak Goal",
                badgeColor = if (isStepStreakAchieved) GlowOrange else GlowEmerald,
                modifier = Modifier.weight(1f),
                testTag = "metric_steps_card",
                onClick = { onNavigate("step_counter") }
            )

            MetricCard(
                title = "Hydration",
                value = "%,d".format(currentWater),
                unit = "/ %,d ml".format(waterGoal),
                icon = Icons.Default.WaterDrop,
                iconTint = GlowCyan,
                iconBgColor = GlowCyan.copy(alpha = 0.2f),
                badgeText = if (isWaterTargetAchieved) "Target achieved" else null,
                badgeColor = GlowEmerald,
                modifier = Modifier.weight(1f),
                testTag = "metric_water_card",
                onClick = { onNavigate("water_tracker") }
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            MetricCard(
                title = "Distance",
                value = "%.2f".format(distanceKm),
                unit = "km",
                icon = Icons.Default.LocationOn,
                iconTint = GlowBlue,
                iconBgColor = GlowBlue.copy(alpha = 0.2f),
                modifier = Modifier.weight(1f),
                testTag = "metric_distance_card",
                onClick = { onNavigate("gps_tracker") }
            )

            MetricCard(
                title = "Calories",
                value = "%,d".format(calories),
                unit = "kcal",
                icon = Icons.Default.LocalFireDepartment,
                iconTint = GlowOrange,
                iconBgColor = GlowOrange.copy(alpha = 0.2f),
                badgeText = if (isStepStreakAchieved) "burned" else null,
                badgeColor = GlowOrange,
                modifier = Modifier.weight(1f),
                testTag = "metric_calories_card"
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Sleep & Wake-up Alarm Section (Translucent Glass Card)
        val sleepSound = remember(userSettings.sleepSoundType) {
            SleepSoundType.fromId(userSettings.sleepSoundType)
        }
        val bedtimeDisplay = remember(userSettings.sleepBedtimeHour, userSettings.sleepBedtimeMinute) {
            SleepReminderScheduler.formatTime12Hour(userSettings.sleepBedtimeHour, userSettings.sleepBedtimeMinute)
        }
        val wakeupDisplay = remember(userSettings.sleepWakeupHour, userSettings.sleepWakeupMinute) {
            SleepReminderScheduler.formatTime12Hour(userSettings.sleepWakeupHour, userSettings.sleepWakeupMinute)
        }
        val nextSleepReminder = remember(userSettings) {
            SleepReminderScheduler.getNextReminderDescription(userSettings)
        }

        GlassCard(
            glowColor = if (userSettings.sleepReminderEnabled) GlowPurple.copy(alpha = 0.4f) else Color.Transparent,
            glassAlpha = 0.88f,
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("home_sleep_reminder_section")
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                // Header + Large Toggle Switch
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(
                                    if (userSettings.sleepReminderEnabled) Color(0xFF5C6BC0).copy(alpha = 0.35f)
                                    else if (isDark) Color(0x3338BDF8) else Color(0x1F0F172A)
                                )
                                .border(
                                    1.dp,
                                    if (userSettings.sleepReminderEnabled) Color(0xFF9FA8DA) else Color.Transparent,
                                    CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Bedtime,
                                contentDescription = null,
                                tint = if (userSettings.sleepReminderEnabled) Color(0xFF9FA8DA) else textSecondary,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Sleep & Wake-up Alarm",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = textPrimary
                            )
                            Text(
                                text = if (userSettings.sleepReminderEnabled) "Sleep Reminder: ON" else "Sleep Reminder: OFF",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = if (userSettings.sleepReminderEnabled) GlowCyan else textSecondary
                            )
                        }
                    }

                    Switch(
                        checked = userSettings.sleepReminderEnabled,
                        onCheckedChange = { isChecked ->
                            viewModel.toggleSleepReminder(isChecked)
                            val msg = if (isChecked) "Sleep Reminder Enabled! $nextSleepReminder" else "Sleep Reminder Disabled"
                            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = Color(0xFF5C6BC0)
                        ),
                        modifier = Modifier.testTag("home_sleep_reminder_switch")
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Schedule times grid
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Bedtime Card
                    GlassCard(
                        glowColor = if (userSettings.sleepReminderEnabled) Color(0xFF9FA8DA).copy(alpha = 0.2f) else Color.Transparent,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .weight(1f)
                            .clickable { showSleepDialog = true }
                            .testTag("home_sleep_bedtime_card")
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Bedtime,
                                    contentDescription = null,
                                    tint = if (userSettings.sleepReminderEnabled) Color(0xFF9FA8DA) else textSecondary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Bedtime",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = textSecondary
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = bedtimeDisplay,
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = textPrimary
                            )
                        }
                    }

                    // Wake-up Card
                    GlassCard(
                        glowColor = if (userSettings.sleepReminderEnabled) GlowOrange.copy(alpha = 0.2f) else Color.Transparent,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .weight(1f)
                            .clickable { showSleepDialog = true }
                            .testTag("home_sleep_wakeup_card")
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.WbSunny,
                                    contentDescription = null,
                                    tint = GlowOrange,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Wake-up",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = textSecondary
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = wakeupDisplay,
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = textPrimary
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Sound & Next Reminder Info
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.MusicNote,
                            contentDescription = null,
                            tint = GlowCyan,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "${sleepSound.title} (${(userSettings.sleepSoundVolume * 100).toInt()}%)",
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                            color = textPrimary
                        )
                    }

                    Text(
                        text = nextSleepReminder,
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                        color = if (userSettings.sleepReminderEnabled) GlowOrange else textSecondary
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Action Buttons: Edit Schedule & Test Sound
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = { showSleepDialog = true },
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (userSettings.sleepReminderEnabled) Color(0xFF5C6BC0) else Color(0xFF3F51B5)
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp)
                            .testTag("home_sleep_edit_schedule_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = Color.White
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Edit Schedule", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 13.sp)
                    }

                    OutlinedButton(
                        onClick = {
                            viewModel.testSleepSound(userSettings.sleepSoundType, userSettings.sleepSoundVolume)
                            Toast.makeText(context, "Playing test sound: ${sleepSound.title}", Toast.LENGTH_SHORT).show()
                        },
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .height(44.dp)
                            .testTag("home_sleep_test_sound_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.MusicNote,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Test", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(28.dp))
    }

    if (showCreatorDialog) {
        AlertDialog(
            onDismissRequest = { showCreatorDialog = false },
            modifier = Modifier.testTag("creator_dialog"),
            icon = {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = null,
                    tint = GlowCyan,
                    modifier = Modifier.size(32.dp)
                )
            },
            title = {
                Text(
                    text = "Application Info",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Enjoy simple Realtime Ad-free version",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        text = "created by satya",
                        style = MaterialTheme.typography.labelSmall,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = { showCreatorDialog = false },
                    colors = ButtonDefaults.buttonColors(containerColor = HealthBlue),
                    modifier = Modifier.testTag("creator_dialog_confirm")
                ) {
                    Text("OK", color = Color.White)
                }
            }
        )
    }

    // Sleep Reminder Settings Dialog
    if (showSleepDialog) {
        SleepReminderDialog(
            currentSettings = userSettings,
            onDismiss = { showSleepDialog = false },
            onSave = { enabled, bedtimeH, bedtimeM, wakeupH, wakeupM, sound, vol ->
                viewModel.updateSleepReminder(
                    enabled = enabled,
                    bedtimeHour = bedtimeH,
                    bedtimeMinute = bedtimeM,
                    wakeupHour = wakeupH,
                    wakeupMinute = wakeupM,
                    soundType = sound,
                    volume = vol
                )
                showSleepDialog = false
                val nextDesc = SleepReminderScheduler.getNextReminderDescription(
                    userSettings.copy(
                        sleepReminderEnabled = enabled,
                        sleepBedtimeHour = bedtimeH,
                        sleepBedtimeMinute = bedtimeM,
                        sleepWakeupHour = wakeupH,
                        sleepWakeupMinute = wakeupM
                    )
                )
                Toast.makeText(
                    context,
                    if (enabled) "Sleep schedule updated! $nextDesc" else "Sleep reminder settings saved",
                    Toast.LENGTH_LONG
                ).show()
            },
            onTestSound = { sound, vol ->
                viewModel.testSleepSound(sound, vol)
            },
            onStopSound = {
                viewModel.stopSleepSound()
            }
        )
    }

    // Active Sleep Alert Dialog (Bedtime / Wake-up alarm trigger)
    if (activeSleepAlert != null) {
        val alert = activeSleepAlert!!
        SleepAlertDialog(
            alert = alert,
            onDismiss = {
                viewModel.dismissActiveSleepAlert()
            },
            onSnooze = {
                viewModel.snoozeActiveWakeup()
                Toast.makeText(context, "Alarm snoozed for 10 minutes", Toast.LENGTH_SHORT).show()
            }
        )
    }
}

@Composable
private fun rememberGreeting(): String {
    val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
    return when (hour) {
        in 5..11 -> "Good morning 👋"
        in 12..16 -> "Good afternoon 👋"
        in 17..21 -> "Good evening 👋"
        else -> "Good night 👋"
    }
}
