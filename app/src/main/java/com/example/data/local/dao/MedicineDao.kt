package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.local.entity.Medicine
import com.example.data.local.entity.MedicineHistory
import kotlinx.coroutines.flow.Flow

@Dao
interface MedicineDao {
    @Query("SELECT * FROM medicines ORDER BY reminderHour ASC, reminderMinute ASC")
    fun getAllMedicines(): Flow<List<Medicine>>

    @Query("SELECT * FROM medicines WHERE isActive = 1 ORDER BY reminderHour ASC, reminderMinute ASC")
    fun getActiveMedicines(): Flow<List<Medicine>>

    @Query("SELECT * FROM medicines WHERE id = :id LIMIT 1")
    suspend fun getMedicineById(id: Long): Medicine?

    @Query("SELECT * FROM medicines WHERE isActive = 1")
    suspend fun getActiveMedicinesSync(): List<Medicine>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMedicine(medicine: Medicine): Long

    @Update
    suspend fun updateMedicine(medicine: Medicine)

    @Delete
    suspend fun deleteMedicine(medicine: Medicine)

    @Query("DELETE FROM medicines WHERE id = :id")
    suspend fun deleteMedicineById(id: Long)

    // History Queries
    @Query("SELECT * FROM medicine_history ORDER BY timestamp DESC")
    fun getAllHistory(): Flow<List<MedicineHistory>>

    @Query("SELECT * FROM medicine_history WHERE date = :date ORDER BY timestamp DESC")
    fun getHistoryForDate(date: String): Flow<List<MedicineHistory>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHistory(history: MedicineHistory): Long

    @Query("DELETE FROM medicine_history WHERE id = :id")
    suspend fun deleteHistoryById(id: Long)

    @Query("DELETE FROM medicine_history")
    suspend fun clearAllHistory()
}
