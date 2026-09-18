package com.example.sensor

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorManager
import android.os.Build
import androidx.core.content.ContextCompat
import com.example.service.StepCounterService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

enum class SensorTypeUsed {
    HARDWARE_STEP_COUNTER,
    HARDWARE_STEP_DETECTOR,
    SIMULATOR_TEST,
    NONE_AVAILABLE
}

enum class SensorSensitivity {
    LOW,
    MEDIUM,
    HIGH
}

data class StepSensorState(
    val sessionSteps: Int = 0,
    val isTracking: Boolean = true,
    val isSessionActive: Boolean = false,
    val sensorTypeUsed: SensorTypeUsed = SensorTypeUsed.HARDWARE_STEP_COUNTER,
    val isSensorAvailable: Boolean = true,
    val sensorName: String = "Hardware Step Pedometer",
    val currentSpm: Int = 0, // Live steps per minute (cadence)
    val sensitivity: SensorSensitivity = SensorSensitivity.LOW,
    val isSimulatingWalk: Boolean = false,
    val lastStepTimestamp: Long = 0L,
    val errorMessage: String? = null
)

/**
 * Pure hardware pedometer sensor manager that interfaces with StepCounterService.
 * Replaced all Sensor.TYPE_ACCELEROMETER custom threshold math with built-in hardware pedometer.
 */
class StepSensorManager(
    private val context: Context,
    private val onStepCountIncremented: (Int) -> Unit
) {

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
    private val coroutineScope = CoroutineScope(Dispatchers.Main)
    private val stepDataManager = StepDataManager.getInstance(context)

    private val _sensorState = MutableStateFlow(StepSensorState())
    val sensorState: StateFlow<StepSensorState> = _sensorState.asStateFlow()

    private var stateSyncJob: Job? = null

    init {
        detectHardwareSensors()
        observeServiceFlows()
    }

    private fun detectHardwareSensors() {
        val hasStepCounter = sensorManager?.getDefaultSensor(Sensor.TYPE_STEP_COUNTER) != null
        val hasStepDetector = sensorManager?.getDefaultSensor(Sensor.TYPE_STEP_DETECTOR) != null

        val detectedType = when {
            hasStepCounter -> SensorTypeUsed.HARDWARE_STEP_COUNTER
            hasStepDetector -> SensorTypeUsed.HARDWARE_STEP_DETECTOR
            else -> SensorTypeUsed.NONE_AVAILABLE
        }

        val displayName = when {
            hasStepCounter -> sensorManager?.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)?.name ?: "Hardware Step Counter"
            hasStepDetector -> sensorManager?.getDefaultSensor(Sensor.TYPE_STEP_DETECTOR)?.name ?: "Hardware Step Detector"
            else -> "Hardware Pedometer (Simulated on Cloud Emulator)"
        }

        _sensorState.value = _sensorState.value.copy(
            isSensorAvailable = hasStepCounter || hasStepDetector,
            sensorTypeUsed = detectedType,
            sensorName = displayName,
            sensitivity = SensorSensitivity.LOW,
            sessionSteps = stepDataManager.getTodayTotalSteps()
        )
    }

    private fun observeServiceFlows() {
        stateSyncJob?.cancel()
        stateSyncJob = coroutineScope.launch {
            launch {
                StepCounterService.liveStepsFlow.collect { steps ->
                    if (steps != null) {
                        _sensorState.value = _sensorState.value.copy(
                            sessionSteps = steps,
                            lastStepTimestamp = System.currentTimeMillis()
                        )
                        onStepCountIncremented(steps)
                    }
                }
            }
            launch {
                StepCounterService.liveSpmFlow.collect { spm ->
                    _sensorState.value = _sensorState.value.copy(currentSpm = spm)
                }
            }
            launch {
                StepCounterService.sensorTypeFlow.collect { type ->
                    _sensorState.value = _sensorState.value.copy(sensorTypeUsed = type)
                }
            }
            launch {
                StepCounterService.isSimulatingFlow.collect { isSim ->
                    _sensorState.value = _sensorState.value.copy(isSimulatingWalk = isSim)
                }
            }
        }
    }

    fun startListening() {
        val hasPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACTIVITY_RECOGNITION
            ) == PackageManager.PERMISSION_GRANTED
        } else true

        if (!hasPermission) {
            _sensorState.value = _sensorState.value.copy(
                isTracking = false,
                sensorTypeUsed = SensorTypeUsed.NONE_AVAILABLE
            )
            return
        }

        _sensorState.value = _sensorState.value.copy(isTracking = true)
        StepCounterService.startService(context)
    }

    fun pauseListening() {
        _sensorState.value = _sensorState.value.copy(isTracking = false, currentSpm = 0)
        StepCounterService.stopService(context)
    }

    fun resumeListening() {
        startListening()
    }

    fun setSensitivity(sensitivity: SensorSensitivity = SensorSensitivity.LOW) {
        _sensorState.value = _sensorState.value.copy(sensitivity = SensorSensitivity.LOW)
    }

    fun resetSessionSteps() {
        _sensorState.value = _sensorState.value.copy(
            sessionSteps = 0,
            currentSpm = 0
        )
        StepCounterService.resetManualSteps(context)
    }

    fun startWalkSimulator(targetSpm: Int = 110) {
        _sensorState.value = _sensorState.value.copy(
            isSimulatingWalk = true,
            isSessionActive = true,
            isTracking = true,
            sensorTypeUsed = SensorTypeUsed.SIMULATOR_TEST
        )
        StepCounterService.startWalkSimulation(context, targetSpm)
    }

    fun stopWalkSimulator() {
        _sensorState.value = _sensorState.value.copy(
            isSimulatingWalk = false,
            isSessionActive = false,
            currentSpm = 0
        )
        detectHardwareSensors()
        StepCounterService.stopWalkSimulation(context)
    }

    fun toggleWalkSimulator() {
        if (_sensorState.value.isSimulatingWalk) {
            stopWalkSimulator()
        } else {
            startWalkSimulator(110)
        }
    }

    fun triggerSingleStep() {
        StepCounterService.addManualSteps(context, 1)
    }
}
