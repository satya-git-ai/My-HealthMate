package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.preferences.UserSettings
import com.example.notification.AlarmSoundPlayer
import com.example.notification.SleepReminderScheduler
import com.example.notification.SleepSoundType
import com.example.ui.theme.HealthBlue
import com.example.ui.theme.HealthCyan
import com.example.ui.theme.HealthOrange

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SleepReminderDialog(
    currentSettings: UserSettings,
    onDismiss: () -> Unit,
    onSave: (
        enabled: Boolean,
        bedtimeHour: Int,
        bedtimeMinute: Int,
        wakeupHour: Int,
        wakeupMinute: Int,
        soundType: String,
        volume: Float
    ) -> Unit,
    onTestSound: (soundType: String, volume: Float) -> Unit,
    onStopSound: () -> Unit
) {
    var enabled by remember { mutableStateOf(currentSettings.sleepReminderEnabled) }
    var bedtimeHour by remember { mutableIntStateOf(currentSettings.sleepBedtimeHour) }
    var bedtimeMinute by remember { mutableIntStateOf(currentSettings.sleepBedtimeMinute) }
    var wakeupHour by remember { mutableIntStateOf(currentSettings.sleepWakeupHour) }
    var wakeupMinute by remember { mutableIntStateOf(currentSettings.sleepWakeupMinute) }
    var soundType by remember { mutableStateOf(currentSettings.sleepSoundType) }
    var volume by remember { mutableFloatStateOf(currentSettings.sleepSoundVolume) }

    var isTestingSound by remember { mutableStateOf(false) }

    DisposableEffect(Unit) {
        onDispose {
            onStopSound()
        }
    }

    Dialog(
        onDismissRequest = {
            onStopSound()
            onDismiss()
        },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .heightIn(max = 680.dp)
                .clip(RoundedCornerShape(24.dp))
                .testTag("sleep_reminder_dialog"),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF3F51B5).copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Bedtime,
                                contentDescription = null,
                                tint = Color(0xFF3F51B5),
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Sleep & Wake-up Alarm",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Reliable bedtime & morning alarms",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    IconButton(
                        onClick = {
                            onStopSound()
                            onDismiss()
                        },
                        modifier = Modifier.testTag("sleep_dialog_close_btn")
                    ) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Close")
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Master Toggle ON/OFF
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (enabled) Color(0xFF3F51B5).copy(alpha = 0.12f)
                        else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (enabled) "Sleep Reminder: ON" else "Sleep Reminder: OFF",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = if (enabled) Color(0xFF3F51B5) else MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = if (enabled) "Reminders active at bedtime & wake-up" else "Turn on to schedule daily alarms",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = enabled,
                            onCheckedChange = { enabled = it },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = Color(0xFF3F51B5)
                            ),
                            modifier = Modifier.testTag("sleep_dialog_enable_switch")
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Section 1: Bedtime Picker
                Text(
                    text = "🌙 Bedtime (Time to Sleep)",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(6.dp))
                TimeSelectionCard(
                    hour = bedtimeHour,
                    minute = bedtimeMinute,
                    onTimeChanged = { h, m ->
                        bedtimeHour = h
                        bedtimeMinute = m
                    },
                    accentColor = Color(0xFF5C6BC0),
                    presets = listOf(
                        "9:30 PM" to (21 to 30),
                        "10:00 PM" to (22 to 0),
                        "10:30 PM" to (22 to 30),
                        "11:00 PM" to (23 to 0),
                        "11:30 PM" to (23 to 30)
                    )
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Section 2: Wake-up Time Picker
                Text(
                    text = "☀️ Wake-up Alarm Time",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(6.dp))
                TimeSelectionCard(
                    hour = wakeupHour,
                    minute = wakeupMinute,
                    onTimeChanged = { h, m ->
                        wakeupHour = h
                        wakeupMinute = m
                    },
                    accentColor = HealthOrange,
                    presets = listOf(
                        "5:30 AM" to (5 to 30),
                        "6:00 AM" to (6 to 0),
                        "6:30 AM" to (6 to 30),
                        "7:00 AM" to (7 to 0),
                        "7:30 AM" to (7 to 30)
                    )
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Section 3: Alarm Sound Selection
                Text(
                    text = "🎵 Reminder & Alarm Sound",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(8.dp))

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    SleepSoundType.entries.forEach { st ->
                        val isSelected = soundType == st.id
                        Card(
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isSelected) Color(0xFF3F51B5).copy(alpha = 0.12f)
                                else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                            ),
                            border = if (isSelected) androidx.compose.foundation.BorderStroke(1.5.dp, Color(0xFF3F51B5)) else null,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    soundType = st.id
                                    onTestSound(st.id, volume)
                                    isTestingSound = true
                                }
                                .testTag("sleep_sound_option_${st.id}")
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(
                                        imageVector = if (isSelected) Icons.Default.GraphicEq else Icons.Default.MusicNote,
                                        contentDescription = null,
                                        tint = if (isSelected) Color(0xFF3F51B5) else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column {
                                        Text(
                                            text = st.title,
                                            style = MaterialTheme.typography.bodyMedium.copy(
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                            ),
                                            color = if (isSelected) Color(0xFF3F51B5) else MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = st.description,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }

                                if (isSelected) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = "Selected",
                                        tint = Color(0xFF3F51B5),
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Section 4: Volume Control & Test Sound Button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.VolumeUp,
                            contentDescription = null,
                            tint = Color(0xFF3F51B5),
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Volume: ${(volume * 100).toInt()}%",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    // Test Sound Button
                    Button(
                        onClick = {
                            if (isTestingSound) {
                                onStopSound()
                                isTestingSound = false
                            } else {
                                onTestSound(soundType, volume)
                                isTestingSound = true
                            }
                        },
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isTestingSound) MaterialTheme.colorScheme.error else Color(0xFF3F51B5)
                        ),
                        modifier = Modifier
                            .height(36.dp)
                            .testTag("sleep_test_sound_button")
                    ) {
                        Icon(
                            imageVector = if (isTestingSound) Icons.Default.Stop else Icons.Default.MusicNote,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (isTestingSound) "Stop" else "Test Sound",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }

                Slider(
                    value = volume,
                    onValueChange = { volume = it },
                    valueRange = 0.1f..1.0f,
                    colors = SliderDefaults.colors(
                        thumbColor = Color(0xFF3F51B5),
                        activeTrackColor = Color(0xFF3F51B5)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("sleep_volume_slider")
                )

                Spacer(modifier = Modifier.height(18.dp))

                // Bottom Buttons: Save / Cancel
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            onStopSound()
                            onDismiss()
                        },
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("sleep_dialog_cancel_btn")
                    ) {
                        Text("Cancel")
                    }

                    Button(
                        onClick = {
                            onStopSound()
                            onSave(
                                enabled,
                                bedtimeHour,
                                bedtimeMinute,
                                wakeupHour,
                                wakeupMinute,
                                soundType,
                                volume
                            )
                        },
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3F51B5)),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("sleep_dialog_save_btn")
                    ) {
                        Text(
                            text = "Save Schedule",
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TimeSelectionCard(
    hour: Int,
    minute: Int,
    onTimeChanged: (hour: Int, minute: Int) -> Unit,
    accentColor: Color,
    presets: List<Pair<String, Pair<Int, Int>>>
) {
    val isAm = hour < 12
    val hour12 = if (hour % 12 == 0) 12 else hour % 12

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Time Display + AM/PM Toggle
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Large Digital Time Indicator
                Text(
                    text = "%d:%02d".format(hour12, minute),
                    style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                    color = accentColor
                )

                // AM / PM Toggle Pill
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(MaterialTheme.colorScheme.surface)
                        .padding(2.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(18.dp))
                            .background(if (isAm) accentColor else Color.Transparent)
                            .clickable {
                                if (!isAm) {
                                    val newH = (hour - 12).coerceAtLeast(0)
                                    onTimeChanged(newH, minute)
                                }
                            }
                            .padding(horizontal = 14.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = "AM",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = if (isAm) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(18.dp))
                            .background(if (!isAm) accentColor else Color.Transparent)
                            .clickable {
                                if (isAm) {
                                    val newH = (hour + 12).coerceAtMost(23)
                                    onTimeChanged(newH, minute)
                                }
                            }
                            .padding(horizontal = 14.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = "PM",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = if (!isAm) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Steppers for Hour & Minute
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Hour Adjuster
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surface)
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = {
                            var newH12 = hour12 - 1
                            if (newH12 < 1) newH12 = 12
                            val finalHour24 = if (isAm) {
                                if (newH12 == 12) 0 else newH12
                            } else {
                                if (newH12 == 12) 12 else newH12 + 12
                            }
                            onTimeChanged(finalHour24, minute)
                        },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Text("-", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = accentColor)
                    }

                    Text(
                        text = "$hour12 hr",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    IconButton(
                        onClick = {
                            var newH12 = hour12 + 1
                            if (newH12 > 12) newH12 = 1
                            val finalHour24 = if (isAm) {
                                if (newH12 == 12) 0 else newH12
                            } else {
                                if (newH12 == 12) 12 else newH12 + 12
                            }
                            onTimeChanged(finalHour24, minute)
                        },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Text("+", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = accentColor)
                    }
                }

                // Minute Adjuster
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surface)
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = {
                            var newMin = minute - 5
                            if (newMin < 0) newMin = 55
                            onTimeChanged(hour, newMin)
                        },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Text("-", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = accentColor)
                    }

                    Text(
                        text = "%02d min".format(minute),
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    IconButton(
                        onClick = {
                            var newMin = minute + 5
                            if (newMin >= 60) newMin = 0
                            onTimeChanged(hour, newMin)
                        },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Text("+", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = accentColor)
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Quick Presets Row
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                presets.forEach { (label, timePair) ->
                    val (h, m) = timePair
                    val isMatch = (hour == h && minute == m)
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(
                                if (isMatch) accentColor.copy(alpha = 0.2f)
                                else MaterialTheme.colorScheme.surface
                            )
                            .border(
                                width = 1.dp,
                                color = if (isMatch) accentColor else Color.Transparent,
                                shape = RoundedCornerShape(8.dp)
                            )
                            .clickable { onTimeChanged(h, m) }
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = label,
                            fontSize = 11.sp,
                            fontWeight = if (isMatch) FontWeight.Bold else FontWeight.Normal,
                            color = if (isMatch) accentColor else MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }
    }
}
