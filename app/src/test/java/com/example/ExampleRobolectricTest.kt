package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.preferences.UserPreferences
import com.example.notification.HydrationSoundType
import com.example.notification.WaterReminderScheduler
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("My HealthMate", appName)
  }

  @Test
  fun `test water reminder preferences and scheduler`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val prefs = UserPreferences(context)

    // Update water reminder settings
    prefs.updateWaterReminderSettings(
      enabled = true,
      intervalMinutes = 45,
      startHour = 8,
      endHour = 22,
      alarmSoundEnabled = true,
      alarmSoundType = "WATER_DROP",
      vibrateEnabled = true
    )

    val settings = prefs.settings.value
    assertTrue(settings.waterReminderEnabled)
    assertEquals(45, settings.waterReminderIntervalMinutes)
    assertEquals(8, settings.waterReminderStartHour)
    assertEquals(22, settings.waterReminderEndHour)
    assertTrue(settings.waterAlarmSoundEnabled)
    assertEquals("WATER_DROP", settings.waterAlarmSoundType)
    assertTrue(settings.waterVibrateEnabled)

    val scheduler = WaterReminderScheduler(context, prefs)
    val nextTimeString = scheduler.getNextReminderTimeString()
    assertNotNull(nextTimeString)
    assertTrue(nextTimeString.isNotEmpty())

    val soundType = HydrationSoundType.fromId("WATER_DROP")
    assertEquals("Water Droplet Chime", soundType.title)
  }
}
