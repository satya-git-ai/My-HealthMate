package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.HealthEmerald
import com.example.ui.theme.HealthCyan

data class BarChartItem(
    val label: String, // e.g. "Mon", "Tue", "Today"
    val value: Float,
    val date: String,
    val isToday: Boolean = false
)

@Composable
fun WeeklyBarChart(
    title: String,
    items: List<BarChartItem>,
    goalValue: Float,
    unit: String,
    primaryColor: Color = HealthEmerald,
    secondaryColor: Color = HealthCyan,
    selectedDate: String? = null,
    onItemClick: ((BarChartItem) -> Unit)? = null,
    modifier: Modifier = Modifier,
    testTag: String = "weekly_bar_chart"
) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = modifier
            .fillMaxWidth()
            .testTag(testTag)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }

                val avg = if (items.isNotEmpty()) items.map { it.value }.average().toInt() else 0
                Text(
                    text = "7-Day Avg: %,d %s".format(avg, unit),
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = primaryColor
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            val maxDataValue = (items.maxOfOrNull { it.value } ?: goalValue).coerceAtLeast(goalValue)
            val chartCeiling = (maxDataValue * 1.15f).coerceAtLeast(1f)
            val emptyColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)
            val todayGlowColor = Color(0xFF00E5FF)

            // Canvas Chart with interactive tap support
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(160.dp)
            ) {
                val gridColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                val goalColor = primaryColor.copy(alpha = 0.5f)

                Canvas(
                    modifier = Modifier
                        .matchParentSize()
                ) {
                    val w = size.width
                    val h = size.height
                    val n = items.size.coerceAtLeast(1)
                    val barWidth = (w / n) * 0.42f
                    val slotWidth = w / n

                    // Draw goal line if goalValue > 0
                    if (goalValue > 0f) {
                        val goalY = (h - (goalValue / chartCeiling * h)).coerceIn(10f, h - 5f)
                        drawLine(
                            color = goalColor,
                            start = Offset(0f, goalY),
                            end = Offset(w, goalY),
                            strokeWidth = 2f,
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 6f), 0f)
                        )
                    }

                    // Draw Bars
                    items.forEachIndexed { index, item ->
                        val isSelected = selectedDate == item.date
                        val x = (index * slotWidth) + (slotWidth - barWidth) / 2f

                        if (item.value <= 0f) {
                            // True 0 representation: subtle baseline pill
                            val baseHeight = 6f
                            val y = h - baseHeight
                            drawRoundRect(
                                color = if (item.isToday) todayGlowColor.copy(alpha = 0.7f) else emptyColor,
                                topLeft = Offset(x, y),
                                size = Size(barWidth, baseHeight),
                                cornerRadius = CornerRadius(baseHeight / 2f, baseHeight / 2f)
                            )
                        } else {
                            // Real user count representation: dynamic height
                            val barHeight = ((item.value / chartCeiling) * h).coerceIn(8f, h)
                            val y = h - barHeight

                            val barBrush = Brush.verticalGradient(
                                colors = if (item.isToday) {
                                    listOf(Color(0xFF00E5FF), primaryColor)
                                } else {
                                    listOf(primaryColor, secondaryColor)
                                }
                            )

                            // Draw the bar
                            drawRoundRect(
                                brush = barBrush,
                                topLeft = Offset(x, y),
                                size = Size(barWidth, barHeight),
                                cornerRadius = CornerRadius(barWidth / 2f, barWidth / 2f)
                            )

                            // Outline for Today or Selected bar
                            if (item.isToday || isSelected) {
                                drawRoundRect(
                                    color = if (isSelected) Color.White else todayGlowColor,
                                    topLeft = Offset(x - 1.5f, y - 1.5f),
                                    size = Size(barWidth + 3f, barHeight + 3f),
                                    cornerRadius = CornerRadius((barWidth + 3f) / 2f, (barWidth + 3f) / 2f),
                                    style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2.5f)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Day Labels Row with Today and Active Highlights
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                items.forEach { item ->
                    val isSelected = selectedDate == item.date
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .testTag("chart_day_${item.date}")
                    ) {
                        Text(
                            text = if (item.isToday) "Today" else item.label,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = if (item.isToday) 11.5.sp else 11.sp,
                                fontWeight = if (item.isToday || isSelected) FontWeight.ExtraBold else FontWeight.Normal
                            ),
                            color = when {
                                item.isToday -> primaryColor
                                isSelected -> MaterialTheme.colorScheme.primary
                                else -> MaterialTheme.colorScheme.onSurfaceVariant
                            }
                        )

                        // Real-time count indicator under the label
                        Text(
                            text = if (item.value > 0) {
                                if (item.value >= 1000) "%.1fk".format(item.value / 1000f) else item.value.toInt().toString()
                            } else "0",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 9.sp,
                                fontWeight = if (item.isToday) FontWeight.Bold else FontWeight.Normal
                            ),
                            color = if (item.isToday && item.value > 0) {
                                primaryColor
                            } else if (item.value > 0) {
                                MaterialTheme.colorScheme.onSurface
                            } else {
                                MaterialTheme.colorScheme.outline
                            }
                        )
                    }
                }
            }
        }
    }
}
