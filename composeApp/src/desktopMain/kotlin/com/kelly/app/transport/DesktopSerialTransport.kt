package com.kelly.app.transport

import com.fazecast.jSerialComm.SerialPort
import com.kelly.app.data.transport.Transport
import com.kelly.app.data.transport.TransportState
import com.kelly.app.data.transport.TransportType
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Desktop serial transport using jSerialComm.
 * Works for both USB FT232 and virtual COM ports (Bluetooth).
 */
class DesktopSerialTransport : Transport {
    private val _state = MutableStateFlow<TransportState>(TransportState.Disconnected)
    override val state: StateFlow<TransportState> = _state.asStateFlow()
    override val type = TransportType.USB

    private var serialPort: SerialPort? = null

    override suspend fun connect(address: String) = withContext(Dispatchers.IO) {
        _state.value = TransportState.Connecting
        try {
            val port = SerialPort.getCommPort(address)
            port.baudRate = 19200
            port.numDataBits = 8
            port.numStopBits = SerialPort.ONE_STOP_BIT
            port.parity = SerialPort.NO_PARITY
            port.setComPortTimeouts(SerialPort.TIMEOUT_READ_SEMI_BLOCKING, 300, 0)

            if (!port.openPort()) {
                throw IllegalStateException("Failed to open port: $address")
            }

            serialPort = port
            _state.value = TransportState.Connected
        } catch (e: Exception) {
            _state.value = TransportState.Error(e.message ?: "Serial connect failed")
            throw e
        }
    }

    override suspend fun disconnect() = withContext(Dispatchers.IO) {
        try {
            serialPort?.closePort()
        } catch (_: Exception) {}
        serialPort = null
        _state.value = TransportState.Disconnected
    }

    override suspend fun send(data: ByteArray) = withContext(Dispatchers.IO) {
        val port = serialPort ?: throw IllegalStateException("Not connected")
        val written = port.writeBytes(data, data.size)
        if (written <= 0) throw IllegalStateException("Serial write failed: $written")
    }

    override suspend fun receive(expectedLength: Int, timeoutMs: Long): ByteArray = withContext(Dispatchers.IO) {
        val port = serialPort ?: throw IllegalStateException("Not connected")
        val buffer = ByteArray(32)
        val result = ByteArray(expectedLength)
        var totalRead = 0

        val deadline = System.currentTimeMillis() + timeoutMs
        while (totalRead < expectedLength && System.currentTimeMillis() < deadline) {
            val bytesRead = port.readBytes(buffer, minOf(buffer.size, expectedLength - totalRead))
            if (bytesRead > 0) {
                buffer.copyInto(result, totalRead, 0, bytesRead)
                totalRead += bytesRead
            } else {
                delay(1)
            }
        }

        if (totalRead < 3) throw IllegalStateException("Serial timeout: received $totalRead bytes")
        result.copyOf(totalRead)
    }
}
