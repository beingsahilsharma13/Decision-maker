package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.data.model.BackupRecordEntity
import com.example.data.model.MoodLogEntity
import com.example.data.model.SimulationEntity
import com.example.data.model.UserEntity

@Database(
    entities = [
        UserEntity::class,
        SimulationEntity::class,
        MoodLogEntity::class,
        BackupRecordEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class LifeSimDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun simulationDao(): SimulationDao
    abstract fun moodDao(): MoodDao
    abstract fun backupDao(): BackupDao

    companion object {
        @Volatile
        private var INSTANCE: LifeSimDatabase? = null

        fun getDatabase(context: Context): LifeSimDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    LifeSimDatabase::class.java,
                    "life_simulator_db"
                ).fallbackToDestructiveMigration(true).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
