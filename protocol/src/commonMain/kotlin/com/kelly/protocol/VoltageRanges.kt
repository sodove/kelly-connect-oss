package com.kelly.protocol

/**
 * Voltage range lookup table for Low/Over Volt parameters.
 * From original ACAduserEnglishByteUtil.getVoltRange().
 */
object VoltageRanges {
    private data class VoltageRange(val min: Int, val max: Int)

    private val RANGES = mapOf(
        11 to VoltageRange(18, 132),
        12 to VoltageRange(18, 136),
        14 to VoltageRange(18, 180),
        16 to VoltageRange(18, 200),
        24 to VoltageRange(8, 35),
        32 to VoltageRange(18, 380),
        36 to VoltageRange(18, 45),
        48 to VoltageRange(18, 62),
        60 to VoltageRange(18, 80),
        72 to VoltageRange(18, 90),
        84 to VoltageRange(18, 105),
        96 to VoltageRange(18, 120)
    )

    /**
     * Get the minimum voltage for a given controller voltage code.
     */
    fun getMin(voltageCode: Int): Int = RANGES[voltageCode]?.min ?: 0

    /**
     * Get the maximum voltage for a given controller voltage code.
     */
    fun getMax(voltageCode: Int): Int = RANGES[voltageCode]?.max ?: 0

    /**
     * Calculate voltage range for special "80" code.
     * When voltage code is 80, max = controllerVolt * 125%.
     *
     * @param controllerVolt the actual controller voltage from DataValue[23]*256 + DataValue[24]
     * @return Pair(min, max)
     */
    fun getRangeForCode80(controllerVolt: Int): Pair<Int, Int> =
        Pair(18, (controllerVolt * 125) / 100)

    /**
     * Get voltage range for a parameter, handling the special code-80 case.
     *
     * @param voltageCodeStr ASCII voltage code from DataValue[3..4]
     * @param controllerVolt controller voltage value from DataValue[23]*256 + DataValue[24]
     * @return Pair(min, max)
     */
    fun getVoltageRange(voltageCodeStr: String, controllerVolt: Int): Pair<Int, Int> {
        val code = voltageCodeStr.toIntOrNull() ?: return Pair(0, 0)
        return if (code == 80) {
            getRangeForCode80(controllerVolt)
        } else {
            Pair(getMin(code), getMax(code))
        }
    }
}
