package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.PredictionOutcome
import com.example.data.model.SimulationReport
import com.example.ui.theme.MetricGreen
import com.example.ui.theme.MetricOrange
import com.example.ui.theme.MetricRed
import com.example.ui.theme.PrimaryIndigo
import com.example.ui.theme.SecondaryTeal
import com.example.ui.theme.TertiaryGold
import java.text.NumberFormat
import java.util.Locale

private val PATH_COLORS = listOf(
    PrimaryIndigo,
    SecondaryTeal,
    TertiaryGold
)

@Composable
fun DecisionMatrixTable(
    report: SimulationReport,
    modifier: Modifier = Modifier
) {
    val outcomes = report.outcomes
    if (outcomes.isEmpty()) return

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Side-by-Side Trade-off Matrix",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Side by Side Cards
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                outcomes.forEachIndexed { idx, outcome ->
                    val isRecommended = outcome.optionId == report.recommendedOptionId
                    val brandColor = PATH_COLORS[idx % PATH_COLORS.size]
                    val finalProj = outcome.projections.lastOrNull()

                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .then(
                                if (isRecommended) {
                                    Modifier.border(2.dp, TertiaryGold, RoundedCornerShape(12.dp))
                                } else Modifier
                            ),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            if (isRecommended) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(TertiaryGold.copy(alpha = 0.15f))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Star,
                                        contentDescription = "Recommended",
                                        tint = TertiaryGold,
                                        modifier = Modifier.size(12.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "Top Choice",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = TertiaryGold
                                    )
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                            }

                            Text(
                                text = outcome.optionTitle,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = brandColor,
                                maxLines = 2
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            MatrixDataRow(
                                label = "10-Yr Net Worth",
                                value = "\$${NumberFormat.getNumberInstance(Locale.US).format(finalProj?.netWorth ?: 0.0)}",
                                highlightColor = MetricGreen
                            )

                            MatrixDataRow(
                                label = "Annual Cash Flow",
                                value = "\$${NumberFormat.getNumberInstance(Locale.US).format(finalProj?.annualCashFlow ?: 0.0)}",
                                highlightColor = MaterialTheme.colorScheme.onSurface
                            )

                            MatrixDataRow(
                                label = "Well-Being Score",
                                value = "${outcome.wellBeingScore} / 100",
                                highlightColor = brandColor
                            )

                            MatrixDataRow(
                                label = "Regret Risk",
                                value = "${outcome.regretProbabilityPct}%",
                                highlightColor = if (outcome.regretProbabilityPct > 30) MetricOrange else MetricGreen
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            Text(
                                text = "Key Advantages:",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            outcome.keyAdvantages.take(2).forEach { pro ->
                                Row(
                                    modifier = Modifier.padding(top = 2.dp),
                                    verticalAlignment = Alignment.Top
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = null,
                                        tint = MetricGreen,
                                        modifier = Modifier.size(12.dp).padding(top = 2.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = pro,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontSize = 11.sp
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(6.dp))

                            Text(
                                text = "Downside Vulnerability:",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            outcome.keyRisks.take(2).forEach { con ->
                                Row(
                                    modifier = Modifier.padding(top = 2.dp),
                                    verticalAlignment = Alignment.Top
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = null,
                                        tint = MetricRed,
                                        modifier = Modifier.size(12.dp).padding(top = 2.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = con,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontSize = 11.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MatrixDataRow(
    label: String,
    value: String,
    highlightColor: Color
) {
    Column(modifier = Modifier.padding(vertical = 3.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 10.sp
        )
        Text(
            text = value,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = highlightColor
        )
    }
}
