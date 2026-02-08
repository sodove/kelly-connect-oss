package com.kelly.app.domain.usecase

import com.kelly.app.data.transport.DeviceInfo
import com.kelly.app.domain.repository.KellyRepository

class ConnectUseCase(private val repository: KellyRepository) {
    suspend operator fun invoke(device: DeviceInfo): Result<Unit> {
        return repository.connect(device)
    }
}
