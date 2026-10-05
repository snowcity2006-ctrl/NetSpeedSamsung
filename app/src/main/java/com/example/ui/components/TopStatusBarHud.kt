package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.material.icons.filled.CellTower
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.SignalCellularAlt
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.NetworkTelemetry
import com.example.model.NetworkType
import com.example.model.SpeedUnit
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.EmeraldGreen
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun TopStatusBarHud(
    telemetry: NetworkTelemetry,
    unit: SpeedUnit,
    isActive: Boolean,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(800),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlpha"
    )

    val timeString = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
    val (rxVal, rxUnit) = unit.formatSpeedParts(telemetry.rxBytesPerSec)
    val (txVal, txUnit) = unit.formatSpeedParts(telemetry.txBytesPerSec)

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .testTag("top_status_bar_hud"),
        shape = RoundedCornerShape(20.dp),
        color = Color(0xFF0F172A),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (isActive) CyberCyan.copy(alpha = 0.5f) else Color(0xFF334155)
        ),
        shadowElevation = 6.dp
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Header: Device & Status Indicator
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(if (isActive) EmeraldGreen else Color(0xFF64748B))
                            .alpha(if (isActive) pulseAlpha else 1f)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "ВЕРХНЯЯ СТРОКА СТАТУСА",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF94A3B8),
                        letterSpacing = 1.sp
                    )
                }

                Text(
                    text = if (isActive) "ОТРАЖАЕТСЯ У СИГНАЛА" else "НЕ АКТИВНО",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (isActive) CyberCyan else Color(0xFF64748B),
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Simulated Samsung Galaxy Status Bar Area
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color(0xFF070B13))
                    .border(1.dp, Color(0xFF1E293B), RoundedCornerShape(14.dp))
                    .padding(horizontal = 14.dp, vertical = 10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Left side: Clock & Network Label
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = timeString,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFE2E8F0)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = telemetry.networkName.take(18),
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFF94A3B8)
                        )
                    }

                    // Right side: Signal Strength + Speed Indicator Badge!
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Live Speed Badge (when active)
                        AnimatedVisibility(visible = isActive) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(CyberCyan.copy(alpha = 0.15f))
                                    .border(1.dp, CyberCyan.copy(alpha = 0.4f), RoundedCornerShape(10.dp))
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Download,
                                        contentDescription = "Загрузка",
                                        tint = CyberCyan,
                                        modifier = Modifier.size(13.dp)
                                    )
                                    Text(
                                        text = "$rxVal $rxUnit",
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp,
                                        color = CyberCyan
                                    )
                                }
                            }
                        }

                        // Signal Strength Icon & dBm
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                            if (telemetry.networkType == NetworkType.WIFI) {
                                Icon(
                                    imageVector = Icons.Default.Wifi,
                                    contentDescription = "Wi-Fi",
                                    tint = if (telemetry.signalBars >= 2) EmeraldGreen else Color(0xFFFBBF24),
                                    modifier = Modifier.size(16.dp)
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.SignalCellularAlt,
                                    contentDescription = "Сотовая связь",
                                    tint = if (telemetry.signalBars >= 2) EmeraldGreen else Color(0xFFFBBF24),
                                    modifier = Modifier.size(16.dp)
                                )
                            }

                            // Signal Bars (0-4 dots or text)
                            SignalBarsIndicator(bars = telemetry.signalBars)

                            if (telemetry.signalDbm != null) {
                                Text(
                                    text = "${telemetry.signalDbm}dBm",
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 10.sp,
                                    color = Color(0xFF94A3B8)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Two-column Speed Metrics Detail inside Card
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Download metric
                MetricSubCard(
                    title = "СКОРОСТЬ ЗАГРУЗКИ",
                    value = rxVal,
                    unit = rxUnit,
                    icon = Icons.Default.Download,
                    color = CyberCyan,
                    modifier = Modifier.weight(1f)
                )

                // Upload metric
                MetricSubCard(
                    title = "СКОРОСТЬ ОТДАЧИ",
                    value = txVal,
                    unit = txUnit,
                    icon = Icons.Default.Upload,
                    color = EmeraldGreen,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun SignalBarsIndicator(bars: Int) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(1.5.dp),
        verticalAlignment = Alignment.Bottom,
        modifier = Modifier.height(12.dp)
    ) {
        val safeBars = bars.coerceIn(0, 4)
        for (i in 1..4) {
            val isFilled = i <= safeBars
            val heightDp = (i * 2.5 + 2).dp
            Box(
                modifier = Modifier
                    .width(2.5.dp)
                    .height(heightDp)
                    .clip(RoundedCornerShape(1.dp))
                    .background(
                        if (isFilled) {
                            if (safeBars >= 3) EmeraldGreen else Color(0xFFFBBF24)
                        } else {
                            Color(0xFF334155)
                        }
                    )
            )
        }
    }
}

@Composable
private fun MetricSubCard(
    title: String,
    value: String,
    unit: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFF1E293B).copy(alpha = 0.6f))
            .padding(10.dp)
    ) {
        Column {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.size(14.dp)
                )
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelSmall,
                    fontSize = 10.sp,
                    color = Color(0xFF94A3B8),
                    fontWeight = FontWeight.Medium
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = value,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                    color = color
                )
                Text(
                    text = unit,
                    fontSize = 11.sp,
                    color = Color(0xFFCBD5E1),
                    modifier = Modifier.padding(bottom = 2.dp)
                )
            }
        }
    }
}
