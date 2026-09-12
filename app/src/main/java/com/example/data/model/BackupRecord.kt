package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "backup_records")
data class BackupRecordEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val userEmail: String,
    val timestamp: Long = System.currentTimeMillis(),
    val simulationsCount: Int,
    val moodLogsCount: Int,
    val checksum: String,
    val status: String = "SYNCED", // "SYNCED", "RESTORED", "LOCAL"
    val deviceName: String = "Android Device"
)
