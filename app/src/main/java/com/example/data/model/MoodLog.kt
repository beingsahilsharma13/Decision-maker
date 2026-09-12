package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import org.json.JSONObject

@Entity(tableName = "mood_logs")
data class MoodLogEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val simulationId: String? = null,
    val simulationTitle: String? = null,
    val moodType: String, // "Optimistic", "Confident", "Anxious", "Stressed", "Overwhelmed", "Peaceful"
    val moodEmoji: String,
    val stressLevel: Int, // 1 - 10
    val wellBeingScore: Int, // 1 - 10
    val note: String,
    val timestamp: Long = System.currentTimeMillis()
) {
    fun toJson(): JSONObject {
        return JSONObject().apply {
            put("id", id)
            put("userId", userId)
            put("simulationId", simulationId)
            put("simulationTitle", simulationTitle)
            put("moodType", moodType)
            put("moodEmoji", moodEmoji)
            put("stressLevel", stressLevel)
            put("wellBeingScore", wellBeingScore)
            put("note", note)
            put("timestamp", timestamp)
        }
    }

    companion object {
        fun fromJson(obj: JSONObject): MoodLogEntity {
            return MoodLogEntity(
                id = obj.optString("id"),
                userId = obj.optString("userId"),
                simulationId = if (obj.has("simulationId") && !obj.isNull("simulationId")) obj.optString("simulationId") else null,
                simulationTitle = if (obj.has("simulationTitle") && !obj.isNull("simulationTitle")) obj.optString("simulationTitle") else null,
                moodType = obj.optString("moodType", "Peaceful"),
                moodEmoji = obj.optString("moodEmoji", "😌"),
                stressLevel = obj.optInt("stressLevel", 5),
                wellBeingScore = obj.optInt("wellBeingScore", 7),
                note = obj.optString("note", ""),
                timestamp = obj.optLong("timestamp", System.currentTimeMillis())
            )
        }
    }
}
