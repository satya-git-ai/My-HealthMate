package com.example.sensor

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Robust hardware step manager that handles:
 * 1. Hardware Sensor.TYPE_STEP_COUNTER readings.
 * 2. Midnight reset to 0 while keeping the absolute hardware sensor count preserved in storage.
 * 3. Mid-day reboot & reset handling (when hardware counter drops back to 0).
 * 4. Offline / deep sleep step accumulation without dropping steps.
 */
class StepDataManager(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    fun getTodayDate(): String = dateFormat.format(Date())

    @Synchronized
    fun getStepDate(): String {
        return prefs.getString(KEY_STEP_DATE, getTodayDate()) ?: getTodayDate()
    }

    @Synchronized
    fun getHardwareBaseline(): Long {
        return prefs.getLong(KEY_HARDWARE_BASELINE, -1L)
    }

    @Synchronized
    fun getBootOffset(): Long {
        return prefs.getLong(KEY_BOOT_OFFSET, 0L)
    }

    @Synchronized
    fun getLastHardwareReading(): Long {
        return prefs.getLong(KEY_LAST_HARDWARE_READING, -1L)
    }

    @Synchronized
    fun getTodayTotalSteps(): Int {
        val today = getTodayDate()
        val savedDate = prefs.getString(KEY_STEP_DATE, null)
        if (savedDate != null && savedDate != today) {
            // New day detected on read
            return 0
        }
        return prefs.getInt(KEY_TODAY_TOTAL_STEPS, 0)
    }

    /**
     * Core mathematical processing for Sensor.TYPE_STEP_COUNTER events.
     * @param rawHardwareSteps Total step count reported by Sensor.TYPE_STEP_COUNTER since last device boot.
     * @return Calculated daily steps for today (starting at 0 at midnight).
     */
    @Synchronized
    fun processHardwareStepEvent(rawHardwareSteps: Long): Int {
        val today = getTodayDate()
        val savedDate = prefs.getString(KEY_STEP_DATE, null)
        var baseline = prefs.getLong(KEY_HARDWARE_BASELINE, -1L)
        var bootOffset = prefs.getLong(KEY_BOOT_OFFSET, 0L)
        val lastReading = prefs.getLong(KEY_LAST_HARDWARE_READING, -1L)

        // Case 1: Midnight rollover (Date has changed)
        if (savedDate == null || savedDate != today) {
            Log.d(TAG, "Midnight rollover detected (Previous: $savedDate, Today: $today). Resetting day steps.")
            baseline = rawHardwareSteps
            bootOffset = 0L
            val computedSteps = 0

            prefs.edit()
                .putString(KEY_STEP_DATE, today)
                .putLong(KEY_HARDWARE_BASELINE, baseline)
                .putLong(KEY_BOOT_OFFSET, 0L)
                .putLong(KEY_LAST_HARDWARE_READING, rawHardwareSteps)
                .putInt(KEY_TODAY_TOTAL_STEPS, computedSteps)
                .apply()

            return computedSteps
        }

        // Case 2: First hardware reading of the current day
        if (baseline < 0L) {
            Log.d(TAG, "Initializing hardware baseline for today: $rawHardwareSteps")
            baseline = rawHardwareSteps
            // Preserve any existing steps (e.g. from manual quick additions or previous session today)
            val existingTodaySteps = prefs.getInt(KEY_TODAY_TOTAL_STEPS, 0).toLong()
            bootOffset = existingTodaySteps

            prefs.edit()
                .putLong(KEY_HARDWARE_BASELINE, baseline)
                .putLong(KEY_BOOT_OFFSET, bootOffset)
                .putLong(KEY_LAST_HARDWARE_READING, rawHardwareSteps)
                .apply()

            return existingTodaySteps.toInt()
        }

        // Case 3: Device reboot detected mid-day (hardware sensor reading dropped below last recorded reading)
        if (rawHardwareSteps < lastReading) {
            Log.w(TAG, "Device reboot detected mid-day! Raw: $rawHardwareSteps < Last: $lastReading")
            // Compute steps accumulated in the previous boot session today
            val stepsInPreviousSession = (lastReading - baseline).coerceAtLeast(0L)
            bootOffset += stepsInPreviousSession
            baseline = rawHardwareSteps

            val totalSteps = (bootOffset + (rawHardwareSteps - baseline).coerceAtLeast(0L)).toInt()

            prefs.edit()
                .putLong(KEY_HARDWARE_BASELINE, baseline)
                .putLong(KEY_BOOT_OFFSET, bootOffset)
                .putLong(KEY_LAST_HARDWARE_READING, rawHardwareSteps)
                .putInt(KEY_TODAY_TOTAL_STEPS, totalSteps)
                .apply()

            return totalSteps
        }

        // Case 4: Normal step increment during active session
        val deltaSinceBaseline = (rawHardwareSteps - baseline).coerceAtLeast(0L)
        val totalTodaySteps = (deltaSinceBaseline + bootOffset).toInt()

        prefs.edit()
            .putLong(KEY_LAST_HARDWARE_READING, rawHardwareSteps)
            .putInt(KEY_TODAY_TOTAL_STEPS, totalTodaySteps)
            .apply()

        return totalTodaySteps
    }

    /**
     * Performs a midnight reset. Called by MidnightResetReceiver at 00:00:00 or when date changes.
     */
    @Synchronized
    fun performMidnightReset(currentHardwareReading: Long = -1L): Int {
        val today = getTodayDate()
        val baseline = if (currentHardwareReading >= 0L) {
            currentHardwareReading
        } else {
            getLastHardwareReading()
        }

        prefs.edit()
            .putString(KEY_STEP_DATE, today)
            .putLong(KEY_HARDWARE_BASELINE, baseline)
            .putLong(KEY_BOOT_OFFSET, 0L)
            .putInt(KEY_TODAY_TOTAL_STEPS, 0)
            .apply()

        Log.d(TAG, "Performed midnight step reset for $today. Baseline set to $baseline")
        return 0
    }

    /**
     * Adds manual step adjustment (+N steps) while keeping hardware sensor offsets coherent.
     */
    @Synchronized
    fun addManualSteps(count: Int): Int {
        if (count <= 0) return getTodayTotalSteps()
        val currentBootOffset = getBootOffset()
        val newBootOffset = currentBootOffset + count
        val newTotal = getTodayTotalSteps() + count

        prefs.edit()
            .putLong(KEY_BOOT_OFFSET, newBootOffset)
            .putInt(KEY_TODAY_TOTAL_STEPS, newTotal)
            .apply()

        return newTotal
    }

    /**
     * Manually resets today's steps to 0.
     */
    @Synchronized
    fun resetTodayStepsManually(): Int {
        val today = getTodayDate()
        val lastReading = getLastHardwareReading()
        prefs.edit()
            .putString(KEY_STEP_DATE, today)
            .putLong(KEY_HARDWARE_BASELINE, lastReading)
            .putLong(KEY_BOOT_OFFSET, 0L)
            .putInt(KEY_TODAY_TOTAL_STEPS, 0)
            .apply()
        return 0
    }

    companion object {
        private const val TAG = "StepDataManager"
        private const val PREF_NAME = "healthfit_step_storage"
        private const val KEY_STEP_DATE = "key_step_date"
        private const val KEY_HARDWARE_BASELINE = "key_hardware_baseline"
        private const val KEY_BOOT_OFFSET = "key_boot_offset"
        private const val KEY_LAST_HARDWARE_READING = "key_last_hardware_reading"
        private const val KEY_TODAY_TOTAL_STEPS = "key_today_total_steps"

        @Volatile
        private var INSTANCE: StepDataManager? = null

        fun getInstance(context: Context): StepDataManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: StepDataManager(context.applicationContext).also { INSTANCE = it }
            }
        }
    }
}
