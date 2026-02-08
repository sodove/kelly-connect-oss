package com.kelly.protocol

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class FlashReadPacketTest {
    @Test
    fun generates32Packets() {
        val packets = FlashOperations.buildFlashReadPackets()
        assertEquals(32, packets.size)
    }

    @Test
    fun block0_addressZero() {
        val packets = FlashOperations.buildFlashReadPackets()
        val p = packets[0]
        assertEquals(0xF2.toByte(), p[0]) // CMD
        assertEquals(0x03.toByte(), p[1]) // LEN
        assertEquals(0x00.toByte(), p[2]) // addr low = 0
        assertEquals(0x10.toByte(), p[3]) // length = 16
        assertEquals(0x00.toByte(), p[4]) // addr high = 0
    }

    @Test
    fun block1_address16() {
        val packets = FlashOperations.buildFlashReadPackets()
        val p = packets[1]
        assertEquals(0x10.toByte(), p[2]) // addr low = 16
        assertEquals(0x10.toByte(), p[3]) // length = 16
        assertEquals(0x00.toByte(), p[4]) // addr high = 0
    }

    @Test
    fun block15_address240() {
        val packets = FlashOperations.buildFlashReadPackets()
        val p = packets[15]
        assertEquals(0xF0.toByte(), p[2]) // addr low = 240
        assertEquals(0x10.toByte(), p[3]) // length = 16
        assertEquals(0x00.toByte(), p[4]) // addr high = 0
    }

    @Test
    fun block16_address256_crossesBytesBoundary() {
        val packets = FlashOperations.buildFlashReadPackets()
        val p = packets[16]
        // Address 256 = 0x100 → low=0x00, high=0x01
        assertEquals(0x00.toByte(), p[2]) // addr low = 0
        assertEquals(0x10.toByte(), p[3]) // length = 16
        assertEquals(0x01.toByte(), p[4]) // addr high = 1
    }

    @Test
    fun block31_address496() {
        val packets = FlashOperations.buildFlashReadPackets()
        val p = packets[31]
        // Address 496 = 0x1F0 → low=0xF0, high=0x01
        assertEquals(0xF0.toByte(), p[2]) // addr low = 240
        assertEquals(0x10.toByte(), p[3]) // length = 16
        assertEquals(0x01.toByte(), p[4]) // addr high = 1
    }

    @Test
    fun allPacketsHaveCorrectSize() {
        val packets = FlashOperations.buildFlashReadPackets()
        for (p in packets) {
            assertEquals(6, p.size) // CMD(1) + LEN(1) + DATA(3) + CHECKSUM(1)
        }
    }

    @Test
    fun allPacketsHaveCorrectChecksum() {
        val packets = FlashOperations.buildFlashReadPackets()
        for (p in packets) {
            val expected = EtsChecksum.calculate(p, 0, 5) // CMD + LEN + 3 DATA bytes
            assertEquals(expected, p[5], "Checksum mismatch in packet")
        }
    }

    @Test
    fun coversAll512Bytes() {
        val packets = FlashOperations.buildFlashReadPackets()
        val coveredAddresses = mutableSetOf<Int>()
        for (i in packets.indices) {
            val addrLow = packets[i][2].toInt() and 0xFF
            val addrHigh = packets[i][4].toInt() and 0xFF
            val startAddr = addrLow + (addrHigh shl 8)
            for (j in 0 until 16) {
                coveredAddresses.add(startAddr + j)
            }
        }
        assertEquals(512, coveredAddresses.size)
        for (addr in 0 until 512) {
            assertTrue(addr in coveredAddresses, "Address $addr not covered")
        }
    }
}
