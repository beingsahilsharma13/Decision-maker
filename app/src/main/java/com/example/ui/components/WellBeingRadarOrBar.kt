package com.example.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.PredictionOutcome
import com.example.ui.theme.MetricGreen
import com.example.ui.theme.MetricOrange
import com.example.ui.theme.MetricPurple
import com.example.ui.theme.MetricRed
import com.example.ui.theme.PrimaryIndigo
import com.example.ui.theme.SecondaryTeal
import com.example.ui.theme.TertiaryGold

private val PATH_COLORS = listOf(
    PrimaryIndigo,
    SecondaryTeal,
    TertiaryGold,
    MetricPurple
)

@Composable
fun WellBeingComparisonCard(
    outcomes: List<PredictionOutcome>,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Multi-Dimensional Path Comparison",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "Comparing financial return, mental stress, regret probability & overall life balance",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Overall Score Cards
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                outcomes.forEachIndexed { index, outcome ->
                    val color = PATH_COLORS[index % PATH_COLORS.size]
                    Card(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = outcome.optionTitle.take(18),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                color = color
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "${outcome.overallScore}",
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Black,
                                color = color
                            )
                            Text(
                                text = "Life Score / 100",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(
                                        if (outcome.regretProbabilityPct <= 20) MetricGreen.copy(alpha = 0.15f)
                                        else MetricOrange.copy(alpha = 0.15f)
                                    )
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "${outcome.regretProbabilityPct}% Regret Prob",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Medium,
                                    color = if (outcome.regretProbabilityPct <= 20) MetricGreen else MetricOrange
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Breakdown Dimensions
            MetricComparisonRow(
                metricName = "Well-Being & Mental Peace",
                outcomes = outcomes,
                getValue = { it.wellBeingScore.toFloat() / 100f },
                getDisplay = { "${it.wellBeingScore}/100" }
            )

            Spacer(modifier = Modifier.height(12.dp))

            MetricComparisonRow(
                metricName = "Financial Prosperity Score",
                outcomes = outcomes,
                getValue = { it.financialScore.toFloat() / 100f },
                getDisplay = { "${it.financialScore}/100" }
            )

            Spacer(modifier = Modifier.height(12.dp))

            MetricComparisonRow(
                metricName = "Career & Growth Opportunity",
                outcomes = outcomes,
                getValue = { it.careerScore.toFloat() / 100f },
                getDisplay = { "${it.careerScore}/100" }
            )

            Spacer(modifier = Modifier.height(12.dp))

            MetricComparisonRow(
                metricName = "Downside Risk Exposure",
                outcomes = outcomes,
                getValue = { it.riskScore.toFloat() / 100f },
                getDisplay = { "${it.riskScore}/100" },
                isRiskMetric = true
            )
        }
    }
}

@Composable
private fun MetricComparisonRow(
    metricName: String,
    outcomes: List<PredictionOutcome>,
    getValue: (PredictionOutcome) -> Float,
    getDisplay: (PredictionOutcome) -> String,
    isRiskMetric: Boolean = false
) {
    Column {
        Text(
            text = metricName,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(6.dp))

        outcomes.forEachIndexed { index, outcome ->
            val color = if (isRiskMetric) {
                val r = outcome.riskScore
                if (r > 60) MetricRed else if (r > 40) MetricOrange else MetricGreen
            } else {
                PATH_COLORS[index % PATH_COLORS.size]
            }

            val progress by animateFloatAsState(targetValue = getValue(outcome), label = "metricProgress")

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 3.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = outcome.optionTitle.take(14),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.width(90.dp),
                    maxLines = 1
                )
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(8.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surface)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(progress)
                            .fillMaxHeight()
                            .clip(CircleShape)
                            .background(color)
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = getDisplay(outcome),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = color,
                    modifier = Modifier.width(50.dp)
                )
            }
        }
    }
}
