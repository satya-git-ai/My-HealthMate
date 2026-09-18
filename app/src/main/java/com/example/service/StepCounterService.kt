package com.example.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import com.example.MainActivity
import com.example.data.local.HealthDatabase
import com.example.data.local.entity.DailyHealthRecord
import com.example.data.preferences.UserPreferences
import com.example.sensor.SensorTypeUsed
import com.example.sensor.StepDataManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.ArrayDeque

class StepCounterService : Service(), SensorEventListener {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var sensorManager: SensorManager? = null
    private var stepCounterSensor: Sensor? = null
    private var stepDetectorSensor: Sensor? = null

    private lateinit var stepDataManager: StepDataManager
    private lateinit var userPreferences: UserPreferences
    private lateinit var healthDatabase: HealthDatabase

    private val recentStepTimes = ArrayDeque<Long>(15)
    private var liveCadenceSpm: Int = 0
    private var spmResetJob: Job? = null
    private var dbSyncJob: Job? = null
    private var walkSimulatorJob: Job? = null

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "StepCounterService onCreate")
        stepDataManager = StepDataManager.getInstance(this)
        userPreferences = UserPreferences(this)
        healthDatabase = HealthDatabase.getDatabase(this)
        sensorManager = getSystemService(Context.SENSOR_SERVICE) as? SensorManager

        createNotificationChannel()

        // Discover sensors
        stepCounterSensor = sensorManager?.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)
        stepDetectorSensor = sensorManager?.getDefaultSensor(Sensor.TYPE_STEP_DETECTOR)

        val activeType = when {
            stepCounterSensor != null -> SensorTypeUsed.HARDWARE_STEP_COUNTER
            stepDetectorSensor != null -> SensorTypeUsed.HARDWARE_STEP_DETECTOR
            else -> SensorTypeUsed.NONE_AVAILABLE
        }
        _sensorTypeFlow.value = activeType

        // Start foreground immediately
        val initialSteps = stepDataManager.getTodayTotalSteps()
        _liveStepsFlow.value = initialSteps
        val notification = buildForegroundNotification(initialSteps)
        startInForeground(notification)

        registerSensors()
    }

    private fun startInForeground(notification: Notification) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                ServiceCompat.startForeground(
                    this,
                    NOTIFICATION_ID,
                    notification,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_HEALTH
                )
            } else {
                startForeground(NOTIFICATION_ID, notification)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to startForeground", e)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action ?: ACTION_START
        Log.d(TAG, "StepCounterService onStartCommand action: $action")

        when (action) {
            ACTION_START -> {
                registerSensors()
                val currentSteps = stepDataManager.getTodayTotalSteps()
                _liveStepsFlow.value = currentSteps
                updateNotification(currentSteps)
            }
            ACTION_STOP -> {
                unregisterSensors()
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
                return START_NOT_STICKY
            }
            ACTION_RESET_MIDNIGHT -> {
                val resetSteps = stepDataManager.performMidnightReset()
                _liveStepsFlow.value = resetSteps
                updateNotification(resetSteps)
                serviceScope.launch {
                    syncStepsToDatabase(resetSteps)
                }
            }
            ACTION_MANUAL_ADD -> {
                val addCount = intent?.getIntExtra(EXTRA_STEP_COUNT, 0) ?: 0
                if (addCount > 0) {
                    val newTotal = stepDataManager.addManualSteps(addCount)
                    _liveStepsFlow.value = newTotal
                    updateNotification(newTotal)
                    serviceScope.launch {
                        syncStepsToDatabase(newTotal)
                    }
                }
            }
            ACTION_RESET_MANUAL -> {
                val resetTotal = stepDataManager.resetTodayStepsManually()
                _liveStepsFlow.value = resetTotal
                updateNotification(resetTotal)
                serviceScope.launch {
                    syncStepsToDatabase(resetTotal)
                }
            }
            ACTION_SIMULATE_WALK_START -> {
                val spm = intent?.getIntExtra(EXTRA_SPM, 110) ?: 110
                startWalkSimulator(spm)
            }
            ACTION_SIMULATE_WALK_STOP -> {
                stopWalkSimulator()
            }
        }

        return START_STICKY
    }

    private fun registerSensors() {
        val sm = sensorManager ?: return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val hasPerm = androidx.core.content.ContextCompat.checkSelfPermission(
                this,
                android.Manifest.permission.ACTIVITY_RECOGNITION
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED
            if (!hasPerm) {
                Log.d(TAG, "Sensor registration pending: ACTIVITY_RECOGNITION permission is not yet granted.")
                _sensorTypeFlow.value = SensorTypeUsed.NONE_AVAILABLE
                return
            }
        }
        if (stepCounterSensor != null) {
            sm.registerListener(this, stepCounterSensor, SensorManager.SENSOR_DELAY_NORMAL)
            Log.d(TAG, "Registered hardware pedometer: Sensor.TYPE_STEP_COUNTER")
        } else if (stepDetectorSensor != null) {
            sm.registerListener(this, stepDetectorSensor, SensorManager.SENSOR_DELAY_NORMAL)
            Log.d(TAG, "Registered fallback sensor: Sensor.TYPE_STEP_DETECTOR")
        }
    }

    private fun unregisterSensors() {
        try {
            sensorManager?.unregisterListener(this)
        } catch (e: Exception) {
            Log.e(TAG, "Error unregistering sensor listener", e)
        }
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event == null) return

        val now = System.currentTimeMillis()

        when (event.sensor.type) {
            Sensor.TYPE_STEP_COUNTER -> {
                // event.values[0] is the total number of steps since last device boot
                val rawHardwareSteps = event.values[0].toLong()
                val todaySteps = stepDataManager.processHardwareStepEvent(rawHardwareSteps)

                recordStepCadence(now)
                _liveStepsFlow.value = todaySteps
                updateNotification(todaySteps)
                debouncedDatabaseSync(todaySteps)
            }

            Sensor.TYPE_STEP_DETECTOR -> {
                // Secondary fallback only if TYPE_STEP_COUNTER is absent on device
                if (stepCounterSensor == null && event.values.isNotEmpty() && event.values[0] >= 1.0f) {
                    val todaySteps = stepDataManager.addManualSteps(1)
                    recordStepCadence(now)
                    _liveStepsFlow.value = todaySteps
                    updateNotification(todaySteps)
                    debouncedDatabaseSync(todaySteps)
                }
            }
        }
    }

    private fun recordStepCadence(now: Long) {
        recentStepTimes.addLast(now)
        while (recentStepTimes.size > 10) {
            recentStepTimes.removeFirst()
        }

        if (recentStepTimes.size >= 3) {
            val oldest = recentStepTimes.first()
            val newest = recentStepTimes.last()
            val diffMs = newest - oldest
            if (diffMs in 500..15000) {
                liveCadenceSpm = ((recentStepTimes.size - 1) * 60000L / diffMs).toInt().coerceIn(40, 220)
                _liveSpmFlow.value = liveCadenceSpm
            }
        }

        spmResetJob?.cancel()
        spmResetJob = serviceScope.launch {
            delay(3500L)
            _liveSpmFlow.value = 0
        }
    }

    private fun debouncedDatabaseSync(todaySteps: Int) {
        dbSyncJob?.cancel()
        dbSyncJob = serviceScope.launch {
            delay(300L)
            syncStepsToDatabase(todaySteps)
        }
    }

    private suspend fun syncStepsToDatabase(todaySteps: Int) {
        try {
            val todayDate = stepDataManager.getTodayDate()
            val dao = healthDatabase.healthDao()
            val existing = dao.getDailyRecordSync(todayDate)

            val distanceMeters = todaySteps * 0.762f
            val caloriesBurned = (todaySteps * 0.04f).toInt()
            val activeMinutes = maxOf(0, todaySteps / 100)

            val record = if (existing != null) {
                existing.copy(
                    steps = todaySteps,
                    caloriesBurned = caloriesBurned,
                    distanceMeters = distanceMeters,
                    activeMinutes = activeMinutes,
                    lastUpdated = System.currentTimeMillis()
                )
            } else {
                DailyHealthRecord(
                    date = todayDate,
                    steps = todaySteps,
                    caloriesBurned = caloriesBurned,
                    distanceMeters = distanceMeters,
                    activeMinutes = activeMinutes,
                    lastUpdated = System.currentTimeMillis()
                )
            }
            dao.insertOrUpdateDailyRecord(record)
        } catch (e: Exception) {
            Log.e(TAG, "Error syncing steps to Room database", e)
        }
    }

    private fun buildForegroundNotification(steps: Int): Notification {
        val goal = userPreferences.settings.value.stepGoal.coerceAtLeast(1000)
        val distanceKm = (steps * 0.762f) / 1000f
        val calories = (steps * 0.04f).toInt()
        val percent = ((steps.toFloat() / goal) * 100).toInt().coerceIn(0, 100)

        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val title = "Today's Steps: %,d".format(steps)
        val text = "Goal: %,d steps (%d%%) • %.2f km • %d kcal".format(goal, percent, distanceKm, calories)

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(text)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setProgress(goal, steps.coerceAtMost(goal), false)
            .setContentIntent(pendingIntent)
            .build()
    }

    private fun updateNotification(steps: Int) {
        try {
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            val notification = buildForegroundNotification(steps)
            notificationManager?.notify(NOTIFICATION_ID, notification)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to update foreground notification", e)
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Real-time Step Pedometer",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Persistent foreground step tracking counter and goal progress"
                setShowBadge(false)
                enableVibration(false)
                enableLights(false)
            }
            val nm = getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            nm?.createNotificationChannel(channel)
        }
    }

    private fun startWalkSimulator(targetSpm: Int) {
        walkSimulatorJob?.cancel()
        _isSimulatingFlow.value = true
        _sensorTypeFlow.value = SensorTypeUsed.SIMULATOR_TEST
        val intervalMs = (60000L / targetSpm.coerceIn(60, 180)).coerceIn(333L, 1000L)
        walkSimulatorJob = serviceScope.launch {
            while (isActive && _isSimulatingFlow.value) {
                delay(intervalMs)
                val newTotal = stepDataManager.addManualSteps(1)
                _liveStepsFlow.value = newTotal
                _liveSpmFlow.value = targetSpm
                updateNotification(newTotal)
                debouncedDatabaseSync(newTotal)
            }
        }
    }

    private fun stopWalkSimulator() {
        walkSimulatorJob?.cancel()
        walkSimulatorJob = null
        _isSimulatingFlow.value = false
        _liveSpmFlow.value = 0
        _sensorTypeFlow.value = when {
            stepCounterSensor != null -> SensorTypeUsed.HARDWARE_STEP_COUNTER
            stepDetectorSensor != null -> SensorTypeUsed.HARDWARE_STEP_DETECTOR
            else -> SensorTypeUsed.NONE_AVAILABLE
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "StepCounterService onDestroy")
        unregisterSensors()
        walkSimulatorJob?.cancel()
        spmResetJob?.cancel()
        dbSyncJob?.cancel()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        const val TAG = "StepCounterService"
        const val CHANNEL_ID = "healthfit_step_counter_channel"
        const val NOTIFICATION_ID = 2001

        const val ACTION_START = "com.example.service.action.START_STEP_TRACKING"
        const val ACTION_STOP = "com.example.service.action.STOP_STEP_TRACKING"
        const val ACTION_RESET_MIDNIGHT = "com.example.service.action.RESET_MIDNIGHT"
        const val ACTION_MANUAL_ADD = "com.example.service.action.MANUAL_ADD_STEPS"
        const val ACTION_RESET_MANUAL = "com.example.service.action.RESET_MANUAL_STEPS"
        const val ACTION_SIMULATE_WALK_START = "com.example.service.action.SIMULATE_WALK_START"
        const val ACTION_SIMULATE_WALK_STOP = "com.example.service.action.SIMULATE_WALK_STOP"

        const val EXTRA_STEP_COUNT = "extra_step_count"
        const val EXTRA_SPM = "extra_spm"

        private val _liveStepsFlow = MutableStateFlow<Int?>(null)
        val liveStepsFlow: StateFlow<Int?> = _liveStepsFlow.asStateFlow()

        private val _liveSpmFlow = MutableStateFlow(0)
        val liveSpmFlow: StateFlow<Int> = _liveSpmFlow.asStateFlow()

        private val _sensorTypeFlow = MutableStateFlow(SensorTypeUsed.HARDWARE_STEP_COUNTER)
        val sensorTypeFlow: StateFlow<SensorTypeUsed> = _sensorTypeFlow.asStateFlow()

        private val _isSimulatingFlow = MutableStateFlow(false)
        val isSimulatingFlow: StateFlow<Boolean> = _isSimulatingFlow.asStateFlow()

        fun startService(context: Context) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val hasPermission = androidx.core.content.ContextCompat.checkSelfPermission(
                    context,
                    android.Manifest.permission.ACTIVITY_RECOGNITION
                ) == android.content.pm.PackageManager.PERMISSION_GRANTED
                if (!hasPermission) {
                    Log.d(TAG, "StepCounterService start skipped: ACTIVITY_RECOGNITION permission not yet granted.")
                    return
                }
            }
            val intent = Intent(context, StepCounterService::class.java).apply {
                action = ACTION_START
            }
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(intent)
                } else {
                    context.startService(intent)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to start StepCounterService", e)
            }
        }

        fun stopService(context: Context) {
            val intent = Intent(context, StepCounterService::class.java).apply {
                action = ACTION_STOP
            }
            context.startService(intent)
        }

        fun triggerMidnightReset(context: Context) {
            val intent = Intent(context, StepCounterService::class.java).apply {
                action = ACTION_RESET_MIDNIGHT
            }
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(intent)
                } else {
                    context.startService(intent)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to send midnight reset to StepCounterService", e)
            }
        }

        fun addManualSteps(context: Context, count: Int) {
            val intent = Intent(context, StepCounterService::class.java).apply {
                action = ACTION_MANUAL_ADD
                putExtra(EXTRA_STEP_COUNT, count)
            }
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(intent)
                } else {
                    context.startService(intent)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to add manual steps via service", e)
            }
        }

        fun resetManualSteps(context: Context) {
            val intent = Intent(context, StepCounterService::class.java).apply {
                action = ACTION_RESET_MANUAL
            }
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(intent)
                } else {
                    context.startService(intent)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to reset steps via service", e)
            }
        }

        fun startWalkSimulation(context: Context, spm: Int = 110) {
            val intent = Intent(context, StepCounterService::class.java).apply {
                action = ACTION_SIMULATE_WALK_START
                putExtra(EXTRA_SPM, spm)
            }
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(intent)
                } else {
                    context.startService(intent)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to start walk simulation in service", e)
            }
        }

        fun stopWalkSimulation(context: Context) {
            val intent = Intent(context, StepCounterService::class.java).apply {
                action = ACTION_SIMULATE_WALK_STOP
            }
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(intent)
                } else {
                    context.startService(intent)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to stop walk simulation in service", e)
            }
        }
    }
}
