package com.example.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.PowerManager
import android.util.Log
import com.example.data.local.HealthDatabase
import com.example.data.local.entity.Medicine
import com.example.data.local.entity.MedicineHistory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MedicineReminderReceiver : BroadcastReceiver() {

    private val receiverScope = CoroutineScope(Dispatchers.IO)

    override fun onReceive(context: Context, intent: Intent?) {
        if (intent == null) return
        val action = intent.action ?: return

        val pm = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
        val wakeLock = pm?.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK,
            "HealthFit:MedicineReminderWakeLock"
        )
        wakeLock?.acquire(30000L) // 30s timeout

        val database = HealthDatabase.getDatabase(context)
        val medDao = database.medicineDao()
        val notificationManager = MedicineNotificationManager(context)
        val scheduler = MedicineReminderScheduler(context)

        val medicineId = intent.getLongExtra(MedicineNotificationManager.EXTRA_MEDICINE_ID, -1L)
        val medicineName = intent.getStringExtra(MedicineNotificationManager.EXTRA_MEDICINE_NAME) ?: "Medicine"
        val dosage = intent.getStringExtra(MedicineNotificationManager.EXTRA_MEDICINE_DOSAGE) ?: ""
        val type = intent.getStringExtra(MedicineNotificationManager.EXTRA_MEDICINE_TYPE) ?: "Tablet"
        val scheduledTime = intent.getStringExtra(MedicineNotificationManager.EXTRA_SCHEDULED_TIME) ?: ""
        val notificationId = intent.getIntExtra(
            MedicineNotificationManager.EXTRA_NOTIFICATION_ID,
            MedicineNotificationManager.getNotificationId(medicineId)
        )

        val todayDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

        when (action) {
            MedicineReminderScheduler.ACTION_TRIGGER_MEDICINE -> {
                receiverScope.launch {
                    try {
                        val medicine = if (medicineId > 0) {
                            medDao.getMedicineById(medicineId)
                        } else null

                        val medToRemind = medicine ?: Medicine(
                            id = if (medicineId > 0) medicineId else System.currentTimeMillis(),
                            name = medicineName,
                            dosage = dosage,
                            type = type,
                            startDate = todayDate,
                            soundEnabled = true,
                            vibrate = true
                        )

                        val timeString = if (scheduledTime.isNotBlank()) {
                            scheduledTime
                        } else {
                            MedicineReminderScheduler.formatTime12Hour(medToRemind.reminderHour, medToRemind.reminderMinute)
                        }

                        notificationManager.showReminderNotification(medToRemind, timeString)

                        // Reschedule next recurrence if medicine is recurring
                        if (medicine != null && medicine.repeatType != "Once") {
                            scheduler.scheduleMedicine(medicine)
                        }
                    } catch (e: Exception) {
                        Log.e("MedReminderReceiver", "Error triggering reminder", e)
                    } finally {
                        if (wakeLock?.isHeld == true) wakeLock.release()
                    }
                }
            }

            MedicineNotificationManager.ACTION_MED_TAKEN -> {
                notificationManager.dismissReminder(notificationId)
                receiverScope.launch {
                    try {
                        medDao.insertHistory(
                            MedicineHistory(
                                medicineId = medicineId,
                                medicineName = medicineName,
                                dosage = dosage,
                                type = type,
                                scheduledTime = scheduledTime,
                                status = "Taken",
                                date = todayDate
                            )
                        )
                    } catch (e: Exception) {
                        Log.e("MedReminderReceiver", "Error recording taken action", e)
                    } finally {
                        if (wakeLock?.isHeld == true) wakeLock.release()
                    }
                }
            }

            MedicineNotificationManager.ACTION_MED_SNOOZE -> {
                notificationManager.dismissReminder(notificationId)
                receiverScope.launch {
                    try {
                        medDao.insertHistory(
                            MedicineHistory(
                                medicineId = medicineId,
                                medicineName = medicineName,
                                dosage = dosage,
                                type = type,
                                scheduledTime = scheduledTime,
                                status = "Snoozed",
                                date = todayDate
                            )
                        )
                        scheduler.scheduleSnooze(medicineId, medicineName, dosage, type, delayMinutes = 10)
                    } catch (e: Exception) {
                        Log.e("MedReminderReceiver", "Error recording snooze action", e)
                    } finally {
                        if (wakeLock?.isHeld == true) wakeLock.release()
                    }
                }
            }

            MedicineNotificationManager.ACTION_MED_SKIP -> {
                notificationManager.dismissReminder(notificationId)
                receiverScope.launch {
                    try {
                        medDao.insertHistory(
                            MedicineHistory(
                                medicineId = medicineId,
                                medicineName = medicineName,
                                dosage = dosage,
                                type = type,
                                scheduledTime = scheduledTime,
                                status = "Skipped",
                                date = todayDate
                            )
                        )
                    } catch (e: Exception) {
                        Log.e("MedReminderReceiver", "Error recording skip action", e)
                    } finally {
                        if (wakeLock?.isHeld == true) wakeLock.release()
                    }
                }
            }
        }
    }
}
