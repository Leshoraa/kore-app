package com.leshoraa.kore.presentation.settings

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.leshoraa.kore.R
import com.leshoraa.kore.presentation.theme.AppPalette

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    selectedPalette: AppPalette = AppPalette.FOREST,
    onPaletteChange: (AppPalette) -> Unit = {},
    onNavigateBack: (() -> Unit)? = null,
    onNavigateToFilters: () -> Unit = {}
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings_title)) },
                navigationIcon = {
                    if (onNavigateBack != null) {
                        IconButton(onClick = onNavigateBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.common_back))
                        }
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            // Section: Appearance
            Text(
                text = stringResource(R.string.settings_theme_palette),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                AppPalette.values().forEach { palette ->
                    ColorCircle(
                        color = palette.primaryColor,
                        isSelected = selectedPalette == palette,
                        onClick = { onPaletteChange(palette) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Section: Hardware Configuration
            Text(
                text = stringResource(R.string.settings_section_connectivity),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(12.dp))

            SettingItem(
                title = stringResource(R.string.device_config_title),
                description = stringResource(R.string.device_config_desc),
                icon = Icons.Default.Router,
                onClick = { viewModel.openDialog() }
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Section: Notification Rules
            Text(
                text = stringResource(R.string.settings_section_notifications),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(12.dp))

            SettingItem(
                title = stringResource(R.string.rules_title),
                description = "Manage which apps can send notifications to KoRe.",
                icon = Icons.Default.FilterList,
                onClick = onNavigateToFilters
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Section: System Stability
            Text(
                text = stringResource(R.string.settings_stability_guards),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(12.dp))

            SettingItem(
                title = stringResource(R.string.settings_battery_optimization),
                description = stringResource(R.string.settings_battery_optimization_desc),
                icon = Icons.Default.BatteryChargingFull,
                onClick = { requestIgnoreBatteryOptimizations(context) }
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            SettingItem(
                title = stringResource(R.string.settings_notification_access),
                description = stringResource(R.string.settings_notification_access_desc),
                icon = Icons.Default.Notifications,
                onClick = { openNotificationListenerSettings(context) }
            )
        }
    }

    if (uiState.isDialogOpen) {
        DeviceConfigDialog(
            state = uiState,
            onStaSsidChange = viewModel::onStaSsidChanged,
            onStaPassChange = viewModel::onStaPassChanged,
            onApSsidChange = viewModel::onApSsidChanged,
            onApPassChange = viewModel::onApPassChanged,
            onBleNameChange = viewModel::onBleNameChanged,
            onToggleStaPass = viewModel::toggleStaPassVisibility,
            onToggleApPass = viewModel::toggleApPassVisibility,
            onRefresh = viewModel::refreshFromDevice,
            onSave = viewModel::saveConfig,
            onDismiss = viewModel::closeDialog
        )
    }
}

@Composable
fun DeviceConfigDialog(
    state: DeviceConfigUiState,
    onStaSsidChange: (String) -> Unit,
    onStaPassChange: (String) -> Unit,
    onApSsidChange: (String) -> Unit,
    onApPassChange: (String) -> Unit,
    onBleNameChange: (String) -> Unit,
    onToggleStaPass: () -> Unit,
    onToggleApPass: () -> Unit,
    onRefresh: () -> Unit,
    onSave: () -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.9f),
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                // Header: Clean & Integrated
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 24.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = stringResource(R.string.device_config_title),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = if (state.isBleConnected) stringResource(R.string.status_ble_connected) else stringResource(R.string.status_ble_disconnected),
                            style = MaterialTheme.typography.labelSmall,
                            color = if (state.isBleConnected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                        )
                    }

                    Row {
                        IconButton(onClick = onRefresh, enabled = !state.isLoading) {
                            if (state.isLoading) {
                                CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                            } else {
                                Icon(Icons.Default.Refresh, contentDescription = stringResource(R.string.status_sync), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Default.Close, contentDescription = "Close", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }

                // Main Content
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(24.dp)
                ) {
                    // Feedback Messages (Integrated)
                    if (state.successMessage != null || state.errorMessage != null) {
                        val isError = state.errorMessage != null
                        Surface(
                            color = if (isError) MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.7f) else MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = if (isError) Icons.Default.Error else Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = if (isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = state.errorMessage ?: state.successMessage ?: "",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (isError) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }
                    }

                    // Section: Wi-Fi Router
                    ConfigSection(title = stringResource(R.string.section_wifi_sta), icon = Icons.Default.Wifi) {
                        CustomTextField(
                            value = state.staSsid,
                            onValueChange = onStaSsidChange,
                            label = stringResource(R.string.label_sta_ssid),
                            placeholder = stringResource(R.string.placeholder_sta_ssid)
                        )
                        CustomTextField(
                            value = state.staPass,
                            onValueChange = onStaPassChange,
                            label = stringResource(R.string.label_sta_pass),
                            placeholder = stringResource(R.string.label_keep_existing),
                            isPassword = true,
                            passwordVisible = state.staPassVisible,
                            onToggleVisibility = onToggleStaPass
                        )
                    }

                    // Section: Device Hotspot
                    ConfigSection(title = stringResource(R.string.section_kore_ap), icon = Icons.Default.WifiTethering) {
                        CustomTextField(
                            value = state.apSsid,
                            onValueChange = onApSsidChange,
                            label = stringResource(R.string.label_ap_ssid),
                            placeholder = stringResource(R.string.placeholder_ap_ssid)
                        )
                        CustomTextField(
                            value = state.apPass,
                            onValueChange = onApPassChange,
                            label = stringResource(R.string.label_ap_pass),
                            placeholder = stringResource(R.string.placeholder_ap_pass),
                            isPassword = true,
                            passwordVisible = state.apPassVisible,
                            onToggleVisibility = onToggleApPass
                        )
                    }

                    // Section: Bluetooth
                    ConfigSection(title = stringResource(R.string.section_bluetooth), icon = Icons.Default.Bluetooth) {
                        CustomTextField(
                            value = state.bleName,
                            onValueChange = onBleNameChange,
                            label = stringResource(R.string.label_ble_name),
                            placeholder = stringResource(R.string.placeholder_ble_name)
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))
                }

                // Actions: Cohesive Material 3 Buttons
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(
                        onClick = onDismiss,
                        enabled = !state.isSaving
                    ) {
                        Text(stringResource(R.string.btn_cancel))
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = onSave,
                        enabled = !state.isSaving && state.isBleConnected,
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 24.dp, vertical = 12.dp)
                    ) {
                        if (state.isSaving) {
                            CircularProgressIndicator(modifier = Modifier.size(18.dp), color = Color.White, strokeWidth = 2.dp)
                        } else {
                            Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(stringResource(R.string.btn_save_config))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ConfigSection(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(bottom = 2.dp)
        ) {
            Icon(
                icon, 
                contentDescription = null, 
                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f),
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)
            )
        }
        content()
    }
}

@Composable
private fun CustomTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    placeholder: String,
    isPassword: Boolean = false,
    passwordVisible: Boolean = false,
    onToggleVisibility: () -> Unit = {}
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label, style = MaterialTheme.typography.bodyMedium) },
        placeholder = { Text(placeholder, style = MaterialTheme.typography.bodyMedium) },
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        textStyle = MaterialTheme.typography.bodyLarge,
        colors = OutlinedTextFieldDefaults.colors(
            unfocusedContainerColor = Color.Transparent,
            focusedContainerColor = Color.Transparent,
            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
            focusedBorderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
            unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
        ),
        visualTransformation = if (isPassword && !passwordVisible) PasswordVisualTransformation() else VisualTransformation.None,
        trailingIcon = if (isPassword) {
            {
                IconButton(onClick = onToggleVisibility) {
                    Icon(
                        if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                }
            }
        } else null
    )
}

@Composable
fun ColorCircle(
    color: Color,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(48.dp)
            .clip(CircleShape)
            .background(color)
            .border(
                width = if (isSelected) 3.dp else 0.dp,
                color = if (isSelected) MaterialTheme.colorScheme.onSurface else Color.Transparent,
                shape = CircleShape
            )
            .clickable(onClick = onClick)
    ) {
        if (isSelected) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.align(Alignment.Center).size(24.dp)
            )
        }
    }
}

@Composable
fun SettingItem(
    title: String,
    description: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = title, style = MaterialTheme.typography.bodyLarge)
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

private fun requestIgnoreBatteryOptimizations(context: Context) {
    val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
        data = Uri.parse("package:${context.packageName}")
    }
    context.startActivity(intent)
}

private fun openNotificationListenerSettings(context: Context) {
    context.startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
}
