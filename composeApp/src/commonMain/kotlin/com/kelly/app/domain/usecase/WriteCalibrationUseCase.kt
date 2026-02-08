package com.kelly.app.domain.usecase

import com.kelly.app.domain.model.CalibrationData
import com.kelly.app.domain.repository.KellyRepository

class WriteCalibrationUseCase(private val repository: KellyRepository) {
    suspend operator fun invoke(data: CalibrationData): Result<Unit> {
        return repository.writeCalibration(data)
    }
}
