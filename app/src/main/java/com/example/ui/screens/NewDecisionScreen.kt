package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.SimulationOption
import com.example.ui.LifeSimViewModel
import com.example.ui.UiState
import com.example.ui.theme.PrimaryIndigo
import com.example.ui.theme.SecondaryTeal
import com.example.ui.theme.TertiaryGold
import java.util.UUID

private val CATEGORIES = listOf("Financial", "Career", "Major Purchase", "Relocation", "Education")

data class PresetTemplate(
    val title: String,
    val category: String,
    val description: String,
    val initialSavings: String,
    val monthlyCashFlow: String,
    val timeYears: Int,
    val optionA: SimulationOption,
    val optionB: SimulationOption
)

private val PRESETS = listOf(
    PresetTemplate(
        title = "Buy House with Mortgage vs Rent & Invest Index Funds",
        category = "Financial",
        description = "Should I deploy $80,000 as down payment on a property, or rent and invest the capital in low-cost global equity index funds?",
        initialSavings = "100000",
        monthlyCashFlow = "3200",
        timeYears = 10,
        optionA = SimulationOption(
            id = "opt_a",
            title = "Buy Home (20% Down)",
            description = "Acquire \$450k home with 30-year fixed mortgage. Build real estate equity and fixed housing cost.",
            upfrontCost = 90000.0,
            monthlyCommitment = 2800.0,
            pros = listOf("Forced equity buildup", "Appreciation upside", "Sense of ownership"),
            risks = listOf("Tied-up illiquid capital", "High maintenance/HOA costs")
        ),
        optionB = SimulationOption(
            id = "opt_b",
            title = "Rent & Maximize Index DCA",
            description = "Rent at \$2,000/mo. Invest \$80k in total market index funds and dollar-cost average \$1,200/mo surplus.",
            upfrontCost = 4000.0,
            monthlyCommitment = 2000.0,
            pros = listOf("High liquidity", "No repair headaches", "Compounding global market returns"),
            risks = listOf("Long-term rent inflation", "Requires investing discipline")
        )
    ),
    PresetTemplate(
        title = "Early Startup with 1.5% Equity vs Corporate Senior Tech Lead",
        category = "Career",
        description = "Contemplating joining a Series A tech startup at $90k salary with 1.5% equity vs continuing at Big Tech firm at $180k total comp.",
        initialSavings = "50000",
        monthlyCashFlow = "2500",
        timeYears = 5,
        optionA = SimulationOption(
            id = "opt_a",
            title = "Join Series A Startup",
            description = "Trade \$90k salary discount for 1.5% equity stake and high autonomy/influence.",
            upfrontCost = 0.0,
            monthlyCommitment = 0.0,
            pros = listOf("Exponential equity upside", "C-suite trajectory", "High autonomy"),
            risks = listOf("High startup failure rate", "Burnout & long hours")
        ),
        optionB = SimulationOption(
            id = "opt_b",
            title = "Corporate Big Tech Lead",
            description = "Stay with stable \$180,000 cash and liquid RSU compensation, reliable 401(k) match.",
            upfrontCost = 0.0,
            monthlyCommitment = 0.0,
            pros = listOf("Predictable high cash flow", "40-hr workweek balance", "Safe wealth buildup"),
            risks = listOf("Bureaucracy stagnation", "Corporate restructuring risk")
        )
    ),
    PresetTemplate(
        title = "Full-Time MBA Degree with $100k Loan vs Self-Directed Career",
        category = "Education",
        description = "Should I take 2 years off to attend a top-tier business school with $100k tuition debt, or accelerate internally?",
        initialSavings = "35000",
        monthlyCashFlow = "1800",
        timeYears = 7,
        optionA = SimulationOption(
            id = "opt_a",
            title = "Full-Time Top MBA",
            description = "Take \$100k loan, 2 years foregone salary for elite alumni network and PE/Consulting pivot.",
            upfrontCost = 40000.0,
            monthlyCommitment = 1100.0,
            pros = listOf("Prestigious credential", "Alumni access", "Starting salary bump to \$175k+"),
            risks = listOf("Steep student loan debt", "Opportunity cost of 2 years salary")
        ),
        optionB = SimulationOption(
            id = "opt_b",
            title = "Self-Directed Growth & Certifications",
            description = "Remain employed earning \$90k while completing executive certifications and building network organically.",
            upfrontCost = 3000.0,
            monthlyCommitment = 0.0,
            pros = listOf("Zero debt burden", "Continuous salary compounding", "Practical work experience"),
            risks = listOf("Slower brand prestige", "Harder to pivot into elite investment roles")
        )
    )
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewDecisionScreen(
    viewModel: LifeSimViewModel,
    onNavigateBack: () -> Unit,
    onSimulationFinished: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var title by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("Financial") }
    var description by remember { mutableStateOf("") }
    var initialSavings by remember { mutableStateOf("50000") }
    var monthlyCashFlow by remember { mutableStateOf("2500") }
    var timeHorizonYears by remember { mutableIntStateOf(10) }
    var riskTolerance by remember { mutableStateOf("Moderate") }
    var stressLevelBefore by remember { mutableFloatStateOf(6f) }

    // Options to compare
    var optATitle by remember { mutableStateOf("Option A: Path One") }
    var optADesc by remember { mutableStateOf("Commit capital and take the aggressive growth path.") }
    var optAUpfront by remember { mutableStateOf("20000") }
    var optAMonthly by remember { mutableStateOf("500") }
    var optAPros by remember { mutableStateOf("High potential return, personal fulfillment") }
    var optARisks by remember { mutableStateOf("Illiquidity, higher initial stress") }

    var optBTitle by remember { mutableStateOf("Option B: Alternative Path") }
    var optBDesc by remember { mutableStateOf("Conserve cash and prioritize stability & liquidity.") }
    var optBUpfront by remember { mutableStateOf("2000") }
    var optBMonthly by remember { mutableStateOf("200") }
    var optBPros by remember { mutableStateOf("Low risk, flexibility, peace of mind") }
    var optBRisks by remember { mutableStateOf("Opportunity cost if alternative surges") }

    val simState by viewModel.simulationState.collectAsState()
    val isLoading = simState is UiState.Loading

    fun applyPreset(preset: PresetTemplate) {
        title = preset.title
        selectedCategory = preset.category
        description = preset.description
        initialSavings = preset.initialSavings
        monthlyCashFlow = preset.monthlyCashFlow
        timeHorizonYears = preset.timeYears

        optATitle = preset.optionA.title
        optADesc = preset.optionA.description
        optAUpfront = preset.optionA.upfrontCost.toInt().toString()
        optAMonthly = preset.optionA.monthlyCommitment.toInt().toString()
        optAPros = preset.optionA.pros.joinToString(", ")
        optARisks = preset.optionA.risks.joinToString(", ")

        optBTitle = preset.optionB.title
        optBDesc = preset.optionB.description
        optBUpfront = preset.optionB.upfrontCost.toInt().toString()
        optBMonthly = preset.optionB.monthlyCommitment.toInt().toString()
        optBPros = preset.optionB.pros.joinToString(", ")
        optBRisks = preset.optionB.risks.joinToString(", ")
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Upload Life Decision", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 12.dp)
        ) {
            // Preset Inspiration Chips
            Text(
                text = "Instant Real-Life Scenarios",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(6.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(PRESETS) { preset ->
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
                            .clickable { applyPreset(preset) }
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = TertiaryGold,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = preset.title.take(30) + "...",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Decision Title & Category
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Decision Dilemma / Question") },
                placeholder = { Text("e.g. Buy a $500k house vs Rent & Invest") },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("input_decision_title"),
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Category Row
            Text(
                text = "Decision Category",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                CATEGORIES.forEach { cat ->
                    val isSelected = cat == selectedCategory
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isSelected) PrimaryIndigo else MaterialTheme.colorScheme.surfaceVariant)
                            .clickable { selectedCategory = cat }
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = cat,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("Context & Background Details") },
                placeholder = { Text("Upload or type context notes, financial constraints, family factors...") },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(95.dp)
                    .testTag("input_decision_desc"),
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.height(18.dp))

            // Financial & Parameter Baseline
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Financial & Well-Being Baseline",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedTextField(
                            value = initialSavings,
                            onValueChange = { initialSavings = it },
                            label = { Text("Starting Savings ($)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f).testTag("input_savings"),
                            shape = RoundedCornerShape(10.dp)
                        )

                        OutlinedTextField(
                            value = monthlyCashFlow,
                            onValueChange = { monthlyCashFlow = it },
                            label = { Text("Monthly Surplus ($)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f).testTag("input_cash_flow"),
                            shape = RoundedCornerShape(10.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Time Horizon Slider
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Simulation Horizon:",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "$timeHorizonYears Years Ahead",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = PrimaryIndigo
                        )
                    }
                    Slider(
                        value = timeHorizonYears.toFloat(),
                        onValueChange = { timeHorizonYears = it.toInt() },
                        valueRange = 1f..20f,
                        steps = 18,
                        colors = SliderDefaults.colors(thumbColor = PrimaryIndigo, activeTrackColor = PrimaryIndigo)
                    )

                    // Stress Level Slider
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Current Stress Level:",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "${stressLevelBefore.toInt()}/10",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = SecondaryTeal
                        )
                    }
                    Slider(
                        value = stressLevelBefore,
                        onValueChange = { stressLevelBefore = it },
                        valueRange = 1f..10f,
                        steps = 8,
                        colors = SliderDefaults.colors(thumbColor = SecondaryTeal, activeTrackColor = SecondaryTeal)
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Option A Form
            OptionCardInput(
                pathLabel = "Path A",
                title = optATitle,
                onTitleChange = { optATitle = it },
                description = optADesc,
                onDescChange = { optADesc = it },
                upfront = optAUpfront,
                onUpfrontChange = { optAUpfront = it },
                monthly = optAMonthly,
                onMonthlyChange = { optAMonthly = it },
                pros = optAPros,
                onProsChange = { optAPros = it },
                risks = optARisks,
                onRisksChange = { optARisks = it },
                accentColor = PrimaryIndigo
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Option B Form
            OptionCardInput(
                pathLabel = "Path B",
                title = optBTitle,
                onTitleChange = { optBTitle = it },
                description = optBDesc,
                onDescChange = { optBDesc = it },
                upfront = optBUpfront,
                onUpfrontChange = { optBUpfront = it },
                monthly = optBMonthly,
                onMonthlyChange = { optBMonthly = it },
                pros = optBPros,
                onProsChange = { optBPros = it },
                risks = optBRisks,
                onRisksChange = { optBRisks = it },
                accentColor = SecondaryTeal
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Submit Button
            Button(
                onClick = {
                    if (title.isBlank()) return@Button

                    val options = listOf(
                        SimulationOption(
                            id = "opt_a",
                            title = optATitle.ifBlank { "Path A" },
                            description = optADesc,
                            upfrontCost = optAUpfront.toDoubleOrNull() ?: 0.0,
                            monthlyCommitment = optAMonthly.toDoubleOrNull() ?: 0.0,
                            pros = optAPros.split(",").map { it.trim() }.filter { it.isNotEmpty() },
                            risks = optARisks.split(",").map { it.trim() }.filter { it.isNotEmpty() }
                        ),
                        SimulationOption(
                            id = "opt_b",
                            title = optBTitle.ifBlank { "Path B" },
                            description = optBDesc,
                            upfrontCost = optBUpfront.toDoubleOrNull() ?: 0.0,
                            monthlyCommitment = optBMonthly.toDoubleOrNull() ?: 0.0,
                            pros = optBPros.split(",").map { it.trim() }.filter { it.isNotEmpty() },
                            risks = optBRisks.split(",").map { it.trim() }.filter { it.isNotEmpty() }
                        )
                    )

                    viewModel.runSimulation(
                        title = title,
                        category = selectedCategory,
                        description = description,
                        timeHorizonYears = timeHorizonYears,
                        initialCapital = initialSavings.toDoubleOrNull() ?: 20000.0,
                        monthlySurplus = monthlyCashFlow.toDoubleOrNull() ?: 1000.0,
                        riskTolerance = riskTolerance,
                        stressLevelBefore = stressLevelBefore.toInt(),
                        options = options,
                        onSuccess = onSimulationFinished
                    )
                },
                enabled = !isLoading && title.isNotBlank(),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .testTag("btn_run_simulation"),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryIndigo)
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        color = Color.White,
                        modifier = Modifier.size(24.dp),
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text("AI Reasoning in Progress (Gemini 3.1 Pro)...", fontWeight = FontWeight.Bold)
                } else {
                    Icon(Icons.Default.Psychology, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Simulate Life Outcomes", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            }

            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}

@Composable
fun OptionCardInput(
    pathLabel: String,
    title: String,
    onTitleChange: (String) -> Unit,
    description: String,
    onDescChange: (String) -> Unit,
    upfront: String,
    onUpfrontChange: (String) -> Unit,
    monthly: String,
    onMonthlyChange: (String) -> Unit,
    pros: String,
    onProsChange: (String) -> Unit,
    risks: String,
    onRisksChange: (String) -> Unit,
    accentColor: Color
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(accentColor.copy(alpha = 0.15f))
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = pathLabel,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = accentColor
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            OutlinedTextField(
                value = title,
                onValueChange = onTitleChange,
                label = { Text("Path Name") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp)
            )

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = description,
                onValueChange = onDescChange,
                label = { Text("What happens in this path?") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp)
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = upfront,
                    onValueChange = onUpfrontChange,
                    label = { Text("Upfront Cost ($)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp)
                )
                OutlinedTextField(
                    value = monthly,
                    onValueChange = onMonthlyChange,
                    label = { Text("Monthly Cost ($)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = pros,
                onValueChange = onProsChange,
                label = { Text("Expected Advantages (comma separated)") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp)
            )

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = risks,
                onValueChange = onRisksChange,
                label = { Text("Key Risks & Doubts (comma separated)") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp)
            )
        }
    }
}
