package com.example.sumoroboto.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.BluetoothConnected
import androidx.compose.material.icons.filled.BluetoothDisabled
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.sumoroboto.data.bluetooth.BluetoothConnectionState
import com.example.sumoroboto.data.model.BatteryState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TopSumoAppBar(
    connectionState: BluetoothConnectionState,
    batteryState: BatteryState,
    onOpenDeviceSheet: () -> Unit,
    onDisconnect: () -> Unit
) {
    TopAppBar(
        title = {
            Column {
                Text(
                    text = "Mini Sumo RC",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                // Connection status indicator chip
                ConnectionStatusChip(
                    state = connectionState,
                    onClick = {
                        if (connectionState is BluetoothConnectionState.Connected) {
                            onOpenDeviceSheet()
                        } else {
                            onOpenDeviceSheet()
                        }
                    }
                )
            }
        },
        actions = {
            // Live Battery Widget
            BatteryWidget(batteryState = batteryState)

            Spacer(modifier = Modifier.width(4.dp))

            // Bluetooth action button
            when (connectionState) {
                is BluetoothConnectionState.Connected -> {
                    IconButton(onClick = onDisconnect) {
                        Icon(
                            imageVector = Icons.Default.BluetoothConnected,
                            contentDescription = "Disconnect Bluetooth",
                            tint = Color(0xFF4CAF50)
                        )
                    }
                }
                is BluetoothConnectionState.Connecting -> {
                    IconButton(onClick = onDisconnect) {
                        Icon(
                            imageVector = Icons.Default.Bluetooth,
                            contentDescription = "Connecting...",
                            tint = Color(0xFFFFB300)
                        )
                    }
                }
                else -> {
                    IconButton(onClick = onOpenDeviceSheet) {
                        Icon(
                            imageVector = Icons.Default.BluetoothDisabled,
                            contentDescription = "Connect Bluetooth",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    )
}

@Composable
fun ConnectionStatusChip(
    state: BluetoothConnectionState,
    onClick: () -> Unit
) {
    val (dotColor, label) = when (state) {
        is BluetoothConnectionState.Connected -> Pair(
            Color(0xFF4CAF50), // Green
            state.deviceName
        )
        is BluetoothConnectionState.Connecting -> Pair(
            Color(0xFFFFB300), // Amber
            "Connecting to ${state.deviceName}..."
        )
        is BluetoothConnectionState.Disconnected -> Pair(
            Color(0xFFE53935), // Red
            "Disconnected - Tap to pair"
        )
        is BluetoothConnectionState.Error -> Pair(
            Color(0xFFE53935), // Red
            "Error: ${state.message}"
        )
    }

    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .padding(vertical = 2.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(dotColor)
        )
        Text(
            text = label,
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
