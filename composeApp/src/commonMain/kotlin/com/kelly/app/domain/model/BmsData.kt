package com.kelly.app.domain.model

data class BmsData(
    val voltage: Float = 0f,
    val current: Float = 0f,
    val power: Float = 0f,
    val soc: Float = 0f,
    val charge: Float = 0f,          // remaining charge Ah
    val capacity: Float = 0f,        // full capacity Ah
    val numCycles: Int = 0,
    val cellVoltages: List<Float> = emptyList(),
    val temperatures: List<Float> = emptyList(),
    val chargeEnabled: Boolean = false,
    val dischargeEnabled: Boolean = false,
    val isConnected: Boolean = false
)
