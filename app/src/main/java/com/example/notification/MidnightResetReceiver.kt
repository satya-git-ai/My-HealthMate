package com.example.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.sensor.StepDataManager
import com.example.service.StepCounterService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MidnightResetReceiver : BroadcastReceiver() {

    private val receiverScope = CoroutineScope(Dispatchers.IO)

    override fun onReceive(context: Context, intent: Intent?) {
        Log.d(TAG, "MidnightResetReceiver triggered at midnight. Resetting daily counters...")

        receiverScope.launch {
            try {
                // 1. Reset hardware step baseline for new day
                StepDataManager.getInstance(context).performMidnightReset()

                // 2. Notify running Foreground Service
                StepCounterService.triggerMidnightReset(context)

                // 3. Schedule next day's midnight alarm
                MidnightResetScheduler.scheduleMidnightReset(context)
            } catch (e: Exception) {
                Log.e(TAG, "Error performing midnight reset in receiver", e)
            }
        }
    }

    companion object {
        private const val TAG = "MidnightResetReceiver"
    }
}
