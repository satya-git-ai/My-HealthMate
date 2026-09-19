package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocalDrink
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.HydrationCelebrationOverlay
import com.example.ui.components.WaterWaveIndicator
import com.example.ui.theme.HealthBlue
import com.example.ui.theme.HealthCyan
import com.example.ui.theme.HealthIndigo
import com.example.ui.viewmodel.HealthViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun WaterTrackerScreen(
    viewModel: HealthViewModel,
    onBack: () -> Unit,
    onHome: () -> Unit = onBack,
    modifier: Modifier = Modifier
) {
    val todayRecord by viewModel.todayRecord.collectAsState()
    val userSettings by viewModel.userSettings.collectAsState()
    val waterLogs by viewModel.todayWaterLogs.collectAsState()

    val currentWater = todayRecord?.waterMl ?: 0
    val waterGoal = userSettings.waterGoalMl
    val isWaterTargetAchieved = currentWater >= waterGoal && waterGoal > 0

    var showCustomDialog by remember { mutableStateOf(false) }
    var customAmountText by remember { mutableStateOf("300") }
    var customWarningMessage by remember { mutableStateOf<String?>(null) }
    var showReminderSettingsDialog by remember { mutableStateOf(false) }
    var bannerFeedbackMessage by remember { mutableStateOf<String?>(null) }
    var showHydrationCelebration by remember { mutableStateOf(false) }
    var showResetDialog by remember { mutableStateOf(false) }

    androidx.compose.runtime.LaunchedEffect(isWaterTargetAchieved) {
        if (isWaterTargetAchieved && currentWater > 0) {
            showHydrationCelebration = true
        }
    }

    val logTimeFormat = SimpleDateFormat("h:mm a", Locale.getDefault())
    val isReminderEnabled = userSettings.waterReminderEnabled

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
            .testTag("water_tracker_screen")
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        // Header Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier.testTag("water_tracker_back_button")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = MaterialTheme.colorScheme.onBackground
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            Text(
                text = "Hydration Tracker",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onBackground
            )

            Spacer(modifier = Modifier.weight(1f))

            IconButton(
                onClick = { showResetDialog = true },
                enabled = currentWater > 0,
                modifier = Modifier.testTag("water_tracker_reset_top_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "Reset",
                    tint = if (currentWater > 0) MaterialTheme.colorScheme.onBackground else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.38f)
                )
            }

            IconButton(
                onClick = onHome,
                modifier = Modifier.testTag("water_tracker_home_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Home,
                    contentDescription = "Home",
                    tint = MaterialTheme.colorScheme.onBackground
                )
            }
        }

        // Live Feedback Banner (only displayed when reminder is enabled)
        if (bannerFeedbackMessage != null && isReminderEnabled) {
            Spacer(modifier = Modifier.height(12.dp))
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = HealthBlue.copy(alpha = 0.15f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.WaterDrop,
                            contentDescription = null,
                            tint = HealthBlue,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = bannerFeedbackMessage ?: "",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    }
                    IconButton(
                        onClick = { bannerFeedbackMessage = null },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "Dismiss",
                            tint = HealthBlue,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Visual Water Wave Indicator Card
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                WaterWaveIndicator(
                    currentMl = currentWater,
                    goalMl = waterGoal
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Interactive Timely Reminder & Alarm Status Card
        Card(
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            modifier = Modifier
                .fillMaxWidth()
                .clickable { showReminderSettingsDialog = true }
                .testTag("water_reminder_card")
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(44.dp)
                                .background(
                                    if (isReminderEnabled) HealthBlue.copy(alpha = 0.15f)
                                    else MaterialTheme.colorScheme.surfaceVariant,
                                    CircleShape
                                )
                        ) {
                            Icon(
                                imageVector = Icons.Default.WaterDrop,
                                contentDescription = null,
                                tint = if (isReminderEnabled) HealthBlue else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(24.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column {
                            Text(
                                text = "Timely Drink Reminder",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onBackground
                            )

                            Spacer(modifier = Modifier.height(3.dp))

                            Text(
                                text = if (isReminderEnabled)
                                    "Every ${userSettings.waterReminderIntervalMinutes} min"
                                else
                                    "Tap to configure drink reminder schedule",
                                style = MaterialTheme.typography.bodySmall,
                                color = if (isReminderEnabled) HealthBlue else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Switch(
                        checked = isReminderEnabled,
                        onCheckedChange = { isChecked ->
                            viewModel.updateWaterReminderSettings(
                                enabled = isChecked,
                                intervalMinutes = userSettings.waterReminderIntervalMinutes,
                                startHour = userSettings.waterReminderStartHour,
                                endHour = userSettings.waterReminderEndHour,
                                alarmSoundEnabled = false,
                                alarmSoundType = "WATER_DROP",
                                vibrateEnabled = userSettings.waterVibrateEnabled
                            )
                            if (isChecked) {
                                bannerFeedbackMessage = "Timely reminders scheduled! Next: ${viewModel.getNextWaterReminderTime()}"
                            } else {
                                bannerFeedbackMessage = null
                            }
                        },
                        modifier = Modifier.testTag("water_reminder_switch")
                    )
                }

                if (isReminderEnabled) {
                    Spacer(modifier = Modifier.height(14.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Default.AccessTime,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = HealthBlue
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Next: ${viewModel.getNextWaterReminderTime()}",
                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                                color = MaterialTheme.colorScheme.onBackground
                            )
                        }

                        Button(
                            onClick = { showReminderSettingsDialog = true },
                            colors = ButtonDefaults.buttonColors(containerColor = HealthBlue),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                            modifier = Modifier
                                .height(36.dp)
                                .testTag("open_reminder_settings_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Tune,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Schedule", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Quick Add Buttons Row in a continuous line
        Text(
            text = "Quick Log water / juice / liquids",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onBackground
        )

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = { viewModel.addWater(150) },
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = HealthCyan.copy(alpha = 0.9f)),
                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp),
                modifier = Modifier
                    .weight(1f)
                    .height(50.dp)
                    .testTag("water_add_150")
            ) {
                Icon(Icons.Default.WaterDrop, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(3.dp))
                Text(text = "+150ml", fontWeight = FontWeight.Bold, fontSize = 12.sp, maxLines = 1)
            }

            Button(
                onClick = { viewModel.addWater(250) },
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = HealthBlue),
                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp),
                modifier = Modifier
                    .weight(1f)
                    .height(50.dp)
                    .testTag("water_add_250")
            ) {
                Icon(Icons.Default.WaterDrop, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(3.dp))
                Text(text = "+250ml", fontWeight = FontWeight.Bold, fontSize = 12.sp, maxLines = 1)
            }

            Button(
                onClick = { viewModel.addWater(500) },
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7)),
                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp),
                modifier = Modifier
                    .weight(1f)
                    .height(50.dp)
                    .testTag("water_add_500")
            ) {
                Icon(Icons.Default.LocalDrink, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(3.dp))
                Text(text = "+500ml", fontWeight = FontWeight.Bold, fontSize = 12.sp, maxLines = 1)
            }

            // Custom Button clearly displayed in continuous line with filled style
            Button(
                onClick = { showCustomDialog = true },
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = HealthIndigo,
                    contentColor = Color.White
                ),
                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp),
                modifier = Modifier
                    .weight(1.1f)
                    .height(50.dp)
                    .testTag("water_add_custom")
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(3.dp))
                Text(text = "Custom", fontWeight = FontWeight.Bold, fontSize = 12.sp, maxLines = 1)
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Actions: Undo Last Entry and Reset Today's Hydration
        val canUndo = currentWater > 0
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            OutlinedButton(
                onClick = { viewModel.undoWater() },
                enabled = canUndo,
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = HealthBlue,
                    disabledContentColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                ),
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
                    .testTag("water_undo_button")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Undo,
                    contentDescription = "Undo",
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text("Undo")
            }

            OutlinedButton(
                onClick = { showResetDialog = true },
                enabled = canUndo,
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.error,
                    disabledContentColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                ),
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
                    .testTag("water_reset_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "Reset",
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text("Reset")
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Today's Water Log History
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Today's Logs (${waterLogs.size})",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onBackground
            )

            Row(verticalAlignment = Alignment.CenterVertically) {
                if (waterLogs.isNotEmpty()) {
                    TextButton(
                        onClick = { showResetDialog = true },
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                        modifier = Modifier.testTag("water_logs_reset_button")
                    ) {
                        Text(
                            text = "Reset All",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                }
                Text(
                    text = "Goal: %,d ml".format(waterGoal),
                    style = MaterialTheme.typography.labelMedium,
                    color = HealthBlue
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (waterLogs.isEmpty()) {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "No water logged yet today.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Tap +250 ml to record your first glass!",
                        style = MaterialTheme.typography.bodySmall,
                        color = HealthBlue
                    )
                }
            }
        } else {
            waterLogs.forEach { log ->
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .size(36.dp)
                                    .background(HealthBlue.copy(alpha = 0.15f), shape = CircleShape)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.WaterDrop,
                                    contentDescription = null,
                                    tint = HealthBlue,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = "+${log.amountMl} ml",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onBackground
                            )
                        }

                        Text(
                            text = logTimeFormat.format(Date(log.timestamp)),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(28.dp))
    }

    // Custom Water Intake Dialog
    if (showCustomDialog) {
        val currentAmount = customAmountText.toIntOrNull() ?: 0
        val isOver3700 = currentAmount > 3700

        AlertDialog(
            onDismissRequest = {
                showCustomDialog = false
                customWarningMessage = null
            },
            title = {
                Text(
                    text = "Add Custom Water",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                )
            },
            text = {
                Column {
                    Text(
                        text = "Enter amount of water consumed in milliliters (ml):",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    OutlinedTextField(
                        value = customAmountText,
                        onValueChange = { input ->
                            customAmountText = input.filter { ch -> ch.isDigit() }
                            val typed = customAmountText.toIntOrNull() ?: 0
                            if (typed > 3700) {
                                customWarningMessage = "Warning: Adding more than 3,700 ml is not permitted! Action will be aborted."
                            } else {
                                customWarningMessage = null
                            }
                        },
                        label = { Text("Amount (ml)") },
                        singleLine = true,
                        isError = isOver3700 || customWarningMessage != null,
                        supportingText = {
                            if (isOver3700 || customWarningMessage != null) {
                                Text(
                                    text = customWarningMessage ?: "Warning: Maximum limit is 3,700 ml per log.",
                                    color = MaterialTheme.colorScheme.error,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("water_custom_input")
                    )

                    // Warning card when input exceeds 3700 ml
                    if (isOver3700 || customWarningMessage != null) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Card(
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.45f)
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Warning,
                                    contentDescription = "Warning",
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = "Limit Exceeded (Max 3,700 ml)",
                                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.error
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "Action aborted: Logging more than 3,700 ml at once can be dangerous to health. Please enter a value between 1 and 3,700 ml.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onErrorContainer
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val amount = customAmountText.toIntOrNull() ?: 0
                        if (amount > 3700) {
                            // ABORT ACTION and display warning message
                            customWarningMessage = "Action aborted: Cannot add $amount ml. Maximum single intake is 3,700 ml."
                            bannerFeedbackMessage = "⚠️ Action aborted: Water entry exceeds 3,700 ml limit."
                            return@Button
                        }
                        if (amount > 0) {
                            viewModel.addWater(amount)
                            showCustomDialog = false
                            customWarningMessage = null
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isOver3700) MaterialTheme.colorScheme.error else HealthBlue
                    ),
                    modifier = Modifier.testTag("water_custom_confirm_button")
                ) {
                    Text(
                        text = if (isOver3700) "Over Limit" else "Add Water",
                        color = Color.White
                    )
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showCustomDialog = false
                        customWarningMessage = null
                    }
                ) {
                    Text("Cancel")
                }
            }
        )
    }

    // Timely Hydration Reminder Dialog
    if (showReminderSettingsDialog) {
        HydrationReminderDialog(
            userSettings = userSettings,
            onDismiss = {
                showReminderSettingsDialog = false
            },
            onSaveSettings = { enabled, interval, startHour, endHour, vibrate ->
                viewModel.updateWaterReminderSettings(
                    enabled = enabled,
                    intervalMinutes = interval,
                    startHour = startHour,
                    endHour = endHour,
                    alarmSoundEnabled = false,
                    alarmSoundType = "WATER_DROP",
                    vibrateEnabled = vibrate
                )
                showReminderSettingsDialog = false
                bannerFeedbackMessage = if (enabled) {
                    "Timely reminder set every $interval min! Next: ${viewModel.getNextWaterReminderTime()}"
                } else {
                    null
                }
            }
        )
    }

    // Reset Today's Water Confirmation Dialog
    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            icon = {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(28.dp)
                )
            },
            title = {
                Text(
                    text = "Reset Today's Hydration?",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
            },
            text = {
                Text(
                    text = "This will clear all logged water entries for today and reset your hydration progress back to 0 ml.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.resetWater()
                        showResetDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    modifier = Modifier.testTag("confirm_reset_water_button")
                ) {
                    Text("Reset", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showResetDialog = false }
                ) {
                    Text("Cancel")
                }
            }
        )
    }

    // Temporary hydration celebration animation overlay (only if hydration target achieved)
    if (showHydrationCelebration && isWaterTargetAchieved) {
        HydrationCelebrationOverlay(
            waterGoalMl = waterGoal,
            onDismiss = { showHydrationCelebration = false }
        )
    }
}

@Composable
fun HydrationReminderDialog(
    userSettings: com.example.data.preferences.UserSettings,
    onDismiss: () -> Unit,
    onSaveSettings: (
        enabled: Boolean,
        intervalMinutes: Int,
        startHour: Int,
        endHour: Int,
        vibrateEnabled: Boolean
    ) -> Unit
) {
    var enabled by remember { mutableStateOf(userSettings.waterReminderEnabled) }
    var intervalMinutes by remember { mutableIntStateOf(userSettings.waterReminderIntervalMinutes) }
    var startHour by remember { mutableIntStateOf(userSettings.waterReminderStartHour) }
    var endHour by remember { mutableIntStateOf(userSettings.waterReminderEndHour) }
    var vibrateEnabled by remember { mutableStateOf(userSettings.waterVibrateEnabled) }

    val intervalOptions = listOf(15, 30, 45, 60, 90, 120)
    val startHourOptions = listOf(6, 7, 8, 9, 10)
    val endHourOptions = listOf(19, 20, 21, 22, 23)

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.testTag("hydration_reminder_dialog"),
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(38.dp)
                        .background(HealthBlue.copy(alpha = 0.15f), CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.WaterDrop,
                        contentDescription = null,
                        tint = HealthBlue,
                        modifier = Modifier.size(22.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "Timely Drink Reminder",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    Text(
                        text = "Drink water regularly during your day",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                // Master Enable Switch
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (enabled) HealthBlue.copy(alpha = 0.1f) else MaterialTheme.colorScheme.surfaceVariant
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (enabled) "Timely Drink Reminder: Active" else "Timely Drink Reminder: Off",
                                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onBackground
                            )
                            Text(
                                text = if (enabled) "Reminders active every $intervalMinutes min" else "Reminders are disabled",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = enabled,
                            onCheckedChange = { enabled = it },
                            modifier = Modifier.testTag("dialog_water_reminder_switch")
                        )
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Section 1: Reminder Interval
                Text(
                    text = "Reminder Interval",
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = "How often you want to be reminded to drink water",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    intervalOptions.take(3).forEach { mins ->
                        val label = "${mins}m"
                        FilterChip(
                            selected = intervalMinutes == mins,
                            onClick = { intervalMinutes = mins },
                            label = { Text(label, fontSize = 12.sp, fontWeight = FontWeight.SemiBold) },
                            enabled = enabled,
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = HealthBlue,
                                selectedLabelColor = Color.White
                            ),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    intervalOptions.drop(3).forEach { mins ->
                        val label = when (mins) {
                            60 -> "1 hr"
                            90 -> "1.5 hr"
                            120 -> "2 hr"
                            else -> "${mins}m"
                        }
                        FilterChip(
                            selected = intervalMinutes == mins,
                            onClick = { intervalMinutes = mins },
                            label = { Text(label, fontSize = 12.sp, fontWeight = FontWeight.SemiBold) },
                            enabled = enabled,
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = HealthBlue,
                                selectedLabelColor = Color.White
                            ),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Section 2: Active Window (Daytime hours)
                Text(
                    text = "Active Reminder Window",
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onBackground
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Reminders run every $intervalMinutes min between ${formatHour12(startHour)} and ${formatHour12(endHour)} • ${endHour - startHour} active hours daily",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Start Hour
                    Card(
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = "Wake / Start Time",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                                color = HealthBlue
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = formatHour12(startHour),
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                startHourOptions.forEach { hour ->
                                    val isSelected = hour == startHour
                                    Box(
                                        contentAlignment = Alignment.Center,
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(if (isSelected) HealthBlue else MaterialTheme.colorScheme.surface)
                                            .clickable(enabled = enabled) { startHour = hour }
                                            .padding(vertical = 6.dp)
                                    ) {
                                        Text(
                                            text = formatHourShort(hour),
                                            fontSize = 10.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                            color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // End Hour
                    Card(
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = "Sleep / End Time",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                                color = HealthBlue
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = formatHour12(endHour),
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                endHourOptions.forEach { hour ->
                                    val isSelected = hour == endHour
                                    Box(
                                        contentAlignment = Alignment.Center,
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(if (isSelected) HealthBlue else MaterialTheme.colorScheme.surface)
                                            .clickable(enabled = enabled) { endHour = hour }
                                            .padding(vertical = 6.dp)
                                    ) {
                                        Text(
                                            text = formatHourShort(hour),
                                            fontSize = 10.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                            color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Vibration Toggle
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Vibration,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Vibrate on Reminder",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    }
                    Switch(
                        checked = vibrateEnabled,
                        onCheckedChange = { vibrateEnabled = it },
                        enabled = enabled
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSaveSettings(
                        enabled,
                        intervalMinutes,
                        startHour,
                        endHour,
                        vibrateEnabled
                    )
                },
                colors = ButtonDefaults.buttonColors(containerColor = HealthBlue),
                modifier = Modifier.testTag("save_water_reminder_button")
            ) {
                Text("Save & Set Reminder", color = Color.White, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

private fun formatHour12(hour: Int): String {
    val h = when {
        hour == 0 -> 12
        hour > 12 -> hour - 12
        else -> hour
    }
    val amPm = if (hour < 12) "AM" else "PM"
    return "$h:00 $amPm"
}

private fun formatHourShort(hour: Int): String {
    val h = when {
        hour == 0 -> 12
        hour > 12 -> hour - 12
        else -> hour
    }
    val amPm = if (hour < 12) "AM" else "PM"
    return "$h $amPm"
}
