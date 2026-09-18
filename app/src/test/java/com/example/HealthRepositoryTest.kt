package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.HealthDatabase
import com.example.data.preferences.UserPreferences
import com.example.data.repository.HealthRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class HealthRepositoryTest {

    private lateinit var database: HealthDatabase
    private lateinit var repository: HealthRepository

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, HealthDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        val prefs = UserPreferences(context)
        repository = HealthRepository(database.healthDao(), database.medicineDao(), prefs)
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun testInitialStepIncrement() = runBlocking {
        repository.incrementSteps(500)
        val record = repository.getDailyRecord(repository.getTodayDate()).first()
        assertNotNull(record)
        assertEquals(500, record?.steps)
        assertTrue((record?.distanceMeters ?: 0f) > 0f)
        assertTrue((record?.caloriesBurned ?: 0) > 0)
    }

    @Test
    fun testWaterIntakeAndUndo() = runBlocking {
        repository.addWater(250)
        repository.addWater(500)
        val record = repository.getDailyRecord(repository.getTodayDate()).first()
        assertEquals(750, record?.waterMl)

        repository.undoLastWater()
        val updatedRecord = repository.getDailyRecord(repository.getTodayDate()).first()
        assertEquals(250, updatedRecord?.waterMl)
    }

    @Test
    fun testGoalSettingsPersistence() {
        repository.updateStepGoal(12000)
        repository.updateWaterGoal(3000)
        repository.updateWeeklyGoal(6)

        val settings = repository.userSettings.value
        assertEquals(12000, settings.stepGoal)
        assertEquals(3000, settings.waterGoalMl)
        assertEquals(6, settings.weeklyGoalDays)
    }

    @Test
    fun testMedicineCrudAndHistory() = runBlocking {
        val medicine = com.example.data.local.entity.Medicine(
            id = 1L,
            name = "Aspirin",
            dosage = "100 mg",
            type = "Tablet",
            startDate = repository.getTodayDate(),
            reminderHour = 8,
            reminderMinute = 30,
            repeatType = "Daily"
        )
        repository.insertOrUpdateMedicine(medicine)

        val list = repository.allMedicines.first()
        assertEquals(1, list.size)
        assertEquals("Aspirin", list[0].name)
        assertEquals("100 mg", list[0].dosage)

        repository.recordMedicineAction(
            medicineId = 1L,
            medicineName = "Aspirin",
            dosage = "100 mg",
            type = "Tablet",
            scheduledTime = "08:30 AM",
            status = "Taken"
        )

        val history = repository.getMedicineHistoryForDate(repository.getTodayDate()).first()
        assertEquals(1, history.size)
        assertEquals("Taken", history[0].status)
    }
}
