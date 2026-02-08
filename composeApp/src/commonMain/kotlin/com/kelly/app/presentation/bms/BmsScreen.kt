package com.kelly.app.presentation.bms

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kelly.app.domain.model.BmsData
import kotlin.math.abs
import kotlin.math.roundToInt

@Composable
fun BmsScreen(component: BmsComponent) {
    val bmsData by component.bmsData.collectAsState()
    val isConnected by component.isConnected.collectAsState()

    if (!isConnected) {
        NotConnectedView()
    } else {
        ConnectedView(bmsData)
    }
}

@Composable
private fun NotConnectedView() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "BMS not connected \u2014 configure in Settings",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun ConnectedView(data: BmsData) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp),
        contentPadding = PaddingValues(vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Summary row
        item { SummarySection(data) }

        // Energy section
        item { EnergySection(data) }

        // Cell voltages section
        if (data.cellVoltages.isNotEmpty()) {
            item { CellVoltagesSection(data.cellVoltages) }
        }

        // Temperatures section
        if (data.temperatures.isNotEmpty()) {
            item { TemperaturesSection(data.temperatures) }
        }
    }
}

// =============================================================================
// Summary
// =============================================================================

@Composable
private fun SummarySection(data: BmsData) {
    SectionCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            SummaryItem("Voltage", "%.1f V".format(data.voltage))
            SummaryItem("Current", "%.1f A".format(data.current))
            SummaryItem("Power", "%.0f W".format(data.power))
            SummaryItem("SOC", "%.0f%%".format(data.soc))
        }
    }
}

@Composable
private fun SummaryItem(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(2.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

// =============================================================================
// Energy
// =============================================================================

@Composable
private fun EnergySection(data: BmsData) {
    SectionCard {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                "Energy",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                SummaryItem("Charge", "%.1f Ah".format(data.charge))
                SummaryItem("Capacity", "%.1f Ah".format(data.capacity))
                if (data.numCycles > 0) {
                    SummaryItem("Cycles", data.numCycles.toString())
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally)
            ) {
                SwitchIndicator("Charge", data.chargeEnabled)
                SwitchIndicator("Discharge", data.dischargeEnabled)
            }
        }
    }
}

@Composable
private fun SwitchIndicator(label: String, enabled: Boolean) {
    val color = if (enabled) Color(0xFF4CAF50) else MaterialTheme.colorScheme.error
    val text = if (enabled) "ON" else "OFF"
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Surface(
            shape = RoundedCornerShape(4.dp),
            color = color.copy(alpha = 0.15f)
        ) {
            Text(
                text,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = color
            )
        }
    }
}

// =============================================================================
// Cell Voltages
// =============================================================================

@Composable
private fun CellVoltagesSection(cells: List<Float>) {
    val minV = cells.min()
    val maxV = cells.max()
    val deltaV = maxV - minV
    val deltaMv = (deltaV * 1000f).roundToInt()

    SectionCard {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Cell Voltages (${cells.size})",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    "\u0394 ${deltaMv} mV",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = deltaColor(deltaMv)
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                Text(
                    "Min: %.3f V".format(minV),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    "Max: %.3f V".format(maxV),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Cell grid
            val columns = if (cells.size <= 8) 4 else if (cells.size <= 16) 4 else 5
            val rows = (cells.size + columns - 1) / columns
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                for (row in 0 until rows) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        for (col in 0 until columns) {
                            val idx = row * columns + col
                            if (idx < cells.size) {
                                CellItem(
                                    modifier = Modifier.weight(1f),
                                    index = idx + 1,
                                    voltage = cells[idx],
                                    minV = minV,
                                    maxV = maxV
                                )
                            } else {
                                Spacer(Modifier.weight(1f))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CellItem(
    modifier: Modifier,
    index: Int,
    voltage: Float,
    minV: Float,
    maxV: Float
) {
    val range = maxV - minV
    val color = if (range < 0.001f) {
        Color(0xFF4CAF50) // All balanced
    } else {
        val deviation = abs(voltage - (minV + maxV) / 2f) / (range / 2f)
        when {
            deviation < 0.3f -> Color(0xFF4CAF50)  // Green — balanced
            deviation < 0.7f -> Color(0xFFF9A825)   // Yellow — slight outlier
            else -> Color(0xFFD32F2F)                // Red — significant outlier
        }
    }

    // Bar fill fraction: 0.0 at minV, 1.0 at maxV
    val fill = if (range < 0.001f) 1f else ((voltage - minV) / range).coerceIn(0f, 1f)

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(4.dp))
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(4.dp))
            .padding(4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            "#$index",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 9.sp
        )
        Spacer(Modifier.height(2.dp))
        // Color bar
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(4.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(fill)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(2.dp))
                    .background(color)
            )
        }
        Spacer(Modifier.height(2.dp))
        Text(
            "%.3f".format(voltage),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface,
            fontSize = 10.sp
        )
    }
}

// =============================================================================
// Temperatures
// =============================================================================

@Composable
private fun TemperaturesSection(temps: List<Float>) {
    SectionCard {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                "Temperatures (${temps.size})",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                temps.forEachIndexed { idx, temp ->
                    TemperatureItem(
                        modifier = Modifier.weight(1f),
                        index = idx + 1,
                        temp = temp
                    )
                }
            }
        }
    }
}

@Composable
private fun TemperatureItem(modifier: Modifier, index: Int, temp: Float) {
    val color = when {
        temp >= 60f -> Color(0xFFD32F2F)   // Red
        temp >= 40f -> Color(0xFFF9A825)    // Yellow
        else -> Color(0xFF4CAF50)            // Green
    }

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .background(color.copy(alpha = 0.1f))
            .padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            "T$index",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(2.dp))
        Text(
            "%.0f\u00B0C".format(temp),
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = color
        )
    }
}

// =============================================================================
// Helpers
// =============================================================================

@Composable
private fun SectionCard(content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            content = content
        )
    }
}

private fun deltaColor(deltaMv: Int): Color = when {
    deltaMv <= 10 -> Color(0xFF4CAF50)  // Green — well balanced
    deltaMv <= 30 -> Color(0xFFF9A825)   // Yellow — acceptable
    else -> Color(0xFFD32F2F)             // Red — needs attention
}
