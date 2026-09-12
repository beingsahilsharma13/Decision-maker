package com.example.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.SimulationEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SimulationDao {
    @Query("SELECT * FROM simulations WHERE userId = :userId ORDER BY createdAt DESC")
    fun getSimulationsForUser(userId: String): Flow<List<SimulationEntity>>

    @Query("SELECT * FROM simulations WHERE id = :id LIMIT 1")
    suspend fun getSimulationById(id: String): SimulationEntity?

    @Query("SELECT * FROM simulations WHERE id = :id LIMIT 1")
    fun getSimulationFlowById(id: String): Flow<SimulationEntity?>

    @Query("SELECT * FROM simulations WHERE userId = :userId AND isFavorite = 1 ORDER BY createdAt DESC")
    fun getFavoriteSimulations(userId: String): Flow<List<SimulationEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSimulation(simulation: SimulationEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(simulations: List<SimulationEntity>)

    @Update
    suspend fun updateSimulation(simulation: SimulationEntity)

    @Delete
    suspend fun deleteSimulation(simulation: SimulationEntity)

    @Query("DELETE FROM simulations WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("SELECT COUNT(*) FROM simulations WHERE userId = :userId")
    suspend fun getCountForUser(userId: String): Int
}
