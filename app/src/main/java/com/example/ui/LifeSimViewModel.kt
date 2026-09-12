package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.LifeSimDatabase
import com.example.data.model.BackupRecordEntity
import com.example.data.model.MoodLogEntity
import com.example.data.model.SimulationEntity
import com.example.data.model.SimulationOption
import com.example.data.model.SimulationReport
import com.example.data.model.UserEntity
import com.example.data.remote.ChatMessage
import com.example.data.repository.LifeSimRepository
import com.example.data.sync.CloudVaultSnapshot
import com.example.data.sync.SyncResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface UiState<out T> {
    object Idle : UiState<Nothing>
    object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val message: String) : UiState<Nothing>
}

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class LifeSimViewModel(application: Application) : AndroidViewModel(application) {
    private val database = LifeSimDatabase.getDatabase(application)
    val repository = LifeSimRepository(application, database)

    val currentUser: StateFlow<UserEntity?> = repository.currentUser

    // Reactive simulations list based on current user
    val simulations: StateFlow<List<SimulationEntity>> = currentUser
        .flatMapLatest { user ->
            if (user != null) repository.getSimulationsForCurrentUser() else flowOf(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Reactive mood logs based on current user
    val moodLogs: StateFlow<List<MoodLogEntity>> = currentUser
        .flatMapLatest { user ->
            if (user != null) repository.getMoodLogsForCurrentUser() else flowOf(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Backup history
    val backupRecords: StateFlow<List<BackupRecordEntity>> = currentUser
        .flatMapLatest { user ->
            if (user != null) repository.getBackupRecords() else flowOf(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Active selected simulation for detail screen
    private val _selectedSimulation = MutableStateFlow<SimulationEntity?>(null)
    val selectedSimulation: StateFlow<SimulationEntity?> = _selectedSimulation.asStateFlow()

    // Simulation generation state
    private val _simulationState = MutableStateFlow<UiState<SimulationEntity>>(UiState.Idle)
    val simulationState: StateFlow<UiState<SimulationEntity>> = _simulationState.asStateFlow()

    // Cloud backup sync state
    private val _syncState = MutableStateFlow<UiState<SyncResult>>(UiState.Idle)
    val syncState: StateFlow<UiState<SyncResult>> = _syncState.asStateFlow()

    // Fast low-latency advice
    private val _fastAdvice = MutableStateFlow<String?>("Decisions with healthy liquidity reserves yield 35% higher well-being and lower stress.")
    val fastAdvice: StateFlow<String?> = _fastAdvice.asStateFlow()

    // AI Advisor Chat state
    private val _chatMessages = MutableStateFlow<List<ChatMessage>>(
        listOf(
            ChatMessage(
                role = "model",
                content = "Greetings. I am your Life & Financial Decision Advisor. Tell me about the dilemma, major purchase, career path, or investment you are weighing, and we will stress-test every consequence."
            )
        )
    )
    val chatMessages: StateFlow<List<ChatMessage>> = _chatMessages.asStateFlow()

    private val _chatLoading = MutableStateFlow(false)
    val chatLoading: StateFlow<Boolean> = _chatLoading.asStateFlow()

    // Auth error state
    private val _authError = MutableStateFlow<String?>(null)
    val authError: StateFlow<String?> = _authError.asStateFlow()

    init {
        viewModelScope.launch {
            repository.initializeSession()
        }
    }

    fun selectSimulation(simulation: SimulationEntity) {
        _selectedSimulation.value = simulation
    }

    fun selectSimulationById(id: String) {
        viewModelScope.launch {
            val sim = repository.getSimulationById(id)
            if (sim != null) {
                _selectedSimulation.value = sim
            }
        }
    }

    // Auth operations
    fun register(email: String, name: String, pass: String, onSuccess: () -> Unit) {
        viewModelScope.launch {
            _authError.value = null
            val result = repository.register(email, name, pass)
            result.onSuccess {
                onSuccess()
            }.onFailure {
                _authError.value = it.message ?: "Registration failed."
            }
        }
    }

    fun login(email: String, pass: String, onSuccess: () -> Unit) {
        viewModelScope.launch {
            _authError.value = null
            val result = repository.login(email, pass)
            result.onSuccess {
                onSuccess()
            }.onFailure {
                _authError.value = it.message ?: "Login failed."
            }
        }
    }

    fun logout(onLoggedOut: () -> Unit) {
        repository.logout()
        _selectedSimulation.value = null
        onLoggedOut()
    }

    // Run new simulation
    fun runSimulation(
        title: String,
        category: String,
        description: String,
        timeHorizonYears: Int,
        initialCapital: Double,
        monthlySurplus: Double,
        riskTolerance: String,
        stressLevelBefore: Int,
        options: List<SimulationOption>,
        onSuccess: (String) -> Unit
    ) {
        viewModelScope.launch {
            _simulationState.value = UiState.Loading
            try {
                val sim = repository.runAndSaveSimulation(
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
                _selectedSimulation.value = sim
                _simulationState.value = UiState.Success(sim)
                onSuccess(sim.id)
            } catch (e: Exception) {
                _simulationState.value = UiState.Error(e.message ?: "Simulation failed.")
            }
        }
    }

    fun toggleFavorite(simulation: SimulationEntity) {
        viewModelScope.launch {
            repository.toggleFavorite(simulation)
        }
    }

    fun deleteSimulation(id: String) {
        viewModelScope.launch {
            repository.deleteSimulation(id)
            if (_selectedSimulation.value?.id == id) {
                _selectedSimulation.value = null
            }
        }
    }

    fun recordActualChoice(simulationId: String, chosenOptionId: String, reflection: String?) {
        viewModelScope.launch {
            repository.recordActualDecision(simulationId, chosenOptionId, reflection)
            _selectedSimulation.value = repository.getSimulationById(simulationId)
        }
    }

    // Mood Logging
    fun logMood(
        moodType: String,
        moodEmoji: String,
        stressLevel: Int,
        wellBeingScore: Int,
        note: String,
        simulationId: String? = null,
        simulationTitle: String? = null
    ) {
        viewModelScope.launch {
            repository.logMood(
                moodType = moodType,
                moodEmoji = moodEmoji,
                stressLevel = stressLevel,
                wellBeingScore = wellBeingScore,
                note = note,
                simulationId = simulationId,
                simulationTitle = simulationTitle
            )
            // Fetch fast advice using gemini-3.1-flash-lite
            fetchFastAdvice("User logged mood '$moodType' with stress level $stressLevel/10. Provide one sentence of grounded life advice.")
        }
    }

    fun fetchFastAdvice(prompt: String) {
        viewModelScope.launch {
            val tip = repository.getFastAdvice(prompt)
            _fastAdvice.value = tip
        }
    }

    fun deleteMoodLog(id: String) {
        viewModelScope.launch {
            repository.deleteMoodLog(id)
        }
    }

    // Cloud Backup
    fun backupToCloud() {
        viewModelScope.launch {
            _syncState.value = UiState.Loading
            val res = repository.triggerCloudBackup()
            if (res.success) {
                _syncState.value = UiState.Success(res)
            } else {
                _syncState.value = UiState.Error(res.message)
            }
        }
    }

    fun restoreFromCloud() {
        viewModelScope.launch {
            _syncState.value = UiState.Loading
            val res = repository.triggerCloudRestore()
            if (res.success) {
                _syncState.value = UiState.Success(res)
            } else {
                _syncState.value = UiState.Error(res.message)
            }
        }
    }

    fun getCloudBackupSnapshot(): CloudVaultSnapshot? {
        return repository.getCloudBackupMetadata()
    }

    // Chatbot
    fun sendChatMessage(
        userText: String,
        modelName: String = "gemini-3.1-pro-preview",
        enableSearch: Boolean = false
    ) {
        if (userText.isBlank()) return
        val current = _chatMessages.value
        val userMsg = ChatMessage(role = "user", content = userText)
        _chatMessages.value = current + userMsg
        _chatLoading.value = true

        viewModelScope.launch {
            val decisionContext = _selectedSimulation.value?.let {
                "Decision: ${it.title} (${it.category}), Horizon: ${it.timeHorizonYears} yrs, Capital: $${it.initialCapital}"
            }
            val replyText = repository.sendChatAdvisorMessage(
                modelName = modelName,
                history = current,
                userMessage = userText,
                decisionContext = decisionContext,
                enableSearch = enableSearch
            )
            val botMsg = ChatMessage(
                role = "model",
                content = replyText,
                modelTag = modelName
            )
            _chatMessages.value = _chatMessages.value + botMsg
            _chatLoading.value = false
        }
    }
}
