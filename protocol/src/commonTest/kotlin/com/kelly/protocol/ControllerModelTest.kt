package com.kelly.protocol

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ControllerModelTest {
    @Test
    fun kbls_v265_detectsKBLS0109() {
        val result = ControllerModel.detect("KBLS7218", 265)
        assertTrue(result.isSuccess)
        assertEquals(ControllerModel.KBLS_0109, result.getOrThrow())
    }

    @Test
    fun kbls_v270_detectsKBLS0109() {
        val result = ControllerModel.detect("KBLS7230", 270)
        assertTrue(result.isSuccess)
        assertEquals(ControllerModel.KBLS_0109, result.getOrThrow())
    }

    @Test
    fun kbls_v262_detectsKBLS0106() {
        val result = ControllerModel.detect("KBLS7212", 262)
        assertTrue(result.isSuccess)
        assertEquals(ControllerModel.KBLS_0106, result.getOrThrow())
    }

    @Test
    fun kbls_v264_detectsKBLS0106() {
        val result = ControllerModel.detect("KBLS7218", 264)
        assertTrue(result.isSuccess)
        assertEquals(ControllerModel.KBLS_0106, result.getOrThrow())
    }

    @Test
    fun kls_shortName_detected() {
        // "KLS" → substring(1,3) = "LS"
        val result = ControllerModel.detect("KLS72180", 265)
        assertTrue(result.isSuccess)
        assertEquals(ControllerModel.KBLS_0109, result.getOrThrow())
    }

    @Test
    fun bss_detected() {
        val result = ControllerModel.detect("KBSS7218", 265)
        assertTrue(result.isSuccess)
        assertEquals(ControllerModel.KBLS_0109, result.getOrThrow())
    }

    @Test
    fun oldFirmware_rejected() {
        val result = ControllerModel.detect("KBLS7218", 261)
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is UnsupportedControllerException)
    }

    @Test
    fun kaci_rejected() {
        val result = ControllerModel.detect("KACI1234", 265)
        assertTrue(result.isFailure)
    }

    @Test
    fun unknown_rejected() {
        val result = ControllerModel.detect("XXXX1234", 265)
        assertTrue(result.isFailure)
    }

    @Test
    fun shortName_rejected() {
        val result = ControllerModel.detect("AB", 265)
        assertTrue(result.isFailure)
    }
}
