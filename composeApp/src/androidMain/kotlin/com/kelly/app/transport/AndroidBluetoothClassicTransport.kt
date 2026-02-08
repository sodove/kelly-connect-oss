package com.kelly.app.transport

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothSocket
import com.kelly.app.data.transport.Transport
import com.kelly.app.data.transport.TransportState
import com.kelly.app.data.transport.TransportType
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.InputStream
import java.io.OutputStream
import java.util.UUID

class AndroidBluetoothClassicTransport : Transport {
    companion object {
        private val SPP_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
    }

    private val _state = MutableStateFlow<TransportState>(TransportState.Disconnected)
    override val state: StateFlow<TransportState> = _state.asStateFlow()
    override val type = TransportType.BLUETOOTH_CLASSIC

    private var socket: BluetoothSocket? = null
    private var inputStream: InputStream? = null
    private var outputStream: OutputStream? = null

    override suspend fun connect(address: String) = withContext(Dispatchers.IO) {
        _state.value = TransportState.Connecting
        try {
            val adapter = BluetoothAdapter.getDefaultAdapter()
                ?: throw IllegalStateException("Bluetooth not available")
            adapter.cancelDiscovery()
            val device = adapter.getRemoteDevice(address)
            val sock = device.createRfcommSocketToServiceRecord(SPP_UUID)
            sock.connect()
            socket = sock
            inputStream = sock.inputStream
            outputStream = sock.outputStream
            _state.value = TransportState.Connected
        } catch (e: Exception) {
            _state.value = TransportState.Error(e.message ?: "BT connect failed")
            throw e
        }
    }

    override suspend fun disconnect() = withContext(Dispatchers.IO) {
        try {
            socket?.close()
        } catch (_: Exception) {}
        socket = null
        inputStream = null
        outputStream = null
        _state.value = TransportState.Disconnected
    }

    override suspend fun send(data: ByteArray) = withContext(Dispatchers.IO) {
        outputStream?.write(data) ?: throw IllegalStateException("Not connected")
    }

    override suspend fun receive(expectedLength: Int, timeoutMs: Long): ByteArray = withContext(Dispatchers.IO) {
        val stream = inputStream ?: throw IllegalStateException("Not connected")
        val buffer = ByteArray(32)
        val result = ByteArray(expectedLength)
        var totalRead = 0

        val deadline = System.currentTimeMillis() + timeoutMs
        while (totalRead < expectedLength && System.currentTimeMillis() < deadline) {
            if (stream.available() > 0) {
                val bytesRead = stream.read(buffer, 0, minOf(buffer.size, expectedLength - totalRead))
                if (bytesRead > 0) {
                    buffer.copyInto(result, totalRead, 0, bytesRead)
                    totalRead += bytesRead
                }
            } else {
                delay(1)
            }
        }

        if (totalRead == 0) throw IllegalStateException("Timeout: device not responding")
        result.copyOf(totalRead)
    }

    override suspend fun drain() = withContext(Dispatchers.IO) {
        val stream = inputStream ?: return@withContext
        val avail = stream.available()
        if (avail > 0) {
            stream.skip(avail.toLong())
        }
    }
}
