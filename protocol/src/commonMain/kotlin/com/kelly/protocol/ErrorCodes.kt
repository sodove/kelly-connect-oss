package com.kelly.protocol

/**
 * Kelly controller error codes.
 * Error status is a 16-bit bitmask where each bit represents a specific error.
 * Decoded from DataValue or monitor response.
 */
object ErrorCodes {
    /**
     * Error names indexed by bit position (0-15).
     * From original Error_Value_Array.
     */
    val ERROR_NAMES = arrayOf(
        "Identify Err",       // bit 0
        "Over Volt",          // bit 1
        "Low Volt",           // bit 2
        "Reserved",           // bit 3
        "Locking",            // bit 4
        "V+ Err",             // bit 5
        "Overtemp",           // bit 6
        "High Pedel",         // bit 7
        "Reserved",           // bit 8
        "Reset Error",        // bit 9
        "Pedel Error",        // bit 10
        "Hall Sensor Error",  // bit 11
        "Reserved",           // bit 12
        "Emergency Rev Err",  // bit 13
        "Motor OverTemp Err", // bit 14
        "Current Meter Err"   // bit 15
    )

    /**
     * Decode a 16-bit error bitmask into a list of active error names.
     *
     * @param errorCode 16-bit error bitmask (0-65535)
     * @return list of error name strings for set bits, or empty if no errors
     */
    fun decode(errorCode: Int): List<String> {
        if (errorCode <= 0 || errorCode > 65535) return emptyList()

        val errors = mutableListOf<String>()
        for (bit in 0 until 16) {
            if ((errorCode shr bit) and 1 == 1) {
                errors.add(ERROR_NAMES[bit])
            }
        }
        return errors
    }

    /**
     * Decode error code to comma-separated string (matching original format).
     */
    fun decodeToString(errorCode: Int): String {
        val errors = decode(errorCode)
        return if (errors.isEmpty()) "" else errors.joinToString(",")
    }
}
