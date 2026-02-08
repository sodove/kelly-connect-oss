package com.kelly.app.transport

import android.content.Context
import android.hardware.usb.UsbDeviceConnection
import android.hardware.usb.UsbEndpoint
import android.hardware.usb.UsbManager
import com.kelly.app.data.transport.Transport
import com.kelly.app.data.transport.TransportState
import com.kelly.app.data.transport.TransportType
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class AndroidUsbTransport(private val context: Context) : Transport {
    companion object {
        const val KELLY_PRODUCT_ID = 24577  // FT232BM
        const val BAUD_RATE = 19200
    }

    private val _state = MutableStateFlow<TransportState>(TransportState.Disconnected)
    override val state: StateFlow<TransportState> = _state.asStateFlow()
    override val type = TransportType.USB

    private var connection: UsbDeviceConnection? = null
    private var endpointIn: UsbEndpoint? = null
    private var endpointOut: UsbEndpoint? = null

    override suspend fun connect(address: String) = withContext(Dispatchers.IO) {
        _state.value = TransportState.Connecting
        try {
            val usbManager = context.getSystemService(Context.USB_SERVICE) as UsbManager
            val device = usbManager.deviceList.values.firstOrNull { it.deviceName == address }
                ?: throw IllegalStateException("USB device not found: $address")

            if (!usbManager.hasPermission(device)) {
                throw SecurityException("No USB permission for device")
            }

            val conn = usbManager.openDevice(device)
            val intf = device.getInterface(0)
            conn.claimInterface(intf, true)

            // Configure FT232 - set baud rate, 8N1
            conn.controlTransfer(0x40, 0, 0, 0, null, 0, 100)  // Reset
            conn.controlTransfer(0x40, 0, 1, 0, null, 0, 100)  // Purge RX
            conn.controlTransfer(0x40, 0, 2, 0, null, 0, 100)  // Purge TX
            conn.controlTransfer(0x40, 3, 2, 0, null, 0, 100)  // Set latency timer
            // Baud rate divisor for 19200: 24MHz / 19200 = 1250
            conn.controlTransfer(0x40, 3, 0x04E2, 0, null, 0, 100)  // Set baud
            conn.controlTransfer(0x40, 4, 0x0008, 0, null, 0, 100)  // 8N1

            endpointIn = intf.getEndpoint(0)
            endpointOut = intf.getEndpoint(1)
            connection = conn

            _state.value = TransportState.Connected
        } catch (e: Exception) {
            _state.value = TransportState.Error(e.message ?: "USB connect failed")
            throw e
        }
    }

    override suspend fun disconnect() = withContext(Dispatchers.IO) {
        try {
            connection?.close()
        } catch (_: Exception) {}
        connection = null
        endpointIn = null
        endpointOut = null
        _state.value = TransportState.Disconnected
    }

    override suspend fun send(data: ByteArray) = withContext(Dispatchers.IO) {
        val conn = connection ?: throw IllegalStateException("Not connected")
        val ep = endpointOut ?: throw IllegalStateException("No output endpoint")
        val result = conn.bulkTransfer(ep, data, data.size, 100)
        if (result <= 0) throw IllegalStateException("USB send failed: $result")
    }

    override suspend fun receive(expectedLength: Int, timeoutMs: Long): ByteArray = withContext(Dispatchers.IO) {
        val conn = connection ?: throw IllegalStateException("Not connected")
        val ep = endpointIn ?: throw IllegalStateException("No input endpoint")
        val buffer = ByteArray(32)
        val result = ByteArray(expectedLength)
        var totalRead = 0

        val deadline = System.currentTimeMillis() + timeoutMs
        while (totalRead < expectedLength && System.currentTimeMillis() < deadline) {
            val bytesRead = conn.bulkTransfer(ep, buffer, buffer.size, 100)
            if (bytesRead > 2) {
                // FT232 prepends 2 status bytes
                val dataBytes = bytesRead - 2
                buffer.copyInto(result, totalRead, 2, 2 + dataBytes)
                totalRead += dataBytes
            }
        }

        if (totalRead < 3) throw IllegalStateException("USB timeout: received $totalRead bytes")
        result.copyOf(totalRead)
    }
}
