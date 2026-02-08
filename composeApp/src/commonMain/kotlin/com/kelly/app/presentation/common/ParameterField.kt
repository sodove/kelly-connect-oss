package com.kelly.app.presentation.common

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.kelly.protocol.ParamSize
import com.kelly.protocol.ParameterDef
import com.kelly.protocol.SafetyLevel

/**
 * A single parameter field rendered as a Card with safety indicators.
 *
 * - Left colored border: transparent for SAFE/READ_ONLY, orange for CAUTION, red for DANGEROUS
 * - Name row with optional safety dot indicator
 * - Description/tips below the name
 * - Value field on the right side (checkbox for BIT, text field for others)
 * - READ_ONLY parameters use disabled styling
 */
@Composable
fun ParameterField(
    param: ParameterDef,
    value: String,
    onValueChange: (String) -> Unit,
    onFocus: () -> Unit
) {
    val isReadOnly = param.safety == SafetyLevel.READ_ONLY
    val borderColor = when (param.safety) {
        SafetyLevel.CAUTION -> Color(0xFFFF9800) // Orange
        SafetyLevel.DANGEROUS -> Color(0xFFF44336) // Red
        else -> Color.Transparent
    }
    val cardColors = if (isReadOnly) {
        CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    } else {
        CardDefaults.cardColors()
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = cardColors
    ) {
        Row(modifier = Modifier.fillMaxWidth()) {
            // Left colored border strip
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .fillMaxHeight()
                    .background(borderColor)
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Left side: name + description
                Column(modifier = Modifier.weight(1f)) {
                    // Name row with safety indicator
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        // Safety indicator dot
                        when (param.safety) {
                            SafetyLevel.CAUTION -> {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFFFF9800))
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                            }
                            SafetyLevel.DANGEROUS -> {
                                Text(
                                    text = "\u26A0",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color(0xFFF44336)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFFF44336))
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                            }
                            else -> { /* No indicator for SAFE and READ_ONLY */ }
                        }

                        Text(
                            text = param.name,
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (isReadOnly)
                                MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                            else
                                MaterialTheme.colorScheme.onSurface
                        )
                    }

                    // Description / tips below the name
                    if (param.tips.isNotBlank()) {
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = param.tips,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Right side: value field
                val effectiveEditable = param.editable && !isReadOnly

                if (param.size == ParamSize.BIT) {
                    // Checkbox for bit parameters
                    val checked = value == "1"
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(
                            checked = checked,
                            onCheckedChange = { isChecked ->
                                if (effectiveEditable) {
                                    onValueChange(if (isChecked) "1" else "0")
                                }
                                onFocus()
                            },
                            enabled = effectiveEditable
                        )
                        Text(
                            text = if (checked) "Yes" else "No",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (isReadOnly)
                                MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                            else
                                MaterialTheme.colorScheme.onSurface
                        )
                    }
                } else {
                    // Text field for byte/word parameters
                    var textValue by remember(value) { mutableStateOf(value) }

                    OutlinedTextField(
                        value = textValue,
                        onValueChange = { newVal ->
                            textValue = newVal
                            onValueChange(newVal)
                        },
                        modifier = Modifier.width(120.dp).onFocusChanged { if (it.isFocused) onFocus() },
                        enabled = effectiveEditable,
                        singleLine = true,
                        textStyle = MaterialTheme.typography.bodyMedium,
                        colors = if (effectiveEditable) {
                            OutlinedTextFieldDefaults.colors()
                        } else {
                            OutlinedTextFieldDefaults.colors(
                                disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                                disabledTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    )
                }
            }
        }
    }
}
