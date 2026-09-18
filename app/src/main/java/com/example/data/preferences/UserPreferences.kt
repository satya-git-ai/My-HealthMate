package com.example.data.preferences

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class UserSettings(
    val stepGoal: Int = 10000,
    val waterGoalMl: Int = 2500,
    val weeklyGoalDays: Int = 5,
    val isDarkMode: Boolean = false,
    val hasCompletedOnboarding: Boolean = false,
    val hasConfiguredPermissions: Boolean = false,
    val hasConfiguredProfile: Boolean = false,
    val notifyWater: Boolean = true,
    val notifyMove: Boolean = true,
    val notifyGoal: Boolean = true,
    val notifyWorkout: Boolean = true,
    val userWeightKg: Float = 55f,
    val userHeightCm: Float = 165f,
    val userAge: Int = 26,
    val userGender: String = "Female",
    val userPrimaryGoal: String = "General Fitness & Health",
    val targetWeightKg: Float = 52f,
    val isStepTrackingActive: Boolean = true,
    val waterReminderEnabled: Boolean = true,
    val waterReminderIntervalMinutes: Int = 60,
    val waterReminderStartHour: Int = 8,
    val waterReminderEndHour: Int = 22,
    val waterAlarmSoundEnabled: Boolean = true,
    val waterAlarmSoundType: String = "ALARM_RING",
    val waterVibrateEnabled: Boolean = true,
    val sleepReminderEnabled: Boolean = false,
    val sleepBedtimeHour: Int = 22,
    val sleepBedtimeMinute: Int = 30,
    val sleepWakeupHour: Int = 6,
    val sleepWakeupMinute: Int = 30,
    val sleepSoundType: String = "GENTLE_ALARM",
    val sleepSoundVolume: Float = 0.85f
)

class UserPreferences(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("healthfit_user_prefs", Context.MODE_PRIVATE)

    private val _settings = MutableStateFlow(loadSettings())
    val settings: StateFlow<UserSettings> = _settings.asStateFlow()

    private fun loadSettings(): UserSettings {
        return UserSettings(
            stepGoal = prefs.getInt(KEY_STEP_GOAL, 8000),
            waterGoalMl = prefs.getInt(KEY_WATER_GOAL, 2000),
            weeklyGoalDays = prefs.getInt(KEY_WEEKLY_GOAL, 5),
            isDarkMode = prefs.getBoolean(KEY_DARK_MODE, false),
            hasCompletedOnboarding = prefs.getBoolean(KEY_ONBOARDING, false),
            hasConfiguredPermissions = prefs.getBoolean(KEY_PERMISSIONS, false),
            hasConfiguredProfile = prefs.getBoolean(KEY_PROFILE_CONFIGURED, false),
            notifyWater = prefs.getBoolean(KEY_NOTIFY_WATER, true),
            notifyMove = prefs.getBoolean(KEY_NOTIFY_MOVE, true),
            notifyGoal = prefs.getBoolean(KEY_NOTIFY_GOAL, true),
            notifyWorkout = prefs.getBoolean(KEY_NOTIFY_WORKOUT, true),
            userWeightKg = prefs.getFloat(KEY_WEIGHT, 55f),
            userHeightCm = prefs.getFloat(KEY_HEIGHT, 165f),
            userAge = prefs.getInt(KEY_AGE, 26),
            userGender = prefs.getString(KEY_GENDER, "Female") ?: "Female",
            userPrimaryGoal = prefs.getString(KEY_PRIMARY_GOAL, "General Fitness & Health") ?: "General Fitness & Health",
            targetWeightKg = prefs.getFloat(KEY_TARGET_WEIGHT, 52f),
            isStepTrackingActive = prefs.getBoolean(KEY_TRACKING_ACTIVE, true),
            waterReminderEnabled = prefs.getBoolean(KEY_WATER_REMINDER_ENABLED, true),
            waterReminderIntervalMinutes = prefs.getInt(KEY_WATER_REMINDER_INTERVAL, 60),
            waterReminderStartHour = prefs.getInt(KEY_WATER_REMINDER_START_HOUR, 8),
            waterReminderEndHour = prefs.getInt(KEY_WATER_REMINDER_END_HOUR, 22),
            waterAlarmSoundEnabled = prefs.getBoolean(KEY_WATER_ALARM_SOUND_ENABLED, true),
            waterAlarmSoundType = prefs.getString(KEY_WATER_ALARM_SOUND_TYPE, "ALARM_RING") ?: "ALARM_RING",
            waterVibrateEnabled = prefs.getBoolean(KEY_WATER_VIBRATE_ENABLED, true),
            sleepReminderEnabled = prefs.getBoolean(KEY_SLEEP_REMINDER_ENABLED, false),
            sleepBedtimeHour = prefs.getInt(KEY_SLEEP_BEDTIME_HOUR, 22),
            sleepBedtimeMinute = prefs.getInt(KEY_SLEEP_BEDTIME_MINUTE, 30),
            sleepWakeupHour = prefs.getInt(KEY_SLEEP_WAKEUP_HOUR, 6),
            sleepWakeupMinute = prefs.getInt(KEY_SLEEP_WAKEUP_MINUTE, 30),
            sleepSoundType = prefs.getString(KEY_SLEEP_SOUND_TYPE, "GENTLE_ALARM") ?: "GENTLE_ALARM",
            sleepSoundVolume = prefs.getFloat(KEY_SLEEP_SOUND_VOLUME, 0.85f)
        )
    }

    fun updateStepGoal(goal: Int) {
        prefs.edit().putInt(KEY_STEP_GOAL, goal).apply()
        _settings.value = _settings.value.copy(stepGoal = goal)
    }

    fun updateWaterGoal(goalMl: Int) {
        prefs.edit().putInt(KEY_WATER_GOAL, goalMl).apply()
        _settings.value = _settings.value.copy(waterGoalMl = goalMl)
    }

    fun updateWeeklyGoal(days: Int) {
        prefs.edit().putInt(KEY_WEEKLY_GOAL, days).apply()
        _settings.value = _settings.value.copy(weeklyGoalDays = days)
    }

    fun setDarkMode(isDark: Boolean) {
        prefs.edit().putBoolean(KEY_DARK_MODE, isDark).apply()
        _settings.value = _settings.value.copy(isDarkMode = isDark)
    }

    fun setOnboardingCompleted() {
        prefs.edit().putBoolean(KEY_ONBOARDING, true).apply()
        _settings.value = _settings.value.copy(hasCompletedOnboarding = true)
    }

    fun setPermissionsConfigured() {
        prefs.edit().putBoolean(KEY_PERMISSIONS, true).apply()
        _settings.value = _settings.value.copy(hasConfiguredPermissions = true)
    }

    fun updateNotifications(
        water: Boolean = _settings.value.notifyWater,
        move: Boolean = _settings.value.notifyMove,
        goal: Boolean = _settings.value.notifyGoal,
        workout: Boolean = _settings.value.notifyWorkout
    ) {
        prefs.edit()
            .putBoolean(KEY_NOTIFY_WATER, water)
            .putBoolean(KEY_NOTIFY_MOVE, move)
            .putBoolean(KEY_NOTIFY_GOAL, goal)
            .putBoolean(KEY_NOTIFY_WORKOUT, workout)
            .apply()
        _settings.value = _settings.value.copy(
            notifyWater = water,
            notifyMove = move,
            notifyGoal = goal,
            notifyWorkout = workout
        )
    }

    fun setStepTrackingActive(active: Boolean) {
        prefs.edit().putBoolean(KEY_TRACKING_ACTIVE, active).apply()
        _settings.value = _settings.value.copy(isStepTrackingActive = active)
    }

    fun updateWaterReminderSettings(
        enabled: Boolean = _settings.value.waterReminderEnabled,
        intervalMinutes: Int = _settings.value.waterReminderIntervalMinutes,
        startHour: Int = _settings.value.waterReminderStartHour,
        endHour: Int = _settings.value.waterReminderEndHour,
        alarmSoundEnabled: Boolean = _settings.value.waterAlarmSoundEnabled,
        alarmSoundType: String = _settings.value.waterAlarmSoundType,
        vibrateEnabled: Boolean = _settings.value.waterVibrateEnabled
    ) {
        prefs.edit()
            .putBoolean(KEY_WATER_REMINDER_ENABLED, enabled)
            .putInt(KEY_WATER_REMINDER_INTERVAL, intervalMinutes)
            .putInt(KEY_WATER_REMINDER_START_HOUR, startHour)
            .putInt(KEY_WATER_REMINDER_END_HOUR, endHour)
            .putBoolean(KEY_WATER_ALARM_SOUND_ENABLED, alarmSoundEnabled)
            .putString(KEY_WATER_ALARM_SOUND_TYPE, alarmSoundType)
            .putBoolean(KEY_WATER_VIBRATE_ENABLED, vibrateEnabled)
            .apply()

        _settings.value = _settings.value.copy(
            waterReminderEnabled = enabled,
            waterReminderIntervalMinutes = intervalMinutes,
            waterReminderStartHour = startHour,
            waterReminderEndHour = endHour,
            waterAlarmSoundEnabled = alarmSoundEnabled,
            waterAlarmSoundType = alarmSoundType,
            waterVibrateEnabled = vibrateEnabled
        )
    }

    fun updateSleepReminderSettings(
        enabled: Boolean = _settings.value.sleepReminderEnabled,
        bedtimeHour: Int = _settings.value.sleepBedtimeHour,
        bedtimeMinute: Int = _settings.value.sleepBedtimeMinute,
        wakeupHour: Int = _settings.value.sleepWakeupHour,
        wakeupMinute: Int = _settings.value.sleepWakeupMinute,
        soundType: String = _settings.value.sleepSoundType,
        volume: Float = _settings.value.sleepSoundVolume
    ) {
        prefs.edit()
            .putBoolean(KEY_SLEEP_REMINDER_ENABLED, enabled)
            .putInt(KEY_SLEEP_BEDTIME_HOUR, bedtimeHour)
            .putInt(KEY_SLEEP_BEDTIME_MINUTE, bedtimeMinute)
            .putInt(KEY_SLEEP_WAKEUP_HOUR, wakeupHour)
            .putInt(KEY_SLEEP_WAKEUP_MINUTE, wakeupMinute)
            .putString(KEY_SLEEP_SOUND_TYPE, soundType)
            .putFloat(KEY_SLEEP_SOUND_VOLUME, volume)
            .apply()

        _settings.value = _settings.value.copy(
            sleepReminderEnabled = enabled,
            sleepBedtimeHour = bedtimeHour,
            sleepBedtimeMinute = bedtimeMinute,
            sleepWakeupHour = wakeupHour,
            sleepWakeupMinute = wakeupMinute,
            sleepSoundType = soundType,
            sleepSoundVolume = volume
        )
    }

    fun updateUserProfile(
        weightKg: Float,
        heightCm: Float,
        age: Int,
        gender: String,
        primaryGoal: String,
        targetWeightKg: Float = _settings.value.targetWeightKg
    ) {
        prefs.edit()
            .putFloat(KEY_WEIGHT, weightKg)
            .putFloat(KEY_HEIGHT, heightCm)
            .putInt(KEY_AGE, age)
            .putString(KEY_GENDER, gender)
            .putString(KEY_PRIMARY_GOAL, primaryGoal)
            .putFloat(KEY_TARGET_WEIGHT, targetWeightKg)
            .putBoolean(KEY_PROFILE_CONFIGURED, true)
            .apply()

        _settings.value = _settings.value.copy(
            userWeightKg = weightKg,
            userHeightCm = heightCm,
            userAge = age,
            userGender = gender,
            userPrimaryGoal = primaryGoal,
            targetWeightKg = targetWeightKg,
            hasConfiguredProfile = true
        )
    }

    fun setSleepReminderEnabled(enabled: Boolean) {
        updateSleepReminderSettings(enabled = enabled)
    }

    companion object {
        private const val KEY_STEP_GOAL = "step_goal"
        private const val KEY_WATER_GOAL = "water_goal"
        private const val KEY_WEEKLY_GOAL = "weekly_goal"
        private const val KEY_DARK_MODE = "dark_mode"
        private const val KEY_ONBOARDING = "has_completed_onboarding"
        private const val KEY_PERMISSIONS = "has_configured_permissions"
        private const val KEY_PROFILE_CONFIGURED = "has_configured_profile"
        private const val KEY_NOTIFY_WATER = "notify_water"
        private const val KEY_NOTIFY_MOVE = "notify_move"
        private const val KEY_NOTIFY_GOAL = "notify_goal"
        private const val KEY_NOTIFY_WORKOUT = "notify_workout"
        private const val KEY_WEIGHT = "user_weight"
        private const val KEY_HEIGHT = "user_height"
        private const val KEY_AGE = "user_age"
        private const val KEY_GENDER = "user_gender"
        private const val KEY_PRIMARY_GOAL = "user_primary_goal"
        private const val KEY_TARGET_WEIGHT = "user_target_weight"
        private const val KEY_TRACKING_ACTIVE = "tracking_active"
        private const val KEY_WATER_REMINDER_ENABLED = "water_reminder_enabled"
        private const val KEY_WATER_REMINDER_INTERVAL = "water_reminder_interval_minutes"
        private const val KEY_WATER_REMINDER_START_HOUR = "water_reminder_start_hour"
        private const val KEY_WATER_REMINDER_END_HOUR = "water_reminder_end_hour"
        private const val KEY_WATER_ALARM_SOUND_ENABLED = "water_alarm_sound_enabled"
        private const val KEY_WATER_ALARM_SOUND_TYPE = "water_alarm_sound_type"
        private const val KEY_WATER_VIBRATE_ENABLED = "water_vibrate_enabled"
        private const val KEY_SLEEP_REMINDER_ENABLED = "sleep_reminder_enabled"
        private const val KEY_SLEEP_BEDTIME_HOUR = "sleep_bedtime_hour"
        private const val KEY_SLEEP_BEDTIME_MINUTE = "sleep_bedtime_minute"
        private const val KEY_SLEEP_WAKEUP_HOUR = "sleep_wakeup_hour"
        private const val KEY_SLEEP_WAKEUP_MINUTE = "sleep_wakeup_minute"
        private const val KEY_SLEEP_SOUND_TYPE = "sleep_sound_type"
        private const val KEY_SLEEP_SOUND_VOLUME = "sleep_sound_volume"
    }
}
