package com.pennywiseai.tracker.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pennywiseai.tracker.ui.theme.Spacing
import com.pennywiseai.tracker.utils.CurrencyFormatter
import java.math.BigDecimal
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

data class BalancePoint(
    val timestamp: LocalDateTime,
    val balance: BigDecimal
)

@Composable
fun BalanceChart(
    balanceHistory: List<BalancePoint>,
    modifier: Modifier = Modifier,
    height: Int = 200
) {
    if (balanceHistory.isEmpty()) return
    
    // Sort by timestamp to ensure chronological order (oldest to newest)
    val sortedHistory = remember(balanceHistory) {
        balanceHistory.sortedBy { it.timestamp }
    }
    
    val lineColor = MaterialTheme.colorScheme.primary
    val gridColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f)
    val backgroundColor = MaterialTheme.colorScheme.surface
    
    // Calculate min and max for scaling
    val maxBalance = sortedHistory.maxOf { it.balance }
    val minBalance = sortedHistory.minOf { it.balance }
    val balanceRange = maxBalance - minBalance
    
    // Add some padding to the range
    val paddedMin = minBalance - (balanceRange * BigDecimal("0.1"))
    val paddedMax = maxBalance + (balanceRange * BigDecimal("0.1"))
    val paddedRange = paddedMax - paddedMin
    
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .height(height.dp),
        color = backgroundColor,
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(Spacing.md)
        ) {
            // Y-axis labels (max and min balance)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = CurrencyFormatter.formatCurrency(paddedMax),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Balance Trend",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Medium
                )
            }
            
            // Chart Canvas
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                Canvas(
                    modifier = Modifier.fillMaxSize()
                ) {
                    val canvasWidth = size.width
                    val canvasHeight = size.height
                    
                    // Draw horizontal grid lines
                    val gridLines = 4
                    for (i in 0..gridLines) {
                        val y = canvasHeight * (i.toFloat() / gridLines)
                        drawLine(
                            color = gridColor,
                            start = Offset(0f, y),
                            end = Offset(canvasWidth, y),
                            strokeWidth = 1.dp.toPx(),
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
                        )
                    }
                    
                    if (sortedHistory.size < 2) {
                        // Just draw a single point if we only have one data point
                        val point = sortedHistory.first()
                        val y = canvasHeight * (1f - ((point.balance - paddedMin).toFloat() / paddedRange.toFloat()))
                        drawCircle(
                            color = lineColor,
                            radius = 4.dp.toPx(),
                            center = Offset(canvasWidth / 2, y)
                        )
                    } else {
                        // Draw the line chart
                        val path = Path()
                        val points = mutableListOf<Offset>()
                        
                        sortedHistory.forEachIndexed { index, point ->
                            val x = canvasWidth * (index.toFloat() / (sortedHistory.size - 1).coerceAtLeast(1))
                            val y = canvasHeight * (1f - ((point.balance - paddedMin).toFloat() / paddedRange.toFloat()))
                            
                            val offset = Offset(x, y)
                            points.add(offset)
                            
                            if (index == 0) {
                                path.moveTo(x, y)
                            } else {
                                path.lineTo(x, y)
                            }
                        }
                        
                        // Draw gradient fill under the line
                        val fillPath = Path().apply {
                            addPath(path)
                            lineTo(canvasWidth, canvasHeight)
                            lineTo(0f, canvasHeight)
                            close()
                        }
                        
                        drawPath(
                            path = fillPath,
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    lineColor.copy(alpha = 0.3f),
                                    lineColor.copy(alpha = 0.05f)
                                ),
                                startY = 0f,
                                endY = canvasHeight
                            )
                        )
                        
                        // Draw the line
                        drawPath(
                            path = path,
                            color = lineColor,
                            style = Stroke(
                                width = 2.dp.toPx(),
                                cap = StrokeCap.Round,
                                join = StrokeJoin.Round
                            )
                        )
                        
                        // Draw points
                        points.forEach { point ->
                            drawCircle(
                                color = backgroundColor,
                                radius = 4.dp.toPx(),
                                center = point
                            )
                            drawCircle(
                                color = lineColor,
                                radius = 4.dp.toPx(),
                                center = point,
                                style = Stroke(width = 2.dp.toPx())
                            )
                        }
                    }
                }
            }
            
            // X-axis labels (dates) - oldest on left, newest on right
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                if (sortedHistory.isNotEmpty()) {
                    // First (oldest) date on the left
                    Text(
                        text = sortedHistory.first().timestamp.format(
                            DateTimeFormatter.ofPattern("MMM d")
                        ),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    if (sortedHistory.size > 1) {
                        // Last (newest) date on the right
                        Text(
                            text = sortedHistory.last().timestamp.format(
                                DateTimeFormatter.ofPattern("MMM d")
                            ),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            
            // Min balance label
            Text(
                text = CurrencyFormatter.formatCurrency(paddedMin),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}