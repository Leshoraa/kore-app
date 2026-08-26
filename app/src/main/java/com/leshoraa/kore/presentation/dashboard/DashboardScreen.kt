package com.leshoraa.kore.presentation.dashboard

import android.bluetooth.BluetoothProfile
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BluetoothConnected
import androidx.compose.material.icons.filled.BluetoothDisabled
import androidx.compose.material.icons.filled.BrightnessLow
import androidx.compose.material.icons.filled.BrightnessMedium
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.leshoraa.kore.domain.model.NotificationEvent
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel,
    isDarkTheme: Boolean = false,
    onToggleTheme: () -> Unit = {},
    onNavigateToScanner: () -> Unit
) {
    val connectionState by viewModel.connectionState.collectAsState()
    val connectedDeviceName by viewModel.connectedDeviceName.collectAsState()
    val brightness by viewModel.brightness.collectAsState()
    val logs by viewModel.logs.collectAsState()
    
    val testTitle by viewModel.testTitle.collectAsState()
    val testMessage by viewModel.testMessage.collectAsState()

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("KoRe Dashboard", style = MaterialTheme.typography.titleMedium) },
                actions = {
                    IconButton(onClick = onToggleTheme) {
                        Icon(
                            if (isDarkTheme) Icons.Default.LightMode else Icons.Default.DarkMode,
                            contentDescription = "Toggle Theme"
                        )
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            ConnectionStatusCard(
                state = connectionState,
                deviceName = connectedDeviceName,
                onConnectClick = onNavigateToScanner,
                onDisconnectClick = { viewModel.disconnect() }
            )
            
            Spacer(modifier = Modifier.height(12.dp))

            BrightnessControlCard(
                brightness = brightness,
                connectionState = connectionState,
                onBrightnessChange = { viewModel.onBrightnessChange(it) },
                onBrightnessChangeFinished = { viewModel.onBrightnessChangeFinished() }
            )

            Spacer(modifier = Modifier.height(12.dp))
            
            Text(
                text = "Live Event Log",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.secondary
            )
            
            Spacer(modifier = Modifier.height(8.dp))

            TestMessageSection(
                title = testTitle,
                message = testMessage,
                onTitleChange = { viewModel.onTestTitleChange(it) },
                onMessageChange = { viewModel.onTestMessageChange(it) },
                onSendClick = { viewModel.sendTestMessage() }
            )

            Spacer(modifier = Modifier.height(12.dp))
            
            LogList(logs = logs)
        }
    }
}

@Composable
fun ConnectionStatusCard(
    state: Int, 
    deviceName: String?, 
    onConnectClick: () -> Unit, 
    onDisconnectClick: () -> Unit
) {
    val isConnected = state == BluetoothProfile.STATE_CONNECTED
    val isConnecting = state == BluetoothProfile.STATE_CONNECTING
    
    OutlinedCard(
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                color = when {
                    isConnected -> MaterialTheme.colorScheme.primaryContainer
                    isConnecting -> MaterialTheme.colorScheme.tertiaryContainer
                    else -> MaterialTheme.colorScheme.errorContainer
                },
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier.size(48.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    if (isConnecting) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(22.dp),
                            strokeWidth = 2.5.dp,
                            color = MaterialTheme.colorScheme.tertiary
                        )
                    } else {
                        Icon(
                            imageVector = if (isConnected) Icons.Default.BluetoothConnected else Icons.Default.BluetoothDisabled,
                            contentDescription = null,
                            tint = if (isConnected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = when {
                        isConnected -> deviceName ?: "Connected"
                        isConnecting -> deviceName ?: "Connecting..."
                        else -> "Disconnected"
                    },
                    style = MaterialTheme.typography.titleSmall
                )
                Text(
                    text = when {
                        isConnected -> "Hardware active"
                        isConnecting -> "Connecting to device..."
                        else -> "No hardware linked"
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            when {
                isConnected -> {
                    TextButton(onClick = onDisconnectClick) {
                        Text("Stop")
                    }
                }
                isConnecting -> {
                    OutlinedButton(
                        onClick = onDisconnectClick,
                        contentPadding = PaddingValues(horizontal = 10.dp),
                        modifier = Modifier.height(34.dp)
                    ) {
                        Text("Cancel", style = MaterialTheme.typography.labelSmall)
                    }
                }
                else -> {
                    FilledTonalButton(onClick = onConnectClick, contentPadding = PaddingValues(horizontal = 12.dp)) {
                        Text("Link")
                    }
                }
            }
        }
    }
}

@Composable
fun BrightnessControlCard(
    brightness: Int,
    connectionState: Int,
    onBrightnessChange: (Int) -> Unit,
    onBrightnessChangeFinished: () -> Unit
) {
    val isConnected = connectionState == BluetoothProfile.STATE_CONNECTED
    val isConnecting = connectionState == BluetoothProfile.STATE_CONNECTING
    val percent = brightness
    
    OutlinedCard(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (brightness > 66) Icons.Default.LightMode else if (brightness > 33) Icons.Default.BrightnessMedium else Icons.Default.BrightnessLow,
                        contentDescription = "Brightness",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Display Brightness",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                Text(
                    text = "$percent%",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            Slider(
                value = brightness.toFloat(),
                onValueChange = { 
                    val value = it.toInt()
                    onBrightnessChange(if (value == 0) 1 else value)
                },
                onValueChangeFinished = onBrightnessChangeFinished,
                valueRange = 0f..100f,
                steps = 4,
                modifier = Modifier.fillMaxWidth()
            )

            if (!isConnected) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = if (isConnecting) 
                        "Connecting to KoRe device..." 
                    else 
                        "Hardware offline (saved preference will sync upon connection)",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun TestMessageSection(
    title: String,
    message: String,
    onTitleChange: (String) -> Unit,
    onMessageChange: (String) -> Unit,
    onSendClick: () -> Unit
) {
    var isExpanded by remember { mutableStateOf(false) }

    OutlinedCard(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { isExpanded = !isExpanded },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Push Test Notification",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Icon(
                    imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = if (isExpanded) "Collapse" else "Expand",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
            
            AnimatedVisibility(visible = isExpanded) {
                Column {
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = title,
                        onValueChange = onTitleChange,
                        label = { Text("Title") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.medium
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = message,
                        onValueChange = onMessageChange,
                        label = { Text("Message") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.medium
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    FilledTonalButton(
                        onClick = onSendClick,
                        modifier = Modifier.fillMaxWidth(),
                        enabled = title.isNotBlank() && message.isNotBlank()
                    ) {
                        Text("Send Notification")
                    }
                }
            }
        }
    }
}

@Composable
fun LogList(logs: List<NotificationEvent>) {
    if (logs.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(160.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                "Waiting for events...",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            items(logs) { event ->
                LogItem(event)
            }
        }
    }
}

@Composable
fun LogItem(event: NotificationEvent) {
    val timeFormatter = remember { SimpleDateFormat("HH:mm:ss", Locale.getDefault()) }
    
    ListItem(
        headlineContent = { Text(event.title) },
        supportingContent = { 
            if (event.text.isNotEmpty()) {
                Text(event.text, maxLines = 1)
            }
        },
        overlineContent = {
            Text("${event.appName} • ${timeFormatter.format(Date(event.postTimeMillis))}")
        },
        colors = ListItemDefaults.colors(
            containerColor = Color.Transparent
        )
    )
    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), thickness = 0.5.dp)
}
