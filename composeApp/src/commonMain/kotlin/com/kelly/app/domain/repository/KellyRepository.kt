package com.kelly.app.domain.repository

import com.kelly.app.data.transport.DeviceInfo
import com.kelly.app.data.transport.TransportType
import com.kelly.app.domain.model.CalibrationData
import com.kelly.app.domain.model.ConnectionState
import com.kelly.app.domain.model.MonitorData
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

interface KellyRepository {
    val connectionState: StateFlow<ConnectionState>
    val monitorData: StateFlow<MonitorData>

    suspend fun scanDevices(type: TransportType): List<DeviceInfo>
    suspend fun connect(device: DeviceInfo): Result<Unit>
    suspend fun disconnect()
    suspend fun readCalibration(): Result<CalibrationData>
    suspend fun writeCalibration(data: CalibrationData): Result<Unit>
    fun startMonitoring(): Flow<MonitorData>
    fun stopMonitoring()
    suspend fun readPhaseCurrentZero(): Result<IntArray>
}
