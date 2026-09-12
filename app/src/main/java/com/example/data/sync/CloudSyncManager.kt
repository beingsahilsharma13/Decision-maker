package com.example.data.sync

import android.content.Context
import com.example.data.local.LifeSimDatabase
import com.example.data.model.BackupRecordEntity
import com.example.data.model.MoodLogEntity
import com.example.data.model.SimulationEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.security.MessageDigest
import java.util.UUID

data class SyncResult(
    val success: Boolean,
    val message: String,
    val backupId: String? = null,
    val checksum: String? = null,
    val totalSimulations: Int = 0,
    val totalMoodLogs: Int = 0
)

data class CloudVaultSnapshot(
    val backupId: String,
    val userId: String,
    val userEmail: String,
    val deviceName: String,
    val timestamp: Long,
    val checksum: String,
    val payloadJson: String,
    val simulationsCount: Int,
    val moodLogsCount: Int
)

class CloudSyncManager(private val context: Context, private val database: LifeSimDatabase) {
    private val prefs = context.getSharedPreferences("life_sim_cloud_vault", Context.MODE_PRIVATE)

    suspend fun performCloudBackup(userId: String, userEmail: String, deviceName: String = "Pixel Device"): SyncResult = withContext(Dispatchers.IO) {
        try {
            // Fetch local user data
            val simulationDao = database.simulationDao()
            val moodDao = database.moodDao()
            val backupDao = database.backupDao()

            val simulationsList = mutableListOf<SimulationEntity>()
            val moodLogsList = mutableListOf<MoodLogEntity>()

            // Read items synchronously from DB for user
            // We can query using standard query
            val allSims = database.openHelper.readableDatabase.query(
                "SELECT * FROM simulations WHERE userId = ?", arrayOf(userId)
            )
            while (allSims.moveToNext()) {
                val sim = SimulationEntity(
                    id = allSims.getString(allSims.getColumnIndexOrThrow("id")),
                    userId = allSims.getString(allSims.getColumnIndexOrThrow("userId")),
                    title = allSims.getString(allSims.getColumnIndexOrThrow("title")),
                    category = allSims.getString(allSims.getColumnIndexOrThrow("category")),
                    description = allSims.getString(allSims.getColumnIndexOrThrow("description")),
                    timeHorizonYears = allSims.getInt(allSims.getColumnIndexOrThrow("timeHorizonYears")),
                    initialCapital = allSims.getDouble(allSims.getColumnIndexOrThrow("initialCapital")),
                    monthlySurplus = allSims.getDouble(allSims.getColumnIndexOrThrow("monthlySurplus")),
                    riskTolerance = allSims.getString(allSims.getColumnIndexOrThrow("riskTolerance")),
                    stressLevelBefore = allSims.getInt(allSims.getColumnIndexOrThrow("stressLevelBefore")),
                    optionsJson = allSims.getString(allSims.getColumnIndexOrThrow("optionsJson")),
                    reportJson = allSims.getString(allSims.getColumnIndexOrThrow("reportJson")),
                    isFavorite = allSims.getInt(allSims.getColumnIndexOrThrow("isFavorite")) == 1,
                    actualChosenOptionId = if (allSims.isNull(allSims.getColumnIndexOrThrow("actualChosenOptionId"))) null else allSims.getString(allSims.getColumnIndexOrThrow("actualChosenOptionId")),
                    reflectionNote = if (allSims.isNull(allSims.getColumnIndexOrThrow("reflectionNote"))) null else allSims.getString(allSims.getColumnIndexOrThrow("reflectionNote")),
                    createdAt = allSims.getLong(allSims.getColumnIndexOrThrow("createdAt")),
                    updatedAt = allSims.getLong(allSims.getColumnIndexOrThrow("updatedAt"))
                )
                simulationsList.add(sim)
            }
            allSims.close()

            val allMoods = database.openHelper.readableDatabase.query(
                "SELECT * FROM mood_logs WHERE userId = ?", arrayOf(userId)
            )
            while (allMoods.moveToNext()) {
                val mood = MoodLogEntity(
                    id = allMoods.getString(allMoods.getColumnIndexOrThrow("id")),
                    userId = allMoods.getString(allMoods.getColumnIndexOrThrow("userId")),
                    simulationId = if (allMoods.isNull(allMoods.getColumnIndexOrThrow("simulationId"))) null else allMoods.getString(allMoods.getColumnIndexOrThrow("simulationId")),
                    simulationTitle = if (allMoods.isNull(allMoods.getColumnIndexOrThrow("simulationTitle"))) null else allMoods.getString(allMoods.getColumnIndexOrThrow("simulationTitle")),
                    moodType = allMoods.getString(allMoods.getColumnIndexOrThrow("moodType")),
                    moodEmoji = allMoods.getString(allMoods.getColumnIndexOrThrow("moodEmoji")),
                    stressLevel = allMoods.getInt(allMoods.getColumnIndexOrThrow("stressLevel")),
                    wellBeingScore = allMoods.getInt(allMoods.getColumnIndexOrThrow("wellBeingScore")),
                    note = allMoods.getString(allMoods.getColumnIndexOrThrow("note")),
                    timestamp = allMoods.getLong(allMoods.getColumnIndexOrThrow("timestamp"))
                )
                moodLogsList.add(mood)
            }
            allMoods.close()

            // Construct JSON payload
            val root = JSONObject().apply {
                put("version", 1)
                put("userId", userId)
                put("userEmail", userEmail)
                put("exportedAt", System.currentTimeMillis())
                put("deviceName", deviceName)

                val simsArr = JSONArray()
                simulationsList.forEach { s ->
                    val sObj = JSONObject().apply {
                        put("id", s.id)
                        put("userId", s.userId)
                        put("title", s.title)
                        put("category", s.category)
                        put("description", s.description)
                        put("timeHorizonYears", s.timeHorizonYears)
                        put("initialCapital", s.initialCapital)
                        put("monthlySurplus", s.monthlySurplus)
                        put("riskTolerance", s.riskTolerance)
                        put("stressLevelBefore", s.stressLevelBefore)
                        put("optionsJson", s.optionsJson)
                        put("reportJson", s.reportJson)
                        put("isFavorite", s.isFavorite)
                        put("actualChosenOptionId", s.actualChosenOptionId)
                        put("reflectionNote", s.reflectionNote)
                        put("createdAt", s.createdAt)
                        put("updatedAt", s.updatedAt)
                    }
                    simsArr.put(sObj)
                }
                put("simulations", simsArr)

                val moodsArr = JSONArray()
                moodLogsList.forEach { m -> moodsArr.put(m.toJson()) }
                put("moodLogs", moodsArr)
            }

            val payloadStr = root.toString()
            val checksum = computeSha256(payloadStr)
            val backupId = UUID.randomUUID().toString().take(12)

            // Simulate cloud upload handshake & encryption
            delay(900)

            // Store in simulated secure cloud storage
            prefs.edit()
                .putString("vault_${userId}_latest", payloadStr)
                .putString("vault_${userId}_backup_id", backupId)
                .putString("vault_${userId}_checksum", checksum)
                .putLong("vault_${userId}_time", System.currentTimeMillis())
                .apply()

            // Record in local history
            val record = BackupRecordEntity(
                id = backupId,
                userId = userId,
                userEmail = userEmail,
                timestamp = System.currentTimeMillis(),
                simulationsCount = simulationsList.size,
                moodLogsCount = moodLogsList.size,
                checksum = checksum.take(8),
                status = "VERIFIED_CLOUD",
                deviceName = deviceName
            )
            backupDao.insertBackup(record)

            SyncResult(
                success = true,
                message = "Secure backup synced successfully to cloud vault!",
                backupId = backupId,
                checksum = checksum.take(8),
                totalSimulations = simulationsList.size,
                totalMoodLogs = moodLogsList.size
            )
        } catch (e: Exception) {
            SyncResult(
                success = false,
                message = "Cloud backup failed: ${e.message}"
            )
        }
    }

    suspend fun restoreFromCloud(userId: String): SyncResult = withContext(Dispatchers.IO) {
        try {
            val payloadStr = prefs.getString("vault_${userId}_latest", null)
                ?: return@withContext SyncResult(false, "No cloud backup found for this account in the secure vault.")

            val root = JSONObject(payloadStr)
            val simsArr = root.optJSONArray("simulations")
            val restoredSims = mutableListOf<SimulationEntity>()
            if (simsArr != null) {
                for (i in 0 until simsArr.length()) {
                    val s = simsArr.getJSONObject(i)
                    restoredSims.add(
                        SimulationEntity(
                            id = s.getString("id"),
                            userId = userId, // Ensure current user owns restored records
                            title = s.getString("title"),
                            category = s.getString("category"),
                            description = s.getString("description"),
                            timeHorizonYears = s.getInt("timeHorizonYears"),
                            initialCapital = s.getDouble("initialCapital"),
                            monthlySurplus = s.getDouble("monthlySurplus"),
                            riskTolerance = s.getString("riskTolerance"),
                            stressLevelBefore = s.getInt("stressLevelBefore"),
                            optionsJson = s.getString("optionsJson"),
                            reportJson = s.getString("reportJson"),
                            isFavorite = s.optBoolean("isFavorite", false),
                            actualChosenOptionId = if (s.has("actualChosenOptionId") && !s.isNull("actualChosenOptionId")) s.getString("actualChosenOptionId") else null,
                            reflectionNote = if (s.has("reflectionNote") && !s.isNull("reflectionNote")) s.getString("reflectionNote") else null,
                            createdAt = s.optLong("createdAt", System.currentTimeMillis()),
                            updatedAt = s.optLong("updatedAt", System.currentTimeMillis())
                        )
                    )
                }
            }

            val moodsArr = root.optJSONArray("moodLogs")
            val restoredMoods = mutableListOf<MoodLogEntity>()
            if (moodsArr != null) {
                for (i in 0 until moodsArr.length()) {
                    val m = moodsArr.getJSONObject(i)
                    val mood = MoodLogEntity.fromJson(m).copy(userId = userId)
                    restoredMoods.add(mood)
                }
            }

            // Save to Room DB
            database.simulationDao().insertAll(restoredSims)
            database.moodDao().insertAll(restoredMoods)

            val backupId = prefs.getString("vault_${userId}_backup_id", "vault_restore")
            val checksum = prefs.getString("vault_${userId}_checksum", "verified")

            database.backupDao().insertBackup(
                BackupRecordEntity(
                    id = UUID.randomUUID().toString().take(12),
                    userId = userId,
                    userEmail = root.optString("userEmail", ""),
                    timestamp = System.currentTimeMillis(),
                    simulationsCount = restoredSims.size,
                    moodLogsCount = restoredMoods.size,
                    checksum = checksum?.take(8) ?: "restored",
                    status = "RESTORED",
                    deviceName = "Cloud Restore"
                )
            )

            SyncResult(
                success = true,
                message = "Restored ${restoredSims.size} simulations and ${restoredMoods.size} mood records!",
                backupId = backupId,
                checksum = checksum?.take(8),
                totalSimulations = restoredSims.size,
                totalMoodLogs = restoredMoods.size
            )
        } catch (e: Exception) {
            SyncResult(false, "Restore failed: ${e.message}")
        }
    }

    fun getCloudBackupMetadata(userId: String): CloudVaultSnapshot? {
        val payload = prefs.getString("vault_${userId}_latest", null) ?: return null
        return try {
            val root = JSONObject(payload)
            CloudVaultSnapshot(
                backupId = prefs.getString("vault_${userId}_backup_id", "unknown") ?: "unknown",
                userId = userId,
                userEmail = root.optString("userEmail", ""),
                deviceName = root.optString("deviceName", "Android Device"),
                timestamp = prefs.getLong("vault_${userId}_time", System.currentTimeMillis()),
                checksum = prefs.getString("vault_${userId}_checksum", "verified") ?: "verified",
                payloadJson = payload,
                simulationsCount = root.optJSONArray("simulations")?.length() ?: 0,
                moodLogsCount = root.optJSONArray("moodLogs")?.length() ?: 0
            )
        } catch (e: Exception) {
            null
        }
    }

    private fun computeSha256(input: String): String {
        val bytes = MessageDigest.getInstance("SHA-256").digest(input.toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
    }
}
