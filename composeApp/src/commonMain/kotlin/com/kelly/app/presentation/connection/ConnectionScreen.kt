package com.kelly.app.presentation.connection

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.kelly.app.data.transport.DeviceInfo
import com.kelly.app.data.transport.TransportType
import com.kelly.app.domain.model.ConnectionState

@Composable
fun ConnectionScreen(component: ConnectionComponent) {
    val selectedTransport by component.selectedTransport.collectAsState()
    val devices by component.devices.collectAsState()
    val isScanning by component.isScanning.collectAsState()
    val errorMessage by component.errorMessage.collectAsState()
    val connectionState by component.connectionState.collectAsState()

    Column(
        modifier = Modifier.fillMaxSize().systemBarsPadding().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Kelly Connect",
            style = MaterialTheme.typography.headlineLarge
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "KBLS/KLS Controller Configuration",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Transport type selection
        Text("Connection Type", style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(8.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TransportType.entries.forEach { type ->
                FilterChip(
                    selected = selectedTransport == type,
                    onClick = { component.selectTransport(type) },
                    label = {
                        Text(
                            when (type) {
                                TransportType.USB -> "USB"
                                TransportType.BLUETOOTH_CLASSIC -> "Bluetooth"
                                TransportType.BLE -> "BLE"
                                TransportType.MOCK -> "Test"
                            }
                        )
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Scan button
        Button(
            onClick = { component.scanDevices() },
            enabled = !isScanning && connectionState !is ConnectionState.Connecting
        ) {
            if (isScanning) {
                CircularProgressIndicator(
                    modifier = Modifier.size(16.dp),
                    strokeWidth = 2.dp
                )
                Spacer(modifier = Modifier.width(8.dp))
            }
            Text(if (isScanning) "Scanning..." else "Scan Devices")
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Connection status
        when (connectionState) {
            is ConnectionState.Connecting -> {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                Spacer(modifier = Modifier.height(8.dp))
                Text("Connecting...")
            }
            is ConnectionState.Error -> {
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)) {
                    Text(
                        text = (connectionState as ConnectionState.Error).message,
                        modifier = Modifier.padding(12.dp),
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }
            else -> {}
        }

        // Error message
        errorMessage?.let { msg ->
            Spacer(modifier = Modifier.height(8.dp))
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = msg,
                        modifier = Modifier.weight(1f),
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                    TextButton(onClick = { component.clearError() }) {
                        Text("Dismiss")
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Device list
        if (devices.isNotEmpty()) {
            Text("Available Devices", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(8.dp))
        }

        LazyColumn(
            modifier = Modifier.fillMaxWidth().weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            items(devices) { device ->
                DeviceItem(
                    device = device,
                    enabled = connectionState !is ConnectionState.Connecting,
                    onClick = { component.connect(device) }
                )
            }
        }

        // Safety warning
        Spacer(modifier = Modifier.height(16.dp))
        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer)) {
            Text(
                text = "Warning: Incorrect parameter writes can damage the controller. Always verify values before writing.",
                modifier = Modifier.padding(12.dp),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onTertiaryContainer
            )
        }
    }
}

@Composable
private fun DeviceItem(
    device: DeviceInfo,
    enabled: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(enabled = enabled, onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = device.name.ifEmpty { "Unknown Device" },
                    style = MaterialTheme.typography.bodyLarge
                )
                Text(
                    text = device.address,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                text = when (device.type) {
                    TransportType.USB -> "USB"
                    TransportType.BLUETOOTH_CLASSIC -> "BT"
                    TransportType.BLE -> "BLE"
                    TransportType.MOCK -> "TEST"
                },
                style = MaterialTheme.typography.labelSmall
            )
        }
    }
}
