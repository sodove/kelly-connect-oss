package com.kelly.app.transport

import com.kelly.app.data.transport.*

class DesktopTransportFactory : TransportFactory {
    override fun getAvailableTransports(): List<TransportType> {
        return listOf(TransportType.USB, TransportType.BLUETOOTH_CLASSIC, TransportType.MOCK)
    }

    override fun createTransport(type: TransportType): Transport {
        return when (type) {
            TransportType.USB, TransportType.BLUETOOTH_CLASSIC -> DesktopSerialTransport()
            TransportType.BLE -> DesktopBleTransport()
            TransportType.MOCK -> MockTransport()
        }
    }

    override suspend fun scanDevices(type: TransportType): List<DeviceInfo> {
        return when (type) {
            TransportType.USB, TransportType.BLUETOOTH_CLASSIC -> scanSerialPorts()
            TransportType.BLE -> emptyList()
            TransportType.MOCK -> listOf(
                DeviceInfo("KBLS721S v265 (Mock)", "mock://kbls7218", TransportType.MOCK)
            )
        }
    }

    private fun scanSerialPorts(): List<DeviceInfo> {
        return try {
            val ports = com.fazecast.jSerialComm.SerialPort.getCommPorts()
            ports.map { port ->
                DeviceInfo(
                    name = port.descriptivePortName,
                    address = port.systemPortName,
                    type = TransportType.USB
                )
            }.toList()
        } catch (e: Exception) {
            emptyList()
        }
    }
}
