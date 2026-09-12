package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.engine.SimulationEngine
import com.example.data.local.LifeSimDatabase
import com.example.data.model.SimulationOption
import com.example.data.repository.LifeSimRepository
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    private lateinit var database: LifeSimDatabase
    private lateinit var repository: LifeSimRepository

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, LifeSimDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = LifeSimRepository(context, database)
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun `read app name string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("Life Simulator", appName)
    }

    @Test
    fun `deterministic life simulation generates valid 10-year trajectory and trade-off scores`() {
        val options = listOf(
            SimulationOption(
                id = "opt_home",
                title = "Buy Home",
                description = "Purchase home with 20% down payment",
                upfrontCost = 80000.0,
                monthlyCommitment = 2500.0,
                pros = listOf("Build home equity", "Stability"),
                risks = listOf("Maintenance costs", "Illiquidity")
            ),
            SimulationOption(
                id = "opt_rent",
                title = "Rent & Invest Index Funds",
                description = "Rent and DCA into index funds",
                upfrontCost = 3000.0,
                monthlyCommitment = 1800.0,
                pros = listOf("High liquidity", "No maintenance"),
                risks = listOf("Rent increases")
            )
        )

        val report = SimulationEngine.generateDeterministicSimulation(
            title = "Buy Home vs Rent & Invest",
            category = "Financial",
            description = "10 year forecast",
            timeHorizonYears = 10,
            initialCapital = 100000.0,
            monthlySurplus = 3000.0,
            riskTolerance = "Moderate",
            stressLevelBefore = 5,
            options = options
        )

        assertNotNull(report)
        assertEquals(2, report.outcomes.size)
        assertTrue(report.outcomes[0].projections.size >= 4)
        assertTrue(report.outcomes[1].projections.last().netWorth > 100000.0)
        assertTrue(report.recommendedOptionId.isNotBlank())
        assertTrue(report.executiveSummary.isNotBlank())
    }

    @Test
    fun `repository handles user registration and cloud backup sync cycle`() = runBlocking {
        val regResult = repository.register("user.test@lifesim.ai", "Test User", "password123")
        assertTrue(regResult.isSuccess)
        val user = regResult.getOrNull()
        assertNotNull(user)

        // Run simulation
        val sim = repository.runAndSaveSimulation(
            title = "Career Pivot vs Stay",
            category = "Career",
            description = "Evaluating risk of startup transition",
            timeHorizonYears = 5,
            initialCapital = 40000.0,
            monthlySurplus = 1500.0,
            riskTolerance = "Moderate",
            stressLevelBefore = 6,
            options = listOf(
                SimulationOption("opt_1", "Startup", "High growth", 0.0, 0.0, listOf("Equity"), listOf("Failure risk")),
                SimulationOption("opt_2", "Corporate", "Stable comp", 0.0, 0.0, listOf("Safety"), listOf("Boredom"))
            )
        )
        assertNotNull(sim)

        // Log a mood entry
        repository.logMood(
            moodType = "Optimistic",
            moodEmoji = "✨",
            stressLevel = 4,
            wellBeingScore = 8,
            note = "Feeling clear after running simulation",
            simulationId = sim.id,
            simulationTitle = sim.title
        )

        // Perform Cloud Backup
        val backupResult = repository.triggerCloudBackup()
        assertTrue(backupResult.success)
        assertNotNull(backupResult.checksum)
        assertTrue(backupResult.totalSimulations >= 1)
        assertTrue(backupResult.totalMoodLogs >= 1)

        // Test Cloud Restore
        val restoreResult = repository.triggerCloudRestore()
        assertTrue(restoreResult.success)
    }
}
