package com.kelly.protocol

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class FullProtocolFlowTest {
    /**
     * Integration test: full cycle of read → parse → modify → write → verify.
     * Simulates communication with a controller.
     */
    @Test
    fun fullReadWriteCycle() {
        // Step 1: Create simulated flash data (what the controller would have)
        val simulatedFlash = IntArray(512) { 0 }
        // Set module name "KBLS7218"
        "KBLS7218".forEachIndexed { i, c -> simulatedFlash[i] = c.code }
        // Set software version 265 (0x0109) at offset 16-17
        simulatedFlash[16] = 0x01
        simulatedFlash[17] = 0x09
        // Set Controller Volt = 48 at offset 23-24 (big-endian WORD)
        simulatedFlash[23] = 0x00
        simulatedFlash[24] = 0x30 // 48
        // Set current percent at offset 37
        simulatedFlash[37] = 80

        // Step 2: Generate 32 read packets
        val readPackets = FlashOperations.buildFlashReadPackets()
        assertEquals(32, readPackets.size)

        // Step 3: Simulate responses — build response packets for each read
        val dataValue = IntArray(512)
        for (i in readPackets.indices) {
            // Extract address from read packet
            val addrLow = readPackets[i][2].toInt() and 0xFF
            val addrHigh = readPackets[i][4].toInt() and 0xFF
            val startAddr = addrLow + (addrHigh shl 8)

            // Build simulated response
            val responseData = ByteArray(16) { j ->
                simulatedFlash[startAddr + j].toByte()
            }

            // Parse into dataValue (what parseFlashReadResponse does)
            FlashOperations.parseFlashReadResponse(responseData, i, dataValue)
        }

        // Step 4: Verify the read data matches the simulated flash
        for (i in 0 until 512) {
            assertEquals(simulatedFlash[i], dataValue[i], "Mismatch at offset $i")
        }

        // Step 5: Detect controller model
        val moduleName = ParameterCodec.readParam(dataValue, 0, ParamSize.WORD, 7, ParamType.ASCII)
        assertEquals("KBLS7218", moduleName)

        val softwareVersion = ParameterCodec.readParam(dataValue, 16, ParamSize.WORD, 1, ParamType.UNSIGNED)
        assertEquals("265", softwareVersion)

        val model = ControllerModel.detect(moduleName, softwareVersion.toInt())
        assertTrue(model.isSuccess)
        assertEquals(ControllerModel.KBLS_0109, model.getOrThrow())

        // Step 6: Get parameters for this model
        val params = ParameterDefinitions.getParameters(ControllerModel.KBLS_0109)
        assertTrue(params.isNotEmpty())

        // Step 7: Read a parameter value
        val currentPercent = ParameterCodec.readParam(dataValue, 37, ParamSize.BYTE, 0, ParamType.UNSIGNED)
        assertEquals("80", currentPercent)

        // Step 8: Modify a safe parameter (TPS Dead Low at offset 96)
        val tpsDeadLow = ParameterCodec.readParam(dataValue, 96, ParamSize.BYTE, 0, ParamType.UNSIGNED)
        ParameterCodec.writeParam(dataValue, 96, ParamSize.BYTE, 0, ParamType.UNSIGNED, "15")
        assertEquals("15", ParameterCodec.readParam(dataValue, 96, ParamSize.BYTE, 0, ParamType.UNSIGNED))

        // Step 9: Generate 40 write packets
        val writePackets = FlashOperations.buildFlashWritePackets(dataValue)
        assertEquals(40, writePackets.size)

        // Step 10: Verify write packets contain correct data
        val reconstructed = IntArray(512)
        for (i in 0 until 39) {
            val addrLow = writePackets[i][2].toInt() and 0xFF
            val addrHigh = writePackets[i][4].toInt() and 0xFF
            val startAddr = addrLow + (addrHigh shl 8)
            for (j in 0 until 13) {
                reconstructed[startAddr + j] = writePackets[i][j + 5].toInt() and 0xFF
            }
        }
        // Last packet
        for (j in 0 until 5) {
            reconstructed[507 + j] = writePackets[39][j + 5].toInt() and 0xFF
        }

        // Verify the reconstructed data matches dataValue
        for (i in 0 until 512) {
            assertEquals(dataValue[i] and 0xFF, reconstructed[i], "Write mismatch at offset $i")
        }

        // Verify the modified parameter persisted
        assertEquals(15, reconstructed[96])
    }

    @Test
    fun monitorDataParsing() {
        // Simulate monitor data: 48 bytes (3 commands x 16 bytes)
        val monitorData = IntArray(48) { 0 }
        monitorData[0] = 150  // TPS Pedel
        monitorData[1] = 50   // Brake Pedel
        monitorData[9] = 48   // B+ Volt
        monitorData[11] = 35  // Controller Temp
        monitorData[16] = 0x00 // Error Status high
        monitorData[17] = 0x06 // Error Status low (Over Volt + Low Volt)
        monitorData[18] = 0x0B // Motor Speed high
        monitorData[19] = 0xB8 // Motor Speed low (3000)

        val values = MonitorDefinitions.readMonitorValues(monitorData)
        assertEquals("150", values["TPS Pedel"])
        assertEquals("50", values["Brake Pedel"])
        assertEquals("48", values["B+ Volt"])
        assertEquals("35", values["Controller Temp"])
        assertEquals("0006", values["Error Status"]) // hex format
        assertEquals("3000", values["Motor Speed"])

        // Decode errors
        val errorCode = 0x0006 // bits 1 and 2
        val errors = ErrorCodes.decode(errorCode)
        assertEquals(2, errors.size)
        assertTrue("Over Volt" in errors)
        assertTrue("Low Volt" in errors)
    }

    @Test
    fun voltageRangeValidation() {
        val dataValue = IntArray(512) { 0 }
        // Set voltage code "48" at offset 3-4
        dataValue[3] = '4'.code
        dataValue[4] = '8'.code
        // Set controller volt at offset 23-24
        dataValue[23] = 0
        dataValue[24] = 48

        val voltStr = ParameterCodec.readParam(dataValue, 3, ParamSize.WORD, 1, ParamType.ASCII)
        val (min, max) = VoltageRanges.getVoltageRange(voltStr, 0)
        assertEquals(18, min)
        assertEquals(62, max)
    }

    @Test
    fun voltageRangeCode80() {
        val controllerVolt = 400 // DataValue[23]*256 + DataValue[24]
        val (min, max) = VoltageRanges.getRangeForCode80(controllerVolt)
        assertEquals(18, min)
        assertEquals(500, max) // 400 * 125 / 100
    }
}
