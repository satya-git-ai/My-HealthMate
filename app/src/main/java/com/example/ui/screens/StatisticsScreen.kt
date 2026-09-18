package com.example.ui.screens

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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DirectionsRun
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.BarChartItem
import com.example.ui.components.MetricCard
import com.example.ui.components.WeeklyBarChart
import com.example.ui.theme.HealthBlue
import com.example.ui.theme.HealthCyan
import com.example.ui.theme.HealthEmerald
import com.example.ui.theme.HealthOrange
import com.example.ui.viewmodel.HealthViewModel
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@Composable
fun StatisticsScreen(
    viewModel: HealthViewModel,
    onBack: () -> Unit,
    onHome: () -> Unit = onBack,
    modifier: Modifier = Modifier
) {
    val allRecords by viewModel.allDailyRecords.collectAsState()
    val todayRecord by viewModel.todayRecord.collectAsState()
    val userSettings by viewModel.userSettings.collectAsState()

    var showResetDialog by remember { mutableStateOf(false) }
    var selectedDate by remember { mutableStateOf<String?>(viewModel.todayDate) }

    val dayFormat = remember { SimpleDateFormat("EEE", Locale.getDefault()) }
    val parseFormat = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()) }

    // Associate all records by date and ensure today's real-time record is always fresh
    val recordsMap = remember(allRecords, todayRecord) {
        val map = allRecords.associateBy { it.date }.toMutableMap()
        if (todayRecord != null) {
            map[viewModel.todayDate] = todayRecord!!
        }
        map
    }

    // Always build exactly 7 consecutive days ending today (Day -6 to Day 0)
    val weekDays = remember(todayRecord, allRecords) {
        val list = mutableListOf<String>()
        val cal = Calendar.getInstance()
        for (i in 6 downTo 0) {
            cal.time = Date()
            cal.add(Calendar.DAY_OF_YEAR, -i)
            list.add(parseFormat.format(cal.time))
        }
        list
    }

    // Real-time 7-day steps starting from 0
    val stepChartItems = remember(weekDays, recordsMap, viewModel.todayDate) {
        weekDays.map { dateStr ->
            val record = recordsMap[dateStr]
            val isToday = (dateStr == viewModel.todayDate)
            val parsed = try { parseFormat.parse(dateStr) } catch (e: Exception) { null }
            val label = when {
                isToday -> "Today"
                parsed != null -> dayFormat.format(parsed)
                else -> dateStr.takeLast(2)
            }
            BarChartItem(
                label = label,
                value = (record?.steps ?: 0).toFloat(),
                date = dateStr,
                isToday = isToday
            )
        }
    }

    // Real-time 7-day hydration starting from 0
    val waterChartItems = remember(weekDays, recordsMap, viewModel.todayDate) {
        weekDays.map { dateStr ->
            val record = recordsMap[dateStr]
            val isToday = (dateStr == viewModel.todayDate)
            val parsed = try { parseFormat.parse(dateStr) } catch (e: Exception) { null }
            val label = when {
                isToday -> "Today"
                parsed != null -> dayFormat.format(parsed)
                else -> dateStr.takeLast(2)
            }
            BarChartItem(
                label = label,
                value = (record?.waterMl ?: 0).toFloat(),
                date = dateStr,
                isToday = isToday
            )
        }
    }

    val totalSteps = stepChartItems.sumOf { it.value.toInt() }
    val avgSteps = totalSteps / 7
    val totalWater = waterChartItems.sumOf { it.value.toInt() }
    val avgWater = totalWater / 7
    val totalDistanceKm = weekDays.map { recordsMap[it]?.distanceMeters ?: 0f }.sum() / 1000f
    val totalCalories = weekDays.sumOf { recordsMap[it]?.caloriesBurned ?: 0 }

    val todaySteps = todayRecord?.steps ?: 0
    val todayWater = todayRecord?.waterMl ?: 0
    val todayGoalPct = if (userSettings.stepGoal > 0) {
        ((todaySteps.toFloat() / userSettings.stepGoal) * 100).toInt()
    } else 0

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
            .testTag("statistics_screen")
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier.testTag("statistics_back_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = MaterialTheme.colorScheme.onBackground
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                Column {
                    Text(
                        text = "Trends & Statistics",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "Real-time User Activity",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Row {
                IconButton(
                    onClick = onHome,
                    modifier = Modifier.testTag("statistics_home_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Home,
                        contentDescription = "Home",
                        tint = MaterialTheme.colorScheme.onBackground
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Real-Time Live Status Banner
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
            modifier = Modifier
                .fillMaxWidth()
                .border(
                    width = 1.dp,
                    color = Color(0xFF00E676).copy(alpha = 0.4f),
                    shape = RoundedCornerShape(16.dp)
                )
                .testTag("statistics_live_badge")
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
                            .size(10.dp)
                            .background(Color(0xFF00E676), CircleShape)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "LIVE USER COUNT • STARTING FROM 0",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.8.sp
                            ),
                            color = Color(0xFF00E676)
                        )
                        Text(
                            text = "Today: %,d steps • %,d ml (%d%% goal)".format(todaySteps, todayWater, todayGoalPct),
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                IconButton(
                    onClick = { showResetDialog = true },
                    modifier = Modifier.size(36.dp).testTag("statistics_reset_all_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Reset Data",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Real-Time Live Counter Testing / Increment Controls
        Card(
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("statistics_realtime_controls")
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Sensors,
                            contentDescription = null,
                            tint = HealthEmerald,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Real-Time User Step Counter",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    Text(
                        text = "Instant Update",
                        style = MaterialTheme.typography.labelSmall,
                        color = HealthEmerald
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = { viewModel.addManualStepCount(25) },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f).testTag("quick_step_25")
                    ) {
                        Text("+25 steps", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }

                    OutlinedButton(
                        onClick = { viewModel.addManualStepCount(100) },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f).testTag("quick_step_100")
                    ) {
                        Text("+100 steps", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = { viewModel.addWater(250) },
                        colors = ButtonDefaults.buttonColors(containerColor = HealthBlue),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f).testTag("quick_water_250")
                    ) {
                        Text("+250 ml", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // 7-Day Steps Chart
        WeeklyBarChart(
            title = "7-Day Step Count",
            items = stepChartItems,
            goalValue = userSettings.stepGoal.toFloat(),
            unit = "steps",
            primaryColor = HealthEmerald,
            secondaryColor = HealthCyan,
            selectedDate = selectedDate,
            onItemClick = { selectedDate = it.date },
            testTag = "weekly_steps_chart"
        )

        Spacer(modifier = Modifier.height(18.dp))

        // 7-Day Hydration Chart
        WeeklyBarChart(
            title = "7-Day Hydration Trends",
            items = waterChartItems,
            goalValue = userSettings.waterGoalMl.toFloat(),
            unit = "ml",
            primaryColor = HealthBlue,
            secondaryColor = HealthCyan,
            selectedDate = selectedDate,
            onItemClick = { selectedDate = it.date },
            testTag = "weekly_water_chart"
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Weekly Summary Overview
        Text(
            text = "7-Day Summary (Actual User Count)",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onBackground
        )

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            MetricCard(
                title = "Total Steps",
                value = "%,d".format(totalSteps),
                unit = "steps",
                icon = Icons.Default.DirectionsRun,
                iconTint = HealthEmerald,
                iconBgColor = HealthEmerald.copy(alpha = 0.15f),
                subtitle = "Avg: %,d / day".format(avgSteps),
                modifier = Modifier.weight(1f)
            )

            MetricCard(
                title = "Total Water",
                value = "%,d".format(totalWater),
                unit = "ml",
                icon = Icons.Default.WaterDrop,
                iconTint = HealthBlue,
                iconBgColor = HealthBlue.copy(alpha = 0.15f),
                subtitle = "Avg: %,d ml / day".format(avgWater),
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            MetricCard(
                title = "Total Distance",
                value = "%.2f".format(totalDistanceKm),
                unit = "km",
                icon = Icons.Default.LocationOn,
                iconTint = HealthCyan,
                iconBgColor = HealthCyan.copy(alpha = 0.15f),
                subtitle = "From recorded steps & GPS",
                modifier = Modifier.weight(1f)
            )

            MetricCard(
                title = "Calories Burned",
                value = "%,d".format(totalCalories),
                unit = "kcal",
                icon = Icons.Default.LocalFireDepartment,
                iconTint = HealthOrange,
                iconBgColor = HealthOrange.copy(alpha = 0.15f),
                subtitle = "User energy expenditure",
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))
    }

    // Reset All Confirmation Dialog
    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            title = { Text("Reset All Statistics to 0?") },
            text = {
                Text(
                    "This will clear all recorded daily steps, water logs, and workout history, resetting all charts and statistics back to 0 so you can track freshly from scratch."
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.resetAllStatisticsToZero()
                        showResetDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Reset to 0")
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
