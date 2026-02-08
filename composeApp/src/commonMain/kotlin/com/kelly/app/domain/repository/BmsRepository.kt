package com.kelly.app.domain.repository

import com.kelly.app.domain.model.BmsData
import com.kelly.app.domain.model.BmsType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

data class BmsDeviceInfo(
    val name: String,
    val address: String
)

interface BmsRepository {
    val bmsData: StateFlow<BmsData>
    val isConnected: StateFlow<Boolean>
    val statusMessage: StateFlow<String>

    /** Scan for BMS BLE devices. Emits discovered devices as they appear. */
    fun scanDevices(type: BmsType): Flow<BmsDeviceInfo>

    /** Connect to a BMS device by address. Starts polling/streaming automatically. */
    suspend fun connect(address: String, type: BmsType): Result<Unit>

    /** Disconnect from the current BMS device. */
    suspend fun disconnect()
}
