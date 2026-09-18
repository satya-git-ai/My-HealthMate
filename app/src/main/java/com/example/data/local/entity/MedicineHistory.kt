package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "medicine_history")
data class MedicineHistory(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val medicineId: Long,
    val medicineName: String,
    val dosage: String = "",
    val type: String = "Tablet",
    val scheduledTime: String, // e.g. "08:00 AM"
    val status: String, // "Taken", "Snoozed", "Skipped"
    val date: String, // YYYY-MM-DD
    val timestamp: Long = System.currentTimeMillis()
)
