package com.example.notification

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.example.data.local.entity.Medicine
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class MedicineReminderScheduler(private val context: Context) {

    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager

    companion object {
        const val ACTION_TRIGGER_MEDICINE = "com.example.ACTION_TRIGGER_MEDICINE"
        private const val REQUEST_CODE_BASE = 20000

        fun formatTime12Hour(hour: Int, minute: Int): String {
            val cal = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, hour)
                set(Calendar.MINUTE, minute)
            }
            val sdf = SimpleDateFormat("h:mm a", Locale.getDefault())
            return sdf.format(cal.time)
        }

        fun calculateNextTriggerTime(medicine: Medicine): Long {
            val now = System.currentTimeMillis()
            val cal = Calendar.getInstance()

            when (medicine.repeatType) {
                "Once" -> {
                    val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                    val parsedDate = try {
                        sdf.parse(medicine.startDate)
                    } catch (e: Exception) {
                        null
                    }
                    if (parsedDate != null) {
                        cal.time = parsedDate
                    }
                    cal.set(Calendar.HOUR_OF_DAY, medicine.reminderHour)
                    cal.set(Calendar.MINUTE, medicine.reminderMinute)
                    cal.set(Calendar.SECOND, 0)
                    cal.set(Calendar.MILLISECOND, 0)

                    if (cal.timeInMillis <= now) {
                        // If already past today, set for tomorrow at same time so user doesn't miss it
                        cal.add(Calendar.DAY_OF_YEAR, 1)
                    }
                    return cal.timeInMillis
                }

                "Specific days" -> {
                    // Specific days: e.g. "Mon, Wed, Fri"
                    val daysList = medicine.specificDays
                        .split(",")
                        .map { it.trim().lowercase(Locale.getDefault()) }
                        .filter { it.isNotEmpty() }

                    if (daysList.isEmpty()) {
                        // Fallback to daily
                        cal.set(Calendar.HOUR_OF_DAY, medicine.reminderHour)
                        cal.set(Calendar.MINUTE, medicine.reminderMinute)
                        cal.set(Calendar.SECOND, 0)
                        cal.set(Calendar.MILLISECOND, 0)
                        if (cal.timeInMillis <= now) cal.add(Calendar.DAY_OF_YEAR, 1)
                        return cal.timeInMillis
                    }

                    // Look ahead up to 8 days to find the next matching day
                    for (offset in 0..7) {
                        val testCal = Calendar.getInstance().apply {
                            add(Calendar.DAY_OF_YEAR, offset)
                            set(Calendar.HOUR_OF_DAY, medicine.reminderHour)
                            set(Calendar.MINUTE, medicine.reminderMinute)
                            set(Calendar.SECOND, 0)
                            set(Calendar.MILLISECOND, 0)
                        }
                        val dayName = SimpleDateFormat("EEE", Locale.ENGLISH)
                            .format(testCal.time)
                            .lowercase(Locale.ENGLISH)

                        val matches = daysList.any { dayName.startsWith(it.take(3)) }
                        if (matches && testCal.timeInMillis > now) {
                            return testCal.timeInMillis
                        }
                    }

                    // Fallback
                    cal.set(Calendar.HOUR_OF_DAY, medicine.reminderHour)
                    cal.set(Calendar.MINUTE, medicine.reminderMinute)
                    cal.set(Calendar.SECOND, 0)
                    cal.set(Calendar.MILLISECOND, 0)
                    if (cal.timeInMillis <= now) cal.add(Calendar.DAY_OF_YEAR, 1)
                    return cal.timeInMillis
                }

                else -> {
                    // "Daily"
                    cal.set(Calendar.HOUR_OF_DAY, medicine.reminderHour)
                    cal.set(Calendar.MINUTE, medicine.reminderMinute)
                    cal.set(Calendar.SECOND, 0)
                    cal.set(Calendar.MILLISECOND, 0)

                    if (cal.timeInMillis <= now) {
                        cal.add(Calendar.DAY_OF_YEAR, 1)
                    }
                    return cal.timeInMillis
                }
            }
        }
    }

    fun scheduleMedicine(medicine: Medicine) {
        if (!medicine.isActive) {
            cancelMedicine(medicine.id)
            return
        }

        val triggerAtMillis = calculateNextTriggerTime(medicine)
        val intent = Intent(context, MedicineReminderReceiver::class.java).apply {
            action = ACTION_TRIGGER_MEDICINE
            putExtra(MedicineNotificationManager.EXTRA_MEDICINE_ID, medicine.id)
            putExtra(MedicineNotificationManager.EXTRA_MEDICINE_NAME, medicine.name)
            putExtra(MedicineNotificationManager.EXTRA_MEDICINE_DOSAGE, medicine.dosage)
            putExtra(MedicineNotificationManager.EXTRA_MEDICINE_TYPE, medicine.type)
            putExtra(
                MedicineNotificationManager.EXTRA_SCHEDULED_TIME,
                formatTime12Hour(medicine.reminderHour, medicine.reminderMinute)
            )
        }

        val requestCode = REQUEST_CODE_BASE + (medicine.id % 10000).toInt()
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (alarmManager?.canScheduleExactAlarms() == true) {
                    alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        triggerAtMillis,
                        pendingIntent
                    )
                } else {
                    alarmManager?.setAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        triggerAtMillis,
                        pendingIntent
                    )
                }
            } else {
                alarmManager?.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerAtMillis,
                    pendingIntent
                )
            }
            Log.d("MedReminderScheduler", "Scheduled medicine ${medicine.name} for ${Date(triggerAtMillis)}")
        } catch (e: Exception) {
            Log.e("MedReminderScheduler", "Error scheduling medicine alarm", e)
        }
    }

    fun scheduleSnooze(medicineId: Long, name: String, dosage: String, type: String, delayMinutes: Int = 10) {
        val triggerAtMillis = System.currentTimeMillis() + delayMinutes * 60 * 1000L
        val intent = Intent(context, MedicineReminderReceiver::class.java).apply {
            action = ACTION_TRIGGER_MEDICINE
            putExtra(MedicineNotificationManager.EXTRA_MEDICINE_ID, medicineId)
            putExtra(MedicineNotificationManager.EXTRA_MEDICINE_NAME, name)
            putExtra(MedicineNotificationManager.EXTRA_MEDICINE_DOSAGE, dosage)
            putExtra(MedicineNotificationManager.EXTRA_MEDICINE_TYPE, type)
            val cal = Calendar.getInstance().apply { timeInMillis = triggerAtMillis }
            putExtra(
                MedicineNotificationManager.EXTRA_SCHEDULED_TIME,
                formatTime12Hour(cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE))
            )
        }

        val requestCode = REQUEST_CODE_BASE + (medicineId % 10000).toInt()
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && alarmManager?.canScheduleExactAlarms() == true) {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerAtMillis,
                    pendingIntent
                )
            } else {
                alarmManager?.setAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerAtMillis,
                    pendingIntent
                )
            }
        } catch (e: Exception) {
            Log.e("MedReminderScheduler", "Error scheduling snooze", e)
        }
    }

    fun cancelMedicine(medicineId: Long) {
        val intent = Intent(context, MedicineReminderReceiver::class.java).apply {
            action = ACTION_TRIGGER_MEDICINE
        }
        val requestCode = REQUEST_CODE_BASE + (medicineId % 10000).toInt()
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        if (pendingIntent != null) {
            alarmManager?.cancel(pendingIntent)
            pendingIntent.cancel()
        }
    }
}
