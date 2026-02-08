package com.kelly.app.presentation.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.material3.MenuAnchorType
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.kelly.app.domain.model.BmsType
import com.kelly.app.presentation.dashboard.DashboardSettings
import com.kelly.app.presentation.dashboard.SpeedUnit

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(component: SettingsComponent) {
    val settings by component.settings.collectAsState()
    val bmsType by component.bmsType.collectAsState()
    val bmsConnected by component.bmsConnected.collectAsState()
    val bmsStatus by component.bmsStatus.collectAsState()
    val bmsData by component.bmsData.collectAsState()
    val discoveredDevices by component.discoveredDevices.collectAsState()
    val isScanning by component.isScanning.collectAsState()

    var speedUnit by remember(settings) { mutableStateOf(settings.speedUnit) }
    var wheelDiam by remember(settings) { mutableStateOf(settings.wheelDiameterMm.toString()) }
    var gearRatio by remember(settings) { mutableStateOf(settings.gearRatio.toString()) }
    var maxSpeed by remember(settings) { mutableStateOf(settings.maxSpeedDisplay.toString()) }
    var maxCurrent by remember(settings) { mutableStateOf(settings.maxCurrentA.toString()) }
    var dirty by remember { mutableStateOf(false) }

    fun save() {
        val newSettings = DashboardSettings(
            speedUnit = speedUnit,
            wheelDiameterMm = wheelDiam.toIntOrNull()?.coerceIn(100, 2000) ?: 660,
            gearRatio = gearRatio.toFloatOrNull()?.coerceIn(0.1f, 100f) ?: 1.0f,
            maxSpeedDisplay = maxSpeed.toIntOrNull()?.coerceIn(10, 1000) ?: 100,
            maxCurrentA = maxCurrent.toIntOrNull()?.coerceIn(10, 2000) ?: 800
        )
        component.updateSettings(newSettings)
        dirty = false
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // -- Speed & Gauges section --
        Text(
            "Speed & Gauges",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("Speed Display", style = MaterialTheme.typography.labelLarge)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    SpeedUnit.entries.forEach { unit ->
                        val sel = speedUnit == unit
                        FilterChip(
                            selected = sel,
                            onClick = { speedUnit = unit; dirty = true },
                            label = { Text(unit.label) }
                        )
                    }
                }

                if (speedUnit != SpeedUnit.RPM) {
                    OutlinedTextField(
                        value = wheelDiam,
                        onValueChange = { wheelDiam = it.filter { c -> c.isDigit() }; dirty = true },
                        label = { Text("Wheel Diameter (mm)") },
                        placeholder = { Text("660") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text(
                        "Common: 16\"=406, 20\"=508, 26\"=660, 29\"=736",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    OutlinedTextField(
                        value = gearRatio,
                        onValueChange = { v ->
                            if (v.isEmpty() || v.matches(Regex("^\\d*\\.?\\d*$"))) {
                                gearRatio = v; dirty = true
                            }
                        },
                        label = { Text("Gear Ratio (motor:wheel)") },
                        placeholder = { Text("1.0") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text(
                        "1.0 for hub motor / direct drive",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    OutlinedTextField(
                        value = maxSpeed,
                        onValueChange = { maxSpeed = it.filter { c -> c.isDigit() }; dirty = true },
                        label = { Text("Max Speed Display (${speedUnit.suffix})") },
                        placeholder = { Text("100") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                OutlinedTextField(
                    value = maxCurrent,
                    onValueChange = { maxCurrent = it.filter { c -> c.isDigit() }; dirty = true },
                    label = { Text("Max Phase Current (A)") },
                    placeholder = { Text("800") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Text(
                    "Kelly KLS: 150-800A typical",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                if (dirty) {
                    Button(
                        onClick = { save() },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Save Settings")
                    }
                }
            }
        }

        // -- BMS section --
        Text(
            "BMS (Battery Management)",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("BMS Type", style = MaterialTheme.typography.labelLarge)

                var expanded by remember { mutableStateOf(false) }
                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = it }
                ) {
                    OutlinedTextField(
                        value = bmsType.label,
                        onValueChange = {},
                        readOnly = true,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
                        modifier = Modifier.fillMaxWidth().menuAnchor(MenuAnchorType.PrimaryNotEditable)
                    )
                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        BmsType.entries.forEach { type ->
                            DropdownMenuItem(
                                text = { Text(type.label) },
                                onClick = {
                                    component.setBmsType(type)
                                    expanded = false
                                }
                            )
                        }
                    }
                }

                if (bmsType != BmsType.NONE) {
                    // Status
                    Text(
                        text = bmsStatus,
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (bmsConnected) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    if (bmsConnected) {
                        // Show BMS data summary when connected
                        BmsDataSummary(bmsData)

                        Button(
                            onClick = { component.disconnectBms() },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.error
                            )
                        ) {
                            Text("Disconnect BMS")
                        }
                    } else {
                        // Scan and connect UI
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = {
                                    if (isScanning) component.stopScan()
                                    else component.startScan()
                                },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                if (isScanning) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(16.dp),
                                        strokeWidth = 2.dp,
                                        color = MaterialTheme.colorScheme.onPrimary
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    Text("Stop Scan")
                                } else {
                                    Text("Scan for ${bmsType.label}")
                                }
                            }
                        }

                        // Discovered devices list
                        if (discoveredDevices.isNotEmpty()) {
                            Text(
                                "Discovered Devices:",
                                style = MaterialTheme.typography.labelLarge
                            )
                            discoveredDevices.forEach { device ->
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { component.connectBms(device.address) },
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                                    )
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                device.name,
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Text(
                                                device.address,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        TextButton(onClick = { component.connectBms(device.address) }) {
                                            Text("Connect")
                                        }
                                    }
                                }
                            }
                        } else if (!isScanning) {
                            Text(
                                "Tap Scan to find nearby ${bmsType.label} devices",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))
    }
}

@Composable
private fun BmsDataSummary(bmsData: com.kelly.app.domain.model.BmsData) {
    if (!bmsData.isConnected) return

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text("BMS Data", style = MaterialTheme.typography.labelLarge)

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                BmsValue("Voltage", "%.1f V".format(bmsData.voltage))
                BmsValue("Current", "%.1f A".format(bmsData.current))
                BmsValue("Power", "%.0f W".format(bmsData.power))
                BmsValue("SOC", "%.0f%%".format(bmsData.soc))
            }

            if (bmsData.cellVoltages.isNotEmpty()) {
                Text(
                    "Cells: ${bmsData.cellVoltages.size}  " +
                            "Min: %.3f V  Max: %.3f V  Delta: %.0f mV".format(
                                bmsData.cellVoltages.min(),
                                bmsData.cellVoltages.max(),
                                (bmsData.cellVoltages.max() - bmsData.cellVoltages.min()) * 1000
                            ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (bmsData.temperatures.isNotEmpty()) {
                Text(
                    "Temps: ${bmsData.temperatures.joinToString { "%.1f".format(it) + "\u00B0C" }}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun BmsValue(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold
        )
    }
}
