package com.kelly.protocol

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class FlashWritePacketTest {
    private fun createTestData(): IntArray = IntArray(512) { it and 0xFF }

    @Test
    fun generates40Packets() {
        val packets = FlashOperations.buildFlashWritePackets(createTestData())
        assertEquals(40, packets.size)
    }

    @Test
    fun first38PacketsHaveCorrectSize() {
        val packets = FlashOperations.buildFlashWritePackets(createTestData())
        for (i in 0 until 39) {
            // CMD(1) + LEN(1) + DATA(16: 3 addr + 13 data) + CHECKSUM(1) = 19
            assertEquals(19, packets[i].size, "Packet $i size")
        }
    }

    @Test
    fun lastPacketHasCorrectSize() {
        val packets = FlashOperations.buildFlashWritePackets(createTestData())
        // CMD(1) + LEN(1) + DATA(8: 3 addr + 5 data) + CHECKSUM(1) = 11
        assertEquals(11, packets[39].size)
    }

    @Test
    fun packet0_addressAndData() {
        val data = createTestData()
        val packets = FlashOperations.buildFlashWritePackets(data)
        val p = packets[0]
        assertEquals(0xF3.toByte(), p[0]) // CMD
        assertEquals(16.toByte(), p[1])    // LEN = 16 (3 addr + 13 data)
        assertEquals(0x00.toByte(), p[2])  // addr low = 0
        assertEquals(13.toByte(), p[3])    // length = 13
        assertEquals(0x00.toByte(), p[4])  // addr high = 0
        // Data bytes 0-12
        for (j in 0 until 13) {
            assertEquals(data[j].toByte(), p[j + 5])
        }
    }

    @Test
    fun packet1_address13() {
        val data = createTestData()
        val packets = FlashOperations.buildFlashWritePackets(data)
        val p = packets[1]
        assertEquals(13.toByte(), p[2])    // addr low = 13
        assertEquals(13.toByte(), p[3])    // length = 13
        assertEquals(0x00.toByte(), p[4])  // addr high = 0
    }

    @Test
    fun packet19_crossesHighByte() {
        val data = createTestData()
        val packets = FlashOperations.buildFlashWritePackets(data)
        val p = packets[19]
        // addr = 19 * 13 = 247 = 0x00F7
        assertEquals(0xF7.toByte(), p[2])  // addr low = 247
        assertEquals(0x00.toByte(), p[4])  // addr high = 0
    }

    @Test
    fun packet20_inHighByte() {
        val data = createTestData()
        val packets = FlashOperations.buildFlashWritePackets(data)
        val p = packets[20]
        // addr = 20 * 13 = 260 = 0x0104
        assertEquals(0x04.toByte(), p[2])  // addr low = 4
        assertEquals(0x01.toByte(), p[4])  // addr high = 1
    }

    @Test
    fun lastPacket_address507() {
        val data = createTestData()
        val packets = FlashOperations.buildFlashWritePackets(data)
        val p = packets[39]
        assertEquals(0xF3.toByte(), p[0])  // CMD
        assertEquals(8.toByte(), p[1])      // LEN = 8 (3 addr + 5 data)
        assertEquals(0xFB.toByte(), p[2])  // addr low = 251
        assertEquals(5.toByte(), p[3])      // length = 5
        assertEquals(0x01.toByte(), p[4])  // addr high = 1
        // Data: bytes 507-511
        for (j in 0 until 5) {
            assertEquals(data[507 + j].toByte(), p[j + 5])
        }
    }

    @Test
    fun allDataBytesCovered() {
        val data = createTestData()
        val packets = FlashOperations.buildFlashWritePackets(data)

        val writtenAddresses = mutableMapOf<Int, Int>() // addr -> value

        for (i in 0 until 39) {
            val addrLow = packets[i][2].toInt() and 0xFF
            val addrHigh = packets[i][4].toInt() and 0xFF
            val startAddr = addrLow + (addrHigh shl 8)
            for (j in 0 until 13) {
                writtenAddresses[startAddr + j] = packets[i][j + 5].toInt() and 0xFF
            }
        }

        // Last packet
        val lastAddrLow = packets[39][2].toInt() and 0xFF
        val lastAddrHigh = packets[39][4].toInt() and 0xFF
        val lastStartAddr = lastAddrLow + (lastAddrHigh shl 8)
        for (j in 0 until 5) {
            writtenAddresses[lastStartAddr + j] = packets[39][j + 5].toInt() and 0xFF
        }

        // Verify all 512 addresses are covered
        assertEquals(512, writtenAddresses.size)
        for (addr in 0 until 512) {
            assertTrue(addr in writtenAddresses, "Address $addr not covered")
            assertEquals(data[addr] and 0xFF, writtenAddresses[addr], "Value mismatch at address $addr")
        }
    }

    @Test
    fun allPacketsHaveCorrectChecksum() {
        val packets = FlashOperations.buildFlashWritePackets(createTestData())
        for ((i, p) in packets.withIndex()) {
            val expected = EtsChecksum.calculate(p, 0, p.size - 1)
            assertEquals(expected, p[p.size - 1], "Checksum mismatch in write packet $i")
        }
    }
}
