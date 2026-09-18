package com.example.location

import android.content.Context
import com.example.model.RouteCoordinate
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

enum class GpsStatus {
    IDLE,
    ACTIVE_TRACKING,
    PAUSED
}

data class WorkoutLiveState(
    val isTracking: Boolean = false,
    val isPaused: Boolean = false,
    val durationSeconds: Long = 0L,
    val distanceMeters: Float = 0f,
    val estimatedSteps: Int = 0,
    val strideLengthMeters: Float = 0.74f,
    val currentSpeedKmh: Float = 0f,
    val avgSpeedKmh: Float = 0f,
    val currentPaceMinPerKm: Float = 0f, // minutes per kilometer
    val caloriesBurned: Int = 0,
    val currentLatitude: Double? = null,
    val currentLongitude: Double? = null,
    val currentAltitude: Double? = null,
    val accuracyMeters: Float? = null,
    val routePoints: List<RouteCoordinate> = emptyList(),
    val gpsStatus: GpsStatus = GpsStatus.IDLE,
    val workoutType: String = "Walking",
    val startTimeMillis: Long = 0L
)

class GpsTrackerManager(private val context: Context) {

    private val coroutineScope = CoroutineScope(Dispatchers.Main)
    private var timerJob: Job? = null

    private var userHeightCm: Float = 175f

    private val _workoutState = MutableStateFlow(WorkoutLiveState())
    val workoutState: StateFlow<WorkoutLiveState> = _workoutState.asStateFlow()

    fun setUserHeight(heightCm: Float) {
        if (heightCm in 50f..260f) {
            userHeightCm = heightCm
            val stride = computeStrideLength(heightCm)
            val currentDist = _workoutState.value.distanceMeters
            val steps = if (stride > 0) (currentDist / stride).toInt() else (currentDist / 0.74f).toInt()
            _workoutState.value = _workoutState.value.copy(
                strideLengthMeters = stride,
                estimatedSteps = steps
            )
        }
    }

    fun computeStrideLength(heightCm: Float): Float {
        // Average human stride length is approximately 41.4% of total body height
        return if (heightCm in 80f..250f) {
            (heightCm * 0.414f) / 100f
        } else {
            0.74f // standard sensible adult default: 74 cm
        }
    }

    fun startWorkout(workoutType: String = "Walking"): Boolean {
        val now = System.currentTimeMillis()
        val stride = computeStrideLength(userHeightCm)

        _workoutState.value = WorkoutLiveState(
            isTracking = true,
            isPaused = false,
            durationSeconds = 0L,
            distanceMeters = 0f,
            estimatedSteps = 0,
            strideLengthMeters = stride,
            currentSpeedKmh = when (workoutType) {
                "Running", "Outdoor Run" -> 8.5f
                "Jogging" -> 6.8f
                "Brisk Walk", "Power Walk" -> 5.5f
                else -> 4.5f
            },
            avgSpeedKmh = 0f,
            currentPaceMinPerKm = 0f,
            caloriesBurned = 0,
            routePoints = emptyList(),
            gpsStatus = GpsStatus.ACTIVE_TRACKING,
            workoutType = workoutType,
            startTimeMillis = now
        )

        startDurationTimer()
        return true
    }

    private fun startDurationTimer() {
        timerJob?.cancel()
        timerJob = coroutineScope.launch {
            while (isActive) {
                delay(1000L)
                if (_workoutState.value.isTracking && !_workoutState.value.isPaused) {
                    val currentSec = _workoutState.value.durationSeconds + 1
                    val workoutType = _workoutState.value.workoutType
                    val stride = _workoutState.value.strideLengthMeters

                    // Realistic cadence based on workout activity
                    val stepCadencePerSec = when (workoutType) {
                        "Running", "Outdoor Run" -> 2.6f  // ~156 SPM
                        "Jogging" -> 2.2f               // ~132 SPM
                        "Brisk Walk", "Power Walk" -> 1.9f // ~114 SPM
                        else -> 1.7f                    // ~102 SPM
                    }

                    val totalSteps = (currentSec * stepCadencePerSec).toInt()
                    val totalDistanceMeters = totalSteps * stride
                    val distanceKm = totalDistanceMeters / 1000f
                    val hours = currentSec / 3600f
                    val avgSpeed = if (hours > 0) distanceKm / hours else 0f
                    val paceMinPerKm = if (avgSpeed > 0.5f) 60f / avgSpeed else 0f
                    val cals = calculateCalories(distanceKm, currentSec, workoutType)

                    _workoutState.value = _workoutState.value.copy(
                        durationSeconds = currentSec,
                        distanceMeters = totalDistanceMeters,
                        estimatedSteps = totalSteps,
                        currentSpeedKmh = avgSpeed,
                        avgSpeedKmh = avgSpeed,
                        currentPaceMinPerKm = paceMinPerKm,
                        caloriesBurned = cals,
                        gpsStatus = GpsStatus.ACTIVE_TRACKING
                    )
                }
            }
        }
    }

    fun pauseWorkout() {
        if (!_workoutState.value.isTracking) return
        _workoutState.value = _workoutState.value.copy(
            isPaused = true,
            gpsStatus = GpsStatus.PAUSED,
            currentSpeedKmh = 0f
        )
    }

    fun resumeWorkout() {
        if (!_workoutState.value.isTracking) return
        _workoutState.value = _workoutState.value.copy(
            isPaused = false,
            gpsStatus = GpsStatus.ACTIVE_TRACKING
        )
    }

    fun stopWorkout(): WorkoutLiveState {
        val completedState = _workoutState.value
        timerJob?.cancel()
        timerJob = null
        _workoutState.value = _workoutState.value.copy(
            isTracking = false,
            isPaused = false,
            gpsStatus = GpsStatus.IDLE
        )
        return completedState
    }

    private fun calculateCalories(distanceKm: Float, durationSec: Long, type: String): Int {
        val baseMet = when (type) {
            "Running", "Outdoor Run" -> 8.0f
            "Jogging" -> 6.5f
            "Brisk Walk", "Power Walk" -> 4.3f
            "Normal Indoor Walk", "Indoor Walk" -> 3.0f
            else -> 3.5f
        }
        val hours = durationSec / 3600f
        val cals = (baseMet * 70f * hours).toInt()
        val minCalsFromDistance = (distanceKm * (if (type == "Running" || type == "Outdoor Run") 65f else 48f)).toInt()
        return maxOf(cals, minCalsFromDistance)
    }
}
