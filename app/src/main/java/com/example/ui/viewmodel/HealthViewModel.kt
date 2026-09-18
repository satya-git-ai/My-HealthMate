package com.example.ui.viewmodel

import android.Manifest
import android.app.Application
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.HealthDatabase
import com.example.data.local.entity.DailyHealthRecord
import com.example.data.local.entity.Medicine
import com.example.data.local.entity.MedicineHistory
import com.example.data.local.entity.WaterLog
import com.example.data.local.entity.WorkoutSession
import com.example.data.preferences.UserPreferences
import com.example.data.preferences.UserSettings
import com.example.data.repository.HealthRepository
import com.example.location.GpsTrackerManager
import com.example.location.WorkoutLiveState
import com.example.notification.AlarmSoundPlayer
import com.example.notification.HealthNotificationManager
import com.example.notification.MedicineNotificationManager
import com.example.notification.MedicineReminderScheduler
import com.example.notification.SleepNotificationManager
import com.example.notification.SleepReminderReceiver
import com.example.notification.SleepReminderScheduler
import com.example.notification.WaterReminderScheduler
import com.example.sensor.StepSensorManager
import com.example.sensor.StepSensorState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class HealthViewModel(application: Application) : AndroidViewModel(application) {

    private val db = HealthDatabase.getDatabase(application)
    private val prefs = UserPreferences(application)
    val repository = HealthRepository(db.healthDao(), db.medicineDao(), prefs)

    val notificationManager = HealthNotificationManager(application)
    val gpsTrackerManager = GpsTrackerManager(application)
    val waterReminderScheduler = WaterReminderScheduler(application, prefs)
    val medicineNotificationManager = MedicineNotificationManager(application)
    val medicineReminderScheduler = MedicineReminderScheduler(application)
    val sleepNotificationManager = SleepNotificationManager(application)
    val sleepReminderScheduler = SleepReminderScheduler(application)

    val activeSleepAlert = SleepReminderReceiver.activeAlert
    val isAlarmPlaying: StateFlow<Boolean> = notificationManager.alarmPlayer.isPlaying

    private val _stepSensorManager: StepSensorManager

    val userSettings: StateFlow<UserSettings> = repository.userSettings

    val todayDate: String = repository.getTodayDate()

    val todayRecord: StateFlow<DailyHealthRecord?> = repository.getDailyRecord(todayDate)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val recentRecords: StateFlow<List<DailyHealthRecord>> = repository.getRecentDailyRecords(7)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allDailyRecords: StateFlow<List<DailyHealthRecord>> = repository.getAllDailyRecords()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allWorkouts: StateFlow<List<WorkoutSession>> = repository.getAllWorkouts()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val todayWaterLogs: StateFlow<List<WaterLog>> = repository.getWaterLogsForDate(todayDate)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Medicine Flows
    val allMedicines: StateFlow<List<Medicine>> = repository.allMedicines
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val activeMedicines: StateFlow<List<Medicine>> = repository.activeMedicines
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val todayMedicineHistory: StateFlow<List<MedicineHistory>> = repository.getMedicineHistoryForDate(todayDate)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allMedicineHistory: StateFlow<List<MedicineHistory>> = repository.allMedicineHistory
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val workoutLiveState: StateFlow<WorkoutLiveState> = gpsTrackerManager.workoutState

    private val _selectedHistoryDate = MutableStateFlow(todayDate)
    val selectedHistoryDate: StateFlow<String> = _selectedHistoryDate.asStateFlow()

    private val _lastSavedWorkoutId = MutableStateFlow<Long?>(null)
    val lastSavedWorkoutId: StateFlow<Long?> = _lastSavedWorkoutId.asStateFlow()

    // Real-time instantaneous step count state for 0ms UI latency
    private val _liveTodaySteps = MutableStateFlow<Int?>(null)
    val liveTodaySteps: StateFlow<Int?> = _liveTodaySteps.asStateFlow()

    init {
        gpsTrackerManager.setUserHeight(userSettings.value.userHeightCm)

        // Schedule daily midnight reset to refresh steps at 00:00:00
        com.example.notification.MidnightResetScheduler.scheduleMidnightReset(application)

        _stepSensorManager = StepSensorManager(application) { totalTodaySteps ->
            _liveTodaySteps.value = totalTodaySteps
        }

        viewModelScope.launch {
            todayRecord.collect { record ->
                if (record != null) {
                    val live = _liveTodaySteps.value
                    if (live == null || record.steps > live) {
                        _liveTodaySteps.value = record.steps
                    }
                }
            }
        }

        // Seed initial history if first install so user has rich initial view
        viewModelScope.launch {
            repository.seedInitialHistoryIfNeeded()
        }

        viewModelScope.launch {
            userSettings.collect { settings ->
                gpsTrackerManager.setUserHeight(settings.userHeightCm)
            }
        }

        if (userSettings.value.isStepTrackingActive) {
            val hasPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                ContextCompat.checkSelfPermission(
                    application,
                    Manifest.permission.ACTIVITY_RECOGNITION
                ) == PackageManager.PERMISSION_GRANTED
            } else true
            if (hasPermission) {
                _stepSensorManager.startListening()
            }
        }

        if (userSettings.value.waterReminderEnabled) {
            waterReminderScheduler.scheduleNextReminder()
        }
    }

    val stepSensorState: StateFlow<StepSensorState> = _stepSensorManager.sensorState

    fun toggleStepTracking(active: Boolean) {
        repository.setStepTrackingActive(active)
        if (active) {
            _stepSensorManager.startListening()
        } else {
            _stepSensorManager.pauseListening()
        }
    }

    fun addManualStepCount(count: Int) {
        val current = _liveTodaySteps.value ?: (todayRecord.value?.steps ?: 0)
        _liveTodaySteps.value = current + count
        com.example.service.StepCounterService.addManualSteps(getApplication(), count)
        viewModelScope.launch {
            repository.incrementSteps(count)
        }
    }

    fun removeManualStepCount(count: Int) {
        val current = _liveTodaySteps.value ?: (todayRecord.value?.steps ?: 0)
        val newSteps = maxOf(0, current - count)
        _liveTodaySteps.value = newSteps
        viewModelScope.launch {
            repository.removeSteps(count)
        }
    }

    fun setSensorSensitivity(sensitivity: com.example.sensor.SensorSensitivity) {
        _stepSensorManager.setSensitivity(sensitivity)
    }

    fun startWalkSimulator(targetSpm: Int = 110) {
        _stepSensorManager.startWalkSimulator(targetSpm)
    }

    fun stopWalkSimulator() {
        _stepSensorManager.stopWalkSimulator()
    }

    fun toggleWalkSimulator() {
        _stepSensorManager.toggleWalkSimulator()
    }

    fun triggerSingleStep() {
        _stepSensorManager.triggerSingleStep()
    }

    // Water Tracking Actions
    fun addWater(amountMl: Int) {
        if (amountMl <= 0 || amountMl > 3700) return
        viewModelScope.launch {
            repository.addWater(amountMl)
        }
    }

    fun undoWater() {
        viewModelScope.launch {
            repository.undoLastWater()
        }
    }

    // GPS Walking & Workout Tracking Actions
    fun startGpsWalking(): Boolean {
        gpsTrackerManager.setUserHeight(userSettings.value.userHeightCm)
        return gpsTrackerManager.startWorkout("Outdoor Walk")
    }

    fun pauseGpsWalking() {
        gpsTrackerManager.pauseWorkout()
    }

    fun resumeGpsWalking() {
        gpsTrackerManager.resumeWorkout()
    }

    fun stopGpsWalking() {
        stopWorkout()
    }

    fun checkGpsProviderStatus() {
        gpsTrackerManager.checkProviderStatus()
    }

    fun startWorkout(workoutType: String = "Normal Indoor Walk"): Boolean {
        gpsTrackerManager.setUserHeight(userSettings.value.userHeightCm)
        return gpsTrackerManager.startWorkout(workoutType)
    }

    fun pauseWorkout() {
        gpsTrackerManager.pauseWorkout()
    }

    fun resumeWorkout() {
        gpsTrackerManager.resumeWorkout()
    }

    fun stopWorkout() {
        val finishedState = gpsTrackerManager.stopWorkout()
        if (finishedState.durationSeconds >= 5 || finishedState.distanceMeters >= 5) {
            viewModelScope.launch {
                val workoutId = repository.saveWorkoutSession(
                    startTime = finishedState.startTimeMillis,
                    endTime = System.currentTimeMillis(),
                    durationSeconds = finishedState.durationSeconds,
                    distanceMeters = finishedState.distanceMeters,
                    caloriesBurned = finishedState.caloriesBurned,
                    avgSpeedKmh = finishedState.avgSpeedKmh,
                    workoutType = finishedState.workoutType,
                    routePoints = finishedState.routePoints,
                    estimatedSteps = finishedState.estimatedSteps
                )
                _lastSavedWorkoutId.value = workoutId
            }
        }
    }

    // History selection
    fun selectHistoryDate(date: String) {
        _selectedHistoryDate.value = date
    }

    // Preferences
    fun resetTodaySteps() {
        _liveTodaySteps.value = 0
        _stepSensorManager.resetSessionSteps()
        viewModelScope.launch {
            repository.resetTodaySteps()
        }
    }

    fun resetAllStatisticsToZero() {
        _liveTodaySteps.value = 0
        _stepSensorManager.resetSessionSteps()
        viewModelScope.launch {
            repository.resetAllStatisticsToZero()
        }
    }

    fun updateStepGoal(goal: Int) = repository.updateStepGoal(goal)
    fun updateWaterGoal(goalMl: Int) = repository.updateWaterGoal(goalMl)
    fun updateWeeklyGoal(days: Int) = repository.updateWeeklyGoal(days)
    fun setDarkMode(isDark: Boolean) = repository.setDarkMode(isDark)
    fun completeOnboarding() = repository.setOnboardingCompleted()
    fun completePermissionSetup() = repository.setPermissionsConfigured()
    fun setOnboardingCompleted() = repository.setOnboardingCompleted()
    fun setPermissionsConfigured() = repository.setPermissionsConfigured()

    fun updateUserProfile(
        weightKg: Float,
        heightCm: Float,
        age: Int,
        gender: String,
        primaryGoal: String,
        targetWeightKg: Float,
        autoCalculateGoals: Boolean = true
    ) {
        repository.updateUserProfile(
            weightKg = weightKg,
            heightCm = heightCm,
            age = age,
            gender = gender,
            primaryGoal = primaryGoal,
            targetWeightKg = targetWeightKg
        )
        gpsTrackerManager.setUserHeight(heightCm)
        if (autoCalculateGoals) {
            val recommendation = com.example.util.HealthCalculations.calculatePersonalizedRecommendation(
                weightKg = weightKg,
                heightCm = heightCm,
                age = age,
                gender = gender,
                primaryGoal = primaryGoal
            )
            repository.updateStepGoal(recommendation.recommendedDailySteps)
            repository.updateWaterGoal(recommendation.recommendedDailyWaterMl)
        }
    }

    fun updateNotifications(water: Boolean, move: Boolean, goal: Boolean, workout: Boolean) {
        repository.updateNotifications(water, move, goal, workout)
    }

    fun updateWaterReminderSettings(
        enabled: Boolean,
        intervalMinutes: Int,
        startHour: Int,
        endHour: Int,
        alarmSoundEnabled: Boolean,
        alarmSoundType: String,
        vibrateEnabled: Boolean
    ) {
        repository.updateWaterReminderSettings(
            enabled = enabled,
            intervalMinutes = intervalMinutes,
            startHour = startHour,
            endHour = endHour,
            alarmSoundEnabled = alarmSoundEnabled,
            alarmSoundType = alarmSoundType,
            vibrateEnabled = vibrateEnabled
        )
        if (enabled) {
            waterReminderScheduler.scheduleNextReminder()
        } else {
            waterReminderScheduler.cancelReminders()
        }
    }

    fun previewAlarmSound(soundType: String) {
        notificationManager.alarmPlayer.play(soundType)
    }

    fun triggerWaterReminderWithAlarm(customMessage: String? = null) {
        val settings = userSettings.value
        notificationManager.sendWaterReminder(
            customMessage = customMessage,
            playAlarmSound = settings.waterAlarmSoundEnabled,
            soundType = settings.waterAlarmSoundType,
            vibrate = settings.waterVibrateEnabled
        )
    }

    fun getNextWaterReminderTime(): String {
        return waterReminderScheduler.getNextReminderTimeString()
    }

    fun triggerNotificationDemo(type: String) {
        val settings = userSettings.value
        when (type) {
            "water" -> if (settings.notifyWater) {
                notificationManager.sendWaterReminder(
                    playAlarmSound = settings.waterAlarmSoundEnabled,
                    soundType = settings.waterAlarmSoundType,
                    vibrate = settings.waterVibrateEnabled
                )
            }
            "move" -> if (settings.notifyMove) notificationManager.sendMovementReminder()
            "goal" -> if (settings.notifyGoal) notificationManager.sendGoalMilestone(
                todayRecord.value?.steps ?: 7200,
                settings.stepGoal
            )
            "workout" -> if (settings.notifyWorkout) notificationManager.sendWorkoutReminder()
        }
    }

    // Medicine Management
    fun saveMedicine(medicine: Medicine) {
        viewModelScope.launch {
            val id = repository.insertOrUpdateMedicine(medicine)
            val updated = if (medicine.id <= 0) medicine.copy(id = id) else medicine
            if (updated.isActive) {
                medicineReminderScheduler.scheduleMedicine(updated)
            } else {
                medicineReminderScheduler.cancelMedicine(updated.id)
            }
        }
    }

    fun deleteMedicine(medicine: Medicine) {
        viewModelScope.launch {
            repository.deleteMedicine(medicine)
            medicineReminderScheduler.cancelMedicine(medicine.id)
            medicineNotificationManager.dismissReminder(
                MedicineNotificationManager.getNotificationId(medicine.id)
            )
        }
    }

    fun toggleMedicineActive(medicine: Medicine) {
        viewModelScope.launch {
            val updated = medicine.copy(isActive = !medicine.isActive)
            repository.insertOrUpdateMedicine(updated)
            if (updated.isActive) {
                medicineReminderScheduler.scheduleMedicine(updated)
            } else {
                medicineReminderScheduler.cancelMedicine(updated.id)
            }
        }
    }

    fun recordMedicineAction(
        medicineId: Long,
        medicineName: String,
        dosage: String,
        type: String,
        scheduledTime: String,
        action: String
    ) {
        viewModelScope.launch {
            // Dismiss notification & stop sound
            medicineNotificationManager.dismissReminder(
                MedicineNotificationManager.getNotificationId(medicineId)
            )

            // Insert into history
            repository.recordMedicineAction(
                medicineId = medicineId,
                medicineName = medicineName,
                dosage = dosage,
                type = type,
                scheduledTime = scheduledTime,
                status = action
            )

            if (action == "Snoozed") {
                medicineReminderScheduler.scheduleSnooze(
                    medicineId = medicineId,
                    name = medicineName,
                    dosage = dosage,
                    type = type,
                    delayMinutes = 10
                )
            }
        }
    }

    fun testMedicineReminder(medicine: Medicine) {
        viewModelScope.launch {
            val timeString = MedicineReminderScheduler.formatTime12Hour(
                medicine.reminderHour,
                medicine.reminderMinute
            )
            medicineNotificationManager.showReminderNotification(medicine, timeString)
        }
    }

    fun stopAlarmSound() {
        notificationManager.alarmPlayer.stop()
        medicineNotificationManager.alarmPlayer.stop()
        sleepNotificationManager.alarmPlayer.stop()
        AlarmSoundPlayer.stopActiveSound()
    }

    fun updateSleepReminder(
        enabled: Boolean,
        bedtimeHour: Int,
        bedtimeMinute: Int,
        wakeupHour: Int,
        wakeupMinute: Int,
        soundType: String,
        volume: Float
    ) {
        repository.updateSleepReminderSettings(
            enabled = enabled,
            bedtimeHour = bedtimeHour,
            bedtimeMinute = bedtimeMinute,
            wakeupHour = wakeupHour,
            wakeupMinute = wakeupMinute,
            soundType = soundType,
            volume = volume
        )
        val updatedSettings = repository.userSettings.value
        sleepReminderScheduler.scheduleSleepReminders(updatedSettings)
    }

    fun toggleSleepReminder(enabled: Boolean) {
        repository.setSleepReminderEnabled(enabled)
        val updatedSettings = repository.userSettings.value
        sleepReminderScheduler.scheduleSleepReminders(updatedSettings)
    }

    fun testSleepSound(soundType: String, volume: Float) {
        sleepNotificationManager.alarmPlayer.play(soundType, volume)
    }

    fun stopSleepSound() {
        sleepNotificationManager.alarmPlayer.stop()
        AlarmSoundPlayer.stopActiveSound()
    }

    fun dismissActiveSleepAlert() {
        SleepReminderReceiver.clearActiveAlert()
        sleepNotificationManager.dismissReminder(SleepNotificationManager.NOTIFICATION_ID_BEDTIME)
        sleepNotificationManager.dismissReminder(SleepNotificationManager.NOTIFICATION_ID_WAKEUP)
    }

    fun snoozeActiveWakeup() {
        val settings = userSettings.value
        dismissActiveSleepAlert()
        sleepReminderScheduler.scheduleSnooze(10, settings.sleepSoundType, settings.sleepSoundVolume)
    }

    fun clearAllMedicineHistory() {
        viewModelScope.launch {
            repository.clearMedicineHistory()
        }
    }

    override fun onCleared() {
        super.onCleared()
        _stepSensorManager.pauseListening()
        notificationManager.alarmPlayer.stop()
        medicineNotificationManager.alarmPlayer.stop()
        sleepNotificationManager.alarmPlayer.stop()
        AlarmSoundPlayer.stopActiveSound()
    }
}
