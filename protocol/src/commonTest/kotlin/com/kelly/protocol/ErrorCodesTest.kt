package com.kelly.protocol

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ErrorCodesTest {
    @Test
    fun noErrors() {
        assertEquals(emptyList(), ErrorCodes.decode(0))
    }

    @Test
    fun singleError_bit0() {
        val errors = ErrorCodes.decode(1)
        assertEquals(1, errors.size)
        assertEquals("Identify Err", errors[0])
    }

    @Test
    fun singleError_bit1_overVolt() {
        val errors = ErrorCodes.decode(2)
        assertEquals(1, errors.size)
        assertEquals("Over Volt", errors[0])
    }

    @Test
    fun singleError_bit2_lowVolt() {
        val errors = ErrorCodes.decode(4)
        assertEquals(1, errors.size)
        assertEquals("Low Volt", errors[0])
    }

    @Test
    fun multipleErrors() {
        // bits 1 and 2: Over Volt + Low Volt
        val errors = ErrorCodes.decode(6)
        assertEquals(2, errors.size)
        assertTrue("Over Volt" in errors)
        assertTrue("Low Volt" in errors)
    }

    @Test
    fun allErrors() {
        val errors = ErrorCodes.decode(0xFFFF)
        assertEquals(16, errors.size)
    }

    @Test
    fun bit15_currentMeterErr() {
        val errors = ErrorCodes.decode(0x8000)
        assertEquals(1, errors.size)
        assertEquals("Current Meter Err", errors[0])
    }

    @Test
    fun errorCodeOutOfRange() {
        assertEquals(emptyList(), ErrorCodes.decode(-1))
        assertEquals(emptyList(), ErrorCodes.decode(65536))
    }

    @Test
    fun decodeToString_singleError() {
        assertEquals("Over Volt", ErrorCodes.decodeToString(2))
    }

    @Test
    fun decodeToString_multipleErrors() {
        val result = ErrorCodes.decodeToString(6) // bits 1+2
        assertTrue(result.contains("Over Volt"))
        assertTrue(result.contains("Low Volt"))
    }

    @Test
    fun decodeToString_noErrors() {
        assertEquals("", ErrorCodes.decodeToString(0))
    }
}
