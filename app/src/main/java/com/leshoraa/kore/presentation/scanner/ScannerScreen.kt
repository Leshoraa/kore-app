package com.leshoraa.kore.presentation.scanner

import android.annotation.SuppressLint
import android.bluetooth.BluetoothProfile
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@SuppressLint("MissingPermission")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScannerScreen(
    viewModel: BleScannerViewModel,
    onEnableBluetooth: () -> Unit = {},
    onDeviceSelected: () -> Unit,
    onNavigateBack: () -> Unit
) {
    val devices by viewModel.foundDevices.collectAsState()
    val isScanning by viewModel.isScanning.collectAsState()
    val connectionState by viewModel.connectionState.collectAsState()
    val isBluetoothEnabled by viewModel.isBluetoothEnabled.collectAsState()

    var deviceToConnect by remember { mutableStateOf<android.bluetooth.le.ScanResult?>(null) }
    var isConnectingByDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.startScan()
    }

    DisposableEffect(Unit) {
        onDispose {
            viewModel.stopScan()
        }
    }

    // Auto-navigate to dashboard once successfully connected
    LaunchedEffect(connectionState) {
        if (connectionState == BluetoothProfile.STATE_CONNECTED) {
            isConnectingByDialog = false
            deviceToConnect = null
            onDeviceSelected()
        } else if (connectionState == BluetoothProfile.STATE_DISCONNECTED && isConnectingByDialog) {
            isConnectingByDialog = false
        }
    }

    if (deviceToConnect != null) {
        val targetName = deviceToConnect?.device?.name ?: "Unknown Device"
        val isConnecting = isConnectingByDialog || connectionState == BluetoothProfile.STATE_CONNECTING

        AlertDialog(
            onDismissRequest = {
                if (!isConnecting) {
                    deviceToConnect = null
                }
            },
            title = {
                Text(if (isConnecting) "Connecting..." else "Connect Device")
            },
            text = {
                if (isConnecting) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(28.dp),
                            strokeWidth = 3.dp,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Column {
                            Text(
                                text = "Connecting to $targetName",
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Text(
                                text = "Please wait...",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                } else {
                    Text("Do you want to connect to $targetName?")
                }
            },
            confirmButton = {
                if (!isConnecting) {
                    Button(onClick = {
                        val device = deviceToConnect!!
                        isConnectingByDialog = true
                        viewModel.connect(device.device.address, device.device.name)
                    }) {
                        Text("Connect")
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    if (isConnecting) {
                        viewModel.disconnect()
                        isConnectingByDialog = false
                    }
                    deviceToConnect = null
                }) {
                    Text("Cancel")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("BLE Scanner", style = MaterialTheme.typography.titleMedium) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(
                        onClick = { viewModel.startScan() },
                        enabled = isBluetoothEnabled
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                    }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding)) {
            if (!isBluetoothEnabled) {
                Box(modifier = Modifier.padding(16.dp)) {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer,
                            contentColor = MaterialTheme.colorScheme.onErrorContainer
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Bluetooth, contentDescription = null)
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = "Bluetooth is off",
                                modifier = Modifier.weight(1f),
                                style = MaterialTheme.typography.bodyMedium
                            )
                            TextButton(onClick = onEnableBluetooth) {
                                Text("ENABLE")
                            }
                        }
                    }
                }
            }

            if (isScanning) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }

            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(devices) { result ->
                    DeviceItem(
                        name = result.device.name ?: "Unknown Device",
                        address = result.device.address,
                        rssi = result.rssi,
                        onClick = {
                            deviceToConnect = result
                        }
                    )
                    HorizontalDivider()
                }
            }
        }
    }
}

@Composable
fun DeviceItem(name: String, address: String, rssi: Int, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            Icons.Default.Bluetooth,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(32.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = name, style = MaterialTheme.typography.titleMedium)
            Text(
                text = address, 
                style = MaterialTheme.typography.labelSmall, 
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Text(text = "$rssi dBm", style = MaterialTheme.typography.labelMedium)
    }
}
