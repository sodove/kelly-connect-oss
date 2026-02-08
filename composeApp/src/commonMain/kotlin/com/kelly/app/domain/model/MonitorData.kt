package com.kelly.app.domain.model

data class MonitorData(
    val values: Map<String, String> = emptyMap(),
    val errorStatus: Int = 0,
    val errorMessages: List<String> = emptyList(),
    val isActive: Boolean = false,
    val communicationError: String? = null
)
