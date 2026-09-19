package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DirectionsRun
import androidx.compose.material.icons.filled.DirectionsWalk
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.InteractiveRouteMap
import com.example.ui.theme.HealthBlue
import com.example.ui.theme.HealthCyan
import com.example.ui.theme.HealthEmerald
import com.example.ui.theme.HealthOrange
import com.example.ui.theme.HealthRose
import com.example.ui.viewmodel.HealthViewModel

@Composable
fun GpsTrackerScreen(
    viewModel: HealthViewModel,
    onBack: () -> Unit,
    onViewRouteDetail: (Long) -> Unit,
    onHome: () -> Unit = onBack,
    modifier: Modifier = Modifier
) {
    val workoutState by viewModel.workoutLiveState.collectAsState()
    var selectedWorkoutType by remember { mutableStateOf("Normal Indoor Walk") }

    val hours = workoutState.durationSeconds / 3600
    val minutes = (workoutState.durationSeconds % 3600) / 60
    val seconds = workoutState.durationSeconds % 60
    val formattedDuration = "%02d:%02d:%02d".format(hours, minutes, seconds)
    val distanceKm = workoutState.distanceMeters / 1000f

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
            .testTag("gps_tracker_screen")
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
                    modifier = Modifier.testTag("gps_tracker_back_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = MaterialTheme.colorScheme.onBackground
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                Text(
                    text = "Activity & Workout Tracker",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onBackground
                )
            }

            IconButton(
                onClick = onHome,
                modifier = Modifier.testTag("gps_tracker_home_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Home,
                    contentDescription = "Home",
                    tint = MaterialTheme.colorScheme.onBackground
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Activity Type Chips (Normal Indoor Walk / Outdoor Walk / Outdoor Run)
        if (!workoutState.isTracking) {
            Text(
                text = "WORKOUT TYPE",
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
            ) {
                FilterChip(
                    selected = selectedWorkoutType == "Normal Indoor Walk",
                    onClick = { selectedWorkoutType = "Normal Indoor Walk" },
                    label = { Text("Normal Indoor Walk") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.DirectionsWalk,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = HealthEmerald.copy(alpha = 0.25f),
                        selectedLabelColor = HealthEmerald,
                        selectedLeadingIconColor = HealthEmerald
                    ),
                    modifier = Modifier.testTag("workout_chip_indoor_walk")
                )

                FilterChip(
                    selected = selectedWorkoutType == "Outdoor Walk",
                    onClick = { selectedWorkoutType = "Outdoor Walk" },
                    label = { Text("Outdoor Walk") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.DirectionsWalk,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = HealthBlue.copy(alpha = 0.2f),
                        selectedLabelColor = HealthBlue,
                        selectedLeadingIconColor = HealthBlue
                    ),
                    modifier = Modifier.testTag("workout_chip_outdoor_walk")
                )

                FilterChip(
                    selected = selectedWorkoutType == "Outdoor Run",
                    onClick = { selectedWorkoutType = "Outdoor Run" },
                    label = { Text("Outdoor Run") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.DirectionsRun,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = HealthOrange.copy(alpha = 0.2f),
                        selectedLabelColor = HealthOrange,
                        selectedLeadingIconColor = HealthOrange
                    ),
                    modifier = Modifier.testTag("workout_chip_outdoor_run")
                )
            }

            Spacer(modifier = Modifier.height(14.dp))
        }

        // Live Workout Stats Card
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Large Live Duration
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Box(
                        modifier = Modifier
                            .background(HealthEmerald.copy(alpha = 0.15f), shape = RoundedCornerShape(8.dp))
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = if (workoutState.isTracking) workoutState.workoutType else selectedWorkoutType,
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = HealthEmerald
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "WORKOUT DURATION",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = formattedDuration,
                    style = MaterialTheme.typography.displayMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 42.sp
                    ),
                    color = if (workoutState.isTracking && !workoutState.isPaused) HealthEmerald else MaterialTheme.colorScheme.onBackground
                )

                Spacer(modifier = Modifier.height(18.dp))

                // 4 Metrics Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "Distance",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "%.2f km".format(distanceKm),
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    }

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "Current Pace",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        val paceMin = workoutState.currentPaceMinPerKm.toInt()
                        val paceSec = ((workoutState.currentPaceMinPerKm - paceMin) * 60).toInt()
                        Text(
                            text = if (workoutState.currentPaceMinPerKm > 0) "%d'%02d\"/km".format(paceMin, paceSec) else "--'--\"/km",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = HealthBlue
                        )
                    }

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "Speed",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "%.1f km/h".format(workoutState.currentSpeedKmh),
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = HealthCyan
                        )
                    }

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "Calories",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "${workoutState.caloriesBurned} kcal",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = HealthOrange
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Control Buttons (Start Workout / Pause / Resume / Stop)
        if (!workoutState.isTracking) {
            Button(
                onClick = {
                    viewModel.startWorkout(selectedWorkoutType)
                },
                shape = RoundedCornerShape(18.dp),
                colors = ButtonDefaults.buttonColors(containerColor = HealthEmerald),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .testTag("gps_start_workout_button")
            ) {
                Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(24.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Start $selectedWorkoutType",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = Color.White
                )
            }
        } else {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Pause / Resume Button
                Button(
                    onClick = {
                        if (workoutState.isPaused) {
                            viewModel.resumeWorkout()
                        } else {
                            viewModel.pauseWorkout()
                        }
                    },
                    shape = RoundedCornerShape(18.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (workoutState.isPaused) HealthEmerald else HealthOrange
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .height(56.dp)
                        .testTag("gps_pause_resume_button")
                ) {
                    Icon(
                        imageVector = if (workoutState.isPaused) Icons.Default.PlayArrow else Icons.Default.Pause,
                        contentDescription = null
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (workoutState.isPaused) "Resume" else "Pause",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = Color.White
                    )
                }

                // Stop Workout Button
                Button(
                    onClick = {
                        viewModel.stopWorkout()
                        selectedWorkoutType = "Normal Indoor Walk"
                    },
                    shape = RoundedCornerShape(18.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = HealthRose),
                    modifier = Modifier
                        .weight(1f)
                        .height(56.dp)
                        .testTag("gps_stop_workout_button")
                ) {
                    Icon(Icons.Default.Stop, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Finish",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = Color.White
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(28.dp))
    }
}
