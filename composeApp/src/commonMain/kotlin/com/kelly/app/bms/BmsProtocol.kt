package com.kelly.app.bms

import com.kelly.app.domain.model.BmsData

/**
 * BLE UUIDs for a BMS type.
 * [writeCharUuid] may equal [notifyCharUuid] for single-characteristic BMS (JK, ANT).
 */
data class BmsUuids(
    val serviceUuid: String,
    val notifyCharUuid: String,
    val writeCharUuid: String
)

/**
 * Abstract BMS protocol handler.
 *
 * Each implementation knows how to:
 * - Build BLE command frames
 * - Accumulate notification chunks into complete messages
 * - Parse complete messages into [BmsData]
 */
abstract class BmsProtocol {

    abstract val uuids: BmsUuids

    /** Commands to send once after connecting (handshake / init). */
    abstract fun handshakeCommands(): List<ByteArray>

    /**
     * Commands to send each poll cycle.
     * Empty for streaming protocols (JK BMS sends data continuously after handshake).
     */
    abstract fun pollCommands(): List<ByteArray>

    /** Delay between poll cycles in ms. */
    open val pollIntervalMs: Long = 1000L

    /** Feed an incoming BLE notification chunk. Call [latestData] afterwards. */
    abstract fun onNotification(data: ByteArray)

    /**
     * Return the latest fully-parsed BMS data, or null if no complete reading available yet.
     * Does NOT clear the data — returns the same value until new data arrives.
     */
    abstract fun latestData(): BmsData?

    /** Reset all internal buffers and state. */
    abstract fun reset()
}
