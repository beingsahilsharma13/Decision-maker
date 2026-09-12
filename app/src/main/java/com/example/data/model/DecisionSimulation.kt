package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import org.json.JSONArray
import org.json.JSONObject

@Entity(tableName = "simulations")
data class SimulationEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val title: String,
    val category: String,
    val description: String,
    val timeHorizonYears: Int,
    val initialCapital: Double,
    val monthlySurplus: Double,
    val riskTolerance: String,
    val stressLevelBefore: Int,
    val optionsJson: String,
    val reportJson: String,
    val isFavorite: Boolean = false,
    val actualChosenOptionId: String? = null,
    val reflectionNote: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

data class SimulationOption(
    val id: String,
    val title: String,
    val description: String,
    val upfrontCost: Double = 0.0,
    val monthlyCommitment: Double = 0.0,
    val pros: List<String> = emptyList(),
    val risks: List<String> = emptyList()
) {
    fun toJson(): JSONObject {
        return JSONObject().apply {
            put("id", id)
            put("title", title)
            put("description", description)
            put("upfrontCost", upfrontCost)
            put("monthlyCommitment", monthlyCommitment)
            val prosArr = JSONArray()
            pros.forEach { prosArr.put(it) }
            put("pros", prosArr)
            val risksArr = JSONArray()
            risks.forEach { risksArr.put(it) }
            put("risks", risksArr)
        }
    }

    companion object {
        fun fromJson(obj: JSONObject): SimulationOption {
            val prosList = mutableListOf<String>()
            val prosArr = obj.optJSONArray("pros")
            if (prosArr != null) {
                for (i in 0 until prosArr.length()) {
                    prosList.add(prosArr.optString(i))
                }
            }
            val risksList = mutableListOf<String>()
            val risksArr = obj.optJSONArray("risks")
            if (risksArr != null) {
                for (i in 0 until risksArr.length()) {
                    risksList.add(risksArr.optString(i))
                }
            }
            return SimulationOption(
                id = obj.optString("id"),
                title = obj.optString("title"),
                description = obj.optString("description"),
                upfrontCost = obj.optDouble("upfrontCost", 0.0),
                monthlyCommitment = obj.optDouble("monthlyCommitment", 0.0),
                pros = prosList,
                risks = risksList
            )
        }
    }
}

data class YearlyProjection(
    val year: Int,
    val netWorth: Double,
    val liquidCapital: Double,
    val annualCashFlow: Double,
    val stressScore: Int,
    val wellBeingScore: Int
) {
    fun toJson(): JSONObject {
        return JSONObject().apply {
            put("year", year)
            put("netWorth", netWorth)
            put("liquidCapital", liquidCapital)
            put("annualCashFlow", annualCashFlow)
            put("stressScore", stressScore)
            put("wellBeingScore", wellBeingScore)
        }
    }

    companion object {
        fun fromJson(obj: JSONObject): YearlyProjection {
            return YearlyProjection(
                year = obj.optInt("year", 1),
                netWorth = obj.optDouble("netWorth", 0.0),
                liquidCapital = obj.optDouble("liquidCapital", 0.0),
                annualCashFlow = obj.optDouble("annualCashFlow", 0.0),
                stressScore = obj.optInt("stressScore", 50),
                wellBeingScore = obj.optInt("wellBeingScore", 50)
            )
        }
    }
}

data class TimelineMilestone(
    val year: Int,
    val title: String,
    val description: String,
    val impactType: String // "financial", "career", "wellbeing", "risk"
) {
    fun toJson(): JSONObject {
        return JSONObject().apply {
            put("year", year)
            put("title", title)
            put("description", description)
            put("impactType", impactType)
        }
    }

    companion object {
        fun fromJson(obj: JSONObject): TimelineMilestone {
            return TimelineMilestone(
                year = obj.optInt("year", 1),
                title = obj.optString("title"),
                description = obj.optString("description"),
                impactType = obj.optString("impactType", "financial")
            )
        }
    }
}

data class PredictionOutcome(
    val optionId: String,
    val optionTitle: String,
    val summary: String,
    val projections: List<YearlyProjection>,
    val financialScore: Int, // 0-100
    val wellBeingScore: Int, // 0-100
    val careerScore: Int, // 0-100
    val riskScore: Int, // 0-100 (lower is safer)
    val overallScore: Int, // 0-100
    val regretProbabilityPct: Int, // 0-100
    val keyAdvantages: List<String>,
    val keyRisks: List<String>,
    val milestones: List<TimelineMilestone>,
    val financialSummary: String,
    val wellBeingSummary: String
) {
    fun toJson(): JSONObject {
        val obj = JSONObject()
        obj.put("optionId", optionId)
        obj.put("optionTitle", optionTitle)
        obj.put("summary", summary)
        
        val projArr = JSONArray()
        projections.forEach { projArr.put(it.toJson()) }
        obj.put("projections", projArr)

        obj.put("financialScore", financialScore)
        obj.put("wellBeingScore", wellBeingScore)
        obj.put("careerScore", careerScore)
        obj.put("riskScore", riskScore)
        obj.put("overallScore", overallScore)
        obj.put("regretProbabilityPct", regretProbabilityPct)

        val advArr = JSONArray()
        keyAdvantages.forEach { advArr.put(it) }
        obj.put("keyAdvantages", advArr)

        val riskArr = JSONArray()
        keyRisks.forEach { riskArr.put(it) }
        obj.put("keyRisks", riskArr)

        val mArr = JSONArray()
        milestones.forEach { mArr.put(it.toJson()) }
        obj.put("milestones", mArr)

        obj.put("financialSummary", financialSummary)
        obj.put("wellBeingSummary", wellBeingSummary)
        return obj
    }

    companion object {
        fun fromJson(obj: JSONObject): PredictionOutcome {
            val projList = mutableListOf<YearlyProjection>()
            val projArr = obj.optJSONArray("projections")
            if (projArr != null) {
                for (i in 0 until projArr.length()) {
                    val p = projArr.optJSONObject(i)
                    if (p != null) projList.add(YearlyProjection.fromJson(p))
                }
            }

            val advList = mutableListOf<String>()
            val advArr = obj.optJSONArray("keyAdvantages")
            if (advArr != null) {
                for (i in 0 until advArr.length()) {
                    advList.add(advArr.optString(i))
                }
            }

            val riskList = mutableListOf<String>()
            val riskArr = obj.optJSONArray("keyRisks")
            if (riskArr != null) {
                for (i in 0 until riskArr.length()) {
                    riskList.add(riskArr.optString(i))
                }
            }

            val mList = mutableListOf<TimelineMilestone>()
            val mArr = obj.optJSONArray("milestones")
            if (mArr != null) {
                for (i in 0 until mArr.length()) {
                    val m = mArr.optJSONObject(i)
                    if (m != null) mList.add(TimelineMilestone.fromJson(m))
                }
            }

            return PredictionOutcome(
                optionId = obj.optString("optionId"),
                optionTitle = obj.optString("optionTitle"),
                summary = obj.optString("summary"),
                projections = projList,
                financialScore = obj.optInt("financialScore", 70),
                wellBeingScore = obj.optInt("wellBeingScore", 70),
                careerScore = obj.optInt("careerScore", 70),
                riskScore = obj.optInt("riskScore", 40),
                overallScore = obj.optInt("overallScore", 75),
                regretProbabilityPct = obj.optInt("regretProbabilityPct", 20),
                keyAdvantages = advList,
                keyRisks = riskList,
                milestones = mList,
                financialSummary = obj.optString("financialSummary"),
                wellBeingSummary = obj.optString("wellBeingSummary")
            )
        }
    }
}

data class SimulationReport(
    val decisionTitle: String,
    val executiveSummary: String,
    val recommendedOptionId: String,
    val recommendationReason: String,
    val outcomes: List<PredictionOutcome>,
    val tradeOffAnalysis: String,
    val blindSpots: List<String>,
    val aiModelUsed: String,
    val generatedAt: Long = System.currentTimeMillis()
) {
    fun toJson(): JSONObject {
        val obj = JSONObject()
        obj.put("decisionTitle", decisionTitle)
        obj.put("executiveSummary", executiveSummary)
        obj.put("recommendedOptionId", recommendedOptionId)
        obj.put("recommendationReason", recommendationReason)

        val outArr = JSONArray()
        outcomes.forEach { outArr.put(it.toJson()) }
        obj.put("outcomes", outArr)

        obj.put("tradeOffAnalysis", tradeOffAnalysis)

        val bsArr = JSONArray()
        blindSpots.forEach { bsArr.put(it) }
        obj.put("blindSpots", bsArr)

        obj.put("aiModelUsed", aiModelUsed)
        obj.put("generatedAt", generatedAt)
        return obj
    }

    companion object {
        fun fromJson(obj: JSONObject): SimulationReport {
            val outList = mutableListOf<PredictionOutcome>()
            val outArr = obj.optJSONArray("outcomes")
            if (outArr != null) {
                for (i in 0 until outArr.length()) {
                    val item = outArr.optJSONObject(i)
                    if (item != null) outList.add(PredictionOutcome.fromJson(item))
                }
            }

            val bsList = mutableListOf<String>()
            val bsArr = obj.optJSONArray("blindSpots")
            if (bsArr != null) {
                for (i in 0 until bsArr.length()) {
                    bsList.add(bsArr.optString(i))
                }
            }

            return SimulationReport(
                decisionTitle = obj.optString("decisionTitle"),
                executiveSummary = obj.optString("executiveSummary"),
                recommendedOptionId = obj.optString("recommendedOptionId"),
                recommendationReason = obj.optString("recommendationReason"),
                outcomes = outList,
                tradeOffAnalysis = obj.optString("tradeOffAnalysis"),
                blindSpots = bsList,
                aiModelUsed = obj.optString("aiModelUsed", "Gemini 3.1 Pro (Thinking Mode)"),
                generatedAt = obj.optLong("generatedAt", System.currentTimeMillis())
            )
        }
    }
}
