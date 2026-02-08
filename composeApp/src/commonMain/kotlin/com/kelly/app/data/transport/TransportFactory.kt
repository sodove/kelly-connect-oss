package com.kelly.app.data.transport

data class DeviceInfo(
    val name: String,
    val address: String,
    val type: TransportType
)

/**
 * Platform-specific transport factory.
 * Each platform provides an implementation via DI.
 */
interface TransportFactory {
    fun getAvailableTransports(): List<TransportType>
    fun createTransport(type: TransportType): Transport
    suspend fun scanDevices(type: TransportType): List<DeviceInfo>
}
