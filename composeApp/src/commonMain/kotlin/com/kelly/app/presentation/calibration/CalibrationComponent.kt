package com.kelly.app.presentation.calibration

import com.arkivanov.decompose.ComponentContext
import com.kelly.app.domain.model.CalibrationData
import com.kelly.app.domain.repository.KellyRepository
import com.kelly.protocol.ParameterCodec
import com.kelly.protocol.ParameterDef
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class CalibrationComponent(
    componentContext: ComponentContext,
    private val repository: KellyRepository
) : ComponentContext by componentContext {

    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    private val _calibrationData = MutableStateFlow<CalibrationData?>(null)
    val calibrationData: StateFlow<CalibrationData?> = _calibrationData.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _statusMessage = MutableStateFlow<String?>(null)
    val statusMessage: StateFlow<String?> = _statusMessage.asStateFlow()

    private val _selectedTip = MutableStateFlow("")
    val selectedTip: StateFlow<String> = _selectedTip.asStateFlow()

    fun readCalibration() {
        scope.launch {
            _isLoading.value = true
            _statusMessage.value = "Reading calibration data..."
            val result = repository.readCalibration()
            result.onSuccess { data ->
                _calibrationData.value = data
                _statusMessage.value = "Read complete"
            }
            result.onFailure { e ->
                _statusMessage.value = "Read error: ${e.message}"
            }
            _isLoading.value = false
        }
    }

    fun writeCalibration() {
        val data = _calibrationData.value ?: return
        scope.launch {
            _isLoading.value = true
            _statusMessage.value = "Writing calibration data..."
            val result = repository.writeCalibration(data)
            result.onSuccess {
                _statusMessage.value = "Write complete"
            }
            result.onFailure { e ->
                _statusMessage.value = "Write error: ${e.message}"
            }
            _isLoading.value = false
        }
    }

    fun updateParameter(param: ParameterDef, value: String) {
        val data = _calibrationData.value ?: return
        // Skip write for empty/non-numeric input — user is still typing
        if (value.isBlank()) return
        val success = try {
            ParameterCodec.writeParam(
                data.dataValue, param.offset, param.size, param.position, param.type, value
            )
        } catch (_: NumberFormatException) {
            false
        }
        if (success) {
            // Trigger recomposition by creating a new copy
            _calibrationData.value = data.copy(dataValue = data.dataValue.copyOf())
        }
    }

    fun selectParameter(param: ParameterDef) {
        _selectedTip.value = "TIPS: ${param.tips}"
    }

    fun clearStatus() {
        _statusMessage.value = null
    }
}
