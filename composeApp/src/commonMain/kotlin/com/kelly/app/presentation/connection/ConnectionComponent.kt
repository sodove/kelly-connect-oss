package com.kelly.app.presentation.connection

import com.arkivanov.decompose.ComponentContext
import com.kelly.app.data.transport.DeviceInfo
import com.kelly.app.data.transport.TransportType
import com.kelly.app.domain.SettingsStore
import com.kelly.app.domain.model.ConnectionState
import com.kelly.app.domain.repository.KellyRepository
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class ConnectionComponent(
    componentContext: ComponentContext,
    private val repository: KellyRepository,
    private val settingsStore: SettingsStore,
    private val onConnected: () -> Unit
) : ComponentContext by componentContext {

    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    private val _selectedTransport = MutableStateFlow(TransportType.BLUETOOTH_CLASSIC)
    val selectedTransport: StateFlow<TransportType> = _selectedTransport.asStateFlow()

    private val _devices = MutableStateFlow<List<DeviceInfo>>(emptyList())
    val devices: StateFlow<List<DeviceInfo>> = _devices.asStateFlow()

    private val _isScanning = MutableStateFlow(false)
    val isScanning: StateFlow<Boolean> = _isScanning.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    val connectionState: StateFlow<ConnectionState> = repository.connectionState

    init {
        tryAutoReconnect()
    }

    private fun tryAutoReconnect() {
        val savedAddress = settingsStore.getString("lastKellyAddress", "")
        val savedName = settingsStore.getString("lastKellyName", "")
        val savedTransport = settingsStore.getString("lastKellyTransport", "")

        if (savedAddress.isNotEmpty() && savedTransport.isNotEmpty()) {
            val type = try { TransportType.valueOf(savedTransport) } catch (_: Exception) { null }
            if (type != null) {
                _selectedTransport.value = type
                val device = DeviceInfo(name = savedName, address = savedAddress, type = type)
                scope.launch {
                    val result = repository.connect(device)
                    result.onSuccess {
                        onConnected()
                    }
                    result.onFailure {
                        _errorMessage.value = "Auto-connect to $savedName failed: ${it.message}"
                    }
                }
            }
        }
    }

    fun selectTransport(type: TransportType) {
        _selectedTransport.value = type
        _devices.value = emptyList()
    }

    fun scanDevices() {
        scope.launch {
            _isScanning.value = true
            _errorMessage.value = null
            try {
                val found = repository.scanDevices(_selectedTransport.value)
                _devices.value = found
            } catch (e: Exception) {
                _errorMessage.value = e.message ?: "Scan failed"
            } finally {
                _isScanning.value = false
            }
        }
    }

    fun connect(device: DeviceInfo) {
        scope.launch {
            _errorMessage.value = null
            val result = repository.connect(device)
            result.onSuccess {
                // Save for auto-reconnect next time
                settingsStore.putString("lastKellyAddress", device.address)
                settingsStore.putString("lastKellyName", device.name)
                settingsStore.putString("lastKellyTransport", device.type.name)
                onConnected()
            }
            result.onFailure { e ->
                _errorMessage.value = e.message ?: "Connection failed"
            }
        }
    }

    fun clearError() {
        _errorMessage.value = null
    }
}
