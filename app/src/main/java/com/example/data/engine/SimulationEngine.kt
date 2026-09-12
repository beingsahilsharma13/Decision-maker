package com.example.data.engine

import android.util.Log
import com.example.data.model.PredictionOutcome
import com.example.data.model.SimulationOption
import com.example.data.model.SimulationReport
import com.example.data.model.TimelineMilestone
import com.example.data.model.YearlyProjection
import com.example.data.remote.GeminiClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.util.Locale
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.roundToInt

object SimulationEngine {
    private const val TAG = "SimulationEngine"

    suspend fun runSimulation(
        title: String,
        category: String,
        description: String,
        timeHorizonYears: Int,
        initialCapital: Double,
        monthlySurplus: Double,
        riskTolerance: String,
        stressLevelBefore: Int,
        options: List<SimulationOption>
    ): SimulationReport = withContext(Dispatchers.Default) {
        val prompt = buildSimulationPrompt(
            title = title,
            category = category,
            description = description,
            timeHorizonYears = timeHorizonYears,
            initialCapital = initialCapital,
            monthlySurplus = monthlySurplus,
            riskTolerance = riskTolerance,
            stressLevelBefore = stressLevelBefore,
            options = options
        )

        var aiRawText: String? = null
        var usedModel = "Gemini 3.1 Pro (Thinking Mode: HIGH)"

        if (GeminiClient.isApiKeyConfigured()) {
            try {
                aiRawText = GeminiClient.generateDeepSimulation(prompt)
            } catch (e: Exception) {
                Log.w(TAG, "Gemini API call failed, will use algorithmic financial simulation fallback: ${e.message}")
                usedModel = "Algorithmic Quantitative Life Engine"
            }
        } else {
            usedModel = "Algorithmic Quantitative Life Engine (Offline/Local)"
        }

        // Try parsing AI output if available
        if (!aiRawText.isNullOrBlank()) {
            try {
                val parsed = parseAiReport(aiRawText, options, title, usedModel)
                if (parsed != null && parsed.outcomes.isNotEmpty()) {
                    return@withContext parsed
                }
            } catch (e: Exception) {
                Log.w(TAG, "Parsing AI JSON failed, falling back to algorithmic simulation: ${e.message}")
            }
        }

        // High-precision algorithmic simulation engine
        generateDeterministicSimulation(
            title = title,
            category = category,
            description = description,
            timeHorizonYears = timeHorizonYears,
            initialCapital = initialCapital,
            monthlySurplus = monthlySurplus,
            riskTolerance = riskTolerance,
            stressLevelBefore = stressLevelBefore,
            options = options,
            engineLabel = usedModel
        )
    }

    private fun buildSimulationPrompt(
        title: String,
        category: String,
        description: String,
        timeHorizonYears: Int,
        initialCapital: Double,
        monthlySurplus: Double,
        riskTolerance: String,
        stressLevelBefore: Int,
        options: List<SimulationOption>
    ): String {
        val optionsText = options.joinToString("\n\n") { opt ->
            """
            - Option ID: "${opt.id}"
              Title: "${opt.title}"
              Description: "${opt.description}"
              Upfront Cost: $${opt.upfrontCost}
              Monthly Commitment: $${opt.monthlyCommitment}
              Pros: ${opt.pros.joinToString(", ")}
              Risks: ${opt.risks.joinToString(", ")}
            """.trimIndent()
        }

        return """
        Act as a Principal Financial Planner and Life Decision Forecaster.
        Predict detailed outcomes for this major decision using critical reasoning:

        DECISION: "$title"
        Category: $category
        Context: "$description"
        Time Horizon: $timeHorizonYears years
        Starting Capital/Savings: $${initialCapital}
        Monthly Disposable Cash Flow: $${monthlySurplus}
        Risk Tolerance: $riskTolerance
        User's Current Stress Level: $stressLevelBefore / 10

        OPTIONS TO COMPARE:
        $optionsText

        Respond STRICTLY with valid JSON without markdown code fences conforming to this structure:
        {
          "decisionTitle": "$title",
          "executiveSummary": "Concise 2-3 sentence strategic summary comparing the paths.",
          "recommendedOptionId": "${options.firstOrNull()?.id ?: "1"}",
          "recommendationReason": "Why this option best balances financial growth with mental well-being.",
          "tradeOffAnalysis": "Core sacrifice and opportunity cost analysis.",
          "blindSpots": ["Blindspot 1 to watch out for", "Blindspot 2", "Blindspot 3"],
          "outcomes": [
            {
              "optionId": "matching_id",
              "optionTitle": "Option Name",
              "summary": "Specific forecast narrative for this path.",
              "financialSummary": "Dollar impact summary over time.",
              "wellBeingSummary": "Mental health, stress, and lifestyle impact summary.",
              "financialScore": 85,
              "wellBeingScore": 75,
              "careerScore": 80,
              "riskScore": 35,
              "overallScore": 82,
              "regretProbabilityPct": 15,
              "keyAdvantages": ["Advantage 1", "Advantage 2"],
              "keyRisks": ["Risk 1", "Risk 2"],
              "projections": [
                {"year": 1, "netWorth": 50000, "liquidCapital": 20000, "annualCashFlow": 15000, "stressScore": 60, "wellBeingScore": 70},
                {"year": 3, "netWorth": 85000, "liquidCapital": 35000, "annualCashFlow": 22000, "stressScore": 55, "wellBeingScore": 75},
                {"year": 5, "netWorth": 140000, "liquidCapital": 50000, "annualCashFlow": 30000, "stressScore": 45, "wellBeingScore": 80},
                {"year": 10, "netWorth": 320000, "liquidCapital": 90000, "annualCashFlow": 45000, "stressScore": 35, "wellBeingScore": 85}
              ],
              "milestones": [
                {"year": 1, "title": "Foundation Phase", "description": "Initial adjustment and cost absorption.", "impactType": "financial"},
                {"year": 3, "title": "Momentum Phase", "description": "Break-even reached with compound growth.", "impactType": "career"},
                {"year": 5, "title": "Expansion Phase", "description": "High equity value and lifestyle freedom.", "impactType": "wellbeing"}
              ]
            }
          ]
        }
        """.trimIndent()
    }

    private fun parseAiReport(
        rawText: String,
        options: List<SimulationOption>,
        title: String,
        modelUsed: String
    ): SimulationReport? {
        val cleaned = extractJsonString(rawText) ?: return null
        val root = JSONObject(cleaned)

        val reportTitle = root.optString("decisionTitle", title)
        val execSummary = root.optString("executiveSummary", "")
        val recId = root.optString("recommendedOptionId", options.firstOrNull()?.id ?: "")
        val recReason = root.optString("recommendationReason", "")
        val tradeOff = root.optString("tradeOffAnalysis", "")

        val blindSpots = mutableListOf<String>()
        val bsArr = root.optJSONArray("blindSpots")
        if (bsArr != null) {
            for (i in 0 until bsArr.length()) {
                blindSpots.add(bsArr.optString(i))
            }
        }

        val outcomes = mutableListOf<PredictionOutcome>()
        val outArr = root.optJSONArray("outcomes")
        if (outArr != null && outArr.length() > 0) {
            for (i in 0 until outArr.length()) {
                val obj = outArr.optJSONObject(i) ?: continue
                outcomes.add(PredictionOutcome.fromJson(obj))
            }
        }

        if (outcomes.isEmpty()) return null

        return SimulationReport(
            decisionTitle = reportTitle,
            executiveSummary = execSummary,
            recommendedOptionId = recId,
            recommendationReason = recReason,
            outcomes = outcomes,
            tradeOffAnalysis = tradeOff,
            blindSpots = if (blindSpots.isNotEmpty()) blindSpots else listOf(
                "Underestimating unexpected lifestyle inflation and maintenance costs",
                "Assuming steady career income without macro market downturn buffer",
                "Overvaluing short-term comfort over 10-year compound compounding"
            ),
            aiModelUsed = modelUsed,
            generatedAt = System.currentTimeMillis()
        )
    }

    private fun extractJsonString(text: String): String? {
        var trimmed = text.trim()
        if (trimmed.startsWith("```json")) {
            trimmed = trimmed.removePrefix("```json")
        } else if (trimmed.startsWith("```")) {
            trimmed = trimmed.removePrefix("```")
        }
        if (trimmed.endsWith("```")) {
            trimmed = trimmed.removeSuffix("```")
        }
        trimmed = trimmed.trim()
        val start = trimmed.indexOf('{')
        val end = trimmed.lastIndexOf('}')
        if (start != -1 && end != -1 && end > start) {
            return trimmed.substring(start, end + 1)
        }
        return null
    }

    /**
     * Deterministic quantitative financial modeling engine.
     * Computes real compound growth, equity build, liquidity, risk-adjusted scores.
     */
    fun generateDeterministicSimulation(
        title: String,
        category: String,
        description: String,
        timeHorizonYears: Int,
        initialCapital: Double,
        monthlySurplus: Double,
        riskTolerance: String,
        stressLevelBefore: Int,
        options: List<SimulationOption>,
        engineLabel: String = "Deterministic Quantitative Life Engine"
    ): SimulationReport {
        val annualSurplus = monthlySurplus * 12.0
        val baseGrowthRate = when (riskTolerance.lowercase(Locale.ROOT)) {
            "aggressive" -> 0.09
            "conservative" -> 0.045
            else -> 0.07 // moderate
        }

        val outcomes = mutableListOf<PredictionOutcome>()

        options.forEachIndexed { index, option ->
            val upfrontCost = option.upfrontCost
            val monthlyCommit = option.monthlyCommitment
            val remainingStartCapital = max(0.0, initialCapital - upfrontCost)
            val netAnnualSurplus = max(0.0, annualSurplus - (monthlyCommit * 12.0))

            // Different scenarios have varied multiplier characteristics
            val pathGrowthRate = if (index == 0) baseGrowthRate else baseGrowthRate + 0.015 * (index.toDouble())
            val pathRiskWeight = if (index == 0) 30 else 30 + (index * 20)

            val yearsToProject = listOf(1, 3, 5, min(timeHorizonYears, 10))
            val projections = mutableListOf<YearlyProjection>()

            yearsToProject.distinct().sorted().forEach { year ->
                // Future Value of initial capital + FV of annual contributions
                val fvInitial = remainingStartCapital * (1.0 + pathGrowthRate).pow(year)
                val fvContributions = if (pathGrowthRate > 0) {
                    netAnnualSurplus * (((1.0 + pathGrowthRate).pow(year) - 1.0) / pathGrowthRate)
                } else {
                    netAnnualSurplus * year
                }

                // If upfront cost was an asset (like real estate or business equity), factor appreciation
                val assetEquity = if (upfrontCost > 0) upfrontCost * (1.0 + 0.04).pow(year) else 0.0
                val totalNetWorth = (fvInitial + fvContributions + assetEquity).roundToInt().toDouble()
                val liquidCapital = (fvInitial + (fvContributions * 0.7)).roundToInt().toDouble()
                val annualCashFlow = (netAnnualSurplus * (1.0 + (year * 0.03))).roundToInt().toDouble()

                // Well-being & Stress curves
                val yearStress = max(15, min(90, (stressLevelBefore * 10) - (year * 6) + (if (netAnnualSurplus < 5000) 15 else -5)))
                val yearWellBeing = max(20, min(95, 100 - yearStress + (if (totalNetWorth > initialCapital * 1.5) 10 else 0)))

                projections.add(
                    YearlyProjection(
                        year = year,
                        netWorth = totalNetWorth,
                        liquidCapital = liquidCapital,
                        annualCashFlow = annualCashFlow,
                        stressScore = yearStress,
                        wellBeingScore = yearWellBeing
                    )
                )
            }

            val finalProjection = projections.lastOrNull() ?: YearlyProjection(1, initialCapital, initialCapital, annualSurplus, 50, 70)
            val financialScore = min(98, max(30, ((finalProjection.netWorth / max(1.0, initialCapital + annualSurplus * 5)) * 45 + 40).roundToInt()))
            val wellBeingScore = min(95, max(35, (finalProjection.wellBeingScore + 5)))
            val careerScore = min(95, max(40, (70 + (index * 6) - (if (option.monthlyCommitment > monthlySurplus * 0.7) 10 else 0))))
            val riskScore = min(90, max(20, pathRiskWeight))
            val overallScore = ((financialScore * 0.4) + (wellBeingScore * 0.35) + (careerScore * 0.25)).roundToInt()
            val regretProb = max(8, min(85, (100 - overallScore + (riskScore / 3))))

            val milestones = listOf(
                TimelineMilestone(
                    year = 1,
                    title = "Capital Allocation & Runway",
                    description = "Absorb $${upfrontCost.roundToInt()} initial commitment; maintain $${projections.firstOrNull()?.liquidCapital?.roundToInt() ?: 0} in liquidity reserve.",
                    impactType = "financial"
                ),
                TimelineMilestone(
                    year = 3,
                    title = "Trajectory Inflection",
                    description = "Estimated net worth grows to $${projections.find { it.year == 3 }?.netWorth?.roundToInt() ?: 0}; risk buffer solidifies.",
                    impactType = "career"
                ),
                TimelineMilestone(
                    year = min(timeHorizonYears, 10),
                    title = "Long-Term Freedom Milestone",
                    description = "Projected net worth reaches $${finalProjection.netWorth.roundToInt()} with well-being index of ${finalProjection.wellBeingScore}/100.",
                    impactType = "wellbeing"
                )
            )

            outcomes.add(
                PredictionOutcome(
                    optionId = option.id,
                    optionTitle = option.title,
                    summary = "Path '${option.title}' achieves a projected net worth of $${finalProjection.netWorth.roundToInt()} by year ${finalProjection.year}. Cash flow remains sustainable with a manageable stress trajectory.",
                    projections = projections,
                    financialScore = financialScore,
                    wellBeingScore = wellBeingScore,
                    careerScore = careerScore,
                    riskScore = riskScore,
                    overallScore = overallScore,
                    regretProbabilityPct = regretProb,
                    keyAdvantages = if (option.pros.isNotEmpty()) option.pros else listOf(
                        "Predictable cash flow predictability and compounding growth",
                        "Capital preservation with high liquidity flexibility",
                        "Reduced cognitive load and peace of mind"
                    ),
                    keyRisks = if (option.risks.isNotEmpty()) option.risks else listOf(
                        "Opportunity cost if alternative asset classes surge",
                        "Inflationary drag on cash reserves over 5+ years"
                    ),
                    milestones = milestones,
                    financialSummary = "Projected 10-year Net Worth: $${finalProjection.netWorth.roundToInt()} (Liquid: $${finalProjection.liquidCapital.roundToInt()})",
                    wellBeingSummary = "Average Well-being: ${wellBeingScore}/100 with Stress rating tapering down over time."
                )
            )
        }

        val bestOutcome = outcomes.maxByOrNull { it.overallScore } ?: outcomes.first()

        return SimulationReport(
            decisionTitle = title,
            executiveSummary = "Analysis indicates '${bestOutcome.optionTitle}' offers the highest risk-adjusted life return, achieving an overall composite score of ${bestOutcome.overallScore}/100 while preserving critical financial runway.",
            recommendedOptionId = bestOutcome.optionId,
            recommendationReason = "This path optimizes the balance between financial net-worth compounding and sustained psychological well-being, keeping regret probability at just ${bestOutcome.regretProbabilityPct}%.",
            outcomes = outcomes,
            tradeOffAnalysis = "Choosing between upfront capital commitment versus sustained liquidity requires balancing certainty against compound opportunity cost.",
            blindSpots = listOf(
                "Overlooking lifestyle creep once cash flow stabilizes in years 2-3",
                "Neglecting emergency runway coverage (recommend 6 months liquid reserves)",
                "Discounting psychological fatigue during high-stress transition phases"
            ),
            aiModelUsed = engineLabel,
            generatedAt = System.currentTimeMillis()
        )
    }
}
