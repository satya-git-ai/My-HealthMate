package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.data.local.dao.HealthDao
import com.example.data.local.dao.MedicineDao
import com.example.data.local.entity.DailyHealthRecord
import com.example.data.local.entity.Medicine
import com.example.data.local.entity.MedicineHistory
import com.example.data.local.entity.WaterLog
import com.example.data.local.entity.WorkoutSession

@Database(
    entities = [
        DailyHealthRecord::class,
        WorkoutSession::class,
        WaterLog::class,
        Medicine::class,
        MedicineHistory::class
    ],
    version = 2,
    exportSchema = false
)
abstract class HealthDatabase : RoomDatabase() {
    abstract fun healthDao(): HealthDao
    abstract fun medicineDao(): MedicineDao

    companion object {
        @Volatile
        private var INSTANCE: HealthDatabase? = null

        fun getDatabase(context: Context): HealthDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    HealthDatabase::class.java,
                    "healthfit_database"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
