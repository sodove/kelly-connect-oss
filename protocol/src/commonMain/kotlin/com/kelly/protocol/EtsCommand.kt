package com.kelly.protocol

/**
 * ETS protocol command codes for Kelly controller communication.
 */
object EtsCommand {
    const val CODE_VERSION: Byte = 0x11
    const val WATCHDOG_TEST: Byte = 0x10
    const val A2D_BATCH_READ: Byte = 0x1B
    const val GPIO_PORT_INPUT: Byte = 0x1E
    const val GPIO_PIN_INPUT: Byte = 0x1F
    const val MONITOR: Byte = 0x33
    const val MONITOR1: Byte = 0x34
    const val GET_PHASE_I_AD: Byte = 0x35
    const val USER_MONITOR1: Byte = 0x3A
    const val USER_MONITOR2: Byte = 0x3B
    const val USER_MONITOR3: Byte = 0x3C
    const val QUIT_IDENTIFY: Byte = 0x42
    const val ENTRY_IDENTIFY: Byte = 0x43
    const val CHECK_IDENTIFY_STATUS: Byte = 0x44
    const val GET_PMSM_PARM: Byte = 0x4B
    const val WRITE_PMSM_PARM: Byte = 0x4C
    const val GET_RESOLVER_INIT_ANGLE: Byte = 0x4D
    const val GET_HALL_SEQUENCE: Byte = 0x4E

    // Flash operations - these are negative in Java byte, positive as unsigned
    val FLASH_OPEN: Byte = 0xF1.toByte()     // -15 in signed byte
    val FLASH_READ: Byte = 0xF2.toByte()     // -14
    val FLASH_WRITE: Byte = 0xF3.toByte()    // -13
    val FLASH_CLOSE: Byte = 0xF4.toByte()    // -12
    val FLASH_INFO_VERSION: Byte = 0xFA.toByte()
    val ERASE_FLASH: Byte = 0xB1.toByte()    // -79
    val BURNT_CHECKSUM: Byte = 0xB3.toByte() // -77
    val BURNT_FLASH: Byte = 0xB2.toByte()    // -78
    val BURNT_RESET: Byte = 0xB4.toByte()    // -76
    val INVALID_COMMAND: Byte = 0xE3.toByte()
}
