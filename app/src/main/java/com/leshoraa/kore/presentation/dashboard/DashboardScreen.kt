package com.leshoraa.kore.presentation.dashboard

import android.bluetooth.BluetoothProfile
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BluetoothConnected
import androidx.compose.material.icons.filled.BluetoothDisabled
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
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
    onNavigateToScanner: () -> Unit,
    onNavigateToSettings: () -> Unit
) {
    val connectionState by viewModel.connectionState.collectAsState()
    val logs by viewModel.logs.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("KoRe Dashboard") },
                actions = {
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(16.dp)
        ) {
            ConnectionStatusCard(
                state = connectionState,
                onConnectClick = onNavigateToScanner,
                onDisconnectClick = { viewModel.disconnect() }
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Text(
                text = "Live Event Log",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.secondary
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            LogList(logs = logs)
        }
    }
}

@Composable
fun ConnectionStatusCard(state: Int, onConnectClick: () -> Unit, onDisconnectClick: () -> Unit) {
    val isConnected = state == BluetoothProfile.STATE_CONNECTED
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = if (isConnected) Icons.Default.BluetoothConnected else Icons.Default.BluetoothDisabled,
                contentDescription = null,
                tint = if (isConnected) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.error,
                modifier = Modifier.size(48.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (isConnected) "Engine Connected" else "Engine Disconnected",
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = if (isConnected) "ESP32-S3 active" else "No hardware linked",
                    style = MaterialTheme.typography.bodySmall
                )
            }
            Button(onClick = if (isConnected) onDisconnectClick else onConnectClick) {
                Text(if (isConnected) "Stop" else "Link")
            }
        }
    }
}

@Composable
fun LogList(logs: List<NotificationEvent>) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surface
    ) {
        if (logs.isEmpty()) {
            Box(contentAlignment = Alignment.Center) {
                Text("Waiting for events...", color = Color.Gray)
            }
        } else {
            LazyColumn(contentPadding = PaddingValues(8.dp)) {
                items(logs) { event ->
                    LogItem(event)
                    HorizontalDivider(thickness = 0.5.dp, color = Color.Gray.copy(alpha = 0.5f))
                }
            }
        }
    }
}

@Composable
fun LogItem(event: NotificationEvent) {
    val timeFormatter = remember { SimpleDateFormat("HH:mm:ss", Locale.getDefault()) }
    
    Column(modifier = Modifier.padding(vertical = 8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = event.packageName.substringAfterLast("."),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = timeFormatter.format(Date(event.postTimeMillis)),
                style = MaterialTheme.typography.labelSmall,
                color = Color.Gray
            )
        }
        Text(text = event.title, style = MaterialTheme.typography.bodyMedium)
        if (event.text.isNotEmpty()) {
            Text(
                text = event.text,
                style = MaterialTheme.typography.bodySmall,
                color = Color.LightGray,
                maxLines = 1
            )
        }
    }
}
