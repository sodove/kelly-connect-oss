package com.kelly.protocol

import com.kelly.protocol.ByteUtils.toUnsigned
import com.kelly.protocol.ByteUtils.toBigEndian16
import com.kelly.protocol.ByteUtils.readBigEndian16
import com.kelly.protocol.ByteUtils.toLittleEndianAddress
import com.kelly.protocol.ByteUtils.toHexString
import com.kelly.protocol.ByteUtils.hexToIntArray
import com.kelly.protocol.ByteUtils.toUnsignedIntArray
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertContentEquals

class ByteUtilsTest {
    @Test
    fun byteToUnsigned_positive() {
        assertEquals(127, (127.toByte()).toUnsigned())
    }

    @Test
    fun byteToUnsigned_negative() {
        assertEquals(255, (-1).toByte().toUnsigned())
        assertEquals(128, (-128).toByte().toUnsigned())
        assertEquals(200, (-56).toByte().toUnsigned())
    }

    @Test
    fun byteToUnsigned_zero() {
        assertEquals(0, 0.toByte().toUnsigned())
    }

    @Test
    fun intToBigEndian16() {
        val result = 258.toBigEndian16()
        assertEquals(1.toByte(), result[0]) // high byte
        assertEquals(2.toByte(), result[1]) // low byte
    }

    @Test
    fun intToBigEndian16_zero() {
        val result = 0.toBigEndian16()
        assertEquals(0.toByte(), result[0])
        assertEquals(0.toByte(), result[1])
    }

    @Test
    fun intToBigEndian16_max() {
        val result = 65535.toBigEndian16()
        assertEquals(0xFF.toByte(), result[0])
        assertEquals(0xFF.toByte(), result[1])
    }

    @Test
    fun readBigEndian16_basic() {
        val data = intArrayOf(0, 1, 2)
        assertEquals(258, data.readBigEndian16(1))
    }

    @Test
    fun readBigEndian16_softwareVersion() {
        // Typical software version: 265 = 0x0109
        val data = intArrayOf(0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 9)
        assertEquals(265, data.readBigEndian16(16))
    }

    @Test
    fun toLittleEndianAddress_zero() {
        val (low, high) = 0.toLittleEndianAddress()
        assertEquals(0.toByte(), low)
        assertEquals(0.toByte(), high)
    }

    @Test
    fun toLittleEndianAddress_block16() {
        // Address 256 = 0x0100
        val (low, high) = 256.toLittleEndianAddress()
        assertEquals(0.toByte(), low)
        assertEquals(1.toByte(), high)
    }

    @Test
    fun toLittleEndianAddress_block31() {
        // Address 496 = 0x01F0
        val (low, high) = 496.toLittleEndianAddress()
        assertEquals(0xF0.toByte(), low)
        assertEquals(1.toByte(), high)
    }

    @Test
    fun toHexString_basic() {
        val data = byteArrayOf(0x0A, 0xFF.toByte(), 0x00, 0x7F)
        assertEquals("0A,FF,00,7F", data.toHexString())
    }

    @Test
    fun hexToIntArray_basic() {
        val result = "0A,FF,00,7F".hexToIntArray()
        assertContentEquals(intArrayOf(10, 255, 0, 127), result)
    }

    @Test
    fun hexToIntArray_error() {
        val result = "ERROR".hexToIntArray()
        assertContentEquals(intArrayOf(0), result)
    }

    @Test
    fun toUnsignedIntArray_conversion() {
        val bytes = byteArrayOf(0, 127, -1, -128)
        val result = bytes.toUnsignedIntArray()
        assertContentEquals(intArrayOf(0, 127, 255, 128), result)
    }
}
