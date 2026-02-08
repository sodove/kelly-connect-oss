package com.kelly.app.domain.usecase

import com.kelly.app.domain.model.MonitorData
import com.kelly.app.domain.repository.KellyRepository
import kotlinx.coroutines.flow.Flow

class MonitorUseCase(private val repository: KellyRepository) {
    operator fun invoke(): Flow<MonitorData> {
        return repository.startMonitoring()
    }

    fun stop() {
        repository.stopMonitoring()
    }
}
