package com.example.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.model.MoodLogEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MoodDao {
    @Query("SELECT * FROM mood_logs WHERE userId = :userId ORDER BY timestamp DESC")
    fun getMoodLogsForUser(userId: String): Flow<List<MoodLogEntity>>

    @Query("SELECT * FROM mood_logs WHERE userId = :userId AND simulationId = :simulationId ORDER BY timestamp DESC")
    fun getMoodLogsForSimulation(userId: String, simulationId: String): Flow<List<MoodLogEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMoodLog(moodLog: MoodLogEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(moodLogs: List<MoodLogEntity>)

    @Delete
    suspend fun deleteMoodLog(moodLog: MoodLogEntity)

    @Query("DELETE FROM mood_logs WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("SELECT COUNT(*) FROM mood_logs WHERE userId = :userId")
    suspend fun getCountForUser(userId: String): Int
}
