package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "workout_sessions")
data class WorkoutSession(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val date: String, // format YYYY-MM-DD
    val startTime: Long,
    val endTime: Long,
    val durationSeconds: Long,
    val distanceMeters: Float,
    val caloriesBurned: Int,
    val avgSpeedKmh: Float,
    val workoutType: String = "Walking",
    val routePointsJson: String = "[]" // JSON string of RouteCoordinates
)
