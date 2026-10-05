package com.example.service

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.TrafficStats
import android.net.wifi.WifiInfo
import android.net.wifi.WifiManager
import android.os.Build
import android.os.SystemClock
import android.provider.Settings
import com.example.model.NetworkTelemetry
import com.example.model.NetworkType
import com.example.model.SpeedHistoryPoint
import com.example.model.SpeedUnit
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.Inet4Address
import java.net.NetworkInterface
import java.net.URL
import kotlin.math.max

enum class OverlayPositionPreset(val titleRu: String) {
    TOP_RIGHT("Справа (у сигнала)"),
    TOP_CENTER("По центру"),
    TOP_LEFT("Слева")
}

object SpeedMonitorManager {
    private const val PREFS_NAME = "netspeed_prefs"
    private const val KEY_IS_MONITORING = "is_monitoring_active"
    private const val KEY_SPEED_UNIT = "speed_unit"
    private const val KEY_OVERLAY_ENABLED = "overlay_enabled"
    private const val KEY_OVERLAY_PRESET = "overlay_preset"

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var monitorJob: Job? = null
    private var speedTestJob: Job? = null

    private var prefs: SharedPreferences? = null

    private val _isMonitoringActive = MutableStateFlow(false)
    val isMonitoringActive: StateFlow<Boolean> = _isMonitoringActive.asStateFlow()

    private val _selectedUnit = MutableStateFlow(SpeedUnit.MBPS)
    val selectedUnit: StateFlow<SpeedUnit> = _selectedUnit.asStateFlow()

    private val _isOverlayEnabled = MutableStateFlow(true)
    val isOverlayEnabled: StateFlow<Boolean> = _isOverlayEnabled.asStateFlow()

    private val _overlayPositionPreset = MutableStateFlow(OverlayPositionPreset.TOP_RIGHT)
    val overlayPositionPreset: StateFlow<OverlayPositionPreset> = _overlayPositionPreset.asStateFlow()

    private val _telemetry = MutableStateFlow(NetworkTelemetry())
    val telemetry: StateFlow<NetworkTelemetry> = _telemetry.asStateFlow()

    private val _speedHistory = MutableStateFlow<List<SpeedHistoryPoint>>(emptyList())
    val speedHistory: StateFlow<List<SpeedHistoryPoint>> = _speedHistory.asStateFlow()

    private val _isSpeedTesting = MutableStateFlow(false)
    val isSpeedTesting: StateFlow<Boolean> = _isSpeedTesting.asStateFlow()

    private var lastRxBytes: Long = 0L
    private var lastTxBytes: Long = 0L
    private var lastTimestamp: Long = 0L

    fun init(context: Context) {
        if (prefs == null) {
            val appCtx = context.applicationContext
            prefs = appCtx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

            val unitName = prefs?.getString(KEY_SPEED_UNIT, SpeedUnit.MBPS.name) ?: SpeedUnit.MBPS.name
            _selectedUnit.value = try {
                SpeedUnit.valueOf(unitName)
            } catch (e: Exception) {
                SpeedUnit.MBPS
            }

            _isOverlayEnabled.value = prefs?.getBoolean(KEY_OVERLAY_ENABLED, true) ?: true

            val presetName = prefs?.getString(KEY_OVERLAY_PRESET, OverlayPositionPreset.TOP_RIGHT.name)
            _overlayPositionPreset.value = try {
                OverlayPositionPreset.valueOf(presetName ?: OverlayPositionPreset.TOP_RIGHT.name)
            } catch (e: Exception) {
                OverlayPositionPreset.TOP_RIGHT
            }

            val savedMonitoring = prefs?.getBoolean(KEY_IS_MONITORING, false) ?: false
            if (savedMonitoring) {
                setMonitoringActive(appCtx, true)
            }
        }
    }

    fun setMonitoringActive(context: Context, active: Boolean) {
        _isMonitoringActive.value = active
        prefs?.edit()?.putBoolean(KEY_IS_MONITORING, active)?.apply()

        val appCtx = context.applicationContext
        val intent = Intent(appCtx, NetworkSpeedOverlayService::class.java).apply {
            action = if (active) NetworkSpeedOverlayService.ACTION_START else NetworkSpeedOverlayService.ACTION_STOP
        }

        try {
            if (active) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    appCtx.startForegroundService(intent)
                } else {
                    appCtx.startService(intent)
                }
            } else {
                appCtx.startService(intent)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        if (active) {
            startMeasurementLoop(appCtx)
        } else {
            stopMeasurementLoop()
        }
    }

    fun setSelectedUnit(unit: SpeedUnit) {
        _selectedUnit.value = unit
        prefs?.edit()?.putString(KEY_SPEED_UNIT, unit.name)?.apply()
    }

    fun setOverlayEnabled(context: Context, enabled: Boolean) {
        _isOverlayEnabled.value = enabled
        prefs?.edit()?.putBoolean(KEY_OVERLAY_ENABLED, enabled)?.apply()
        // If monitoring is active, trigger service update
        if (_isMonitoringActive.value) {
            val appCtx = context.applicationContext
            val intent = Intent(appCtx, NetworkSpeedOverlayService::class.java).apply {
                action = NetworkSpeedOverlayService.ACTION_UPDATE_OVERLAY_VISIBILITY
            }
            try {
                appCtx.startService(intent)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun setOverlayPositionPreset(context: Context, preset: OverlayPositionPreset) {
        _overlayPositionPreset.value = preset
        prefs?.edit()?.putString(KEY_OVERLAY_PRESET, preset.name)?.apply()
        if (_isMonitoringActive.value) {
            val appCtx = context.applicationContext
            val intent = Intent(appCtx, NetworkSpeedOverlayService::class.java).apply {
                action = NetworkSpeedOverlayService.ACTION_UPDATE_OVERLAY_POSITION
            }
            try {
                appCtx.startService(intent)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun hasOverlayPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Settings.canDrawOverlays(context)
        } else {
            true
        }
    }

    private fun startMeasurementLoop(context: Context) {
        monitorJob?.cancel()
        lastRxBytes = TrafficStats.getTotalRxBytes()
        lastTxBytes = TrafficStats.getTotalTxBytes()
        lastTimestamp = SystemClock.elapsedRealtime()

        monitorJob = scope.launch {
            while (isActive) {
                delay(1000L)
                updateMetrics(context)
            }
        }
    }

    private fun stopMeasurementLoop() {
        monitorJob?.cancel()
        monitorJob = null
        _telemetry.value = _telemetry.value.copy(
            rxBytesPerSec = 0L,
            txBytesPerSec = 0L
        )
    }

    private fun updateMetrics(context: Context) {
        val now = SystemClock.elapsedRealtime()
        val currentRx = TrafficStats.getTotalRxBytes()
        val currentTx = TrafficStats.getTotalTxBytes()

        val timeDeltaMs = now - lastTimestamp
        val timeDeltaSec = if (timeDeltaMs > 0) timeDeltaMs / 1000.0 else 1.0

        val rxDelta = if (lastRxBytes > 0 && currentRx >= lastRxBytes) currentRx - lastRxBytes else 0L
        val txDelta = if (lastTxBytes > 0 && currentTx >= lastTxBytes) currentTx - lastTxBytes else 0L

        val rxSpeed = (rxDelta / timeDeltaSec).toLong()
        val txSpeed = (txDelta / timeDeltaSec).toLong()

        lastRxBytes = currentRx
        lastTxBytes = currentTx
        lastTimestamp = now

        val networkInfo = queryNetworkInfo(context)

        val newTelemetry = NetworkTelemetry(
            rxBytesPerSec = rxSpeed,
            txBytesPerSec = txSpeed,
            totalRxBytes = currentRx,
            totalTxBytes = currentTx,
            networkType = networkInfo.type,
            networkName = networkInfo.name,
            signalBars = networkInfo.bars,
            signalDbm = networkInfo.dbm,
            signalPercentage = networkInfo.percentage,
            wifiFrequencyGhz = networkInfo.frequency,
            linkSpeedMbps = networkInfo.linkSpeedMbps,
            ipAddress = networkInfo.ipAddress,
            pingMs = _telemetry.value.pingMs,
            isOnline = networkInfo.isOnline,
            timestamp = System.currentTimeMillis()
        )

        _telemetry.value = newTelemetry

        // Update history (keep last 30 seconds)
        val currentHistory = _speedHistory.value.toMutableList()
        currentHistory.add(SpeedHistoryPoint(System.currentTimeMillis(), rxSpeed, txSpeed))
        if (currentHistory.size > 30) {
            currentHistory.removeAt(0)
        }
        _speedHistory.value = currentHistory
    }

    private data class NetworkQuery(
        val type: NetworkType,
        val name: String,
        val bars: Int,
        val dbm: Int?,
        val percentage: Int,
        val frequency: String?,
        val linkSpeedMbps: Int?,
        val ipAddress: String?,
        val isOnline: Boolean
    )

    private fun queryNetworkInfo(context: Context): NetworkQuery {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
        val activeNetwork = cm?.activeNetwork
        val capabilities = cm?.getNetworkCapabilities(activeNetwork)

        if (activeNetwork == null || capabilities == null) {
            return NetworkQuery(
                type = NetworkType.NONE,
                name = "Нет подключения",
                bars = 0,
                dbm = null,
                percentage = 0,
                frequency = null,
                linkSpeedMbps = null,
                ipAddress = null,
                isOnline = false
            )
        }

        val hasInternet = capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)

        if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) {
            val wm = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
            val connectionInfo: WifiInfo? = wm?.connectionInfo

            val rssi = connectionInfo?.rssi ?: -127
            val level = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                wm?.calculateSignalLevel(rssi) ?: 3
            } else {
                @Suppress("DEPRECATION")
                WifiManager.calculateSignalLevel(rssi, 5)
            }
            val safeBars = level.coerceIn(0, 4)
            val percentage = ((rssi + 100) * 2).coerceIn(0, 100)

            val freq = connectionInfo?.frequency ?: 0
            val freqString = when {
                freq >= 5925 -> "6 GHz (Wi-Fi 6E/7)"
                freq >= 4900 -> "5 GHz"
                freq in 2400..2500 -> "2.4 GHz"
                else -> null
            }

            val linkSpeed = connectionInfo?.linkSpeed?.takeIf { it > 0 }
            val rawSsid = connectionInfo?.ssid?.replace("\"", "") ?: "Wi-Fi"
            val ssid = if (rawSsid == "<unknown ssid>") "Беспроводная сеть Wi-Fi" else rawSsid

            return NetworkQuery(
                type = NetworkType.WIFI,
                name = ssid,
                bars = safeBars,
                dbm = if (rssi in -120..0) rssi else -65,
                percentage = percentage,
                frequency = freqString,
                linkSpeedMbps = linkSpeed,
                ipAddress = getLocalIpAddress(),
                isOnline = hasInternet
            )
        } else if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)) {
            // Cellular
            val signalStrength = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                capabilities.signalStrength
            } else {
                -75
            }
            val bars = when {
                signalStrength > -65 -> 4
                signalStrength > -75 -> 3
                signalStrength > -85 -> 2
                signalStrength > -95 -> 1
                else -> 1
            }
            val percentage = ((signalStrength + 110) * (100 / 50)).coerceIn(10, 100)

            return NetworkQuery(
                type = NetworkType.CELLULAR_5G,
                name = "Мобильная сеть (5G / LTE)",
                bars = bars,
                dbm = if (signalStrength in -125..0) signalStrength else -78,
                percentage = percentage,
                frequency = "Cellular Band",
                linkSpeedMbps = null,
                ipAddress = getLocalIpAddress(),
                isOnline = hasInternet
            )
        } else if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)) {
            return NetworkQuery(
                type = NetworkType.ETHERNET,
                name = "Ethernet LAN",
                bars = 4,
                dbm = 0,
                percentage = 100,
                frequency = "1 Gbps+",
                linkSpeedMbps = 1000,
                ipAddress = getLocalIpAddress(),
                isOnline = hasInternet
            )
        }

        return NetworkQuery(
            type = NetworkType.CELLULAR,
            name = "Подключено",
            bars = 3,
            dbm = -70,
            percentage = 70,
            frequency = null,
            linkSpeedMbps = null,
            ipAddress = getLocalIpAddress(),
            isOnline = hasInternet
        )
    }

    private fun getLocalIpAddress(): String? {
        try {
            val interfaces = NetworkInterface.getNetworkInterfaces()
            while (interfaces.hasMoreElements()) {
                val networkInterface = interfaces.nextElement()
                val addresses = networkInterface.inetAddresses
                while (addresses.hasMoreElements()) {
                    val address = addresses.nextElement()
                    if (!address.isLoopbackAddress && address is Inet4Address) {
                        return address.hostAddress
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return null
    }

    fun runSpeedTest() {
        if (_isSpeedTesting.value) return
        speedTestJob?.cancel()

        speedTestJob = scope.launch {
            _isSpeedTesting.value = true
            try {
                // Perform real ping latency check to reliable endpoint
                val pingStart = System.currentTimeMillis()
                val pingUrl = URL("https://www.google.com/generate_204")
                withContext(Dispatchers.IO) {
                    val conn = pingUrl.openConnection() as HttpURLConnection
                    conn.connectTimeout = 3000
                    conn.readTimeout = 3000
                    conn.requestMethod = "GET"
                    conn.responseCode
                    conn.disconnect()
                }
                val pingDuration = max(1L, System.currentTimeMillis() - pingStart)
                _telemetry.value = _telemetry.value.copy(pingMs = pingDuration)

                // Download small test payload chunk to generate measurable traffic for 4 seconds
                val testUrls = listOf(
                    "https://speed.cloudflare.com/__down?bytes=5000000",
                    "https://www.google.com/favicon.ico"
                )

                for (targetUrl in testUrls) {
                    if (!isActive) break
                    withContext(Dispatchers.IO) {
                        try {
                            val conn = URL(targetUrl).openConnection() as HttpURLConnection
                            conn.connectTimeout = 4000
                            conn.readTimeout = 5000
                            conn.inputStream.use { stream ->
                                val buffer = ByteArray(8192)
                                var bytesRead: Int
                                var totalRead = 0
                                while (stream.read(buffer).also { bytesRead = it } != -1 && totalRead < 8_000_000) {
                                    totalRead += bytesRead
                                }
                            }
                            conn.disconnect()
                        } catch (e: Exception) {
                            // Ignore network exceptions during test
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                delay(500)
                _isSpeedTesting.value = false
            }
        }
    }
}
