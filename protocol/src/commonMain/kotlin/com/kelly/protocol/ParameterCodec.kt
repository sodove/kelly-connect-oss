package com.kelly.protocol

import kotlin.math.pow

/**
 * Parameter size in flash memory.
 */
enum class ParamSize(val code: Int) {
    BIT(0),    // Single bit within a byte
    BYTE(1),   // Single byte
    WORD(2);   // Multi-byte (big-endian)

    companion object {
        fun fromCode(code: Int): ParamSize = entries.first { it.code == code }
    }
}

/**
 * Parameter data type / display format.
 */
enum class ParamType(val code: String) {
    UNSIGNED("uo"),  // Unsigned integer / bit
    HEX("h"),        // Hexadecimal display
    ASCII("a"),      // ASCII character(s)
    SIGNED("so");    // Signed byte

    companion object {
        fun fromCode(code: String): ParamType = entries.first { it.code == code }
    }
}

/**
 * Safety level for a calibration parameter.
 * Indicates how risky changing the parameter is.
 */
enum class SafetyLevel {
    READ_ONLY,  // Cannot be changed
    SAFE,       // Safe to change
    CAUTION,    // Change with care
    DANGEROUS   // Can damage hardware or cause unsafe behavior
}

/**
 * Category grouping for calibration parameters.
 */
enum class ParamCategory {
    GENERAL,
    PROTECTION,
    THROTTLE,
    BRAKING,
    SPEED,
    MOTOR,
    PID_TUNING,
    ADVANCED
}

/**
 * Encodes and decodes parameter values from the DataValue array.
 * Faithfully reimplements ACAduserEnglishByteUtil.printIntArrayString and printStringArrayInt.
 */
object ParameterCodec {
    /**
     * Read a parameter value from the data array and return as string.
     *
     * @param data the DataValue array (512 unsigned ints)
     * @param offset byte offset in data
     * @param size BIT/BYTE/WORD
     * @param position bit position (for BIT), or byte count - 1 (for WORD)
     * @param type display format
     * @return string representation of the value
     */
    fun readParam(data: IntArray, offset: Int, size: ParamSize, position: Int, type: ParamType): String {
        val sb = StringBuilder()
        val length = when (size) {
            ParamSize.BIT, ParamSize.BYTE -> 1
            ParamSize.WORD -> position + 1
        }

        when (type) {
            ParamType.UNSIGNED -> {
                when (size) {
                    ParamSize.BIT -> {
                        val bitValue = (data[offset] / 2.0.pow(position).toInt()) and 1
                        sb.append(bitValue)
                    }
                    ParamSize.BYTE -> {
                        sb.append(data[offset])
                    }
                    ParamSize.WORD -> {
                        var finalValue = 0
                        for (i in offset until offset + length) {
                            finalValue = finalValue * 256 + data[i]
                        }
                        sb.append(finalValue)
                    }
                }
            }
            ParamType.HEX -> {
                for (i in offset until offset + length) {
                    val hexStr = data[i].toString(16)
                    if (hexStr.length == 1) sb.append("0")
                    sb.append(hexStr)
                }
            }
            ParamType.ASCII -> {
                for (i in offset until offset + length) {
                    sb.append(data[i].toChar())
                }
            }
            ParamType.SIGNED -> {
                sb.append(data[offset].toByte().toInt())
            }
        }

        return sb.toString()
    }

    /**
     * Write a parameter value string into the data array.
     *
     * @param data the DataValue array (modified in place)
     * @param offset byte offset in data
     * @param size BIT/BYTE/WORD
     * @param position bit position (for BIT), or byte count - 1 (for WORD)
     * @param type data format
     * @param value string value to write
     * @return true if successful, false on format error
     */
    fun writeParam(data: IntArray, offset: Int, size: ParamSize, position: Int, type: ParamType, value: String): Boolean {
        when (type) {
            ParamType.UNSIGNED -> {
                when (size) {
                    ParamSize.BIT -> {
                        when (value) {
                            "1" -> {
                                data[offset] = data[offset] or 2.0.pow(position).toInt()
                                return true
                            }
                            "0" -> {
                                data[offset] = data[offset] and (2.0.pow(position).toInt().inv())
                                return true
                            }
                            else -> return false
                        }
                    }
                    ParamSize.BYTE -> {
                        data[offset] = value.toInt()
                        return true
                    }
                    ParamSize.WORD -> {
                        var remaining = value.toInt()
                        for (i in offset until offset + position + 1) {
                            val power = 256.0.pow(position - (i - offset)).toInt()
                            data[i] = remaining / power
                            if (i < offset + position) {
                                remaining -= data[i] * power
                            }
                        }
                        return true
                    }
                }
            }
            ParamType.HEX -> {
                if (value.length != (position + 1) * 2) return false
                for (i in offset until offset + position + 1) {
                    val idx = (i - offset) * 2
                    data[i] = value.substring(idx, idx + 2).toInt(16)
                }
                return true
            }
            ParamType.ASCII -> {
                if (value.length != position + 1) return false
                for (i in offset until offset + position + 1) {
                    data[i] = value[i - offset].code
                }
                return true
            }
            ParamType.SIGNED -> {
                data[offset] = value.toInt()
                return true
            }
        }
    }
}
