package com.kelly.protocol

import kotlin.test.Test
import kotlin.test.assertEquals

class EtsChecksumTest {
    @Test
    fun emptyPacketCommandOnly() {
        // CMD=0x11, LEN=0x00 → checksum should be 0x11 (sum of 0x11 + 0x00)
        val data = byteArrayOf(0x11, 0x00)
        val checksum = EtsChecksum.calculate(data, 0, 2)
        assertEquals(0x11.toByte(), checksum)
    }

    @Test
    fun packetWithData() {
        // CMD=0xF2, LEN=0x03, DATA=[0x00, 0x10, 0x00]
        // sum = 0xF2 + 0x03 + 0x00 + 0x10 + 0x00 = 0x105 → truncated to 0x05
        val data = byteArrayOf(0xF2.toByte(), 0x03, 0x00, 0x10, 0x00)
        val checksum = EtsChecksum.calculate(data, 0, 5)
        assertEquals(0x05.toByte(), checksum)
    }

    @Test
    fun checksumOverflow() {
        // All 0xFF bytes: 3 * 0xFF = 765 → 765 mod 256 = 253 → 0xFD
        val data = byteArrayOf(0xFF.toByte(), 0xFF.toByte(), 0xFF.toByte())
        val checksum = EtsChecksum.calculate(data, 0, 3)
        assertEquals(0xFD.toByte(), checksum)
    }

    @Test
    fun checksumWithOffset() {
        val data = byteArrayOf(0x00, 0x00, 0x11, 0x00)
        val checksum = EtsChecksum.calculate(data, 2, 2)
        assertEquals(0x11.toByte(), checksum)
    }

    @Test
    fun singleByte() {
        val data = byteArrayOf(0x42)
        val checksum = EtsChecksum.calculate(data, 0, 1)
        assertEquals(0x42.toByte(), checksum)
    }

    @Test
    fun flashOpenCommand() {
        // CMD=0xF1, LEN=0x00 → checksum = 0xF1
        val data = byteArrayOf(0xF1.toByte(), 0x00)
        val checksum = EtsChecksum.calculate(data, 0, 2)
        assertEquals(0xF1.toByte(), checksum)
    }
}
