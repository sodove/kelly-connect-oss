package com.kelly.protocol

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.assertContentEquals

class EtsPacketTest {
    @Test
    fun buildEmptyPacket_codeVersion() {
        // sendcmd() with no data: checksum = CMD
        val packet = EtsPacketBuilder.buildTxPacket(EtsCommand.CODE_VERSION)
        assertEquals(3, packet.size)
        assertEquals(0x11.toByte(), packet[0]) // CMD
        assertEquals(0x00.toByte(), packet[1]) // LEN
        assertEquals(0x11.toByte(), packet[2]) // CHECKSUM = CMD when no data
    }

    @Test
    fun buildEmptyPacket_flashOpen() {
        val packet = EtsPacketBuilder.buildTxPacket(EtsCommand.FLASH_OPEN)
        assertEquals(3, packet.size)
        assertEquals(0xF1.toByte(), packet[0])
        assertEquals(0x00.toByte(), packet[1])
        assertEquals(0xF1.toByte(), packet[2]) // CHECKSUM = CMD
    }

    @Test
    fun buildPacketWithData_flashRead() {
        val data = byteArrayOf(0x00, 0x10, 0x00) // addr=0, len=16, high=0
        val packet = EtsPacketBuilder.buildTxPacket(EtsCommand.FLASH_READ, data)
        assertEquals(6, packet.size) // CMD(1) + LEN(1) + DATA(3) + CHECKSUM(1)
        assertEquals(0xF2.toByte(), packet[0])
        assertEquals(0x03.toByte(), packet[1])
        assertEquals(0x00.toByte(), packet[2])
        assertEquals(0x10.toByte(), packet[3])
        assertEquals(0x00.toByte(), packet[4])
        // checksum = 0xF2 + 0x03 + 0x00 + 0x10 + 0x00 = 0x105 → 0x05
        assertEquals(0x05.toByte(), packet[5])
    }

    @Test
    fun buildEmptyPacket_flashClose() {
        val packet = EtsPacketBuilder.buildTxPacket(EtsCommand.FLASH_CLOSE)
        assertEquals(3, packet.size)
        assertEquals(0xF4.toByte(), packet[0])
        assertEquals(0x00.toByte(), packet[1])
        assertEquals(0xF4.toByte(), packet[2])
    }

    @Test
    fun parseRxResponse_validPacket() {
        // Simulate a valid response: CMD=0x11, LEN=0x02, DATA=[0x01, 0x09], CHECKSUM
        val raw = byteArrayOf(0x11, 0x02, 0x01, 0x09, 0x00) // need correct checksum
        val checksum = EtsChecksum.calculate(raw, 0, 4)
        raw[4] = checksum

        val result = EtsPacketBuilder.parseRxResponse(raw, EtsCommand.CODE_VERSION)
        assertTrue(result.isSuccess)
        val packet = result.getOrThrow()
        assertEquals(0x11.toByte(), packet.command)
        assertEquals(2, packet.dataLength)
        assertEquals(0x01.toByte(), packet.data[0])
        assertEquals(0x09.toByte(), packet.data[1])
    }

    @Test
    fun parseRxResponse_commandMismatch() {
        val raw = byteArrayOf(0x12, 0x00, 0x12)
        val result = EtsPacketBuilder.parseRxResponse(raw, EtsCommand.CODE_VERSION)
        assertTrue(result.isFailure)
    }

    @Test
    fun parseRxResponse_checksumMismatch() {
        val raw = byteArrayOf(0x11, 0x02, 0x01, 0x09, 0xFF.toByte()) // bad checksum
        val result = EtsPacketBuilder.parseRxResponse(raw, EtsCommand.CODE_VERSION)
        assertTrue(result.isFailure)
    }

    @Test
    fun parseRxResponse_emptyData() {
        val raw = byteArrayOf(0xF1.toByte(), 0x00, 0xF1.toByte())
        val result = EtsPacketBuilder.parseRxResponse(raw, EtsCommand.FLASH_OPEN)
        assertTrue(result.isSuccess)
        assertEquals(0, result.getOrThrow().dataLength)
    }

    @Test
    fun maxPacketSize() {
        val data = ByteArray(16) { it.toByte() }
        val packet = EtsPacketBuilder.buildTxPacket(0x11, data)
        assertEquals(19, packet.size) // 1 + 1 + 16 + 1
    }

    @Test
    fun packetTooLargeThrows() {
        val data = ByteArray(17) // exceeds max
        try {
            EtsPacketBuilder.buildTxPacket(0x11, data)
            assertTrue(false, "Should have thrown")
        } catch (e: IllegalArgumentException) {
            // expected
        }
    }
}
