package com.kelly.app.presentation.bms

import com.arkivanov.decompose.ComponentContext
import com.kelly.app.domain.model.BmsData
import com.kelly.app.domain.repository.BmsRepository
import kotlinx.coroutines.flow.StateFlow

class BmsComponent(
    componentContext: ComponentContext,
    private val bmsRepository: BmsRepository
) : ComponentContext by componentContext {

    val bmsData: StateFlow<BmsData> = bmsRepository.bmsData
    val isConnected: StateFlow<Boolean> = bmsRepository.isConnected
}
