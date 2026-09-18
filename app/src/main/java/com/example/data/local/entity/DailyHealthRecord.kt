package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "daily_health_records")
data class DailyHealthRecord(
    @PrimaryKey
    val date: String, // format YYYY-MM-DD
    val steps: Int = 0,
    val waterMl: Int = 0,
    val caloriesBurned: Int = 0,
    val distanceMeters: Float = 0f,
    val activeMinutes: Int = 0,
    val lastUpdated: Long = System.currentTimeMillis()
)
