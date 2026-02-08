package com.kelly.protocol

/**
 * Calculates ETS protocol checksum.
 * Formula: checksum = (sum of all bytes from offset to offset+length) truncated to Byte.
 * This matches the original C implementation:
 *   check_sum = command + no_bytes + sum(data[0..no_bytes-1])
 */
object EtsChecksum {
    /**
     * Calculate checksum over a range of bytes.
     * @param data the byte array
     * @param offset start index (inclusive)
     * @param length number of bytes to sum
     * @return checksum as Byte (sum mod 256)
     */
    fun calculate(data: ByteArray, offset: Int, length: Int): Byte {
        var sum = 0
        for (i in offset until offset + length) {
            sum += data[i]
        }
        return sum.toByte()
    }
}
