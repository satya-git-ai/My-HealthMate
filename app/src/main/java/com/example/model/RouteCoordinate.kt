package com.example.model

data class RouteCoordinate(
    val latitude: Double,
    val longitude: Double,
    val timestamp: Long = System.currentTimeMillis(),
    val speedMps: Float = 0f,
    val altitude: Double = 0.0
)
