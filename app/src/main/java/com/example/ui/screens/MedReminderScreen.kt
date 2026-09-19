package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Medication
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Snooze
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entity.Medicine
import com.example.notification.MedicineReminderScheduler
import com.example.ui.components.AddEditMedicineDialog
import com.example.ui.components.MedicineReminderAlertDialog
import com.example.ui.theme.HealthBlue
import com.example.ui.theme.HealthCyan
import com.example.ui.theme.HealthEmerald
import com.example.ui.theme.HealthOrange
import com.example.ui.viewmodel.HealthViewModel

@Composable
fun MedReminderScreen(
    viewModel: HealthViewModel,
    onBack: () -> Unit,
    onNavigateHistory: () -> Unit,
    onHome: () -> Unit = onBack,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val medicines by viewModel.allMedicines.collectAsState()
    val todayHistory by viewModel.todayMedicineHistory.collectAsState()
    val isAlarmPlaying by viewModel.isAlarmPlaying.collectAsState()

    var showAddDialog by remember { mutableStateOf(false) }
    var medicineToEdit by remember { mutableStateOf<Medicine?>(null) }
    var activeReminderMedicine by remember { mutableStateOf<Medicine?>(null) }
    var medicineToDelete by remember { mutableStateOf<Medicine?>(null) }

    val takenMedicineIds = remember(todayHistory) {
        todayHistory.filter { it.status == "Taken" }.map { it.medicineId }.toSet()
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = HealthEmerald,
                contentColor = Color.White,
                shape = CircleShape,
                modifier = Modifier.testTag("fab_add_medicine")
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Add Medicine",
                    modifier = Modifier.size(28.dp)
                )
            }
        },
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding()
            .testTag("med_reminder_screen")
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(innerPadding)
        ) {
            // Top Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.testTag("med_reminder_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = MaterialTheme.colorScheme.onBackground
                        )
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Column {
                        Text(
                            text = "Medicine",
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Text(
                            text = "Daily medication schedule & reminders",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (isAlarmPlaying) {
                        IconButton(
                            onClick = { viewModel.stopAlarmSound() },
                            modifier = Modifier.testTag("med_stop_sound_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Stop,
                                contentDescription = "Stop Alarm Sound",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                    IconButton(
                        onClick = onNavigateHistory,
                        modifier = Modifier.testTag("med_history_nav_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.History,
                            contentDescription = "Medication History",
                            tint = HealthBlue
                        )
                    }
                    IconButton(
                        onClick = onHome,
                        modifier = Modifier.testTag("med_home_nav_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Home,
                            contentDescription = "Home",
                            tint = HealthEmerald
                        )
                    }
                }
            }

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 20.dp)
            ) {
                // Prominent Add Medicine Button (Elderly friendly large CTA)
                item {
                    Button(
                        onClick = { showAddDialog = true },
                        shape = RoundedCornerShape(18.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = HealthEmerald),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                            .testTag("prominent_add_medicine_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Add medicine",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = Color.White
                            )
                            Spacer(modifier = Modifier.width(5.dp))
                            Text(
                                text = "to remember",
                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                                color = Color.White.copy(alpha = 0.9f)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }

                // Today's Status Banner Card
                item {
                    Card(
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("med_status_summary_card")
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(18.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Today's Schedule",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                val total = medicines.filter { it.isActive }.size
                                val taken = takenMedicineIds.size
                                Text(
                                    text = if (total == 0) "No active medicines scheduled" else "$taken of $total taken today",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(HealthEmerald.copy(alpha = 0.15f))
                                    .clickable { onNavigateHistory() }
                                    .padding(horizontal = 12.dp, vertical = 8.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.History,
                                        contentDescription = null,
                                        tint = HealthEmerald,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "History",
                                        fontWeight = FontWeight.Bold,
                                        color = HealthEmerald,
                                        fontSize = 13.sp
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(18.dp))
                }

                // Section Title
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "My Medicines (${medicines.size})",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Tap to edit or record",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                }

                // Empty State
                if (medicines.isEmpty()) {
                    item {
                        Card(
                            shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 24.dp)
                                .testTag("empty_medicines_card")
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(28.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(64.dp)
                                        .clip(CircleShape)
                                        .background(HealthEmerald.copy(alpha = 0.15f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Medication,
                                        contentDescription = null,
                                        tint = HealthEmerald,
                                        modifier = Modifier.size(36.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.height(14.dp))
                                Text(
                                    text = "No medicines added yet",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "Never miss your prescription. Tap \"Add Medicine\" to set your daily alarms and reminders.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(horizontal = 8.dp)
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Button(
                                    onClick = { showAddDialog = true },
                                    colors = ButtonDefaults.buttonColors(containerColor = HealthEmerald),
                                    shape = RoundedCornerShape(14.dp)
                                ) {
                                    Text("Add Your First Medicine")
                                }
                            }
                        }
                    }
                } else {
                    // Medicines List
                    items(medicines, key = { it.id }) { medicine ->
                        val isTakenToday = takenMedicineIds.contains(medicine.id)
                        MedicineCardItem(
                            medicine = medicine,
                            isTakenToday = isTakenToday,
                            onEdit = { medicineToEdit = medicine },
                            onDelete = { medicineToDelete = medicine },
                            onToggleActive = { viewModel.toggleMedicineActive(medicine) },
                            onTakeNow = {
                                val timeStr = MedicineReminderScheduler.formatTime12Hour(
                                    medicine.reminderHour,
                                    medicine.reminderMinute
                                )
                                viewModel.recordMedicineAction(
                                    medicineId = medicine.id,
                                    medicineName = medicine.name,
                                    dosage = medicine.dosage,
                                    type = medicine.type,
                                    scheduledTime = timeStr,
                                    action = "Taken"
                                )
                                val msg = if (medicine.repeatType.equals("Once", ignoreCase = true)) {
                                    "${medicine.name} marked as taken and completed (disabled)!"
                                } else {
                                    "${medicine.name} marked as taken!"
                                }
                                Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                            },
                            onTestReminder = {
                                activeReminderMedicine = medicine
                                viewModel.testMedicineReminder(medicine)
                            }
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(80.dp))
                }
            }
        }
    }

    // Add Medicine Dialog
    if (showAddDialog) {
        AddEditMedicineDialog(
            initialMedicine = null,
            onDismiss = { showAddDialog = false },
            onSave = { newMed ->
                viewModel.saveMedicine(newMed)
                showAddDialog = false
                Toast.makeText(context, "Medicine reminder added!", Toast.LENGTH_SHORT).show()
            }
        )
    }

    // Edit Medicine Dialog
    if (medicineToEdit != null) {
        AddEditMedicineDialog(
            initialMedicine = medicineToEdit,
            onDismiss = { medicineToEdit = null },
            onSave = { updatedMed ->
                viewModel.saveMedicine(updatedMed)
                medicineToEdit = null
                Toast.makeText(context, "Medicine updated!", Toast.LENGTH_SHORT).show()
            },
            onDelete = { med ->
                viewModel.deleteMedicine(med)
                medicineToEdit = null
                Toast.makeText(context, "Medicine deleted", Toast.LENGTH_SHORT).show()
            }
        )
    }

    // Confirm Delete Dialog
    if (medicineToDelete != null) {
        val med = medicineToDelete!!
        AlertDialog(
            onDismissRequest = { medicineToDelete = null },
            title = { Text("Delete ${med.name}?") },
            text = { Text("Are you sure you want to remove this medicine and cancel its scheduled reminders?") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteMedicine(med)
                        medicineToDelete = null
                        Toast.makeText(context, "Medicine deleted", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { medicineToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Active Reminder Interactive Alert Dialog
    if (activeReminderMedicine != null) {
        val activeMed = activeReminderMedicine!!
        val scheduledTime = MedicineReminderScheduler.formatTime12Hour(
            activeMed.reminderHour,
            activeMed.reminderMinute
        )
        MedicineReminderAlertDialog(
            medicine = activeMed,
            scheduledTimeString = scheduledTime,
            onTaken = {
                viewModel.recordMedicineAction(
                    medicineId = activeMed.id,
                    medicineName = activeMed.name,
                    dosage = activeMed.dosage,
                    type = activeMed.type,
                    scheduledTime = scheduledTime,
                    action = "Taken"
                )
                activeReminderMedicine = null
                Toast.makeText(context, "Recorded: Taken ${activeMed.name}", Toast.LENGTH_SHORT).show()
            },
            onSnooze = {
                viewModel.recordMedicineAction(
                    medicineId = activeMed.id,
                    medicineName = activeMed.name,
                    dosage = activeMed.dosage,
                    type = activeMed.type,
                    scheduledTime = scheduledTime,
                    action = "Snoozed"
                )
                activeReminderMedicine = null
                Toast.makeText(context, "Snoozed 10 minutes", Toast.LENGTH_SHORT).show()
            },
            onSkip = {
                viewModel.recordMedicineAction(
                    medicineId = activeMed.id,
                    medicineName = activeMed.name,
                    dosage = activeMed.dosage,
                    type = activeMed.type,
                    scheduledTime = scheduledTime,
                    action = "Skipped"
                )
                activeReminderMedicine = null
                Toast.makeText(context, "Skipped dose", Toast.LENGTH_SHORT).show()
            }
        )
    }
}

@Composable
fun MedicineCardItem(
    medicine: Medicine,
    isTakenToday: Boolean,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onToggleActive: () -> Unit,
    onTakeNow: () -> Unit,
    onTestReminder: () -> Unit
) {
    val reminderTimeString = MedicineReminderScheduler.formatTime12Hour(
        medicine.reminderHour,
        medicine.reminderMinute
    )

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (medicine.isActive) {
                MaterialTheme.colorScheme.surface
            } else {
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
            }
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (medicine.isActive) 2.dp else 0.dp),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("medicine_card_${medicine.id}")
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            // Header: Name, Dosage, Type, and Active Switch
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .clip(CircleShape)
                            .background(
                                if (medicine.isActive) HealthEmerald.copy(alpha = 0.15f)
                                else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Medication,
                            contentDescription = null,
                            tint = if (medicine.isActive) HealthEmerald else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(26.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = medicine.name,
                                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        Text(
                            text = "${medicine.type}${if (medicine.dosage.isNotBlank()) " • ${medicine.dosage}" else ""}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Switch(
                    checked = medicine.isActive,
                    onCheckedChange = { onToggleActive() },
                    colors = SwitchDefaults.colors(checkedThumbColor = HealthEmerald),
                    modifier = Modifier.testTag("switch_med_active_${medicine.id}")
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Timing & Schedule Badges (Hide time badge if inactive)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Time badge: only shown if medicine is active
                if (medicine.isActive) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(HealthBlue.copy(alpha = 0.12f))
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Schedule,
                                contentDescription = null,
                                tint = HealthBlue,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = reminderTimeString,
                                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                color = HealthBlue
                            )
                        }
                    }
                }

                // Repeat Schedule badge
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Repeat,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        val repText = if (medicine.repeatType == "Specific days" && medicine.specificDays.isNotBlank()) {
                            medicine.specificDays
                        } else {
                            medicine.repeatType
                        }
                        Text(
                            text = repText,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Action Buttons: Taken check, Edit, Delete, Test
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // "Taken" button (Active green when active & pending, disabled/greyed out if inactive or already taken today)
                val isButtonDisabled = !medicine.isActive || isTakenToday
                Button(
                    onClick = onTakeNow,
                    enabled = !isButtonDisabled,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = HealthEmerald,
                        contentColor = Color.White,
                        disabledContainerColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f),
                        disabledContentColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                    ),
                    modifier = Modifier
                        .height(42.dp)
                        .testTag("btn_take_medicine_${medicine.id}")
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        tint = if (isButtonDisabled) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f) else Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Taken",
                        fontWeight = FontWeight.Bold,
                        color = if (isButtonDisabled) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f) else Color.White,
                        fontSize = 13.sp
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Test reminder trigger (Alarm bell): only shown if medicine is active
                    if (medicine.isActive) {
                        IconButton(
                            onClick = onTestReminder,
                            modifier = Modifier.testTag("btn_test_reminder_${medicine.id}")
                        ) {
                            Icon(
                                imageVector = Icons.Default.NotificationsActive,
                                contentDescription = "Test Reminder",
                                tint = HealthBlue
                            )
                        }
                    }

                    // Edit button
                    IconButton(
                        onClick = onEdit,
                        modifier = Modifier.testTag("btn_edit_medicine_${medicine.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Edit Medicine",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // Delete button
                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.testTag("btn_delete_medicine_${medicine.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete Medicine",
                            tint = MaterialTheme.colorScheme.error.copy(alpha = 0.8f)
                        )
                    }
                }
            }
        }
    }
}
