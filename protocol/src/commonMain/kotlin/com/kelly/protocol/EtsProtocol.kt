package com.kelly.protocol

/**
 * High-level ETS protocol operations for Kelly controller communication.
 * Provides suspend functions that coordinate packet building, sending, and response parsing.
 *
 * @param sendAndReceive raw transport: send bytes, receive response bytes
 * @param drainStaleData called before each send to clear leftover bytes from the receive buffer
 */
class EtsProtocol(
    private val sendAndReceive: suspend (ByteArray) -> ByteArray,
    private val drainStaleData: suspend () -> Unit = {}
) {
    /**
     * Send a packet and parse the response, retrying on failure.
     * Drains stale data before each attempt to prevent protocol desync.
     */
    private suspend fun sendWithRetry(
        txPacket: ByteArray,
        expectedCmd: Byte,
        maxAttempts: Int
    ): Result<EtsPacket> {
        var lastError: Throwable? = null
        repeat(maxAttempts) {
            try {
                drainStaleData()
                val rxRaw = sendAndReceive(txPacket)
                val result = EtsPacketBuilder.parseRxResponse(rxRaw, expectedCmd)
                if (result.isSuccess) return result
                lastError = result.exceptionOrNull()
            } catch (e: Exception) {
                lastError = e
            }
        }
        return Result.failure(lastError ?: EtsProtocolException("Failed after $maxAttempts attempts"))
    }

    /**
     * Read firmware version. Sends CODE_VERSION (0x11) and returns the raw response.
     */
    suspend fun readVersion(): Result<EtsPacket> {
        val txPacket = EtsPacketBuilder.buildTxPacket(EtsCommand.CODE_VERSION)
        return sendWithRetry(txPacket, EtsCommand.CODE_VERSION, maxAttempts = 2)
    }

    /**
     * Open flash for reading/writing. Must be called before read or write operations.
     */
    suspend fun openFlash(): Result<EtsPacket> {
        val txPacket = EtsPacketBuilder.buildTxPacket(EtsCommand.FLASH_OPEN)
        return sendWithRetry(txPacket, EtsCommand.FLASH_OPEN, maxAttempts = 2)
    }

    /**
     * Read all 512 bytes of calibration data from flash.
     * Performs 32 reads of 16 bytes each. Each read retries up to 2 times.
     *
     * @return IntArray of 512 unsigned byte values (0-255)
     */
    suspend fun readFlash(): Result<IntArray> {
        val dataValue = IntArray(FlashOperations.DATA_BUFFER_SIZE)
        val readPackets = FlashOperations.buildFlashReadPackets()

        for (i in readPackets.indices) {
            val result = sendWithRetry(readPackets[i], EtsCommand.FLASH_READ, maxAttempts = 2)

            result.onFailure { return Result.failure(it) }
            result.onSuccess { packet ->
                FlashOperations.parseFlashReadResponse(packet.data, i, dataValue)
            }
        }

        return Result.success(dataValue)
    }

    /**
     * Write all 512 bytes of calibration data to flash.
     * Performs 40 writes (39 x 13 bytes + 1 x 5 bytes), then burns to flash.
     * Each write retries up to 3 times.
     */
    suspend fun writeFlash(dataValue: IntArray): Result<Unit> {
        val writePackets = FlashOperations.buildFlashWritePackets(dataValue)

        for (packet in writePackets) {
            val result = sendWithRetry(packet, EtsCommand.FLASH_WRITE, maxAttempts = 3)
            result.onFailure { return Result.failure(it) }
        }

        return Result.success(Unit)
    }

    /**
     * Burn (commit) written data to flash. Must be called after writeFlash.
     * Retries up to 30 times (flash commit can take up to 9 seconds).
     */
    suspend fun burnFlash(): Result<EtsPacket> {
        val txPacket = EtsPacketBuilder.buildTxPacket(EtsCommand.FLASH_CLOSE)
        return sendWithRetry(txPacket, EtsCommand.FLASH_CLOSE, maxAttempts = 30)
    }

    /**
     * Read monitor data. Sends 3 monitor commands and returns concatenated data.
     * No retry — caller (monitoring loop) handles failures via polling.
     *
     * @return IntArray of monitor data (up to 48 bytes)
     */
    suspend fun readMonitor(): Result<IntArray> {
        val monitorData = IntArray(48)

        for (cmdIndex in MonitorDefinitions.MONITOR_COMMANDS.indices) {
            val cmd = MonitorDefinitions.MONITOR_COMMANDS[cmdIndex]
            val txPacket = EtsPacketBuilder.buildTxPacket(cmd)
            drainStaleData()
            val rxRaw = sendAndReceive(txPacket)
            val result = EtsPacketBuilder.parseRxResponse(rxRaw, cmd)

            result.onFailure { return Result.failure(it) }
            result.onSuccess { packet ->
                for (j in 0 until minOf(packet.dataLength, 16)) {
                    monitorData[(cmdIndex * 16) + j] = packet.data[j].toInt() and 0xFF
                }
            }
        }

        return Result.success(monitorData)
    }

    /**
     * Read phase current zero values (for ReadZero dialog).
     */
    suspend fun readPhaseCurrentAD(): Result<IntArray> {
        val txPacket = EtsPacketBuilder.buildTxPacket(EtsCommand.GET_PHASE_I_AD)
        val result = sendWithRetry(txPacket, EtsCommand.GET_PHASE_I_AD, maxAttempts = 2)

        return result.map { packet ->
            IntArray(10) { i ->
                val value = packet.data[i].toInt()
                if (value < 0) value + 256 else value
            }
        }
    }
}
