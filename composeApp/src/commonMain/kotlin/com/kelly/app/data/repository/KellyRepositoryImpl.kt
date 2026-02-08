package com.kelly.app.data.repository

import com.kelly.app.data.transport.DeviceInfo
import com.kelly.app.data.transport.Transport
import com.kelly.app.data.transport.TransportFactory
import com.kelly.app.data.transport.TransportType
import com.kelly.app.domain.model.CalibrationData
import com.kelly.app.domain.model.ConnectionState
import com.kelly.app.domain.model.MonitorData
import com.kelly.app.domain.repository.KellyRepository
import com.kelly.protocol.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class KellyRepositoryImpl(
    private val transportFactory: TransportFactory
) : KellyRepository {

    private var transport: Transport? = null
    private var protocol: EtsProtocol? = null
    private var monitorJob: Job? = null
    private val protocolMutex = Mutex()
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    private val _connectionState = MutableStateFlow<ConnectionState>(ConnectionState.Disconnected)
    override val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()

    private val _monitorData = MutableStateFlow(MonitorData())
    override val monitorData: StateFlow<MonitorData> = _monitorData.asStateFlow()

    override suspend fun scanDevices(type: TransportType): List<DeviceInfo> {
        return transportFactory.scanDevices(type)
    }

    override suspend fun connect(device: DeviceInfo): Result<Unit> {
        val result = connectInternal(device)
        if (result.isSuccess) {
            resumeMonitoring()
        }
        return result
    }

    private suspend fun connectInternal(device: DeviceInfo): Result<Unit> = protocolMutex.withLock {
        try {
            _connectionState.value = ConnectionState.Connecting
            val t = transportFactory.createTransport(device.type)
            t.connect(device.address)
            transport = t

            val proto = EtsProtocol(
                sendAndReceive = { txData ->
                    t.send(txData)
                    t.receive(expectedLength = 19, timeoutMs = if (device.type == TransportType.BLUETOOTH_CLASSIC) 300 else 100)
                },
                drainStaleData = { t.drain() }
            )
            protocol = proto

            // Read version to verify connection
            val openResult = proto.openFlash()
            openResult.onFailure {
                _connectionState.value = ConnectionState.Error("Failed to open flash: ${it.message}")
                return@withLock Result.failure(it)
            }

            val readResult = proto.readFlash()
            readResult.onFailure {
                _connectionState.value = ConnectionState.Error("Failed to read flash: ${it.message}")
                return@withLock Result.failure(it)
            }

            val dataValue = readResult.getOrThrow()
            val moduleName = ParameterCodec.readParam(dataValue, 0, ParamSize.WORD, 7, ParamType.ASCII)
            val softVer = (dataValue[16] shl 8) or dataValue[17]

            val modelResult = ControllerModel.detect(moduleName, softVer)
            modelResult.onFailure {
                _connectionState.value = ConnectionState.Error(it.message ?: "Unknown controller")
                return@withLock Result.failure(it)
            }

            _connectionState.value = ConnectionState.Connected(
                moduleName = moduleName,
                softwareVersion = softVer,
                model = modelResult.getOrThrow()
            )
            Result.success(Unit)
        } catch (e: Exception) {
            _connectionState.value = ConnectionState.Error(e.message ?: "Connection failed")
            Result.failure(e)
        }
    }

    override suspend fun disconnect() {
        pauseMonitoring()
        transport?.disconnect()
        transport = null
        protocol = null
        _connectionState.value = ConnectionState.Disconnected
    }

    override suspend fun readCalibration(): Result<CalibrationData> {
        pauseMonitoring()
        delay(300) // drain in-flight monitor responses
        try {
            return protocolMutex.withLock {
                val proto = protocol ?: return@withLock Result.failure(IllegalStateException("Not connected"))
                val state = _connectionState.value
                if (state !is ConnectionState.Connected) {
                    return@withLock Result.failure(IllegalStateException("Not connected"))
                }

                try {
                    proto.openFlash()
                    val readResult = proto.readFlash()
                    readResult.map { dataValue ->
                        val params = ParameterDefinitions.getParameters(state.model)
                        CalibrationData(
                            model = state.model,
                            dataValue = dataValue,
                            parameters = params
                        )
                    }
                } catch (e: Exception) {
                    Result.failure(e)
                }
            }
        } finally {
            resumeMonitoring()
        }
    }

    override suspend fun writeCalibration(data: CalibrationData): Result<Unit> {
        pauseMonitoring()
        delay(300) // drain in-flight monitor responses
        try {
            return protocolMutex.withLock {
                val proto = protocol ?: return@withLock Result.failure(IllegalStateException("Not connected"))

                try {
                    proto.openFlash()
                    val writeResult = proto.writeFlash(data.dataValue)
                    writeResult.onFailure { return@withLock Result.failure(it) }
                    val burnResult = proto.burnFlash()
                    burnResult.map { }
                } catch (e: Exception) {
                    Result.failure(e)
                }
            }
        } finally {
            resumeMonitoring()
        }
    }

    override fun startMonitoring(): Flow<MonitorData> {
        val proto = protocol ?: return flowOf(MonitorData())

        return flow {
            _monitorData.value = _monitorData.value.copy(isActive = true, communicationError = null)
            var consecutiveErrors = 0

            while (currentCoroutineContext().isActive) {
                try {
                    val result = protocolMutex.withLock { proto.readMonitor() }
                    result.onSuccess { monitorValues ->
                        consecutiveErrors = 0
                        val values = MonitorDefinitions.readMonitorValues(monitorValues)
                        val errorHex = values["Error Status"] ?: "0000"
                        val errorCode = errorHex.toIntOrNull(16) ?: 0
                        val errors = ErrorCodes.decode(errorCode)

                        val data = MonitorData(
                            values = values,
                            errorStatus = errorCode,
                            errorMessages = errors,
                            isActive = true,
                            communicationError = null
                        )
                        _monitorData.value = data
                        emit(data)
                    }
                    result.onFailure {
                        consecutiveErrors++
                        if (consecutiveErrors >= 5) {
                            val data = _monitorData.value.copy(
                                communicationError = "Communication lost: ${it.message}"
                            )
                            _monitorData.value = data
                            emit(data)
                            consecutiveErrors = 0
                        }
                    }
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    consecutiveErrors++
                    if (consecutiveErrors >= 5) {
                        val data = _monitorData.value.copy(
                            communicationError = "Communication error: ${e.message}"
                        )
                        _monitorData.value = data
                        emit(data)
                        consecutiveErrors = 0
                    }
                }
                delay(10) // ~50Hz polling rate, transport send/receive rate-limits naturally
            }
        }.onCompletion {
            _monitorData.value = _monitorData.value.copy(isActive = false, communicationError = null)
        }
    }

    override fun stopMonitoring() {
        pauseMonitoring()
    }

    private fun pauseMonitoring() {
        monitorJob?.cancel()
        monitorJob = null
        _monitorData.value = _monitorData.value.copy(isActive = false, communicationError = null)
    }

    private fun resumeMonitoring() {
        if (protocol != null && _connectionState.value is ConnectionState.Connected) {
            monitorJob = scope.launch { startMonitoring().collect {} }
        }
    }

    override suspend fun readPhaseCurrentZero(): Result<IntArray> {
        val proto = protocol ?: return Result.failure(IllegalStateException("Not connected"))
        return proto.readPhaseCurrentAD()
    }
}
