package com.kelly.app.presentation

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.router.stack.*
import com.arkivanov.decompose.value.Value
import com.kelly.app.data.transport.DeviceInfo
import com.kelly.app.domain.model.ConnectionState
import com.kelly.app.domain.SettingsStore
import com.kelly.app.domain.repository.KellyRepository
import com.kelly.app.domain.repository.BmsRepository
import com.kelly.app.presentation.bms.BmsComponent
import com.kelly.app.presentation.calibration.CalibrationComponent
import com.kelly.app.presentation.connection.ConnectionComponent
import com.kelly.app.presentation.dashboard.DashboardComponent
import com.kelly.app.presentation.monitor.MonitorComponent
import com.kelly.app.presentation.settings.SettingsComponent
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.StateFlow
import kotlinx.serialization.Serializable

@OptIn(com.arkivanov.decompose.DelicateDecomposeApi::class)
class RootComponent(
    componentContext: ComponentContext,
    private val repository: KellyRepository,
    private val settingsStore: SettingsStore,
    private val bmsRepository: BmsRepository
) : ComponentContext by componentContext {

    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private val navigation = StackNavigation<Config>()

    val stack: Value<ChildStack<Config, Child>> = childStack(
        source = navigation,
        serializer = Config.serializer(),
        initialConfiguration = Config.Connection,
        handleBackButton = true,
        childFactory = ::createChild
    )

    private fun createChild(config: Config, componentContext: ComponentContext): Child =
        when (config) {
            is Config.Connection -> Child.Connection(
                ConnectionComponent(
                    componentContext = componentContext,
                    repository = repository,
                    settingsStore = settingsStore,
                    onConnected = { navigation.push(Config.Main) }
                )
            )
            is Config.Main -> Child.Main(
                calibrationComponent = CalibrationComponent(
                    componentContext = componentContext,
                    repository = repository
                ),
                monitorComponent = MonitorComponent(
                    componentContext = componentContext,
                    repository = repository
                ),
                dashboardComponent = DashboardComponent(
                    componentContext = componentContext,
                    repository = repository,
                    settingsStore = settingsStore,
                    bmsRepository = bmsRepository
                ),
                bmsComponent = BmsComponent(
                    componentContext = componentContext,
                    bmsRepository = bmsRepository
                ),
                settingsComponent = SettingsComponent(
                    componentContext = componentContext,
                    settingsStore = settingsStore,
                    bmsRepository = bmsRepository
                ),
                connectionState = repository.connectionState,
                onDisconnect = {
                    scope.launch {
                        repository.disconnect()
                        bmsRepository.disconnect()
                    }
                    // Clear saved devices so we don't auto-reconnect next time
                    settingsStore.putString("lastKellyAddress", "")
                    settingsStore.putString("lastBmsAddress", "")
                    navigation.popTo(0)
                }
            )
        }

    sealed class Child {
        data class Connection(val component: ConnectionComponent) : Child()
        data class Main(
            val calibrationComponent: CalibrationComponent,
            val monitorComponent: MonitorComponent,
            val dashboardComponent: DashboardComponent,
            val bmsComponent: BmsComponent,
            val settingsComponent: SettingsComponent,
            val connectionState: StateFlow<ConnectionState>,
            val onDisconnect: () -> Unit
        ) : Child()
    }

    @Serializable
    sealed interface Config {
        @Serializable
        data object Connection : Config

        @Serializable
        data object Main : Config
    }
}
