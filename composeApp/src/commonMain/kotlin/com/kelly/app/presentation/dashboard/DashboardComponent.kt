package com.kelly.app.presentation.dashboard

import com.arkivanov.decompose.ComponentContext
import com.kelly.app.domain.SettingsStore
import com.kelly.app.domain.model.BmsData
import com.kelly.app.domain.model.MonitorData
import com.kelly.app.domain.repository.BmsRepository
import com.kelly.app.domain.repository.KellyRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class SpeedUnit(val label: String, val suffix: String) {
    RPM("RPM", "RPM"),
    KMH("km/h", "km/h"),
    MPH("mph", "mph")
}

data class DashboardSettings(
    val speedUnit: SpeedUnit = SpeedUnit.RPM,
    val wheelDiameterMm: Int = 660,
    val gearRatio: Float = 1.0f,
    val maxSpeedDisplay: Int = 100,
    val maxCurrentA: Int = 800
)

class DashboardComponent(
    componentContext: ComponentContext,
    private val repository: KellyRepository,
    private val settingsStore: SettingsStore,
    private val bmsRepository: BmsRepository
) : ComponentContext by componentContext {

    val monitorData: StateFlow<MonitorData> = repository.monitorData
    val bmsData: StateFlow<BmsData> = bmsRepository.bmsData

    private val _settings = MutableStateFlow(loadSettings())
    val settings: StateFlow<DashboardSettings> = _settings.asStateFlow()

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

    fun reloadSettings() {
        _settings.value = loadSettings()
    }
}
