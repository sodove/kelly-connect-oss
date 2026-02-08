package com.kelly.app.presentation.settings

import com.arkivanov.decompose.ComponentContext
import com.kelly.app.domain.SettingsStore
import com.kelly.app.domain.model.BmsData
import com.kelly.app.domain.model.BmsType
import com.kelly.app.domain.repository.BmsDeviceInfo
import com.kelly.app.domain.repository.BmsRepository
import com.kelly.app.presentation.dashboard.DashboardSettings
import com.kelly.app.presentation.dashboard.SpeedUnit
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch

class SettingsComponent(
    componentContext: ComponentContext,
    private val settingsStore: SettingsStore,
    private val bmsRepository: BmsRepository
) : ComponentContext by componentContext {

    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    private val _settings = MutableStateFlow(loadSettings())
    val settings: StateFlow<DashboardSettings> = _settings.asStateFlow()

    private val _bmsType = MutableStateFlow(loadBmsType())
    val bmsType: StateFlow<BmsType> = _bmsType.asStateFlow()

    val bmsData: StateFlow<BmsData> = bmsRepository.bmsData
    val bmsConnected: StateFlow<Boolean> = bmsRepository.isConnected
    val bmsStatus: StateFlow<String> = bmsRepository.statusMessage

    private val _discoveredDevices = MutableStateFlow<List<BmsDeviceInfo>>(emptyList())
    val discoveredDevices: StateFlow<List<BmsDeviceInfo>> = _discoveredDevices.asStateFlow()

    private val _isScanning = MutableStateFlow(false)
    val isScanning: StateFlow<Boolean> = _isScanning.asStateFlow()

    private var scanJob: Job? = null

    init {
        tryAutoReconnectBms()
    }

    private fun tryAutoReconnectBms() {
        val savedAddress = settingsStore.getString("lastBmsAddress", "")
        val savedType = settingsStore.getString("lastBmsType", "")

        if (savedAddress.isNotEmpty() && savedType.isNotEmpty()) {
            val type = try { BmsType.valueOf(savedType) } catch (_: Exception) { null }
            if (type != null && type != BmsType.NONE) {
                _bmsType.value = type
                scope.launch {
                    delay(500) // Let BLE stack initialize
                    bmsRepository.connect(savedAddress, type)
                }
            }
        }
    }

    private fun loadSettings(): DashboardSettings {
        val unitName = settingsStore.getString("speedUnit", SpeedUnit.RPM.name)
        val unit = try { SpeedUnit.valueOf(unitName) } catch (_: Exception) { SpeedUnit.RPM }
        return DashboardSettings(
            speedUnit = unit,
            wheelDiameterMm = settingsStore.getInt("wheelDiameterMm", 660),
            gearRatio = settingsStore.getFloat("gearRatio", 1.0f),
            maxSpeedDisplay = settingsStore.getInt("maxSpeedDisplay", 100),
            maxCurrentA = settingsStore.getInt("maxCurrentA", 800)
        )
    }

    private fun loadBmsType(): BmsType {
        val name = settingsStore.getString("bmsType", BmsType.NONE.name)
        return try { BmsType.valueOf(name) } catch (_: Exception) { BmsType.NONE }
    }

    fun updateSettings(newSettings: DashboardSettings) {
        _settings.value = newSettings
        settingsStore.putString("speedUnit", newSettings.speedUnit.name)
        settingsStore.putInt("wheelDiameterMm", newSettings.wheelDiameterMm)
        settingsStore.putFloat("gearRatio", newSettings.gearRatio)
        settingsStore.putInt("maxSpeedDisplay", newSettings.maxSpeedDisplay)
        settingsStore.putInt("maxCurrentA", newSettings.maxCurrentA)
    }

    fun setBmsType(type: BmsType) {
        _bmsType.value = type
        settingsStore.putString("bmsType", type.name)
        // Stop scan when type changes
        stopScan()
        _discoveredDevices.value = emptyList()
    }

    fun startScan() {
        val type = _bmsType.value
        if (type == BmsType.NONE) return

        stopScan()
        _discoveredDevices.value = emptyList()
        _isScanning.value = true

        scanJob = scope.launch {
            val seen = mutableSetOf<String>()
            bmsRepository.scanDevices(type)
                .catch {
                    _isScanning.value = false
                }
                .collect { device ->
                    if (seen.add(device.address)) {
                        _discoveredDevices.value = _discoveredDevices.value + device
                    }
                }
        }

        // Auto-stop scan after 10 seconds
        scope.launch {
            delay(10_000)
            stopScan()
        }
    }

    fun stopScan() {
        scanJob?.cancel()
        scanJob = null
        _isScanning.value = false
    }

    fun connectBms(address: String) {
        stopScan()
        scope.launch {
            val result = bmsRepository.connect(address, _bmsType.value)
            result.onSuccess {
                // Save for auto-reconnect next time
                settingsStore.putString("lastBmsAddress", address)
                settingsStore.putString("lastBmsType", _bmsType.value.name)
            }
        }
    }

    fun disconnectBms() {
        scope.launch {
            bmsRepository.disconnect()
            settingsStore.putString("lastBmsAddress", "")
            settingsStore.putString("lastBmsType", "")
        }
    }
}
