package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.example.data.local.entity.DailyHealthRecord
import com.example.data.local.entity.WaterLog
import com.example.data.local.entity.WorkoutSession
import kotlinx.coroutines.flow.Flow

@Dao
interface HealthDao {

    // Daily Health Record
    @Query("SELECT * FROM daily_health_records WHERE date = :date LIMIT 1")
    fun getDailyRecord(date: String): Flow<DailyHealthRecord?>

    @Query("SELECT * FROM daily_health_records WHERE date = :date LIMIT 1")
    suspend fun getDailyRecordSync(date: String): DailyHealthRecord?

    @Query("SELECT * FROM daily_health_records ORDER BY date DESC")
    fun getAllDailyRecords(): Flow<List<DailyHealthRecord>>

    @Query("SELECT * FROM daily_health_records ORDER BY date DESC LIMIT :limit")
    fun getRecentDailyRecords(limit: Int): Flow<List<DailyHealthRecord>>

    @Query("DELETE FROM daily_health_records WHERE steps IN (8420, 10150, 7300, 9600, 11200, 6100, 6842)")
    suspend fun deleteDummySeedData()

    @Query("DELETE FROM daily_health_records")
    suspend fun clearAllDailyRecords()

    @Query("DELETE FROM workout_sessions")
    suspend fun clearAllWorkouts()

    @Query("DELETE FROM water_logs")
    suspend fun clearAllWaterLogs()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateDailyRecord(record: DailyHealthRecord)

    // Workout Sessions
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWorkout(workout: WorkoutSession): Long

    @Query("SELECT * FROM workout_sessions ORDER BY startTime DESC")
    fun getAllWorkouts(): Flow<List<WorkoutSession>>

    @Query("SELECT * FROM workout_sessions WHERE date = :date ORDER BY startTime DESC")
    fun getWorkoutsForDate(date: String): Flow<List<WorkoutSession>>

    @Query("SELECT * FROM workout_sessions WHERE id = :id LIMIT 1")
    fun getWorkoutById(id: Long): Flow<WorkoutSession?>

    // Water Logs
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWaterLog(log: WaterLog): Long

    @Query("SELECT * FROM water_logs WHERE date = :date ORDER BY timestamp DESC")
    fun getWaterLogsForDate(date: String): Flow<List<WaterLog>>

    @Query("SELECT * FROM water_logs WHERE date = :date ORDER BY timestamp DESC LIMIT 1")
    suspend fun getLatestWaterLog(date: String): WaterLog?

    @Query("DELETE FROM water_logs WHERE id = :id")
    suspend fun deleteWaterLogById(id: Long)

    @Transaction
    suspend fun addWater(date: String, amountMl: Int) {
        insertWaterLog(WaterLog(date = date, amountMl = amountMl))
        val existing = getDailyRecordSync(date)
        if (existing != null) {
            insertOrUpdateDailyRecord(existing.copy(waterMl = (existing.waterMl + amountMl).coerceAtLeast(0)))
        } else {
            insertOrUpdateDailyRecord(DailyHealthRecord(date = date, waterMl = amountMl.coerceAtLeast(0)))
        }
    }

    @Transaction
    suspend fun undoLastWater(date: String) {
        val latest = getLatestWaterLog(date) ?: return
        deleteWaterLogById(latest.id)
        val existing = getDailyRecordSync(date)
        if (existing != null) {
            val newAmount = (existing.waterMl - latest.amountMl).coerceAtLeast(0)
            insertOrUpdateDailyRecord(existing.copy(waterMl = newAmount))
        }
    }
}
