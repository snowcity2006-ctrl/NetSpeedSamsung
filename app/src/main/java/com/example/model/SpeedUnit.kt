package com.example.model

import java.util.Locale

enum class SpeedUnit(
    val shortNameRu: String,
    val fullNameRu: String,
    val symbolRu: String
) {
    MBPS("Мбит/с", "Мегабит в секунду", "Мбит/с"),
    KBPS("Кбит/с", "Килобит в секунду", "Кбит/с");

    /**
     * Converts raw bytes per second to formatted string with unit.
     */
    fun formatSpeed(bytesPerSecond: Long): String {
        val (value, unit) = formatSpeedParts(bytesPerSecond)
        return "$value $unit"
    }

    /**
     * Returns pair of (formattedValue, unitString)
     */
    fun formatSpeedParts(bytesPerSecond: Long): Pair<String, String> {
        val safeBytes = if (bytesPerSecond < 0) 0L else bytesPerSecond
        val bitsPerSec = safeBytes * 8.0

        return when (this) {
            MBPS -> {
                val mbps = bitsPerSec / 1_000_000.0
                val formatted = when {
                    mbps >= 100.0 -> String.format(Locale.US, "%.0f", mbps)
                    mbps >= 10.0 -> String.format(Locale.US, "%.1f", mbps)
                    mbps > 0.0 -> String.format(Locale.US, "%.2f", mbps)
                    else -> "0.0"
                }
                Pair(formatted, shortNameRu)
            }
            KBPS -> {
                val kbps = bitsPerSec / 1_000.0
                val formatted = when {
                    kbps >= 1000.0 -> String.format(Locale.US, "%,.0f", kbps).replace(',', ' ')
                    kbps >= 10.0 -> String.format(Locale.US, "%.0f", kbps)
                    kbps > 0.0 -> String.format(Locale.US, "%.1f", kbps)
                    else -> "0"
                }
                Pair(formatted, shortNameRu)
            }
        }
    }

    /**
     * Converts bytes per second to float number for charting/gauges
     */
    fun toUnitValue(bytesPerSecond: Long): Float {
        val safeBytes = if (bytesPerSecond < 0) 0L else bytesPerSecond
        val bitsPerSec = safeBytes * 8.0
        return when (this) {
            MBPS -> (bitsPerSec / 1_000_000.0).toFloat()
            KBPS -> (bitsPerSec / 1_000.0).toFloat()
        }
    }
}
