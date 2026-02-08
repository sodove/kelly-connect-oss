package com.kelly.protocol

/**
 * Flash memory read/write operations for Kelly controllers.
 *
 * Read: 32 iterations, each reads 16 bytes = 512 bytes total.
 * Write: 40 iterations: 39 chunks of 13 bytes + 1 final chunk of 5 bytes = 512 bytes total.
 *
 * Flash addresses use little-endian format: DATA[0]=addr_low, DATA[2]=addr_high.
 */
object FlashOperations {
    const val DATA_BUFFER_SIZE = 512
    const val READ_BLOCK_SIZE = 16
    const val READ_BLOCK_COUNT = 32    // 32 * 16 = 512
    const val WRITE_CHUNK_SIZE = 13
    const val WRITE_CHUNK_COUNT = 40   // 39 full + 1 partial
    const val LAST_CHUNK_SIZE = 5      // bytes 507-511
    const val LAST_CHUNK_ADDR = 507    // 39 * 13 = 507

    /**
     * Build all 32 flash read packets.
     * Each packet: CMD=0xF2, LEN=3, DATA=[addr_low, 16, addr_high]
     *
     * Address calculation (little-endian):
     *   addr = i * 16
     *   DATA[0] = addr & 0xFF     (low byte)
     *   DATA[1] = 16              (bytes to read)
     *   DATA[2] = (addr >> 8) & 0xFF  (high byte)
     */
    fun buildFlashReadPackets(): List<ByteArray> {
        return (0 until READ_BLOCK_COUNT).map { i ->
            val addr = i * READ_BLOCK_SIZE
            val data = byteArrayOf(
                (addr and 0xFF).toByte(),         // addr low
                READ_BLOCK_SIZE.toByte(),          // length = 16
                ((addr shr 8) and 0xFF).toByte()   // addr high
            )
            EtsPacketBuilder.buildTxPacket(EtsCommand.FLASH_READ, data)
        }
    }

    /**
     * Build all 40 flash write packets from a DataValue array.
     *
     * Packets 0-38: 13 bytes of data each
     *   CMD=0xF3, LEN=16, DATA=[addr_low, 13, addr_high, data[0..12]]
     *
     * Packet 39 (last): 5 bytes of data (bytes 507-511)
     *   CMD=0xF3, LEN=8, DATA=[0xFB, 5, 0x01, data[507..511]]
     *   addr = 507 = 0x01FB -> low=0xFB, high=0x01
     */
    fun buildFlashWritePackets(dataValue: IntArray): List<ByteArray> {
        require(dataValue.size >= DATA_BUFFER_SIZE) { "DataValue must be at least $DATA_BUFFER_SIZE elements" }

        val packets = mutableListOf<ByteArray>()

        // Packets 0-38: 13 bytes each
        for (i in 0 until WRITE_CHUNK_COUNT - 1) {
            val addr = i * WRITE_CHUNK_SIZE
            val data = ByteArray(WRITE_CHUNK_SIZE + 3) // 3 addr bytes + 13 data bytes
            data[0] = (addr and 0xFF).toByte()         // addr low
            data[1] = WRITE_CHUNK_SIZE.toByte()         // length = 13
            data[2] = ((addr shr 8) and 0xFF).toByte()  // addr high
            for (j in 0 until WRITE_CHUNK_SIZE) {
                data[j + 3] = dataValue[addr + j].toByte()
            }
            packets.add(EtsPacketBuilder.buildTxPacket(EtsCommand.FLASH_WRITE, data))
        }

        // Packet 39 (last): bytes 507-511
        val lastData = ByteArray(LAST_CHUNK_SIZE + 3) // 3 addr bytes + 5 data bytes
        lastData[0] = 0xFB.toByte()  // addr low = 251
        lastData[1] = LAST_CHUNK_SIZE.toByte() // length = 5
        lastData[2] = 0x01           // addr high = 1 (256 + 251 = 507)
        for (j in 0 until LAST_CHUNK_SIZE) {
            lastData[j + 3] = dataValue[LAST_CHUNK_ADDR + j].toByte()
        }
        packets.add(EtsPacketBuilder.buildTxPacket(EtsCommand.FLASH_WRITE, lastData))

        return packets
    }

    /**
     * Parse a flash read response and extract data bytes.
     * Response data: [addr_low, length, addr_high, data_bytes...]
     * The actual data starts at index 0 of ETS_RX_DATA (after packet parsing).
     */
    fun parseFlashReadResponse(rxData: ByteArray, blockIndex: Int, target: IntArray) {
        for (j in 0 until READ_BLOCK_SIZE) {
            val value = rxData[j].toInt() and 0xFF
            target[(blockIndex * READ_BLOCK_SIZE) + j] = value
        }
    }
}
