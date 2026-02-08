package com.kelly.app.presentation.monitor

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.kelly.app.domain.model.MonitorData
import com.kelly.protocol.MonitorDefinitions

/**
 * Descriptive labels for monitor parameter names.
 */
private val descriptiveLabels = mapOf(
    "TPS Pedel" to "Throttle Position",
    "Brake Pedel" to "Brake Position",
    "Brake Switch" to "Brake Switch",
    "Foot Switch" to "Foot Switch",
    "Forward Switch" to "Forward Switch",
    "Reversed" to "Reverse Switch",
    "Hall A" to "Hall Sensor A",
    "Hall B" to "Hall Sensor B",
    "Hall C" to "Hall Sensor C",
    "B+ Volt" to "Battery Voltage",
    "Motor Temp" to "Motor Temperature",
    "Controller Temp" to "Controller Temperature",
    "Setting Dir" to "Set Direction",
    "Actual Dir" to "Actual Direction",
    "Brake Switch2" to "Brake Switch 2",
    "Low Speed" to "Low Speed Mode",
    "Motor Speed" to "Motor Speed",
    "Phase Current" to "Phase Current",
    "Error Status" to "Error Status"
)

/**
 * Unit suffixes for monitor parameters.
 */
private val unitSuffixes = mapOf(
    "Motor Speed" to " RPM",
    "Motor Temp" to " \u00B0C",
    "Controller Temp" to " \u00B0C",
    "B+ Volt" to " V",
    "Phase Current" to " A"
)

/**
 * Group definitions for organizing monitor parameters.
 */
private data class MonitorGroup(
    val title: String,
    val paramNames: List<String>
)

private val monitorGroups = listOf(
    MonitorGroup("Readings", listOf(
        "TPS Pedel", "Brake Pedel", "B+ Volt", "Motor Temp", "Controller Temp",
        "Motor Speed", "Phase Current"
    )),
    MonitorGroup("Switches", listOf(
        "Forward Switch", "Reversed", "Brake Switch", "Brake Switch2",
        "Foot Switch", "Low Speed", "Setting Dir", "Actual Dir"
    )),
    MonitorGroup("Hall Sensors", listOf(
        "Hall A", "Hall B", "Hall C"
    ))
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MonitorScreen(component: MonitorComponent) {
    val monitorData by component.monitorData.collectAsState()

    Column(modifier = Modifier.fillMaxSize()) {
        // Communication error banner
        val commError = monitorData.communicationError
        if (commError != null) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = Color(0xFFFFF3E0) // amber background
            ) {
                Text(
                    text = commError,
                    modifier = Modifier.padding(12.dp),
                    color = Color(0xFFE65100),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // Error status section - each error on a separate line
        if (monitorData.errorMessages.isNotEmpty()) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.errorContainer
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = "Active Errors",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    monitorData.errorMessages.forEach { error ->
                        Text(
                            text = error,
                            modifier = Modifier.padding(start = 8.dp, top = 2.dp),
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
        }

        // Grouped monitor values
        LazyColumn(
            modifier = Modifier.fillMaxWidth().weight(1f),
            contentPadding = PaddingValues(8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            monitorGroups.forEach { group ->
                // Section header
                item(key = "header_${group.title}") {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        tonalElevation = 2.dp
                    ) {
                        Text(
                            text = group.title,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Parameter cards in a flow of items
                items(group.paramNames, key = { "param_$it" }) { paramName ->
                    val rawValue = monitorData.values[paramName] ?: "-"
                    val label = descriptiveLabels[paramName] ?: paramName
                    val unit = unitSuffixes[paramName] ?: ""
                    val displayValue = if (rawValue != "-") "$rawValue$unit" else rawValue

                    MonitorValueItem(
                        label = label,
                        value = displayValue,
                        paramName = paramName
                    )
                }
            }
        }

    }
}

@Composable
private fun MonitorValueItem(
    label: String,
    value: String,
    paramName: String
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}
