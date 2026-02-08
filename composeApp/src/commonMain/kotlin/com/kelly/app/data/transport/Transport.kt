package com.kelly.app.data.transport

import kotlinx.coroutines.flow.StateFlow

sealed class TransportState {
    data object Disconnected : TransportState()
    data object Connecting : TransportState()
    data object Connected : TransportState()
    data class Error(val message: String) : TransportState()
}

/**
 * Abstract transport interface for communicating with Kelly controllers.
 * Implementations exist for USB Serial, Bluetooth Classic (RFCOMM), and BLE.
 */
interface Transport {
    val state: StateFlow<TransportState>
    val type: TransportType

    /**
     * Connect to a device.
     * @param address device address (MAC for BT, port name for USB/Serial)
     */
    suspend fun connect(address: String)

    /**
     * Disconnect from the device.
     */
    suspend fun disconnect()

    /**
     * Send raw bytes to the device.
     */
    suspend fun send(data: ByteArray)

    /**
     * Receive raw bytes from the device.
     * @param expectedLength expected number of bytes
     * @param timeoutMs timeout in milliseconds
     * @return received bytes
     */
    suspend fun receive(expectedLength: Int, timeoutMs: Long = 300): ByteArray

    /**
     * Drain any stale/buffered bytes from the receive path.
     * Called before sending a new command to prevent protocol desync.
     * Default no-op; transports with buffered streams should override.
     */
    suspend fun drain() {}
}
