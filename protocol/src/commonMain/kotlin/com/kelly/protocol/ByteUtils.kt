package com.kelly.protocol

/**
 * Byte manipulation utilities matching the original ACAduserEnglishByteUtil.java.
 */
object ByteUtils {
    /**
     * Convert a signed byte to unsigned int (0-255).
     * Original: byte < 0 ? byte + 256 : byte
     */
    fun Byte.toUnsigned(): Int = this.toInt() and 0xFF

    /**
     * Convert an Int to big-endian 16-bit byte array [high, low].
     */
    fun Int.toBigEndian16(): ByteArray = byteArrayOf(
        ((this shr 8) and 0xFF).toByte(),
        (this and 0xFF).toByte()
    )

    /**
     * Read a big-endian 16-bit unsigned value from byte array.
     */
    fun IntArray.readBigEndian16(offset: Int): Int =
        (this[offset] shl 8) or (this[offset + 1] and 0xFF)

    /**
     * Convert address to little-endian pair for flash operations.
     * Returns Pair(low_byte, high_byte)
     */
    fun Int.toLittleEndianAddress(): Pair<Byte, Byte> = Pair(
        (this and 0xFF).toByte(),
        ((this shr 8) and 0xFF).toByte()
    )

    /**
     * Format byte array as hex string: "0A,FF,..."
     * Matches original printByteArrayHex behavior.
     */
    fun ByteArray.toHexString(offset: Int = 0, length: Int = size): String {
        val sb = StringBuilder()
        for (i in offset until offset + length) {
            if (i > offset) sb.append(",")
            val hexInt = this[i].toUnsigned()
            val hexStr = hexInt.toString(16).uppercase()
            if (hexStr.length == 1) {
                sb.append("0")
            }
            sb.append(hexStr)
        }
        return sb.toString()
    }

    /**
     * Parse hex string "0A,FF,..." to int array.
     * Matches original getIntArray behavior.
     */
    fun String.hexToIntArray(): IntArray {
        if (this.isEmpty() || this == "ERROR") return intArrayOf(0)
        val parts = this.split(",")
        return IntArray(parts.size) { parts[it].trim().toInt(16) }
    }

    /**
     * Convert byte array (signed) to unsigned int array.
     * This is what the original code does after reading from device:
     * if (DataValue[i] < 0) DataValue[i] += 256
     */
    fun ByteArray.toUnsignedIntArray(): IntArray = IntArray(size) { this[it].toUnsigned() }
}
