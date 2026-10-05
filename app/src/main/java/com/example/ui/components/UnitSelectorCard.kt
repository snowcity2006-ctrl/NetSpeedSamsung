package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.SpeedUnit
import com.example.ui.theme.CyberCyan

@Composable
fun UnitSelectorCard(
    selectedUnit: SpeedUnit,
    onUnitSelected: (SpeedUnit) -> Unit,
    currentSpeedBps: Long,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .testTag("unit_selector_card"),
        shape = RoundedCornerShape(20.dp),
        color = Color(0xFF0F172A),
        border = BorderStroke(1.dp, Color(0xFF334155)),
        shadowElevation = 4.dp
    ) {
        Column(
            modifier = Modifier.padding(18.dp)
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
                        imageVector = Icons.Default.Tune,
                        contentDescription = null,
                        tint = CyberCyan,
                        modifier = Modifier.size(18.dp)
                    )
                }

                Column {
                    Text(
                        text = "ЕДИНИЦЫ ИЗМЕРЕНИЯ СКОРОСТИ",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF94A3B8),
                        letterSpacing = 0.8.sp
                    )
                    Text(
                        text = "Выберите формат отображения",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF64748B)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Two Unit Options (Mbps vs Kbps)
            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                UnitOptionItem(
                    title = "Мегабит в секунду",
                    shortLabel = "Мбит/с (Mbps)",
                    description = "Рекомендуется для высокоскоростного Wi-Fi и 5G сетей",
                    previewValue = SpeedUnit.MBPS.formatSpeed(currentSpeedBps),
                    isSelected = selectedUnit == SpeedUnit.MBPS,
                    onClick = { onUnitSelected(SpeedUnit.MBPS) },
                    testTag = "unit_option_mbps"
                )

                UnitOptionItem(
                    title = "Килобит в секунду",
                    shortLabel = "Кбит/с (kbps)",
                    description = "Высокая точность для фоновых процессов и мобильного интернета",
                    previewValue = SpeedUnit.KBPS.formatSpeed(currentSpeedBps),
                    isSelected = selectedUnit == SpeedUnit.KBPS,
                    onClick = { onUnitSelected(SpeedUnit.KBPS) },
                    testTag = "unit_option_kbps"
                )
            }
        }
    }
}

@Composable
private fun UnitOptionItem(
    title: String,
    shortLabel: String,
    description: String,
    previewValue: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    testTag: String
) {
    val borderColor by animateColorAsState(
        targetValue = if (isSelected) CyberCyan else Color(0xFF1E293B),
        animationSpec = tween(200),
        label = "unitBorder"
    )

    val bgColor by animateColorAsState(
        targetValue = if (isSelected) CyberCyan.copy(alpha = 0.1f) else Color(0xFF090E17),
        animationSpec = tween(200),
        label = "unitBg"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .testTag(testTag)
            .clip(RoundedCornerShape(14.dp))
            .background(bgColor)
            .border(1.5.dp, borderColor, RoundedCornerShape(14.dp))
            .clickable { onClick() }
            .padding(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                // Radio indicator
                Box(
                    modifier = Modifier
                        .size(22.dp)
                        .clip(CircleShape)
                        .border(
                            2.dp,
                            if (isSelected) CyberCyan else Color(0xFF64748B),
                            CircleShape
                        )
                        .background(if (isSelected) CyberCyan else Color.Transparent),
                    contentAlignment = Alignment.Center
                ) {
                    if (isSelected) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            tint = Color(0xFF0F172A),
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (isSelected) Color(0xFFF8FAFC) else Color(0xFFCBD5E1)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "• $shortLabel",
                            fontSize = 12.sp,
                            color = if (isSelected) CyberCyan else Color(0xFF94A3B8),
                            fontWeight = FontWeight.Medium
                        )
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = description,
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFF64748B),
                        lineHeight = 14.sp
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Current value in this unit
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFF1E293B))
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(
                    text = previewValue,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isSelected) CyberCyan else Color(0xFF94A3B8)
                )
            }
        }
    }
}
