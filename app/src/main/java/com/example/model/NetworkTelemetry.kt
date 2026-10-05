package com.example.model

enum class NetworkType(val title: String) {
    WIFI("Wi-Fi"),
    CELLULAR_5G("5G Mobile"),
    CELLULAR_4G("LTE / 4G"),
    CELLULAR("Мобильная сеть"),
    ETHERNET("Ethernet"),
    NONE("Нет подключения")
}

data class SpeedHistoryPoint(
    val timestamp: Long,
    val rxSpeedBps: Long,
    val txSpeedBps: Long
)

data class NetworkTelemetry(
    val rxBytesPerSec: Long = 0L,
    val txBytesPerSec: Long = 0L,
    val totalRxBytes: Long = 0L,
    val totalTxBytes: Long = 0L,
    val networkType: NetworkType = NetworkType.NONE,
    val networkName: String = "Нет подключения",
    val signalBars: Int = 0, // 0..4
    val signalDbm: Int? = null, // e.g. -65
    val signalPercentage: Int = 0, // 0..100
    val wifiFrequencyGhz: String? = null, // "2.4 GHz", "5 GHz", "6 GHz"
    val linkSpeedMbps: Int? = null, // e.g. 866 Mbps
    val ipAddress: String? = null,
    val pingMs: Long? = null,
    val isOnline: Boolean = false,
    val timestamp: Long = System.currentTimeMillis()
)
