package com.kelly.protocol

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.assertFalse

class ParameterCodecTest {
    @Test
    fun readBit_position0() {
        val data = intArrayOf(0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
            0b00000101) // offset 20, bits 0 and 2 set
        assertEquals("1", ParameterCodec.readParam(data, 20, ParamSize.BIT, 0, ParamType.UNSIGNED))
        assertEquals("0", ParameterCodec.readParam(data, 20, ParamSize.BIT, 1, ParamType.UNSIGNED))
        assertEquals("1", ParameterCodec.readParam(data, 20, ParamSize.BIT, 2, ParamType.UNSIGNED))
    }

    @Test
    fun readBit_position7() {
        val data = IntArray(32) { 0 }
        data[20] = 0b10000000 // bit 7 set
        assertEquals("1", ParameterCodec.readParam(data, 20, ParamSize.BIT, 7, ParamType.UNSIGNED))
        assertEquals("0", ParameterCodec.readParam(data, 20, ParamSize.BIT, 6, ParamType.UNSIGNED))
    }

    @Test
    fun writeBit_set() {
        val data = IntArray(32) { 0 }
        assertTrue(ParameterCodec.writeParam(data, 20, ParamSize.BIT, 0, ParamType.UNSIGNED, "1"))
        assertEquals(1, data[20])
    }

    @Test
    fun writeBit_clear() {
        val data = IntArray(32) { 0 }
        data[20] = 0xFF
        assertTrue(ParameterCodec.writeParam(data, 20, ParamSize.BIT, 0, ParamType.UNSIGNED, "0"))
        assertEquals(0xFE, data[20])
    }

    @Test
    fun writeBit_clearMiddle() {
        val data = IntArray(32) { 0 }
        data[20] = 0b00000100 // bit 2 set
        assertTrue(ParameterCodec.writeParam(data, 20, ParamSize.BIT, 2, ParamType.UNSIGNED, "0"))
        assertEquals(0, data[20])
    }

    @Test
    fun writeBit_invalidValue() {
        val data = IntArray(32) { 0 }
        assertFalse(ParameterCodec.writeParam(data, 20, ParamSize.BIT, 0, ParamType.UNSIGNED, "2"))
    }

    @Test
    fun readByte() {
        val data = IntArray(100) { 0 }
        data[37] = 85
        assertEquals("85", ParameterCodec.readParam(data, 37, ParamSize.BYTE, 0, ParamType.UNSIGNED))
    }

    @Test
    fun writeByte() {
        val data = IntArray(100) { 0 }
        assertTrue(ParameterCodec.writeParam(data, 37, ParamSize.BYTE, 0, ParamType.UNSIGNED, "85"))
        assertEquals(85, data[37])
    }

    @Test
    fun readWord_bigEndian() {
        val data = IntArray(100) { 0 }
        data[23] = 0x01 // high byte
        data[24] = 0x02 // low byte
        // big-endian: 0x01 * 256 + 0x02 = 258
        assertEquals("258", ParameterCodec.readParam(data, 23, ParamSize.WORD, 1, ParamType.UNSIGNED))
    }

    @Test
    fun writeWord_bigEndian() {
        val data = IntArray(100) { 0 }
        assertTrue(ParameterCodec.writeParam(data, 23, ParamSize.WORD, 1, ParamType.UNSIGNED, "258"))
        assertEquals(1, data[23]) // high byte
        assertEquals(2, data[24]) // low byte
    }

    @Test
    fun readWord_softwareVersion265() {
        val data = IntArray(100) { 0 }
        data[16] = 1  // high
        data[17] = 9  // low
        // 1 * 256 + 9 = 265
        assertEquals("265", ParameterCodec.readParam(data, 16, ParamSize.WORD, 1, ParamType.UNSIGNED))
    }

    @Test
    fun readHex() {
        val data = IntArray(20) { 0 }
        data[12] = 0x0A
        data[13] = 0xFF
        data[14] = 0x01
        data[15] = 0x09
        assertEquals("0aff0109", ParameterCodec.readParam(data, 12, ParamSize.WORD, 3, ParamType.HEX))
    }

    @Test
    fun writeHex() {
        val data = IntArray(20) { 0 }
        assertTrue(ParameterCodec.writeParam(data, 12, ParamSize.WORD, 3, ParamType.HEX, "0aff0109"))
        assertEquals(0x0A, data[12])
        assertEquals(0xFF, data[13])
        assertEquals(0x01, data[14])
        assertEquals(0x09, data[15])
    }

    @Test
    fun readAscii() {
        val data = IntArray(10) { 0 }
        data[0] = 'K'.code
        data[1] = 'B'.code
        data[2] = 'L'.code
        data[3] = 'S'.code
        data[4] = '7'.code
        data[5] = '2'.code
        data[6] = '1'.code
        data[7] = '8'.code
        assertEquals("KBLS7218", ParameterCodec.readParam(data, 0, ParamSize.WORD, 7, ParamType.ASCII))
    }

    @Test
    fun writeAscii() {
        val data = IntArray(10) { 0 }
        assertTrue(ParameterCodec.writeParam(data, 0, ParamSize.WORD, 7, ParamType.ASCII, "KBLS7218"))
        assertEquals('K'.code, data[0])
        assertEquals('B'.code, data[1])
        assertEquals('L'.code, data[2])
        assertEquals('S'.code, data[3])
    }

    @Test
    fun readSigned_positive() {
        val data = IntArray(100) { 0 }
        data[50] = 100
        assertEquals("100", ParameterCodec.readParam(data, 50, ParamSize.BYTE, 0, ParamType.SIGNED))
    }

    @Test
    fun readSigned_negative() {
        val data = IntArray(100) { 0 }
        data[50] = 200 // -56 as signed byte
        assertEquals("-56", ParameterCodec.readParam(data, 50, ParamSize.BYTE, 0, ParamType.SIGNED))
    }

    @Test
    fun writeSigned() {
        val data = IntArray(100) { 0 }
        assertTrue(ParameterCodec.writeParam(data, 50, ParamSize.BYTE, 0, ParamType.SIGNED, "-56"))
        assertEquals(-56, data[50])
    }

    @Test
    fun roundTrip_byte() {
        val data = IntArray(100) { 0 }
        ParameterCodec.writeParam(data, 37, ParamSize.BYTE, 0, ParamType.UNSIGNED, "85")
        assertEquals("85", ParameterCodec.readParam(data, 37, ParamSize.BYTE, 0, ParamType.UNSIGNED))
    }

    @Test
    fun roundTrip_word() {
        val data = IntArray(512) { 0 }
        ParameterCodec.writeParam(data, 107, ParamSize.WORD, 1, ParamType.UNSIGNED, "3000")
        assertEquals("3000", ParameterCodec.readParam(data, 107, ParamSize.WORD, 1, ParamType.UNSIGNED))
    }

    @Test
    fun roundTrip_bit() {
        val data = IntArray(32) { 0 }
        ParameterCodec.writeParam(data, 20, ParamSize.BIT, 4, ParamType.UNSIGNED, "1")
        assertEquals("1", ParameterCodec.readParam(data, 20, ParamSize.BIT, 4, ParamType.UNSIGNED))
        assertEquals("0", ParameterCodec.readParam(data, 20, ParamSize.BIT, 3, ParamType.UNSIGNED))
    }

    @Test
    fun hexWriteWrongLength() {
        val data = IntArray(20) { 0 }
        assertFalse(ParameterCodec.writeParam(data, 12, ParamSize.WORD, 3, ParamType.HEX, "0aff"))
    }

    @Test
    fun asciiWriteWrongLength() {
        val data = IntArray(10) { 0 }
        assertFalse(ParameterCodec.writeParam(data, 0, ParamSize.WORD, 7, ParamType.ASCII, "SHORT"))
    }
}
