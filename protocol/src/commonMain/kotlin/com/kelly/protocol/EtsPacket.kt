package com.kelly.protocol

/**
 * ETS protocol packet structure.
 * Frame format: [COMMAND] [DATA_LENGTH] [DATA_0..DATA_N] [CHECKSUM]
 * Max total size: 19 bytes (1 cmd + 1 len + 16 data + 1 checksum)
 */
data class EtsPacket(
    val command: Byte,
    val dataLength: Int,
    val data: ByteArray,
    val checksum: Byte
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is EtsPacket) return false
        return command == other.command &&
                dataLength == other.dataLength &&
                data.contentEquals(other.data) &&
                checksum == other.checksum
    }

    override fun hashCode(): Int {
        var result = command.toInt()
        result = 31 * result + dataLength
        result = 31 * result + data.contentHashCode()
        result = 31 * result + checksum.toInt()
        return result
    }
}

/**
 * Builds and parses ETS protocol packets.
 */
object EtsPacketBuilder {
    const val MAX_DATA_LENGTH = 16
    const val MAX_PACKET_SIZE = 19 // cmd(1) + len(1) + data(16) + checksum(1)

    /**
     * Build a TX packet ready for sending.
     * If data is empty: checksum = command (matching original sendcmd() behavior).
     * If data is present: checksum = sum(command + length + data bytes).
     */
    fun buildTxPacket(command: Byte, data: ByteArray = byteArrayOf()): ByteArray {
        require(data.size <= MAX_DATA_LENGTH) { "Data exceeds max length of $MAX_DATA_LENGTH" }

        val length = data.size.toByte()
        val packet = ByteArray(data.size + 3) // cmd + len + data + checksum

        packet[0] = command
        packet[1] = length

        if (data.isEmpty()) {
            // Original behavior: when no data, checksum = command
            packet[2] = command
        } else {
            data.copyInto(packet, 2)
            packet[data.size + 2] = EtsChecksum.calculate(packet, 0, data.size + 2)
        }

        return packet
    }

    /**
     * Parse a raw received packet into an EtsPacket.
     * @param raw the raw bytes received
     * @return parsed packet
     */
    fun parseRxPacket(raw: ByteArray): EtsPacket {
        require(raw.size >= 3) { "Packet too short: ${raw.size} bytes" }

        val command = raw[0]
        val dataLength = raw[1].toInt() and 0xFF
        val clampedLength = minOf(dataLength, MAX_DATA_LENGTH)

        require(raw.size >= clampedLength + 3) { "Packet incomplete: expected ${clampedLength + 3}, got ${raw.size}" }

        val data = raw.copyOfRange(2, 2 + clampedLength)
        val checksum = raw[clampedLength + 2]

        return EtsPacket(command, clampedLength, data, checksum)
    }

    /**
     * Parse and validate a received response.
     * Checks that command matches expected and checksum is valid.
     */
    fun parseRxResponse(raw: ByteArray, expectedCmd: Byte): Result<EtsPacket> {
        return try {
            val packet = parseRxPacket(raw)

            if (packet.command != expectedCmd) {
                Result.failure(EtsProtocolException(
                    "Command mismatch: expected 0x${expectedCmd.toUByte().toString(16).uppercase()}, " +
                    "got 0x${packet.command.toUByte().toString(16).uppercase()}"
                ))
            } else {
                // Verify checksum
                val expectedChecksum = EtsChecksum.calculate(raw, 0, packet.dataLength + 2)
                if (expectedChecksum != packet.checksum) {
                    Result.failure(EtsProtocolException(
                        "Checksum mismatch: expected 0x${expectedChecksum.toUByte().toString(16).uppercase()}, " +
                        "got 0x${packet.checksum.toUByte().toString(16).uppercase()}"
                    ))
                } else {
                    Result.success(packet)
                }
            }
        } catch (e: IllegalArgumentException) {
            Result.failure(EtsProtocolException(e.message ?: "Invalid packet"))
        }
    }
}

class EtsProtocolException(message: String) : Exception(message)
