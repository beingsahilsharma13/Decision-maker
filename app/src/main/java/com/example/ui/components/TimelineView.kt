package com.example.ui.components

import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.PredictionOutcome
import com.example.data.model.TimelineMilestone
import com.example.ui.theme.MetricGreen
import com.example.ui.theme.MetricOrange
import com.example.ui.theme.MetricPurple
import com.example.ui.theme.PrimaryIndigo

@Composable
fun ConsequenceTimelineView(
    outcomes: List<PredictionOutcome>,
    modifier: Modifier = Modifier
) {
    if (outcomes.isEmpty()) return

    var selectedOutcomeIndex by remember { mutableStateOf(0) }
    val currentOutcome = outcomes.getOrElse(selectedOutcomeIndex) { outcomes.first() }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Consequence Roadmaps by Year",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "Milestones and turning points predicted across your life trajectory",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Tab for switching options
            if (outcomes.size > 1) {
                ScrollableTabRow(
                    selectedTabIndex = selectedOutcomeIndex,
                    edgePadding = 0.dp,
                    containerColor = Color.Transparent,
                    divider = {}
                ) {
                    outcomes.forEachIndexed { index, outcome ->
                        Tab(
                            selected = selectedOutcomeIndex == index,
                            onClick = { selectedOutcomeIndex = index },
                            text = {
                                Text(
                                    text = outcome.optionTitle,
                                    fontWeight = if (selectedOutcomeIndex == index) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        )
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
            }

            // Timeline Items
            val milestones = currentOutcome.milestones
            if (milestones.isEmpty()) {
                Text(
                    text = "No milestones recorded for this path.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                milestones.forEachIndexed { idx, milestone ->
                    TimelineItemRow(
                        milestone = milestone,
                        isLast = idx == milestones.size - 1
                    )
                }
            }
        }
    }
}

@Composable
private fun TimelineItemRow(
    milestone: TimelineMilestone,
    isLast: Boolean
) {
    val (icon, tint) = when (milestone.impactType.lowercase()) {
        "financial" -> Icons.Default.AttachMoney to MetricGreen
        "wellbeing" -> Icons.Default.Favorite to MetricPurple
        "career" -> Icons.Default.Bolt to PrimaryIndigo
        else -> Icons.Default.Flag to MetricOrange
    }

    Row(
        modifier = Modifier.fillMaxWidth()
    ) {
        // Vertical line & node indicator
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.width(36.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(tint.copy(alpha = 0.18f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = milestone.impactType,
                    tint = tint,
                    modifier = Modifier.size(16.dp)
                )
            }
            if (!isLast) {
                Box(
                    modifier = Modifier
                        .width(2.dp)
                        .height(48.dp)
                        .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))
                )
            }
        }

        Spacer(modifier = Modifier.width(10.dp))

        // Content
        Column(modifier = Modifier.padding(bottom = if (isLast) 0.dp else 16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = milestone.title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(MaterialTheme.colorScheme.surface)
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "Year ${milestone.year}",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = tint
                    )
                }
            }

            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = milestone.description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
