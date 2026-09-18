package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "medicines")
data class Medicine(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val dosage: String = "",
    val type: String = "Tablet", // Tablet, Capsule, Syrup, Other
    val startDate: String, // YYYY-MM-DD
    val reminderHour: Int = 8,
    val reminderMinute: Int = 0,
    val repeatType: String = "Daily", // Once, Daily, Specific days
    val specificDays: String = "", // e.g. "Mon, Wed, Fri"
    val soundEnabled: Boolean = true,
    val soundType: String = "MEDICAL_CHIME", // MEDICAL_CHIME, ALARM_BELL, DIGITAL_BEEP, SYSTEM_RINGTONE
    val vibrate: Boolean = true,
    val isActive: Boolean = true,
    val createdAt: Long = System.currentTimeMillis()
)
