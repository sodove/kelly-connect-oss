package com.kelly.app.util

object Formatters {
    fun formatVoltage(value: Int, divisor: Double = 1.84): String {
        val voltage = value / divisor
        return "%.1fV".format(voltage)
    }

    fun formatTemperature(value: Int): String = "${value}C"

    fun formatPercentage(value: Int): String = "${value}%"

    fun formatSpeed(value: Int): String = "${value} RPM"
}
