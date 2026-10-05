package com.example.ui.components

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.SpeedHistoryPoint
import com.example.model.SpeedUnit
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.EmeraldGreen
import kotlin.math.max

@Composable
fun RealTimeSpeedChart(
    history: List<SpeedHistoryPoint>,
    unit: SpeedUnit,
    currentRxBps: Long,
    currentTxBps: Long,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .testTag("real_time_speed_chart"),
        shape = RoundedCornerShape(20.dp),
        color = Color(0xFF0F172A),
        border = BorderStroke(1.dp, Color(0xFF334155)),
        shadowElevation = 4.dp
    ) {
        Column(
            modifier = Modifier.padding(18.dp)
        ) {
            // Header with legend
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(CyberCyan.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.ShowChart,
                            contentDescription = null,
                            tint = CyberCyan,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    Column {
                        Text(
                            text = "ГРАФИК СКОРОСТИ В РЕАЛЬНОМ ВРЕМЕНИ",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF94A3B8),
                            letterSpacing = 0.8.sp
                        )
                        Text(
                            text = "Динамика за 30 секунд (${unit.shortNameRu})",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF64748B)
                        )
                    }
                }

                // Legend
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(CyberCyan)
                        )
                        Text(
                            text = "Загрузка",
                            fontSize = 11.sp,
                            color = Color(0xFF94A3B8)
                        )
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(EmeraldGreen)
                        )
                        Text(
                            text = "Отдача",
                            fontSize = 11.sp,
                            color = Color(0xFF94A3B8)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Canvas Line Chart
            val rxValues = history.map { unit.toUnitValue(it.rxSpeedBps) }
            val txValues = history.map { unit.toUnitValue(it.txSpeedBps) }

            val maxVal = max(
                1f,
                max(
                    rxValues.maxOrNull() ?: 1f,
                    txValues.maxOrNull() ?: 1f
                ) * 1.2f
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF070B13))
                    .padding(horizontal = 8.dp, vertical = 8.dp)
            ) {
                Canvas(modifier = Modifier.fillMaxWidth().height(124.dp)) {
                    val width = size.width
                    val height = size.height

                    // Grid lines (3 horizontal lines)
                    val gridColor = Color(0xFF1E293B)
                    for (i in 0..2) {
                        val y = height * (i / 2f)
                        drawLine(
                            color = gridColor,
                            start = Offset(0f, y),
                            end = Offset(width, y),
                            strokeWidth = 1f
                        )
                    }

                    if (rxValues.size >= 2) {
                        val stepX = width / (rxValues.size - 1).coerceAtLeast(1)

                        // 1. Download Path & Gradient Fill
                        val rxPath = Path()
                        val rxFillPath = Path()
                        rxFillPath.moveTo(0f, height)

                        rxValues.forEachIndexed { index, value ->
                            val x = index * stepX
                            val y = height - (value / maxVal) * height
                            if (index == 0) {
                                rxPath.moveTo(x, y)
                                rxFillPath.lineTo(x, y)
                            } else {
                                rxPath.lineTo(x, y)
                                rxFillPath.lineTo(x, y)
                            }
                        }
                        rxFillPath.lineTo(width, height)
                        rxFillPath.close()

                        drawPath(
                            path = rxFillPath,
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    CyberCyan.copy(alpha = 0.25f),
                                    CyberCyan.copy(alpha = 0.0f)
                                ),
                                startY = 0f,
                                endY = height
                            )
                        )

                        drawPath(
                            path = rxPath,
                            color = CyberCyan,
                            style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
                        )

                        // 2. Upload Path
                        val txPath = Path()
                        txValues.forEachIndexed { index, value ->
                            val x = index * stepX
                            val y = height - (value / maxVal) * height
                            if (index == 0) {
                                txPath.moveTo(x, y)
                            } else {
                                txPath.lineTo(x, y)
                            }
                        }
                        drawPath(
                            path = txPath,
                            color = EmeraldGreen,
                            style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Peak indicator footer
            val peakRx = history.maxOfOrNull { it.rxSpeedBps } ?: currentRxBps
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Пиковая скорость сессии: ${unit.formatSpeed(peakRx)}",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp,
                    color = Color(0xFF64748B)
                )

                Text(
                    text = "Макс. шкала: ${String.format("%.1f", maxVal)} ${unit.shortNameRu}",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp,
                    color = Color(0xFF64748B)
                )
            }
        }
    }
}
