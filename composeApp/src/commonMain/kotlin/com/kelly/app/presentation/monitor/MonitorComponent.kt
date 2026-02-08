package com.kelly.app.presentation.monitor

import com.arkivanov.decompose.ComponentContext
import com.kelly.app.domain.model.MonitorData
import com.kelly.app.domain.repository.KellyRepository
import kotlinx.coroutines.flow.StateFlow

class MonitorComponent(
    componentContext: ComponentContext,
    private val repository: KellyRepository
) : ComponentContext by componentContext {

    val monitorData: StateFlow<MonitorData> = repository.monitorData
}
