package com.kelly.app.transport

import com.kelly.app.data.transport.Transport
import com.kelly.app.data.transport.TransportState
import com.kelly.app.data.transport.TransportType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Desktop BLE transport using Kable JVM.
 * Supports macOS and Linux. Windows support via Bluelib.
 */
class DesktopBleTransport : Transport {
    private val _state = MutableStateFlow<TransportState>(TransportState.Disconnected)
    override val state: StateFlow<TransportState> = _state.asStateFlow()
    override val type = TransportType.BLE

    override suspend fun connect(address: String) {
        _state.value = TransportState.Error("Desktop BLE not yet implemented")
    }

    override suspend fun disconnect() {
        _state.value = TransportState.Disconnected
    }

    override suspend fun send(data: ByteArray) {
        throw UnsupportedOperationException("Desktop BLE not yet implemented")
    }

    override suspend fun receive(expectedLength: Int, timeoutMs: Long): ByteArray {
        throw UnsupportedOperationException("Desktop BLE not yet implemented")
    }
}
