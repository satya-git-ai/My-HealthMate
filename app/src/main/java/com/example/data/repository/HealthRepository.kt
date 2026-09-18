package com.example.data.repository

import com.example.data.local.dao.HealthDao
import com.example.data.local.dao.MedicineDao
import com.example.data.local.entity.DailyHealthRecord
import com.example.data.local.entity.Medicine
import com.example.data.local.entity.MedicineHistory
import com.example.data.local.entity.WaterLog
import com.example.data.local.entity.WorkoutSession
import com.example.data.preferences.UserPreferences
import com.example.data.preferences.UserSettings
import com.example.model.RouteCoordinate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class HealthRepository(
    private val healthDao: HealthDao,
    private val medicineDao: MedicineDao,
    private val userPreferences: UserPreferences
) {
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    private val stepMutex = Mutex()

    val userSettings: StateFlow<UserSettings> = userPreferences.settings

    fun getTodayDate(): String = dateFormat.format(Date())

    fun formatDate(timestamp: Long): String = dateFormat.format(Date(timestamp))

    // Medicine Operations
    val allMedicines: Flow<List<Medicine>> = medicineDao.getAllMedicines()
    val activeMedicines: Flow<List<Medicine>> = medicineDao.getActiveMedicines()
    val allMedicineHistory: Flow<List<MedicineHistory>> = medicineDao.getAllHistory()

    fun getMedicineHistoryForDate(date: String): Flow<List<MedicineHistory>> =
        medicineDao.getHistoryForDate(date)

    suspend fun getMedicineById(id: Long): Medicine? = withContext(Dispatchers.IO) {
        medicineDao.getMedicineById(id)
    }

    suspend fun insertOrUpdateMedicine(medicine: Medicine): Long = withContext(Dispatchers.IO) {
        medicineDao.insertMedicine(medicine)
    }

    suspend fun deleteMedicine(medicine: Medicine) = withContext(Dispatchers.IO) {
        medicineDao.deleteMedicine(medicine)
    }

    suspend fun deleteMedicineById(id: Long) = withContext(Dispatchers.IO) {
        medicineDao.deleteMedicineById(id)
    }

    suspend fun recordMedicineAction(
        medicineId: Long,
        medicineName: String,
        dosage: String,
        type: String,
        scheduledTime: String,
        status: String
    ): Long = withContext(Dispatchers.IO) {
        val history = MedicineHistory(
            medicineId = medicineId,
            medicineName = medicineName,
            dosage = dosage,
            type = type,
            scheduledTime = scheduledTime,
            status = status,
            date = getTodayDate()
        )
        medicineDao.insertHistory(history)
    }

    suspend fun clearMedicineHistory() = withContext(Dispatchers.IO) {
        medicineDao.clearAllHistory()
    }

    fun getDailyRecord(date: String): Flow<DailyHealthRecord?> = healthDao.getDailyRecord(date)

    fun getAllDailyRecords(): Flow<List<DailyHealthRecord>> = healthDao.getAllDailyRecords()

    fun getRecentDailyRecords(days: Int): Flow<List<DailyHealthRecord>> = healthDao.getRecentDailyRecords(days)

    fun getAllWorkouts(): Flow<List<WorkoutSession>> = healthDao.getAllWorkouts()

    fun getWorkoutsForDate(date: String): Flow<List<WorkoutSession>> = healthDao.getWorkoutsForDate(date)

    fun getWorkoutById(id: Long): Flow<WorkoutSession?> = healthDao.getWorkoutById(id)

    fun getWaterLogsForDate(date: String): Flow<List<WaterLog>> = healthDao.getWaterLogsForDate(date)

    suspend fun incrementSteps(count: Int) = withContext(Dispatchers.IO) {
        if (count < 0) {
            removeSteps(-count)
            return@withContext
        }
        stepMutex.withLock {
            val today = getTodayDate()
            val existing = healthDao.getDailyRecordSync(today)
            val currentSteps = existing?.steps ?: 0
            val newSteps = currentSteps + count

            // 1 step ≈ 0.000762 km (76.2 cm), 1 step ≈ 0.04 kcal
            val distDelta = count * 0.762f
            val calDelta = (count * 0.04f).toInt()

            val updatedRecord = if (existing != null) {
                existing.copy(
                    steps = newSteps,
                    caloriesBurned = existing.caloriesBurned + calDelta,
                    distanceMeters = existing.distanceMeters + distDelta,
                    activeMinutes = existing.activeMinutes + maxOf(0, count / 100),
                    lastUpdated = System.currentTimeMillis()
                )
            } else {
                DailyHealthRecord(
                    date = today,
                    steps = newSteps,
                    caloriesBurned = calDelta,
                    distanceMeters = distDelta,
                    activeMinutes = maxOf(1, count / 100),
                    lastUpdated = System.currentTimeMillis()
                )
            }
            healthDao.insertOrUpdateDailyRecord(updatedRecord)
        }
    }

    suspend fun removeSteps(count: Int) = withContext(Dispatchers.IO) {
        if (count <= 0) return@withContext
        stepMutex.withLock {
            val today = getTodayDate()
            val existing = healthDao.getDailyRecordSync(today)
            val currentSteps = existing?.steps ?: 0
            val actualRemoved = minOf(currentSteps, count)
            val newSteps = currentSteps - actualRemoved

            val distDelta = actualRemoved * 0.762f
            val calDelta = (actualRemoved * 0.04f).toInt()

            if (existing != null) {
                val updatedRecord = existing.copy(
                    steps = newSteps,
                    caloriesBurned = maxOf(0, existing.caloriesBurned - calDelta),
                    distanceMeters = maxOf(0f, existing.distanceMeters - distDelta),
                    lastUpdated = System.currentTimeMillis()
                )
                healthDao.insertOrUpdateDailyRecord(updatedRecord)
            }
        }
    }

    suspend fun addWater(amountMl: Int) = withContext(Dispatchers.IO) {
        val today = getTodayDate()
        healthDao.addWater(today, amountMl)
    }

    suspend fun undoLastWater() = withContext(Dispatchers.IO) {
        val today = getTodayDate()
        healthDao.undoLastWater(today)
    }

    suspend fun saveWorkoutSession(
        startTime: Long,
        endTime: Long,
        durationSeconds: Long,
        distanceMeters: Float,
        caloriesBurned: Int,
        avgSpeedKmh: Float,
        workoutType: String,
        routePoints: List<RouteCoordinate>,
        estimatedSteps: Int = 0
    ): Long = withContext(Dispatchers.IO) {
        val date = formatDate(startTime)
        val jsonArray = JSONArray()
        routePoints.forEach { point ->
            val obj = JSONObject().apply {
                put("lat", point.latitude)
                put("lng", point.longitude)
                put("time", point.timestamp)
                put("speed", point.speedMps)
                put("alt", point.altitude)
            }
            jsonArray.put(obj)
        }

        val session = WorkoutSession(
            date = date,
            startTime = startTime,
            endTime = endTime,
            durationSeconds = durationSeconds,
            distanceMeters = distanceMeters,
            caloriesBurned = caloriesBurned,
            avgSpeedKmh = avgSpeedKmh,
            workoutType = workoutType,
            routePointsJson = jsonArray.toString()
        )
        val workoutId = healthDao.insertWorkout(session)

        // Update today's record with workout calories, distance, steps, and minutes
        val existing = healthDao.getDailyRecordSync(date)
        val durationMin = maxOf(1, (durationSeconds / 60).toInt())
        val updated = if (existing != null) {
            existing.copy(
                steps = existing.steps + estimatedSteps,
                caloriesBurned = existing.caloriesBurned + caloriesBurned,
                distanceMeters = existing.distanceMeters + distanceMeters,
                activeMinutes = existing.activeMinutes + durationMin,
                lastUpdated = System.currentTimeMillis()
            )
        } else {
            DailyHealthRecord(
                date = date,
                steps = estimatedSteps,
                caloriesBurned = caloriesBurned,
                distanceMeters = distanceMeters,
                activeMinutes = durationMin,
                lastUpdated = System.currentTimeMillis()
            )
        }
        healthDao.insertOrUpdateDailyRecord(updated)

        workoutId
    }

    // Parse route points from JSON
    fun parseRoutePoints(json: String): List<RouteCoordinate> {
        if (json.isBlank() || json == "[]") return emptyList()
        val list = mutableListOf<RouteCoordinate>()
        try {
            val arr = JSONArray(json)
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                list.add(
                    RouteCoordinate(
                        latitude = obj.optDouble("lat", 0.0),
                        longitude = obj.optDouble("lng", 0.0),
                        timestamp = obj.optLong("time", 0L),
                        speedMps = obj.optDouble("speed", 0.0).toFloat(),
                        altitude = obj.optDouble("alt", 0.0)
                    )
                )
            }
        } catch (e: Exception) {
            // Return whatever parsed so far
        }
        return list
    }

    suspend fun resetTodaySteps() = withContext(Dispatchers.IO) {
        stepMutex.withLock {
            val today = getTodayDate()
            val existing = healthDao.getDailyRecordSync(today)
            if (existing != null) {
                healthDao.insertOrUpdateDailyRecord(
                    existing.copy(
                        steps = 0,
                        caloriesBurned = 0,
                        distanceMeters = 0f,
                        activeMinutes = 0,
                        lastUpdated = System.currentTimeMillis()
                    )
                )
            } else {
                healthDao.insertOrUpdateDailyRecord(
                    DailyHealthRecord(
                        date = today,
                        steps = 0,
                        waterMl = 0,
                        caloriesBurned = 0,
                        distanceMeters = 0f,
                        activeMinutes = 0,
                        lastUpdated = System.currentTimeMillis()
                    )
                )
            }
        }
    }

    suspend fun ensureBaselineInitialized() = withContext(Dispatchers.IO) {
        // Delete any artificial dummy seed data so user records start purely from 0
        healthDao.deleteDummySeedData()

        val today = getTodayDate()
        val records = healthDao.getDailyRecordSync(today)
        if (records == null) {
            // Initialize today strictly from 0
            healthDao.insertOrUpdateDailyRecord(
                DailyHealthRecord(
                    date = today,
                    steps = 0,
                    waterMl = 0,
                    caloriesBurned = 0,
                    distanceMeters = 0f,
                    activeMinutes = 0,
                    lastUpdated = System.currentTimeMillis()
                )
            )
        }
    }

    suspend fun seedInitialHistoryIfNeeded() = ensureBaselineInitialized()

    suspend fun resetAllStatisticsToZero() = withContext(Dispatchers.IO) {
        healthDao.clearAllDailyRecords()
        healthDao.clearAllWorkouts()
        healthDao.clearAllWaterLogs()
        val today = getTodayDate()
        healthDao.insertOrUpdateDailyRecord(
            DailyHealthRecord(
                date = today,
                steps = 0,
                waterMl = 0,
                caloriesBurned = 0,
                distanceMeters = 0f,
                activeMinutes = 0,
                lastUpdated = System.currentTimeMillis()
            )
        )
    }

    // User Preferences Delegation
    fun updateStepGoal(goal: Int) = userPreferences.updateStepGoal(goal)
    fun updateWaterGoal(goalMl: Int) = userPreferences.updateWaterGoal(goalMl)
    fun updateWeeklyGoal(days: Int) = userPreferences.updateWeeklyGoal(days)
    fun setDarkMode(isDark: Boolean) = userPreferences.setDarkMode(isDark)
    fun setOnboardingCompleted() = userPreferences.setOnboardingCompleted()
    fun setPermissionsConfigured() = userPreferences.setPermissionsConfigured()
    fun updateNotifications(water: Boolean, move: Boolean, goal: Boolean, workout: Boolean) =
        userPreferences.updateNotifications(water, move, goal, workout)
    fun setStepTrackingActive(active: Boolean) = userPreferences.setStepTrackingActive(active)
    fun updateWaterReminderSettings(
        enabled: Boolean,
        intervalMinutes: Int,
        startHour: Int,
        endHour: Int,
        alarmSoundEnabled: Boolean,
        alarmSoundType: String,
        vibrateEnabled: Boolean
    ) = userPreferences.updateWaterReminderSettings(
        enabled = enabled,
        intervalMinutes = intervalMinutes,
        startHour = startHour,
        endHour = endHour,
        alarmSoundEnabled = alarmSoundEnabled,
        alarmSoundType = alarmSoundType,
        vibrateEnabled = vibrateEnabled
    )

    fun updateSleepReminderSettings(
        enabled: Boolean,
        bedtimeHour: Int,
        bedtimeMinute: Int,
        wakeupHour: Int,
        wakeupMinute: Int,
        soundType: String,
        volume: Float
    ) = userPreferences.updateSleepReminderSettings(
        enabled = enabled,
        bedtimeHour = bedtimeHour,
        bedtimeMinute = bedtimeMinute,
        wakeupHour = wakeupHour,
        wakeupMinute = wakeupMinute,
        soundType = soundType,
        volume = volume
    )

    fun updateUserProfile(
        weightKg: Float,
        heightCm: Float,
        age: Int,
        gender: String,
        primaryGoal: String,
        targetWeightKg: Float
    ) = userPreferences.updateUserProfile(
        weightKg = weightKg,
        heightCm = heightCm,
        age = age,
        gender = gender,
        primaryGoal = primaryGoal,
        targetWeightKg = targetWeightKg
    )

    fun setSleepReminderEnabled(enabled: Boolean) = userPreferences.setSleepReminderEnabled(enabled)
}
