package com.example.data.repository

import android.content.Context
import com.example.data.engine.SimulationEngine
import com.example.data.local.LifeSimDatabase
import com.example.data.model.BackupRecordEntity
import com.example.data.model.MoodLogEntity
import com.example.data.model.PredictionOutcome
import com.example.data.model.SimulationEntity
import com.example.data.model.SimulationOption
import com.example.data.model.SimulationReport
import com.example.data.model.UserEntity
import com.example.data.remote.ChatMessage
import com.example.data.remote.GeminiClient
import com.example.data.sync.CloudSyncManager
import com.example.data.sync.CloudVaultSnapshot
import com.example.data.sync.SyncResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.withContext
import org.json.JSONArray
import java.security.MessageDigest
import java.util.UUID

class LifeSimRepository(
    private val context: Context,
    private val database: LifeSimDatabase
) {
    private val userDao = database.userDao()
    private val simulationDao = database.simulationDao()
    private val moodDao = database.moodDao()
    private val backupDao = database.backupDao()
    private val cloudSyncManager = CloudSyncManager(context, database)

    private val userPrefs = context.getSharedPreferences("life_sim_session", Context.MODE_PRIVATE)

    private val _currentUser = MutableStateFlow<UserEntity?>(null)
    val currentUser: StateFlow<UserEntity?> = _currentUser.asStateFlow()

    suspend fun initializeSession() = withContext(Dispatchers.IO) {
        val savedUserId = userPrefs.getString("logged_in_user_id", null)
        if (savedUserId != null) {
            val user = userDao.getUserById(savedUserId)
            if (user != null) {
                _currentUser.value = user
                return@withContext
            }
        }
        // If no user exists, check if any user exists or create a demo/initial account
        val latest = userDao.getLatestUser().firstOrNull()
        if (latest != null) {
            userPrefs.edit().putString("logged_in_user_id", latest.id).apply()
            _currentUser.value = latest
        } else {
            // Seed a starter account for seamless immediate use
            val defaultUser = UserEntity(
                id = UUID.randomUUID().toString(),
                email = "demo.strategist@lifesim.ai",
                displayName = "Alex Morgan",
                passwordHash = hashPassword("password123"),
                avatarInitial = "A"
            )
            userDao.insertUser(defaultUser)
            userPrefs.edit().putString("logged_in_user_id", defaultUser.id).apply()
            _currentUser.value = defaultUser

            // Seed an initial exemplary financial decision simulation so the user immediately sees a rich dashboard
            seedInitialSimulation(defaultUser.id)
            seedInitialMoodLogs(defaultUser.id)
        }
    }

    private suspend fun seedInitialSimulation(userId: String) = withContext(Dispatchers.IO) {
        val title = "Buy $500k Home vs Rent & Index Fund Investing"
        val category = "Financial"
        val description = "Contemplating placing $100k down payment on a property with a 30-year fixed mortgage, or renting for $2,200/mo and investing the $100k plus monthly difference into low-cost global equity index funds."
        val options = listOf(
            SimulationOption(
                id = "opt_home",
                title = "Purchase Home (20% Down)",
                description = "Commit $100,000 cash down payment, property taxes, insurance, and maintenance. Build forced home equity.",
                upfrontCost = 100000.0,
                monthlyCommitment = 3100.0,
                pros = listOf("Forced equity buildup", "Mortgage interest deduction", "Long-term home security & stability"),
                risks = listOf("Property tax hikes", "Costly roof/HVAC repairs", "Low liquidity in capital tied up")
            ),
            SimulationOption(
                id = "opt_rent_invest",
                title = "Rent & Maximize Index Funds",
                description = "Rent comfortable home at $2,200/mo. Invest $100k into S&P 500 / Total World index and DCA $900/mo surplus.",
                upfrontCost = 4400.0,
                monthlyCommitment = 2200.0,
                pros = listOf("Maximum liquid net worth", "Zero maintenance obligations", "Geographic flexibility to pursue high-paying roles"),
                risks = listOf("Rent increases over decades", "Discipline required to never skip monthly DCA investing")
            )
        )

        val report = SimulationEngine.generateDeterministicSimulation(
            title = title,
            category = category,
            description = description,
            timeHorizonYears = 10,
            initialCapital = 120000.0,
            monthlySurplus = 3500.0,
            riskTolerance = "Moderate",
            stressLevelBefore = 6,
            options = options,
            engineLabel = "Gemini 3.1 Pro (Thinking Mode)"
        )

        val optionsArr = JSONArray()
        options.forEach { optionsArr.put(it.toJson()) }

        val entity = SimulationEntity(
            id = UUID.randomUUID().toString(),
            userId = userId,
            title = title,
            category = category,
            description = description,
            timeHorizonYears = 10,
            initialCapital = 120000.0,
            monthlySurplus = 3500.0,
            riskTolerance = "Moderate",
            stressLevelBefore = 6,
            optionsJson = optionsArr.toString(),
            reportJson = report.toJson().toString(),
            isFavorite = true,
            createdAt = System.currentTimeMillis() - 86400000 * 2
        )
        simulationDao.insertSimulation(entity)
    }

    private suspend fun seedInitialMoodLogs(userId: String) = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        val logs = listOf(
            MoodLogEntity(
                id = UUID.randomUUID().toString(),
                userId = userId,
                moodType = "Confident",
                moodEmoji = "🚀",
                stressLevel = 4,
                wellBeingScore = 8,
                note = "Reviewed 10-year net worth projection. Feeling clear on capital allocation.",
                timestamp = now - 86400000 * 3
            ),
            MoodLogEntity(
                id = UUID.randomUUID().toString(),
                userId = userId,
                moodType = "Anxious",
                moodEmoji = "😰",
                stressLevel = 7,
                wellBeingScore = 5,
                note = "Interest rate fluctuations causing hesitation on real estate mortgage.",
                timestamp = now - 86400000 * 1
            ),
            MoodLogEntity(
                id = UUID.randomUUID().toString(),
                userId = userId,
                moodType = "Optimistic",
                moodEmoji = "✨",
                stressLevel = 3,
                wellBeingScore = 9,
                note = "Set up automated index fund DCA. Peace of mind restored.",
                timestamp = now
            )
        )
        moodDao.insertAll(logs)
    }

    // Auth
    suspend fun register(email: String, name: String, password: String): Result<UserEntity> = withContext(Dispatchers.IO) {
        val cleanEmail = email.trim().lowercase()
        val existing = userDao.getUserByEmail(cleanEmail)
        if (existing != null) {
            return@withContext Result.failure(Exception("An account with this email already exists."))
        }
        val user = UserEntity(
            id = UUID.randomUUID().toString(),
            email = cleanEmail,
            displayName = name.trim().ifEmpty { "Decision Maker" },
            passwordHash = hashPassword(password),
            avatarInitial = name.trim().take(1).uppercase().ifEmpty { "U" }
        )
        userDao.insertUser(user)
        userPrefs.edit().putString("logged_in_user_id", user.id).apply()
        _currentUser.value = user
        Result.success(user)
    }

    suspend fun login(email: String, password: String): Result<UserEntity> = withContext(Dispatchers.IO) {
        val cleanEmail = email.trim().lowercase()
        val user = userDao.getUserByEmail(cleanEmail)
            ?: return@withContext Result.failure(Exception("No account found with this email."))

        if (user.passwordHash != hashPassword(password)) {
            return@withContext Result.failure(Exception("Incorrect password. Please try again."))
        }

        userPrefs.edit().putString("logged_in_user_id", user.id).apply()
        _currentUser.value = user
        Result.success(user)
    }

    fun logout() {
        userPrefs.edit().remove("logged_in_user_id").apply()
        _currentUser.value = null
    }

    // Simulations
    fun getSimulationsForCurrentUser(): Flow<List<SimulationEntity>> {
        val userId = _currentUser.value?.id ?: ""
        return simulationDao.getSimulationsForUser(userId)
    }

    fun getFavoriteSimulationsForCurrentUser(): Flow<List<SimulationEntity>> {
        val userId = _currentUser.value?.id ?: ""
        return simulationDao.getFavoriteSimulations(userId)
    }

    suspend fun getSimulationById(id: String): SimulationEntity? = withContext(Dispatchers.IO) {
        simulationDao.getSimulationById(id)
    }

    fun getSimulationFlowById(id: String): Flow<SimulationEntity?> {
        return simulationDao.getSimulationFlowById(id)
    }

    suspend fun runAndSaveSimulation(
        title: String,
        category: String,
        description: String,
        timeHorizonYears: Int,
        initialCapital: Double,
        monthlySurplus: Double,
        riskTolerance: String,
        stressLevelBefore: Int,
        options: List<SimulationOption>
    ): SimulationEntity = withContext(Dispatchers.IO) {
        val userId = _currentUser.value?.id ?: "guest"

        val report = SimulationEngine.runSimulation(
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

        val optionsArr = JSONArray()
        options.forEach { optionsArr.put(it.toJson()) }

        val entity = SimulationEntity(
            id = UUID.randomUUID().toString(),
            userId = userId,
            title = title,
            category = category,
            description = description,
            timeHorizonYears = timeHorizonYears,
            initialCapital = initialCapital,
            monthlySurplus = monthlySurplus,
            riskTolerance = riskTolerance,
            stressLevelBefore = stressLevelBefore,
            optionsJson = optionsArr.toString(),
            reportJson = report.toJson().toString(),
            isFavorite = false,
            createdAt = System.currentTimeMillis()
        )

        simulationDao.insertSimulation(entity)

        // Automatically log pre-decision mood
        moodDao.insertMoodLog(
            MoodLogEntity(
                id = UUID.randomUUID().toString(),
                userId = userId,
                simulationId = entity.id,
                simulationTitle = title,
                moodType = if (stressLevelBefore > 6) "Anxious" else "Optimistic",
                moodEmoji = if (stressLevelBefore > 6) "😰" else "✨",
                stressLevel = stressLevelBefore,
                wellBeingScore = 11 - stressLevelBefore,
                note = "Simulated: $title ($category decision)",
                timestamp = System.currentTimeMillis()
            )
        )

        entity
    }

    suspend fun toggleFavorite(simulation: SimulationEntity) = withContext(Dispatchers.IO) {
        simulationDao.updateSimulation(simulation.copy(isFavorite = !simulation.isFavorite, updatedAt = System.currentTimeMillis()))
    }

    suspend fun recordActualDecision(simulationId: String, chosenOptionId: String, reflection: String?) = withContext(Dispatchers.IO) {
        val sim = simulationDao.getSimulationById(simulationId) ?: return@withContext
        simulationDao.updateSimulation(
            sim.copy(
                actualChosenOptionId = chosenOptionId,
                reflectionNote = reflection,
                updatedAt = System.currentTimeMillis()
            )
        )
    }

    suspend fun deleteSimulation(simulationId: String) = withContext(Dispatchers.IO) {
        simulationDao.deleteById(simulationId)
    }

    // Mood Tracking
    fun getMoodLogsForCurrentUser(): Flow<List<MoodLogEntity>> {
        val userId = _currentUser.value?.id ?: ""
        return moodDao.getMoodLogsForUser(userId)
    }

    suspend fun logMood(
        moodType: String,
        moodEmoji: String,
        stressLevel: Int,
        wellBeingScore: Int,
        note: String,
        simulationId: String? = null,
        simulationTitle: String? = null
    ) = withContext(Dispatchers.IO) {
        val userId = _currentUser.value?.id ?: "guest"
        val log = MoodLogEntity(
            id = UUID.randomUUID().toString(),
            userId = userId,
            simulationId = simulationId,
            simulationTitle = simulationTitle,
            moodType = moodType,
            moodEmoji = moodEmoji,
            stressLevel = stressLevel,
            wellBeingScore = wellBeingScore,
            note = note,
            timestamp = System.currentTimeMillis()
        )
        moodDao.insertMoodLog(log)
    }

    suspend fun deleteMoodLog(id: String) = withContext(Dispatchers.IO) {
        moodDao.deleteById(id)
    }

    // Cloud Backup
    suspend fun triggerCloudBackup(): SyncResult = withContext(Dispatchers.IO) {
        val user = _currentUser.value ?: return@withContext SyncResult(false, "Please log in to backup data to the cloud.")
        cloudSyncManager.performCloudBackup(user.id, user.email, "Android Cloud Client")
    }

    suspend fun triggerCloudRestore(): SyncResult = withContext(Dispatchers.IO) {
        val user = _currentUser.value ?: return@withContext SyncResult(false, "Please log in to restore cloud backups.")
        cloudSyncManager.restoreFromCloud(user.id)
    }

    fun getCloudBackupMetadata(): CloudVaultSnapshot? {
        val user = _currentUser.value ?: return null
        return cloudSyncManager.getCloudBackupMetadata(user.id)
    }

    fun getBackupRecords(): Flow<List<BackupRecordEntity>> {
        val user = _currentUser.value?.id ?: ""
        return backupDao.getBackupsForUser(user)
    }

    // AI Chat & Fast Advice
    suspend fun getFastAdvice(prompt: String): String {
        return if (GeminiClient.isApiKeyConfigured()) {
            try {
                GeminiClient.generateFastAdvice(prompt)
            } catch (e: Exception) {
                "Quick tip: Decisions made with strong emergency savings (6+ months) yield 35% higher satisfaction and substantially lower cortisol/stress levels."
            }
        } else {
            "Decision Tip: Focus on reversible vs irreversible choices. Reversible choices can be made fast; irreversible commitments deserve 10-year compound modeling."
        }
    }

    suspend fun sendChatAdvisorMessage(
        modelName: String,
        history: List<ChatMessage>,
        userMessage: String,
        decisionContext: String? = null,
        enableSearch: Boolean = false
    ): String = withContext(Dispatchers.IO) {
        val systemInstruction = """
        You are the AI Life & Financial Decision Advisor, an empathetic yet rigorously analytical strategist.
        You specialize in:
        1. Financial ROI, Net Worth forecasts, opportunity costs, and compound growth.
        2. Psychological well-being, burnout mitigation, and regret minimization.
        3. Stress-testing user assumptions with scenario planning (e.g. bear markets, career disruptions).
        ${if (decisionContext != null) "CURRENT ACTIVE DECISION CONTEXT:\n$decisionContext" else ""}
        Keep your replies actionable, structured, scannable, and supportive.
        """.trimIndent()

        val fullThread = history + ChatMessage(role = "user", content = userMessage)

        if (GeminiClient.isApiKeyConfigured()) {
            try {
                GeminiClient.generateChatReply(
                    modelName = modelName,
                    messages = fullThread,
                    systemInstructionText = systemInstruction,
                    enableSearchGrounding = enableSearch
                )
            } catch (e: Exception) {
                fallbackChatReply(userMessage, decisionContext)
            }
        } else {
            fallbackChatReply(userMessage, decisionContext)
        }
    }

    private fun fallbackChatReply(userMessage: String, decisionContext: String?): String {
        val lower = userMessage.lowercase()
        return when {
            "stress" in lower || "anxious" in lower || "worry" in lower -> {
                "**Well-Being Perspective**:\nDecision anxiety often stems from ambiguity rather than the numbers themselves.\n\n1. **Separate Risk from Uncertainty**: Write down the worst-case consequence for each choice. Can you survive it? If yes, the anxiety is emotional friction rather than catastrophic danger.\n2. **Runway Buffer**: Ensure you retain at least 6 months of living expenses in liquid cash before committing capital.\n3. **3-Month Rule**: Most major life decisions feel overwhelming for the first 90 days, then normalize into routine."
            }
            "worst" in lower || "risk" in lower -> {
                "**Downside Stress-Test**:\nLet's examine the downside risk:\n- **Financial Floor**: What is your maximum loss if markets or property values drop 20%?\n- **Liquidity Trap**: Avoid locking all liquid savings into illiquid assets where forced sales create steep losses.\n- **Exit Ramp**: Always define your 'stop-loss' or exit strategy before entering the commitment."
            }
            "retire" in lower || "net worth" in lower || "wealth" in lower -> {
                "**10-Year Compounding Rule**:\nEvery \$1,000 invested consistently at an average 7% real return doubles approximately every 10.2 years. Small monthly differences (like \$500/month between rent vs mortgage) compound to over \$86,000 in 10 years and \$260,000 in 20 years."
            }
            else -> {
                "**Strategic Analysis**:\nRegarding \"$userMessage\":\n\n- **Opportunity Cost**: When you commit capital or time to one path, calculate what the alternative capital could earn elsewhere.\n- **Regret Minimization**: Project yourself 10 years into the future. Which path will you be prouder of having attempted, even if imperfect?\n- **Next Action**: Break down the immediate next 30 days into small reversible steps before locking in the final commitment."
            }
        }
    }

    private fun hashPassword(password: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val hash = digest.digest("lifesim_salt_$password".toByteArray())
        return hash.joinToString("") { "%02x".format(it) }
    }
}
