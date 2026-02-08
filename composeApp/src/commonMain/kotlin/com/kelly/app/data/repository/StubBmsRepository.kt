package com.kelly.app.data.repository

import com.kelly.app.domain.model.BmsData
import com.kelly.app.domain.model.BmsType
import com.kelly.app.domain.repository.BmsDeviceInfo
import com.kelly.app.domain.repository.BmsRepository
import kotlinx.coroutines.flow.*

class StubBmsRepository : BmsRepository {

    private val _bmsData = MutableStateFlow(BmsData())
    override val bmsData: StateFlow<BmsData> = _bmsData.asStateFlow()

    private val _isConnected = MutableStateFlow(false)
    override val isConnected: StateFlow<Boolean> = _isConnected.asStateFlow()

    override val statusMessage: StateFlow<String> = MutableStateFlow("BMS not available")

    override fun scanDevices(type: BmsType): Flow<BmsDeviceInfo> = emptyFlow()

    override suspend fun connect(address: String, type: BmsType): Result<Unit> {
        return Result.failure(NotImplementedError("BMS support coming soon"))
    }

    override suspend fun disconnect() {
        // No-op
    }
}
