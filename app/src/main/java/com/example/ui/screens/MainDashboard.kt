package com.example.ui.screens

import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.SpeedUnit
import com.example.service.SpeedMonitorManager
import com.example.ui.components.MasterToggleCard
import com.example.ui.components.NetworkDiagnosticsCard
import com.example.ui.components.OverlaySettingsCard
import com.example.ui.components.RealTimeSpeedChart
import com.example.ui.components.SpeedTestCard
import com.example.ui.components.TopStatusBarHud
import com.example.ui.components.UnitSelectorCard
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.EmeraldGreen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainDashboard(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    val isMonitoringActive by SpeedMonitorManager.isMonitoringActive.collectAsState()
    val selectedUnit by SpeedMonitorManager.selectedUnit.collectAsState()
    val isOverlayEnabled by SpeedMonitorManager.isOverlayEnabled.collectAsState()
    val overlayPreset by SpeedMonitorManager.overlayPositionPreset.collectAsState()
    val telemetry by SpeedMonitorManager.telemetry.collectAsState()
    val history by SpeedMonitorManager.speedHistory.collectAsState()
    val isSpeedTesting by SpeedMonitorManager.isSpeedTesting.collectAsState()

    val hasOverlayPermission = SpeedMonitorManager.hasOverlayPermission(context)

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = Color(0xFF090D16),
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(CyberCyan.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Speed,
                                contentDescription = null,
                                tint = CyberCyan,
                                modifier = Modifier.size(22.dp)
                            )
                        }

                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "NetSpeed",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFF8FAFC)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(Color(0xFF1E293B))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = "Galaxy S24 / Tab S11",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = CyberCyan
                                    )
                                }
                            }
                            Text(
                                text = "Индикатор скорости и силы сигнала",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFF94A3B8)
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF090D16),
                    scrolledContainerColor = Color(0xFF0F172A)
                )
            )
        }
    ) { innerPadding ->
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            val isTabletExpanded = maxWidth >= 760.dp

            if (isTabletExpanded) {
                // Adaptive layout for Samsung Galaxy Tab S11 Ultra (Expanded / Tablet)
                TabletTwoPaneLayout(
                    isActive = isMonitoringActive,
                    onToggleActive = { SpeedMonitorManager.setMonitoringActive(context, it) },
                    selectedUnit = selectedUnit,
                    onSelectUnit = { SpeedMonitorManager.setSelectedUnit(it) },
                    isOverlayEnabled = isOverlayEnabled,
                    onToggleOverlay = { SpeedMonitorManager.setOverlayEnabled(context, it) },
                    currentPreset = overlayPreset,
                    onSelectPreset = { SpeedMonitorManager.setOverlayPositionPreset(context, it) },
                    hasOverlayPermission = hasOverlayPermission,
                    telemetry = telemetry,
                    history = history,
                    isTesting = isSpeedTesting,
                    onStartTest = { SpeedMonitorManager.runSpeedTest() }
                )
            } else {
                // Adaptive layout for Samsung Galaxy S24 Ultra (Compact / Phone)
                PhoneSingleColumnLayout(
                    isActive = isMonitoringActive,
                    onToggleActive = { SpeedMonitorManager.setMonitoringActive(context, it) },
                    selectedUnit = selectedUnit,
                    onSelectUnit = { SpeedMonitorManager.setSelectedUnit(it) },
                    isOverlayEnabled = isOverlayEnabled,
                    onToggleOverlay = { SpeedMonitorManager.setOverlayEnabled(context, it) },
                    currentPreset = overlayPreset,
                    onSelectPreset = { SpeedMonitorManager.setOverlayPositionPreset(context, it) },
                    hasOverlayPermission = hasOverlayPermission,
                    telemetry = telemetry,
                    history = history,
                    isTesting = isSpeedTesting,
                    onStartTest = { SpeedMonitorManager.runSpeedTest() }
                )
            }
        }
    }
}

/**
 * Layout optimized for Samsung Galaxy S24 Ultra and standard phone screens
 */
@Composable
private fun PhoneSingleColumnLayout(
    isActive: Boolean,
    onToggleActive: (Boolean) -> Unit,
    selectedUnit: SpeedUnit,
    onSelectUnit: (SpeedUnit) -> Unit,
    isOverlayEnabled: Boolean,
    onToggleOverlay: (Boolean) -> Unit,
    currentPreset: com.example.service.OverlayPositionPreset,
    onSelectPreset: (com.example.service.OverlayPositionPreset) -> Unit,
    hasOverlayPermission: Boolean,
    telemetry: com.example.model.NetworkTelemetry,
    history: List<com.example.model.SpeedHistoryPoint>,
    isTesting: Boolean,
    onStartTest: () -> Unit
) {
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Top Status Bar HUD preview (shows live speed next to signal bars)
        TopStatusBarHud(
            telemetry = telemetry,
            unit = selectedUnit,
            isActive = isActive
        )

        // 2. Master ON/OFF Switch (Вкл./Выкл.)
        MasterToggleCard(
            isActive = isActive,
            onToggle = onToggleActive
        )

        // 3. Unit Selector (Мегабит в секунду / Килобит в секунду)
        UnitSelectorCard(
            selectedUnit = selectedUnit,
            onUnitSelected = onSelectUnit,
            currentSpeedBps = telemetry.rxBytesPerSec
        )

        // 4. Overlay Settings Card (Floating widget on top bar)
        OverlaySettingsCard(
            isOverlayEnabled = isOverlayEnabled,
            onOverlayToggled = onToggleOverlay,
            currentPreset = currentPreset,
            onPresetSelected = onSelectPreset,
            hasOverlayPermission = hasOverlayPermission
        )

        // 5. Speed Test Button (Generate instant traffic)
        SpeedTestCard(
            isTesting = isTesting,
            onStartTest = onStartTest,
            currentUnit = selectedUnit,
            currentSpeedBps = telemetry.rxBytesPerSec
        )

        // 6. Real-time Speed Chart
        RealTimeSpeedChart(
            history = history,
            unit = selectedUnit,
            currentRxBps = telemetry.rxBytesPerSec,
            currentTxBps = telemetry.txBytesPerSec
        )

        // 7. Network and Signal Diagnostics
        NetworkDiagnosticsCard(
            telemetry = telemetry
        )

        Spacer(modifier = Modifier.height(24.dp))
    }
}

/**
 * Layout optimized for Samsung Galaxy Tab S11 Ultra (large tablet screen)
 */
@Composable
private fun TabletTwoPaneLayout(
    isActive: Boolean,
    onToggleActive: (Boolean) -> Unit,
    selectedUnit: SpeedUnit,
    onSelectUnit: (SpeedUnit) -> Unit,
    isOverlayEnabled: Boolean,
    onToggleOverlay: (Boolean) -> Unit,
    currentPreset: com.example.service.OverlayPositionPreset,
    onSelectPreset: (com.example.service.OverlayPositionPreset) -> Unit,
    hasOverlayPermission: Boolean,
    telemetry: com.example.model.NetworkTelemetry,
    history: List<com.example.model.SpeedHistoryPoint>,
    isTesting: Boolean,
    onStartTest: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // Left Column: Controls & Configuration Pane
        val leftScrollState = rememberScrollState()
        Column(
            modifier = Modifier
                .width(420.dp)
                .fillMaxHeight()
                .verticalScroll(leftScrollState),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            MasterToggleCard(
                isActive = isActive,
                onToggle = onToggleActive
            )

            UnitSelectorCard(
                selectedUnit = selectedUnit,
                onUnitSelected = onSelectUnit,
                currentSpeedBps = telemetry.rxBytesPerSec
            )

            OverlaySettingsCard(
                isOverlayEnabled = isOverlayEnabled,
                onOverlayToggled = onToggleOverlay,
                currentPreset = currentPreset,
                onPresetSelected = onSelectPreset,
                hasOverlayPermission = hasOverlayPermission
            )

            SpeedTestCard(
                isTesting = isTesting,
                onStartTest = onStartTest,
                currentUnit = selectedUnit,
                currentSpeedBps = telemetry.rxBytesPerSec
            )

            Spacer(modifier = Modifier.height(20.dp))
        }

        // Right Column: Live Telemetry, Top HUD, Real-time Chart & Diagnostics
        val rightScrollState = rememberScrollState()
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .verticalScroll(rightScrollState),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            TopStatusBarHud(
                telemetry = telemetry,
                unit = selectedUnit,
                isActive = isActive
            )

            RealTimeSpeedChart(
                history = history,
                unit = selectedUnit,
                currentRxBps = telemetry.rxBytesPerSec,
                currentTxBps = telemetry.txBytesPerSec
            )

            NetworkDiagnosticsCard(
                telemetry = telemetry
            )

            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}
