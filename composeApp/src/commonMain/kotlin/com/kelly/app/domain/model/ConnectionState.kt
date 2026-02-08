package com.kelly.app.domain.model

import com.kelly.protocol.ControllerModel

sealed class ConnectionState {
    data object Disconnected : ConnectionState()
    data object Connecting : ConnectionState()
    data class Connected(
        val moduleName: String,
        val softwareVersion: Int,
        val model: ControllerModel
    ) : ConnectionState()
    data class Error(val message: String) : ConnectionState()
}
