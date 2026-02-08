package com.kelly.app.transport

import android.Manifest
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.pm.PackageManager
import android.hardware.usb.UsbManager
import android.os.Build
import androidx.core.content.ContextCompat
import com.juul.kable.Scanner
import com.kelly.app.data.transport.*
import kotlinx.coroutines.withTimeoutOrNull

class AndroidTransportFactory(private val context: Context) : TransportFactory {
    override fun getAvailableTransports(): List<TransportType> {
        val types = mutableListOf<TransportType>()

        val usbManager = context.getSystemService(Context.USB_SERVICE) as? UsbManager
        if (usbManager != null) {
            types.add(TransportType.USB)
        }

        val btManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
        if (btManager?.adapter != null) {
            types.add(TransportType.BLUETOOTH_CLASSIC)
            types.add(TransportType.BLE)
        }

        types.add(TransportType.MOCK)
        return types
    }

    override fun createTransport(type: TransportType): Transport {
        return when (type) {
            TransportType.USB -> AndroidUsbTransport(context)
            TransportType.BLUETOOTH_CLASSIC -> AndroidBluetoothClassicTransport()
            TransportType.BLE -> AndroidBleTransport()
            TransportType.MOCK -> MockTransport()
        }
    }

    override suspend fun scanDevices(type: TransportType): List<DeviceInfo> {
        return when (type) {
            TransportType.USB -> scanUsbDevices()
            TransportType.BLUETOOTH_CLASSIC -> scanBluetoothDevices()
            TransportType.BLE -> scanBleDevices()
            TransportType.MOCK -> listOf(
                DeviceInfo("KBLS721S v265 (Mock)", "mock://kbls7218", TransportType.MOCK)
            )
        }
    }

    private fun scanUsbDevices(): List<DeviceInfo> {
        val usbManager = context.getSystemService(Context.USB_SERVICE) as? UsbManager ?: return emptyList()
        return usbManager.deviceList.values
            .filter { it.productId == 24577 } // FT232BM
            .map { DeviceInfo(it.productName ?: "Kelly USB", it.deviceName, TransportType.USB) }
    }

    private fun scanBluetoothDevices(): List<DeviceInfo> {
        // Check runtime permissions first
        if (!hasBluetoothPermission()) {
            throw SecurityException(
                "Bluetooth permission not granted. Please allow Bluetooth access in system settings."
            )
        }

        val btManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
        val adapter = btManager?.adapter
            ?: throw IllegalStateException("Bluetooth adapter not available")

        if (!adapter.isEnabled) {
            throw IllegalStateException("Bluetooth is disabled. Please enable Bluetooth.")
        }

        val bonded = adapter.bondedDevices
        if (bonded.isNullOrEmpty()) {
            throw IllegalStateException(
                "No paired Bluetooth devices found. Pair your Kelly controller in Android Bluetooth settings first."
            )
        }

        return bonded.map {
            DeviceInfo(it.name ?: "Unknown", it.address, TransportType.BLUETOOTH_CLASSIC)
        }
    }

    private fun hasBluetoothPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) ==
                PackageManager.PERMISSION_GRANTED
        } else {
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) ==
                PackageManager.PERMISSION_GRANTED
        }
    }

    private suspend fun scanBleDevices(): List<DeviceInfo> {
        if (!hasBluetoothPermission()) {
            throw SecurityException(
                "Bluetooth permission not granted. Please allow Bluetooth access in system settings."
            )
        }

        val btManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
        val adapter = btManager?.adapter
            ?: throw IllegalStateException("Bluetooth adapter not available")

        if (!adapter.isEnabled) {
            throw IllegalStateException("Bluetooth is disabled. Please enable Bluetooth.")
        }

        // Collect bonded BLE/Dual-mode devices
        val bondedBle = mutableListOf<DeviceInfo>()
        adapter.bondedDevices?.forEach { device ->
            if (device.type == BluetoothDevice.DEVICE_TYPE_LE ||
                device.type == BluetoothDevice.DEVICE_TYPE_DUAL) {
                bondedBle.add(
                    DeviceInfo(
                        name = device.name ?: "Unknown BLE",
                        address = device.address,
                        type = TransportType.BLE
                    )
                )
            }
        }

        // Scan for advertising BLE devices (5 second timeout)
        val scannedDevices = mutableMapOf<String, DeviceInfo>()
        try {
            val scanner = Scanner()
            withTimeoutOrNull(5000L) {
                scanner.advertisements.collect { advertisement ->
                    val addr = advertisement.identifier.toString()
                    if (addr !in scannedDevices) {
                        scannedDevices[addr] = DeviceInfo(
                            name = advertisement.name ?: "BLE $addr",
                            address = addr,
                            type = TransportType.BLE
                        )
                    }
                }
            }
        } catch (_: Exception) {
            // Scan may fail if permissions not fully granted; still return bonded
        }

        // Merge: bonded first, then scanned (deduplicated)
        val result = bondedBle.toMutableList()
        for (device in scannedDevices.values) {
            if (result.none { it.address == device.address }) {
                result.add(device)
            }
        }

        return result
    }
}
