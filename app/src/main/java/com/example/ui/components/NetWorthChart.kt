package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.PredictionOutcome
import com.example.ui.theme.MetricGreen
import com.example.ui.theme.MetricOrange
import com.example.ui.theme.MetricPurple
import com.example.ui.theme.PrimaryIndigo
import com.example.ui.theme.SecondaryTeal
import com.example.ui.theme.TertiaryGold
import java.text.NumberFormat
import java.util.Locale
import kotlin.math.max

private val PATH_COLORS = listOf(
    PrimaryIndigo,
    SecondaryTeal,
    TertiaryGold,
    MetricGreen,
    MetricOrange,
    MetricPurple
)

@Composable
fun NetWorthChart(
    outcomes: List<PredictionOutcome>,
    modifier: Modifier = Modifier
) {
    if (outcomes.isEmpty()) return

    // Extract all year points (usually 1, 3, 5, 10)
    val allYears = outcomes.flatMap { it.projections.map { p -> p.year } }.distinct().sorted()
    if (allYears.isEmpty()) return

    val maxNetWorth = max(10000.0, outcomes.flatMap { it.projections.map { p -> p.netWorth } }.maxOrNull() ?: 100000.0)
    val minNetWorth = 0.0

    var selectedYearIndex by remember { mutableStateOf<Int?>(null) }

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
                Column {
                    Text(
                        text = "10-Year Net Worth Trajectory",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Tap on chart points to inspect milestones",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Text(
                    text = "Peak: \$${formatCompactNumber(maxNetWorth)}",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Canvas Chart
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(210.dp)
            ) {
                Canvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(210.dp)
                        .pointerInput(outcomes) {
                            detectTapGestures { tapOffset ->
                                val chartWidth = size.width - 60f
                                val step = if (allYears.size > 1) chartWidth / (allYears.size - 1) else chartWidth
                                val relativeX = (tapOffset.x - 40f).coerceIn(0f, chartWidth)
                                val closestIdx = (relativeX / step).toInt().coerceIn(0, allYears.size - 1)
                                selectedYearIndex = closestIdx
                            }
                        }
                ) {
                    val w = size.width
                    val h = size.height
                    val paddingLeft = 50f
                    val paddingRight = 30f
                    val paddingTop = 20f
                    val paddingBottom = 35f

                    val chartWidth = w - paddingLeft - paddingRight
                    val chartHeight = h - paddingTop - paddingBottom

                    // Draw Horizontal Grid Lines
                    val gridLines = 4
                    for (i in 0..gridLines) {
                        val y = paddingTop + (chartHeight * i / gridLines)
                        val value = maxNetWorth - (maxNetWorth * i / gridLines)

                        drawLine(
                            color = Color.Gray.copy(alpha = 0.2f),
                            start = Offset(paddingLeft, y),
                            end = Offset(w - paddingRight, y),
                            strokeWidth = 1.dp.toPx()
                        )

                        // Y-axis text
                        drawContext.canvas.nativeCanvas.drawText(
                            "$${formatCompactNumber(value)}",
                            10f,
                            y + 10f,
                            android.graphics.Paint().apply {
                                color = android.graphics.Color.GRAY
                                textSize = 24f
                                isAntiAlias = true
                            }
                        )
                    }

                    // Draw X-axis Year Labels
                    allYears.forEachIndexed { index, year ->
                        val x = if (allYears.size > 1) paddingLeft + (chartWidth * index / (allYears.size - 1)) else paddingLeft + chartWidth / 2

                        drawLine(
                            color = Color.Gray.copy(alpha = 0.15f),
                            start = Offset(x, paddingTop),
                            end = Offset(x, h - paddingBottom),
                            strokeWidth = 1.dp.toPx()
                        )

                        drawContext.canvas.nativeCanvas.drawText(
                            "Yr $year",
                            x - 20f,
                            h - 8f,
                            android.graphics.Paint().apply {
                                color = android.graphics.Color.GRAY
                                textSize = 26f
                                isAntiAlias = true
                                isFakeBoldText = true
                            }
                        )
                    }

                    // Draw Trajectory Curves for each option
                    outcomes.forEachIndexed { optionIdx, outcome ->
                        val color = PATH_COLORS[optionIdx % PATH_COLORS.size]
                        val path = Path()
                        val fillPath = Path()

                        val points = mutableListOf<Offset>()
                        outcome.projections.forEach { proj ->
                            val yearIdx = allYears.indexOf(proj.year)
                            if (yearIdx >= 0) {
                                val x = if (allYears.size > 1) {
                                    paddingLeft + (chartWidth * yearIdx / (allYears.size - 1))
                                } else {
                                    paddingLeft + chartWidth / 2
                                }
                                val ratio = (proj.netWorth - minNetWorth) / (maxNetWorth - minNetWorth)
                                val y = (paddingTop + chartHeight * (1.0 - ratio)).toFloat().coerceIn(paddingTop, h - paddingBottom)
                                points.add(Offset(x, y))
                            }
                        }

                        if (points.isNotEmpty()) {
                            path.moveTo(points.first().x, points.first().y)
                            fillPath.moveTo(points.first().x, h - paddingBottom)
                            fillPath.lineTo(points.first().x, points.first().y)

                            for (k in 1 until points.size) {
                                val prev = points[k - 1]
                                val cur = points[k]
                                val cX = (prev.x + cur.x) / 2
                                path.cubicTo(cX, prev.y, cX, cur.y, cur.x, cur.y)
                                fillPath.cubicTo(cX, prev.y, cX, cur.y, cur.x, cur.y)
                            }

                            fillPath.lineTo(points.last().x, h - paddingBottom)
                            fillPath.close()

                            // Subtle Area Gradient
                            drawPath(
                                path = fillPath,
                                brush = Brush.verticalGradient(
                                    colors = listOf(color.copy(alpha = 0.18f), color.copy(alpha = 0.02f)),
                                    startY = paddingTop,
                                    endY = h - paddingBottom
                                )
                            )

                            // Main Line
                            drawPath(
                                path = path,
                                color = color,
                                style = Stroke(width = 3.5.dp.toPx(), cap = StrokeCap.Round)
                            )

                            // Point Dots
                            points.forEachIndexed { pIdx, pt ->
                                val isSelected = selectedYearIndex == pIdx
                                drawCircle(
                                    color = Color.White,
                                    radius = if (isSelected) 6.dp.toPx() else 4.dp.toPx(),
                                    center = pt
                                )
                                drawCircle(
                                    color = color,
                                    radius = if (isSelected) 4.5.dp.toPx() else 2.8.dp.toPx(),
                                    center = pt
                                )
                            }
                        }
                    }

                    // Scrubber line if year selected
                    selectedYearIndex?.let { selIdx ->
                        if (selIdx in allYears.indices) {
                            val scrubX = paddingLeft + (chartWidth * selIdx / (allYears.size - 1))
                            drawLine(
                                color = Color.White.copy(alpha = 0.8f),
                                start = Offset(scrubX, paddingTop),
                                end = Offset(scrubX, h - paddingBottom),
                                strokeWidth = 1.5.dp.toPx()
                            )
                        }
                    }
                }
            }

            // Interactive Tooltip if tapped
            selectedYearIndex?.let { selIdx ->
                if (selIdx in allYears.indices) {
                    val selYear = allYears[selIdx]
                    Spacer(modifier = Modifier.height(10.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(12.dp))
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Column {
                            Text(
                                text = "Year $selYear Projection Snapshot",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                outcomes.forEachIndexed { idx, o ->
                                    val proj = o.projections.find { it.year == selYear }
                                    val col = PATH_COLORS[idx % PATH_COLORS.size]
                                    Column(modifier = Modifier.padding(end = 8.dp)) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Box(modifier = Modifier.size(8.dp).background(col, CircleShape))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                text = o.optionTitle.take(16),
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        Text(
                                            text = "\$${NumberFormat.getNumberInstance(Locale.US).format(proj?.netWorth ?: 0.0)}",
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = col
                                        )
                                        Text(
                                            text = "Liquid: \$${formatCompactNumber(proj?.liquidCapital ?: 0.0)}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Legend
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                outcomes.forEachIndexed { index, outcome ->
                    val color = PATH_COLORS[index % PATH_COLORS.size]
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .background(color, CircleShape)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = outcome.optionTitle,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }
    }
}

private fun formatCompactNumber(number: Double): String {
    return when {
        number >= 1_000_000 -> "%.1fM".format(number / 1_000_000)
        number >= 1_000 -> "%.0fk".format(number / 1_000)
        else -> "%.0f".format(number)
    }
}
