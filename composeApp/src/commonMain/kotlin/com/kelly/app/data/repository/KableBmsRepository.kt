package com.kelly.app.data.repository

import com.juul.kable.Peripheral
import com.juul.kable.Scanner
import com.juul.kable.WriteType
import com.juul.kable.characteristicOf
import com.kelly.app.bms.*
import com.kelly.app.domain.model.BmsData
import com.kelly.app.domain.model.BmsType
import com.kelly.app.domain.repository.BmsDeviceInfo
import com.kelly.app.domain.repository.BmsRepository
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

/**
 * BLE-based BMS repository using Kable.
 *
 * Handles scanning, connecting, and polling/streaming data from various BMS types
 * (JK, JBD, ANT, Daly) using their respective protocol implementations.
 */
@OptIn(ExperimentalUuidApi::class)
class KableBmsRepository : BmsRepository {

    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    private val _bmsData = MutableStateFlow(BmsData())
    override val bmsData: StateFlow<BmsData> = _bmsData.asStateFlow()

    private val _isConnected = MutableStateFlow(false)
    override val isConnected: StateFlow<Boolean> = _isConnected.asStateFlow()

    private val _statusMessage = MutableStateFlow("Disconnected")
    override val statusMessage: StateFlow<String> = _statusMessage.asStateFlow()

    private var pollingJob: Job? = null
    private var observeJob: Job? = null
    private var peripheral: com.juul.kable.Peripheral? = null
    private var protocol: BmsProtocol? = null

    // Store advertisements from scanning so we can create peripherals later
    private val advertisementCache = mutableMapOf<String, com.juul.kable.Advertisement>()

    override fun scanDevices(type: BmsType): Flow<BmsDeviceInfo> = flow {
        if (type == BmsType.NONE) return@flow

        val proto = createProtocol(type)
        val targetServiceUuid = proto.uuids.serviceUuid.lowercase()

        val scanner = Scanner()

        scanner.advertisements.collect { advertisement ->
            // Filter by service UUID if available in advertisement
            val adServices = advertisement.uuids.map { it.toString().lowercase() }
            val matches = adServices.any { it.contains(targetServiceUuid.substring(4, 8)) }

            // Also accept by name pattern if no service UUID match
            val nameMatch = advertisement.name?.let { name ->
                when (type) {
                    BmsType.JK_BMS -> name.startsWith("JK_", ignoreCase = true) ||
                            name.startsWith("JK-", ignoreCase = true)
                    BmsType.JBD_BMS -> name.startsWith("xiaoxiang", ignoreCase = true) ||
                            name.startsWith("JBD", ignoreCase = true) ||
                            name.startsWith("SP", ignoreCase = true)
                    BmsType.ANT_BMS -> name.startsWith("ANT", ignoreCase = true)
                    BmsType.DALY_BMS -> name.startsWith("DL-", ignoreCase = true) ||
                            name.startsWith("Daly", ignoreCase = true)
                    BmsType.NONE -> false
                }
            } ?: false

            if (matches || nameMatch) {
                val id = advertisement.identifier.toString()
                advertisementCache[id] = advertisement
                emit(
                    BmsDeviceInfo(
                        name = advertisement.name ?: id,
                        address = id
                    )
                )
            }
        }
    }.flowOn(Dispatchers.Default)

    override suspend fun connect(address: String, type: BmsType): Result<Unit> {
        if (type == BmsType.NONE) {
            return Result.failure(IllegalArgumentException("No BMS type selected"))
        }

        return try {
            disconnect()

            _statusMessage.value = "Connecting..."
            val proto = createProtocol(type)
            protocol = proto

            // Try cached advertisement first, otherwise do a quick scan
            var advertisement = advertisementCache[address]
            if (advertisement == null) {
                _statusMessage.value = "Scanning for device..."
                advertisement = withTimeoutOrNull(5_000L) {
                    Scanner().advertisements.first { ad ->
                        ad.identifier.toString() == address
                    }
                }
                if (advertisement != null) {
                    advertisementCache[address] = advertisement
                }
            }
            if (advertisement == null) {
                _statusMessage.value = "Device not found"
                return Result.failure(IllegalStateException("Device not found. Please scan again."))
            }

            val p = Peripheral(advertisement)
            peripheral = p

            p.connect()
            _statusMessage.value = "Connected, initializing..."

            // Set up notification observation
            val notifyChar = characteristicOf(
                service = Uuid.parse(proto.uuids.serviceUuid),
                characteristic = Uuid.parse(proto.uuids.notifyCharUuid)
            )

            observeJob = scope.launch {
                p.observe(notifyChar).collect { data ->
                    proto.onNotification(data)
                    proto.latestData()?.let { bms ->
                        _bmsData.value = bms
                    }
                }
            }

            // Small delay for notification subscription to settle
            delay(200)

            // Send handshake commands
            val writeChar = characteristicOf(
                service = Uuid.parse(proto.uuids.serviceUuid),
                characteristic = Uuid.parse(proto.uuids.writeCharUuid)
            )

            for (cmd in proto.handshakeCommands()) {
                p.write(writeChar, cmd, WriteType.WithoutResponse)
                delay(100)
            }

            // Start polling if protocol requires it
            val pollCmds = proto.pollCommands()
            if (pollCmds.isNotEmpty()) {
                pollingJob = scope.launch {
                    while (isActive) {
                        try {
                            for (cmd in pollCmds) {
                                p.write(writeChar, cmd, WriteType.WithoutResponse)
                                delay(50) // Gap between commands
                            }
                        } catch (e: CancellationException) {
                            throw e
                        } catch (_: Exception) {
                            // Write failed — will retry next cycle
                        }
                        delay(proto.pollIntervalMs)
                    }
                }
            }

            _isConnected.value = true
            _statusMessage.value = "${type.label} connected"
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            _statusMessage.value = "Connection failed: ${e.message}"
            _isConnected.value = false
            Result.failure(e)
        }
    }

    override suspend fun disconnect() {
        pollingJob?.cancel()
        pollingJob = null
        observeJob?.cancel()
        observeJob = null

        try {
            peripheral?.disconnect()
        } catch (_: Exception) { }

        peripheral = null
        protocol?.reset()
        protocol = null

        _isConnected.value = false
        _bmsData.value = BmsData()
        _statusMessage.value = "Disconnected"
    }

    private fun createProtocol(type: BmsType): BmsProtocol = when (type) {
        BmsType.JK_BMS -> JkBmsProtocol()
        BmsType.JBD_BMS -> JbdBmsProtocol()
        BmsType.ANT_BMS -> AntBmsProtocol()
        BmsType.DALY_BMS -> DalyBmsProtocol()
        BmsType.NONE -> error("No BMS type")
    }
}
