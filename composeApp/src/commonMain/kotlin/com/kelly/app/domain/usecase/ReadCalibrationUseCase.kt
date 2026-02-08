package com.kelly.app.domain.usecase

import com.kelly.app.domain.model.CalibrationData
import com.kelly.app.domain.repository.KellyRepository

class ReadCalibrationUseCase(private val repository: KellyRepository) {
    suspend operator fun invoke(): Result<CalibrationData> {
        return repository.readCalibration()
    }
}
