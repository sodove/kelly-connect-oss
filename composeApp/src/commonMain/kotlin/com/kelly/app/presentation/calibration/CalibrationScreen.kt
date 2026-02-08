package com.kelly.app.presentation.calibration

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.kelly.app.presentation.common.ParameterField
import com.kelly.protocol.ParamCategory
import com.kelly.protocol.ParamSize
import com.kelly.protocol.ParameterCodec
import com.kelly.protocol.ParameterDef

/**
 * Maps ParamCategory enum values to human-readable display names.
 */
private fun ParamCategory.displayName(): String = when (this) {
    ParamCategory.GENERAL -> "General"
    ParamCategory.PROTECTION -> "Protection"
    ParamCategory.THROTTLE -> "Throttle"
    ParamCategory.BRAKING -> "Braking"
    ParamCategory.SPEED -> "Speed & Frequency"
    ParamCategory.MOTOR -> "Motor Configuration"
    ParamCategory.PID_TUNING -> "PID Tuning"
    ParamCategory.ADVANCED -> "Advanced"
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun CalibrationScreen(component: CalibrationComponent) {
    val calibrationData by component.calibrationData.collectAsState()
    val isLoading by component.isLoading.collectAsState()
    val statusMessage by component.statusMessage.collectAsState()
    val selectedTip by component.selectedTip.collectAsState()

    Column(modifier = Modifier.fillMaxSize()) {
        // Tips bar
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.surfaceVariant
        ) {
            Text(
                text = selectedTip.ifEmpty { "TIPS: Select a parameter to see description" },
                modifier = Modifier.padding(8.dp),
                style = MaterialTheme.typography.bodySmall
            )
        }

        // Loading indicator
        if (isLoading) {
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
        }

        // Status message
        statusMessage?.let { msg ->
            Text(
                text = msg,
                modifier = Modifier.padding(horizontal = 8.dp),
                style = MaterialTheme.typography.bodySmall,
                color = if (msg.startsWith("Error") || msg.startsWith("Read error") || msg.startsWith("Write error"))
                    MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
            )
        }

        // Parameters grouped by category
        val data = calibrationData
        if (data != null) {
            val visibleParams = data.parameters.filter { it.visible }
            val groupedParams = visibleParams.groupBy { it.category }
            // Maintain consistent ordering based on ParamCategory enum ordinal
            val orderedCategories = ParamCategory.entries.filter { it in groupedParams.keys }

            LazyColumn(
                modifier = Modifier.fillMaxWidth().weight(1f),
                contentPadding = PaddingValues(8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                orderedCategories.forEach { category ->
                    val params = groupedParams[category] ?: emptyList()

                    // Sticky header for the category
                    stickyHeader(key = category.name) {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            tonalElevation = 2.dp
                        ) {
                            Text(
                                text = category.displayName(),
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    // Parameters in this category
                    items(params, key = { "${it.offset}_${it.position}_${it.name}" }) { param ->
                        val value = ParameterCodec.readParam(
                            data.dataValue, param.offset, param.size, param.position, param.type
                        )
                        ParameterField(
                            param = param,
                            value = value,
                            onValueChange = { newValue -> component.updateParameter(param, newValue) },
                            onFocus = { component.selectParameter(param) }
                        )
                    }
                }
            }
        } else {
            Box(
                modifier = Modifier.fillMaxWidth().weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Text("Press Read to load calibration data")
            }
        }

        // Bottom buttons
        Row(
            modifier = Modifier.fillMaxWidth().padding(8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = { component.readCalibration() },
                enabled = !isLoading,
                modifier = Modifier.weight(1f)
            ) { Text("Read") }

            Button(
                onClick = { component.writeCalibration() },
                enabled = !isLoading && calibrationData != null,
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error
                )
            ) { Text("Write") }
        }
    }
}
