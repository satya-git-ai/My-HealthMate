package com.example.ui.components

import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.LocalPharmacy
import androidx.compose.material.icons.filled.Medication
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.local.entity.Medicine
import com.example.notification.MedSoundType
import com.example.ui.theme.HealthBlue
import com.example.ui.theme.HealthEmerald
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun AddEditMedicineDialog(
    initialMedicine: Medicine? = null,
    onDismiss: () -> Unit,
    onSave: (Medicine) -> Unit,
    onDelete: ((Medicine) -> Unit)? = null
) {
    val isEdit = initialMedicine != null

    var name by remember { mutableStateOf(initialMedicine?.name ?: "") }
    var dosage by remember { mutableStateOf(initialMedicine?.dosage ?: "") }
    var type by remember { mutableStateOf(initialMedicine?.type ?: "Tablet") }

    val todayDateString = remember {
        SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
    }
    var startDate by remember { mutableStateOf(initialMedicine?.startDate ?: todayDateString) }

    var reminderHour by remember { mutableIntStateOf(initialMedicine?.reminderHour ?: 8) }
    var reminderMinute by remember { mutableIntStateOf(initialMedicine?.reminderMinute ?: 0) }

    var isAm by remember {
        mutableStateOf(reminderHour < 12)
    }
    var displayHour12 by remember {
        val h = if (reminderHour % 12 == 0) 12 else reminderHour % 12
        mutableIntStateOf(h)
    }

    var repeatType by remember { mutableStateOf(initialMedicine?.repeatType ?: "Daily") }
    var specificDays by remember {
        mutableStateOf(
            if (initialMedicine?.specificDays.isNullOrBlank()) {
                setOf("Mon", "Wed", "Fri")
            } else {
                initialMedicine.specificDays.split(",").map { it.trim() }.toSet()
            }
        )
    }

    var soundEnabled by remember { mutableStateOf(initialMedicine?.soundEnabled ?: true) }
    var soundType by remember { mutableStateOf(initialMedicine?.soundType ?: MedSoundType.MEDICAL_CHIME.id) }
    var vibrate by remember { mutableStateOf(initialMedicine?.vibrate ?: true) }

    var nameError by remember { mutableStateOf(false) }

    val medicineTypes = listOf("Tablet", "Capsule", "Syrup", "Other")
    val repeatOptions = listOf("Once", "Daily", "Specific days")
    val daysOfWeek = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")

    val soundOptions = listOf(
        MedSoundType.MEDICAL_CHIME.id to "Medical Chime",
        MedSoundType.ALARM_BELL.id to "Alarm Bell",
        MedSoundType.DIGITAL_BEEP.id to "Digital Alert"
    )

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .heightIn(max = 680.dp)
                .testTag("add_edit_medicine_dialog")
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
                                .background(HealthEmerald.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Medication,
                                contentDescription = null,
                                tint = HealthEmerald,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = if (isEdit) "Edit Medicine" else "Add Medicine",
                                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Med reminder schedule",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.testTag("close_medicine_dialog_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // 1. Medicine Name
                Text(
                    text = "Medicine Name *",
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(4.dp))
                OutlinedTextField(
                    value = name,
                    onValueChange = {
                        name = it
                        if (it.isNotBlank()) nameError = false
                    },
                    placeholder = { Text("e.g., Paracetamol, Metformin, Vitamin D") },
                    isError = nameError,
                    supportingText = {
                        if (nameError) Text("Please enter medicine name", color = MaterialTheme.colorScheme.error)
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = HealthEmerald,
                        cursorColor = HealthEmerald
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("medicine_name_input")
                )

                Spacer(modifier = Modifier.height(12.dp))

                // 2. Dosage (optional)
                Text(
                    text = "Dosage (Optional)",
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(4.dp))
                OutlinedTextField(
                    value = dosage,
                    onValueChange = { dosage = it },
                    placeholder = { Text("e.g., 500 mg, 1 tablet, 5 ml") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = HealthEmerald,
                        cursorColor = HealthEmerald
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("medicine_dosage_input")
                )

                Spacer(modifier = Modifier.height(14.dp))

                // 3. Medicine Type: Tablet / Capsule / Syrup / Other
                Text(
                    text = "Medicine Type",
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(6.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    medicineTypes.forEach { t ->
                        val selected = type == t
                        FilterChip(
                            selected = selected,
                            onClick = { type = t },
                            label = {
                                Text(
                                    text = t,
                                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                                )
                            },
                            leadingIcon = if (selected) {
                                {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            } else null,
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = HealthEmerald.copy(alpha = 0.2f),
                                selectedLabelColor = HealthEmerald,
                                selectedLeadingIconColor = HealthEmerald
                            ),
                            modifier = Modifier.testTag("type_chip_$t")
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // 4. Reminder Time Selector (Elderly-Friendly Large Buttons)
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Schedule,
                                contentDescription = null,
                                tint = HealthBlue,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Reminder Time",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Hour, Minute, AM/PM selector
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Hour selector
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("Hour", style = MaterialTheme.typography.labelSmall)
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(MaterialTheme.colorScheme.surface)
                                        .padding(horizontal = 10.dp, vertical = 6.dp)
                                ) {
                                    Text(
                                        text = "%02d".format(displayHour12),
                                        fontSize = 24.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = HealthBlue
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column {
                                        Text(
                                            text = "▲",
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = HealthBlue,
                                            modifier = Modifier
                                                .clickable {
                                                    displayHour12 = if (displayHour12 >= 12) 1 else displayHour12 + 1
                                                }
                                                .padding(horizontal = 4.dp, vertical = 2.dp)
                                        )
                                        Text(
                                            text = "▼",
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = HealthBlue,
                                            modifier = Modifier
                                                .clickable {
                                                    displayHour12 = if (displayHour12 <= 1) 12 else displayHour12 - 1
                                                }
                                                .padding(horizontal = 4.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                            }

                            Text(
                                text = ":",
                                fontSize = 26.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 10.dp)
                            )

                            // Minute selector with direct custom input and single-minute steppers
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("Minute (0-59)", style = MaterialTheme.typography.labelSmall)
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(MaterialTheme.colorScheme.surface)
                                        .padding(horizontal = 10.dp, vertical = 6.dp)
                                ) {
                                    Text(
                                        text = "%02d".format(reminderMinute),
                                        fontSize = 24.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = HealthBlue
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column {
                                        Text(
                                            text = "▲",
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = HealthBlue,
                                            modifier = Modifier
                                                .clickable {
                                                    reminderMinute = (reminderMinute + 1) % 60
                                                }
                                                .padding(horizontal = 4.dp, vertical = 2.dp)
                                        )
                                        Text(
                                            text = "▼",
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = HealthBlue,
                                            modifier = Modifier
                                                .clickable {
                                                    reminderMinute = if (reminderMinute - 1 < 0) 59 else reminderMinute - 1
                                                }
                                                .padding(horizontal = 4.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.width(16.dp))

                            // AM/PM Switcher
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("AM/PM", style = MaterialTheme.typography.labelSmall)
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(MaterialTheme.colorScheme.surface)
                                        .padding(4.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(if (isAm) HealthBlue else Color.Transparent)
                                            .clickable { isAm = true }
                                            .padding(horizontal = 10.dp, vertical = 6.dp)
                                    ) {
                                        Text(
                                            text = "AM",
                                            fontWeight = FontWeight.Bold,
                                            color = if (isAm) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(if (!isAm) HealthBlue else Color.Transparent)
                                            .clickable { isAm = false }
                                            .padding(horizontal = 10.dp, vertical = 6.dp)
                                    ) {
                                        Text(
                                            text = "PM",
                                            fontWeight = FontWeight.Bold,
                                            color = if (!isAm) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Quick Time Presets
                        Text(
                            text = "Quick hour presets:",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            listOf(
                                "Morning (8 AM)" to Pair(8, true),
                                "Noon (1 PM)" to Pair(1, false),
                                "Night (8 PM)" to Pair(8, false)
                            ).forEach { (label, time) ->
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(HealthBlue.copy(alpha = 0.1f))
                                        .clickable {
                                            displayHour12 = time.first
                                            reminderMinute = 0
                                            isAm = time.second
                                        }
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = label,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = HealthBlue
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // 5. Repeat Options: Once / Daily / Specific days
                Text(
                    text = "Repeat Schedule",
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    repeatOptions.forEach { rep ->
                        val selected = repeatType == rep
                        FilterChip(
                            selected = selected,
                            onClick = { repeatType = rep },
                            label = {
                                Text(
                                    text = rep,
                                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = HealthEmerald.copy(alpha = 0.2f),
                                selectedLabelColor = HealthEmerald
                            ),
                            modifier = Modifier.testTag("repeat_chip_$rep")
                        )
                    }
                }

                if (repeatType == "Specific days") {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Select days:",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        daysOfWeek.forEach { day ->
                            val isSelected = specificDays.contains(day)
                            Box(
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .background(if (isSelected) HealthEmerald else MaterialTheme.colorScheme.surfaceVariant)
                                    .clickable {
                                        specificDays = if (isSelected) {
                                            if (specificDays.size > 1) specificDays - day else specificDays
                                        } else {
                                            specificDays + day
                                        }
                                    }
                                    .padding(horizontal = 10.dp, vertical = 6.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = day,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // 6. Sound & Vibration Settings
                Text(
                    text = "Alarm & Sound",
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.VolumeUp,
                            contentDescription = null,
                            tint = HealthEmerald,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Play Reminder Alarm Sound",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                    Switch(
                        checked = soundEnabled,
                        onCheckedChange = { soundEnabled = it },
                        colors = SwitchDefaults.colors(checkedThumbColor = HealthEmerald)
                    )
                }

                if (soundEnabled) {
                    Spacer(modifier = Modifier.height(6.dp))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        soundOptions.forEach { (id, title) ->
                            val selected = soundType == id
                            FilterChip(
                                selected = selected,
                                onClick = { soundType = id },
                                label = { Text(title, fontSize = 12.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = HealthEmerald.copy(alpha = 0.2f),
                                    selectedLabelColor = HealthEmerald
                                )
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Vibration,
                            contentDescription = null,
                            tint = HealthBlue,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Vibrate on Reminder",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                    Switch(
                        checked = vibrate,
                        onCheckedChange = { vibrate = it },
                        colors = SwitchDefaults.colors(checkedThumbColor = HealthBlue)
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Save & Delete Buttons (Large, Elderly-Friendly)
                Button(
                    onClick = {
                        if (name.isBlank()) {
                            nameError = true
                            return@Button
                        }
                        val computed24Hour = when {
                            isAm && displayHour12 == 12 -> 0
                            !isAm && displayHour12 < 12 -> displayHour12 + 12
                            else -> displayHour12
                        }

                        val medicineToSave = Medicine(
                            id = initialMedicine?.id ?: 0L,
                            name = name.trim(),
                            dosage = dosage.trim(),
                            type = type,
                            startDate = startDate,
                            reminderHour = computed24Hour,
                            reminderMinute = reminderMinute,
                            repeatType = repeatType,
                            specificDays = specificDays.joinToString(","),
                            soundEnabled = soundEnabled,
                            soundType = soundType,
                            vibrate = vibrate,
                            isActive = initialMedicine?.isActive ?: true,
                            createdAt = initialMedicine?.createdAt ?: System.currentTimeMillis()
                        )
                        onSave(medicineToSave)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = HealthEmerald),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("save_medicine_button")
                ) {
                    Text(
                        text = if (isEdit) "Update Medicine" else "Save Medicine",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = Color.White
                    )
                }

                if (isEdit && onDelete != null) {
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedButton(
                        onClick = { onDelete(initialMedicine) },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("delete_medicine_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Delete Medicine",
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}
