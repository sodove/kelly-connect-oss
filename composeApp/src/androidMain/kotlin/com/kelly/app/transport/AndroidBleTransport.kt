package com.kelly.app.transport

import android.content.Context
import com.juul.kable.Peripheral
import com.juul.kable.WriteType
import com.juul.kable.characteristicOf
import com.juul.kable.peripheral
import com.kelly.app.data.transport.Transport
import com.kelly.app.data.transport.TransportState
import com.kelly.app.data.transport.TransportType
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.Channel.Factory.UNLIMITED
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * BLE transport using Kable library.
 * Supports Nordic UART Service (NUS), HM-10, and CC254x BLE serial modules.
 */
class AndroidBleTransport(private val context: Context) : Transport {
    private val _state = MutableStateFlow<TransportState>(TransportState.Disconnected)
    override val state: StateFlow<TransportState> = _state.asStateFlow()
    override val type = TransportType.BLE

    // Known BLE Serial service/characteristic UUIDs
    companion object {
        // Nordic UART Service
        const val NUS_SERVICE = "6E400001-B5A3-F393-E0A9-E50E24DCCA9E"
        const val NUS_TX_CHAR = "6E400002-B5A3-F393-E0A9-E50E24DCCA9E" // write (phone→device)
        const val NUS_RX_CHAR = "6E400003-B5A3-F393-E0A9-E50E24DCCA9E" // notify (device→phone)
        // HM-10/HM-19
        const val HM10_SERVICE = "0000FFE0-0000-1000-8000-00805F9B34FB"
        const val HM10_CHAR    = "0000FFE1-0000-1000-8000-00805F9B34FB"
        // TI CC254x
        const val CC254X_SERVICE = "0000FFF0-0000-1000-8000-00805F9B34FB"
        const val CC254X_TX_CHAR = "0000FFF1-0000-1000-8000-00805F9B34FB"
        const val CC254X_RX_CHAR = "0000FFF2-0000-1000-8000-00805F9B34FB"
    }

    private data class BleProfile(
        val serviceUuid: String,
        val txCharUuid: String,
        val rxCharUuid: String
    )

    private val knownProfiles = listOf(
        BleProfile(NUS_SERVICE, NUS_TX_CHAR, NUS_RX_CHAR),
        BleProfile(HM10_SERVICE, HM10_CHAR, HM10_CHAR),
        BleProfile(CC254X_SERVICE, CC254X_TX_CHAR, CC254X_RX_CHAR)
    )

    private var scope: CoroutineScope? = null
    private var peripheral: Peripheral? = null
    private var activeProfile: BleProfile? = null
    private val rxChannel = Channel<ByteArray>(UNLIMITED)
    private var observeJob: Job? = null

    override suspend fun connect(address: String) {
        _state.value = TransportState.Connecting
        try {
            val bleScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
            scope = bleScope

            val p = bleScope.peripheral(address)
            peripheral = p

            p.connect()

            // Auto-detect which BLE serial profile the device supports
            val services = p.services ?: throw IllegalStateException("No services discovered")
            val serviceUuids = services.map { it.serviceUuid.toString().uppercase() }.toSet()

            val profile = knownProfiles.firstOrNull { bp ->
                serviceUuids.any { it.equals(bp.serviceUuid, ignoreCase = true) }
            } ?: throw IllegalStateException(
                "No compatible BLE serial service found. " +
                "Supported: NUS, HM-10, CC254x. " +
                "Found services: ${serviceUuids.joinToString()}"
            )
            activeProfile = profile

            // Subscribe to notifications on the RX characteristic
            val rxCharacteristic = characteristicOf(
                service = profile.serviceUuid,
                characteristic = profile.rxCharUuid
            )

            observeJob = bleScope.launch {
                p.observe(rxCharacteristic).collect { data ->
                    rxChannel.send(data)
                }
            }

            _state.value = TransportState.Connected
        } catch (e: Exception) {
            _state.value = TransportState.Error(e.message ?: "BLE connect failed")
            throw e
        }
    }

    override suspend fun disconnect() {
        try {
            observeJob?.cancel()
            peripheral?.disconnect()
        } catch (_: Exception) {}
        scope?.cancel()
        scope = null
        peripheral = null
        activeProfile = null
        observeJob = null
        // Drain remaining data
        while (rxChannel.tryReceive().isSuccess) { /* discard */ }
        _state.value = TransportState.Disconnected
    }

    override suspend fun send(data: ByteArray) {
        val p = peripheral ?: throw IllegalStateException("Not connected")
        val profile = activeProfile ?: throw IllegalStateException("No BLE profile")

        val txCharacteristic = characteristicOf(
            service = profile.serviceUuid,
            characteristic = profile.txCharUuid
        )

        // BLE MTU is typically 20 bytes for write without response.
        // Kelly protocol packets are max 19 bytes, so single write is fine.
        p.write(txCharacteristic, data, WriteType.WithoutResponse)
    }

    override suspend fun receive(expectedLength: Int, timeoutMs: Long): ByteArray {
        val result = mutableListOf<Byte>()
        val deadline = System.currentTimeMillis() + timeoutMs

        while (result.size < expectedLength) {
            val remaining = deadline - System.currentTimeMillis()
            if (remaining <= 0) break

            val chunk = withTimeoutOrNull(remaining) {
                rxChannel.receive()
            } ?: break

            result.addAll(chunk.toList())
        }

        if (result.isEmpty()) throw IllegalStateException("Timeout: device not responding")
        return result.toByteArray()
    }

    override suspend fun drain() {
        while (rxChannel.tryReceive().isSuccess) { /* discard stale data */ }
    }
}
