package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.model.BackupRecordEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface BackupDao {
    @Query("SELECT * FROM backup_records WHERE userId = :userId ORDER BY timestamp DESC")
    fun getBackupsForUser(userId: String): Flow<List<BackupRecordEntity>>

    @Query("SELECT * FROM backup_records WHERE userId = :userId ORDER BY timestamp DESC LIMIT 1")
    fun getLatestBackupForUser(userId: String): Flow<BackupRecordEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBackup(record: BackupRecordEntity)

    @Query("DELETE FROM backup_records WHERE id = :id")
    suspend fun deleteById(id: String)
}
