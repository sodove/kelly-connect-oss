package com.kelly.app.presentation.common

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.kelly.app.domain.model.ConnectionState

@Composable
fun StatusBar(
    connectionState: ConnectionState,
    onDisconnect: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceContainerHighest,
        contentColor = MaterialTheme.colorScheme.onSurface
    ) {
        Row(
            modifier = Modifier
                .windowInsetsPadding(WindowInsets.statusBars)
                .padding(horizontal = 12.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Kelly Connect",
                style = MaterialTheme.typography.labelMedium,
                modifier = Modifier.weight(1f)
            )

            when (connectionState) {
                is ConnectionState.Connected -> {
                    Text(
                        text = "${connectionState.moduleName} v${connectionState.softwareVersion}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    TextButton(onClick = onDisconnect) {
                        Text("Disconnect", style = MaterialTheme.typography.labelSmall)
                    }
                }
                is ConnectionState.Disconnected -> {
                    Text(
                        text = "Disconnected",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
                else -> {}
            }
        }
    }
}
