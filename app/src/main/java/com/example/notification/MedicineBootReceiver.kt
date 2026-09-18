package com.example.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.data.local.HealthDatabase
import com.example.service.StepCounterService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MedicineBootReceiver : BroadcastReceiver() {

    private val receiverScope = CoroutineScope(Dispatchers.IO)

    override fun onReceive(context: Context, intent: Intent?) {
        val action = intent?.action
        if (action == Intent.ACTION_BOOT_COMPLETED || action == Intent.ACTION_MY_PACKAGE_REPLACED) {
            Log.d("MedicineBootReceiver", "Device rebooted or package replaced. Rescheduling all medicine reminders...")
            receiverScope.launch {
                try {
                    val db = HealthDatabase.getDatabase(context)
                    val activeMeds = db.medicineDao().getActiveMedicinesSync()
                    val scheduler = MedicineReminderScheduler(context)
                    activeMeds.forEach { med ->
                        scheduler.scheduleMedicine(med)
                    }
                    Log.d("MedicineBootReceiver", "Rescheduled ${activeMeds.size} active medicine reminders.")

                    // Reschedule Sleep Reminders
                    val prefs = com.example.data.preferences.UserPreferences(context)
                    val settings = prefs.settings.value
                    if (settings.sleepReminderEnabled) {
                        val sleepScheduler = SleepReminderScheduler(context)
                        sleepScheduler.scheduleSleepReminders(settings)
                        Log.d("MedicineBootReceiver", "Rescheduled sleep reminders.")
                    }

                    // Reschedule Water Reminders
                    if (settings.waterReminderEnabled) {
                        val waterScheduler = WaterReminderScheduler(context, prefs)
                        waterScheduler.scheduleNextReminder()
                        Log.d("MedicineBootReceiver", "Rescheduled water reminders.")
                    }

                    // Schedule daily Midnight Reset Alarm
                    MidnightResetScheduler.scheduleMidnightReset(context)
                    Log.d("MedicineBootReceiver", "Scheduled midnight reset alarm.")

                    // Start Step Counter Foreground Service if tracking is active
                    if (settings.isStepTrackingActive) {
                        StepCounterService.startService(context)
                        Log.d("MedicineBootReceiver", "Started StepCounterService on boot.")
                    }
                } catch (e: Exception) {
                    Log.e("MedicineBootReceiver", "Failed to reschedule reminders / start step service on boot", e)
                }
            }
        }
    }
}
