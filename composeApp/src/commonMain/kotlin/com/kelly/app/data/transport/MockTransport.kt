package com.kelly.app.data.transport

import com.kelly.protocol.EtsChecksum
import com.kelly.protocol.EtsCommand
import com.kelly.protocol.EtsPacketBuilder
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.random.Random

/**
 * Simulated transport for testing without real hardware.
 * Emulates a KBLS7218S controller with firmware v265 (KBLS_0109).
 */
class MockTransport : Transport {

    private val _state = MutableStateFlow<TransportState>(TransportState.Disconnected)
    override val state: StateFlow<TransportState> = _state.asStateFlow()
    override val type: TransportType = TransportType.MOCK

    /** Simulated 512-byte flash memory */
    private val flash = IntArray(512) { 0 }

    /** Last received TX packet, used to generate response */
    private var lastTxPacket: ByteArray = byteArrayOf()

    /** Monitor tick counter for varying values */
    private var monitorTick = 0

    init {
        initFlash()
    }

    private fun initFlash() {
        // Module name "KBLS7218" at offset 0-7
        "KBLS721S".forEachIndexed { i, c -> flash[i] = c.code }
        // User name "TEST" at offset 8-11
        "TEST".forEachIndexed { i, c -> flash[8 + i] = c.code }
        // Serial number at offset 12-15 (hex)
        flash[12] = 0x01; flash[13] = 0x02; flash[14] = 0x03; flash[15] = 0x04
        // Software version 265 (0x0109) at offset 16-17
        flash[16] = 0x01; flash[17] = 0x09
        // Config bits at offset 20
        flash[20] = 0b00000101  // Startup H-Pedel + NTL H-Pedel enabled
        // Config bits at offset 21
        flash[21] = 0b00010001  // Foot Switch + Cruise enabled
        // Controller Volt = 48V at offset 23-24
        flash[23] = 0x00; flash[24] = 48
        // Low Volt at offset 25-26
        flash[25] = 0x00; flash[26] = 36
        // Over Volt at offset 27-28
        flash[27] = 0x00; flash[28] = 62
        // Motor Current% at offset 37
        flash[37] = 80
        // Batt Current% at offset 38
        flash[38] = 70
        // Identify Angle at offset 56 (0x55 = disabled)
        flash[56] = 0x55
        // TPS Low at offset 92
        flash[92] = 5
        // TPS High at offset 93
        flash[93] = 95
        // TPS Type at offset 95 (1 = 0-5V)
        flash[95] = 1
        // TPS Dead Low at offset 96
        flash[96] = 10
        // TPS Dead High at offset 97
        flash[97] = 190
        // Brake Type at offset 100 (1 = 0-5V)
        flash[100] = 1
        // Brake Dead Low at offset 101
        flash[101] = 10
        // Brake Dead High at offset 102
        flash[102] = 90
        // Max Output Fre at offset 105-106 = 500Hz
        flash[105] = 0x01; flash[106] = 0xF4.toByte().toInt() and 0xFF
        // Max Speed at offset 107-108 = 6000rpm
        flash[107] = 0x17; flash[108] = 0x70
        // Max Forw Speed% at offset 109
        flash[109] = 100
        // Max Rev Speed% at offset 110
        flash[110] = 50
        // PWM frequency at offset 127 (20 = 20kHz)
        flash[127] = 20
        // Motor Poles at offset 268
        flash[268] = 10
        // Speed Sensor Type at offset 269 (2 = Hall)
        flash[269] = 2
        // Motor Temp Sensor at offset 318 (1 = KTY83-122)
        flash[318] = 1
        // High Temp Cut at offset 319
        flash[319] = 130
        // High Temp Resume at offset 320
        flash[320] = 110
    }

    override suspend fun connect(address: String) {
        _state.value = TransportState.Connecting
        delay(200) // simulate connection delay
        _state.value = TransportState.Connected
    }

    override suspend fun disconnect() {
        _state.value = TransportState.Disconnected
    }

    override suspend fun send(data: ByteArray) {
        lastTxPacket = data.copyOf()
        delay(5) // simulate serial latency
    }

    override suspend fun receive(expectedLength: Int, timeoutMs: Long): ByteArray {
        if (lastTxPacket.size < 3) return byteArrayOf()

        val command = lastTxPacket[0]
        delay(10) // simulate response time

        return when (command) {
            EtsCommand.FLASH_OPEN -> buildResponse(command, byteArrayOf())
            EtsCommand.FLASH_READ -> handleFlashRead()
            EtsCommand.FLASH_WRITE -> handleFlashWrite()
            EtsCommand.FLASH_CLOSE -> buildResponse(command, byteArrayOf())
            EtsCommand.CODE_VERSION -> buildResponse(command, byteArrayOf(0x01, 0x09))
            EtsCommand.USER_MONITOR1 -> buildMonitorResponse(0)
            EtsCommand.USER_MONITOR2 -> buildMonitorResponse(1)
            EtsCommand.USER_MONITOR3 -> buildMonitorResponse(2)
            EtsCommand.GET_PHASE_I_AD -> buildResponse(command, ByteArray(10) { 128.toByte() })
            else -> buildResponse(command, byteArrayOf())
        }
    }

    private fun handleFlashRead(): ByteArray {
        // TX packet: [CMD=0xF2, LEN=3, addr_low, read_len, addr_high, checksum]
        val addrLow = lastTxPacket[2].toInt() and 0xFF
        val readLen = lastTxPacket[3].toInt() and 0xFF
        val addrHigh = lastTxPacket[4].toInt() and 0xFF
        val startAddr = addrLow + (addrHigh shl 8)

        val responseData = ByteArray(minOf(readLen, 16)) { j ->
            val addr = startAddr + j
            if (addr < flash.size) flash[addr].toByte() else 0
        }

        return buildResponse(EtsCommand.FLASH_READ, responseData)
    }

    private fun handleFlashWrite(): ByteArray {
        // TX packet: [CMD=0xF3, LEN, addr_low, write_len, addr_high, data..., checksum]
        val addrLow = lastTxPacket[2].toInt() and 0xFF
        val writeLen = lastTxPacket[3].toInt() and 0xFF
        val addrHigh = lastTxPacket[4].toInt() and 0xFF
        val startAddr = addrLow + (addrHigh shl 8)

        for (j in 0 until writeLen) {
            val addr = startAddr + j
            if (addr < flash.size && (j + 5) < lastTxPacket.size - 1) {
                flash[addr] = lastTxPacket[j + 5].toInt() and 0xFF
            }
        }

        return buildResponse(EtsCommand.FLASH_WRITE, byteArrayOf())
    }

    private fun buildMonitorResponse(commandIndex: Int): ByteArray {
        monitorTick++
        val data = ByteArray(16) { 0 }

        when (commandIndex) {
            0 -> {
                // Monitor command 1 (0x3A): switches, sensors, basic values
                data[0] = (80 + Random.nextInt(-5, 6)).coerceIn(0, 255).toByte()  // TPS
                data[1] = 0    // Brake
                data[2] = 0    // Brake Switch
                data[3] = 1    // Foot Switch
                data[4] = 1    // Forward Switch
                data[5] = 0    // Reversed
                data[6] = ((monitorTick % 6) / 3).toByte()  // Hall A (toggles)
                data[7] = (((monitorTick + 2) % 6) / 3).toByte()  // Hall B
                data[8] = (((monitorTick + 4) % 6) / 3).toByte()  // Hall C
                data[9] = 48   // B+ Volt
                data[10] = (35 + Random.nextInt(-2, 3)).coerceIn(0, 150).toByte()  // Motor Temp
                data[11] = (40 + Random.nextInt(-1, 2)).coerceIn(0, 150).toByte()  // Controller Temp
                data[12] = 0   // Setting Dir (forward)
                data[13] = 0   // Actual Dir (forward)
                data[14] = 0   // Brake Switch 2
                data[15] = 0   // Low Speed
            }
            1 -> {
                // Monitor command 2 (0x3B): error status, speed, current
                data[0] = 0x00  // Error Status high
                data[1] = 0x00  // Error Status low (no errors)
                val speed = (1500 + Random.nextInt(-50, 51)).coerceIn(0, 15000)
                data[2] = ((speed shr 8) and 0xFF).toByte()  // Motor Speed high
                data[3] = (speed and 0xFF).toByte()           // Motor Speed low
                val current = (120 + Random.nextInt(-10, 11)).coerceIn(0, 800)
                data[4] = ((current shr 8) and 0xFF).toByte() // Phase Current high
                data[5] = (current and 0xFF).toByte()          // Phase Current low
            }
            2 -> {
                // Monitor command 3 (0x3C): reserved / extended data
            }
        }

        val cmd = when (commandIndex) {
            0 -> EtsCommand.USER_MONITOR1
            1 -> EtsCommand.USER_MONITOR2
            else -> EtsCommand.USER_MONITOR3
        }
        return buildResponse(cmd, data)
    }

    private fun buildResponse(command: Byte, data: ByteArray): ByteArray {
        val packet = ByteArray(data.size + 3)
        packet[0] = command
        packet[1] = data.size.toByte()
        data.copyInto(packet, 2)
        packet[data.size + 2] = EtsChecksum.calculate(packet, 0, data.size + 2)
        return packet
    }
}
