package com.leshoraa.kore.presentation.settings

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.*
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
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInParent
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
import com.leshoraa.kore.presentation.components.KoReInlineLoading
import com.leshoraa.kore.presentation.theme.AppPalette

/**
 * Primary settings screen for application configuration and hardware setup.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    selectedPalette: AppPalette = AppPalette.FOREST,
    onPaletteChange: (AppPalette) -> Unit = {},
    scrollToSection: String? = null,
    onNavigateBack: (() -> Unit)? = null,
    onNavigateToFilters: () -> Unit = {}
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    val weatherState by viewModel.weatherState.collectAsState()
    val scrollState = rememberScrollState()

    var systemAccessOffset by remember { mutableStateOf(0f) }
    var highlightSection by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(scrollToSection, systemAccessOffset) {
        if (scrollToSection == "system_access" && systemAccessOffset > 0f) {
            scrollState.animateScrollTo(systemAccessOffset.toInt())
            highlightSection = "system_access"
            kotlinx.coroutines.delay(3000)
            highlightSection = null
        }
    }

    val infiniteTransition = rememberInfiniteTransition(label = "highlight")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 0.3f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = LinearOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = permissions.values.any { it }
        if (granted) {
            viewModel.acquireLocationFromPhone()
        }
    }

    val gpsLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            viewModel.acquireLocationFromPhone()
        }
        viewModel.clearResolvableException()
    }

    LaunchedEffect(weatherState.resolvableSettingsException) {
        weatherState.resolvableSettingsException?.let { exception ->
            try {
                gpsLauncher.launch(
                    IntentSenderRequest.Builder(exception.resolution).build()
                )
            } catch (e: Exception) {
                viewModel.clearResolvableException()
            }
        }
    }

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
                .verticalScroll(scrollState)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Section: Appearance
            Column {
                Text(
                    text = stringResource(R.string.settings_theme_palette),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
                )

                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceContainer
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            AppPalette.entries.forEach { palette ->
                                PaletteColorOption(
                                    palette = palette,
                                    isSelected = selectedPalette == palette,
                                    onClick = { onPaletteChange(palette) }
                                )
                            }
                        }
                    }
                }
            }

            // Section: Ambient & Weather
            Column {
                Text(
                    text = stringResource(R.string.settings_section_weather),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
                )

                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceContainer
                ) {
                    Column(
                        modifier = Modifier.padding(8.dp)
                    ) {
                        SettingItem(
                            title = stringResource(R.string.weather_config_title),
                            description = stringResource(R.string.weather_config_desc),
                            icon = Icons.Default.Cloud,
                            onClick = { viewModel.openWeatherBottomSheet() }
                        )

                        HorizontalDivider(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                        )

                        AmbientGlancesContent(
                            isBleConnected = uiState.isBleConnected,
                            onShowClock = viewModel::showClock,
                            onShowWeather = viewModel::showWeather
                        )
                    }
                }
            }

            // Section: Connectivity & Hardware
            Column {
                Text(
                    text = stringResource(R.string.settings_section_connectivity),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
                )

                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceContainer
                ) {
                    Column(
                        modifier = Modifier.padding(8.dp)
                    ) {
                        SettingItem(
                            title = stringResource(R.string.device_config_title),
                            description = stringResource(R.string.device_config_desc),
                            icon = Icons.Default.Router,
                            onClick = { viewModel.openDeviceConfigBottomSheet() }
                        )
                    }
                }
            }

            // Section: Notification Rules
            Column {
                Text(
                    text = stringResource(R.string.settings_section_notifications),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
                )

                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceContainer
                ) {
                    Column(
                        modifier = Modifier.padding(8.dp)
                    ) {
                        SettingItem(
                            title = stringResource(R.string.rules_title),
                            description = "Manage which apps can send notifications to KoRe.",
                            icon = Icons.Default.FilterList,
                            onClick = onNavigateToFilters
                        )
                    }
                }
            }

            // Section: System Access
            Column(
                modifier = Modifier
                    .onGloballyPositioned { coordinates ->
                        systemAccessOffset = coordinates.positionInParent().y
                    }
                    .background(
                        if (highlightSection == "system_access") 
                            MaterialTheme.colorScheme.primary.copy(alpha = 1f - pulseAlpha) 
                        else Color.Transparent,
                        shape = RoundedCornerShape(16.dp)
                    )
                    .padding(if (highlightSection == "system_access") 8.dp else 0.dp)
            ) {
                Text(
                    text = stringResource(R.string.settings_stability_guards),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
                )

                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceContainer
                ) {
                    Column(
                        modifier = Modifier.padding(8.dp)
                    ) {
                        SettingItem(
                            title = stringResource(R.string.settings_battery_optimization),
                            description = stringResource(R.string.settings_battery_optimization_desc),
                            icon = Icons.Default.BatteryChargingFull,
                            onClick = { requestIgnoreBatteryOptimizations(context) }
                        )

                        HorizontalDivider(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                        )

                        SettingItem(
                            title = stringResource(R.string.settings_notification_access),
                            description = stringResource(R.string.settings_notification_access_desc),
                            icon = Icons.Default.Notifications,
                            onClick = { openNotificationListenerSettings(context) }
                        )
                    }
                }
            }
        }
    }

    if (uiState.isDialogOpen) {
        DeviceConfigBottomSheet(
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
            onDismiss = viewModel::closeDeviceConfigBottomSheet
        )
    }

    if (weatherState.isDialogOpen) {
        WeatherConfigBottomSheet(
            state = weatherState,
            isBleConnected = uiState.isBleConnected,
            onCityChange = viewModel::onWeatherCityChanged,
            onLatChange = viewModel::onWeatherLatChanged,
            onLonChange = viewModel::onWeatherLonChanged,
            onEnabledChange = viewModel::onWeatherEnabledChanged,
            onTzChange = viewModel::onWeatherTzChanged,
            onPresetSelected = viewModel::applyWeatherPreset,
            onUseGpsLocation = {
                val hasFine = context.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                val hasCoarse = context.checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
                
                if (hasFine || hasCoarse) {
                    viewModel.acquireLocationFromPhone()
                } else {
                    locationPermissionLauncher.launch(
                        arrayOf(
                            Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_COARSE_LOCATION
                        )
                    )
                }
            },
            onSyncNow = viewModel::syncTimeAndWeatherNow,
            onRefresh = viewModel::loadWeatherConfig,
            onSave = viewModel::saveWeatherConfig,
            onDismiss = viewModel::closeWeatherBottomSheet
        )
    }
}

/**
 * Bottom sheet for configuring KoRe hardware network settings (Wi-Fi, AP, BLE).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeviceConfigBottomSheet(
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
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        dragHandle = { BottomSheetDefaults.DragHandle() },
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 6.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(bottom = 32.dp) // Extra padding for the bottom
        ) {
            // Header: Clean & Integrated
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 16.dp),
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

                IconButton(onClick = onRefresh, enabled = !state.isLoading) {
                    if (state.isLoading) {
                        KoReInlineLoading(modifier = Modifier.size(20.dp))
                    } else {
                        Icon(Icons.Default.Refresh, contentDescription = stringResource(R.string.status_sync), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }

            // Main Content
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                // Feedback Messages
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

                // Actions: Cohesive Material 3 Buttons
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
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
                            KoReInlineLoading(modifier = Modifier.size(18.dp))
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
fun PaletteColorOption(
    palette: AppPalette,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(8.dp)
    ) {
        Box(
            modifier = Modifier.size(48.dp),
            contentAlignment = Alignment.Center
        ) {
            if (isSelected) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .border(
                            width = 2.dp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                            shape = CircleShape
                        )
                )
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(palette.primaryColor),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
            } else {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(palette.primaryColor)
                )
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = palette.label,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            color = if (isSelected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun SettingItem(
    title: String,
    description: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )
        }
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
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

private fun requestIgnoreBatteryOptimizations(context: Context) {
    val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
        data = Uri.parse("package:${context.packageName}")
    }
    context.startActivity(intent)
}

private fun openNotificationListenerSettings(context: Context) {
    context.startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
}

@Composable
fun AmbientGlancesContent(
    isBleConnected: Boolean,
    onShowClock: () -> Unit,
    onShowWeather: () -> Unit
) {
    Column(modifier = Modifier.padding(12.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Widgets,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(
                    text = stringResource(R.string.title_ambient_glances),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = stringResource(R.string.desc_ambient_glances),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            FilledTonalButton(
                onClick = onShowClock,
                modifier = Modifier.weight(1f),
                enabled = isBleConnected,
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.AccessTime,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = stringResource(R.string.btn_show_clock),
                    style = MaterialTheme.typography.labelLarge
                )
            }

            FilledTonalButton(
                onClick = onShowWeather,
                modifier = Modifier.weight(1f),
                enabled = isBleConnected,
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Cloud,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = stringResource(R.string.btn_show_weather),
                    style = MaterialTheme.typography.labelLarge
                )
            }
        }

        if (!isBleConnected) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.msg_hardware_offline),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
            )
        }
    }
}

/**
 * Bottom sheet for configuring weather forecasts and location coordinates.
 */
@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun WeatherConfigBottomSheet(
    state: WeatherConfigUiState,
    isBleConnected: Boolean,
    onCityChange: (String) -> Unit,
    onLatChange: (String) -> Unit,
    onLonChange: (String) -> Unit,
    onEnabledChange: (Boolean) -> Unit,
    onTzChange: (Int) -> Unit,
    onPresetSelected: (com.leshoraa.kore.domain.model.WeatherLocationConfig) -> Unit,
    onUseGpsLocation: () -> Unit,
    onSyncNow: () -> Unit,
    onRefresh: () -> Unit,
    onSave: () -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        dragHandle = { BottomSheetDefaults.DragHandle() },
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 6.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(bottom = 32.dp)
        ) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = stringResource(R.string.weather_config_title),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = if (isBleConnected) stringResource(R.string.status_ble_connected) else stringResource(R.string.status_ble_disconnected),
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isBleConnected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                    )
                }

                IconButton(onClick = onRefresh) {
                    Icon(Icons.Default.Refresh, contentDescription = stringResource(R.string.status_sync), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            // Main Scrollable Content
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                // Feedback Messages
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

                // Section: Location & Coordinates
                ConfigSection(title = stringResource(R.string.label_weather_city), icon = Icons.Default.LocationOn) {
                    OutlinedButton(
                        onClick = onUseGpsLocation,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        enabled = !state.isAcquiringLocation
                    ) {
                        if (state.isAcquiringLocation) {
                            KoReInlineLoading(modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Detecting GPS Location...", style = MaterialTheme.typography.labelMedium)
                        } else {
                            Icon(Icons.Default.MyLocation, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(stringResource(R.string.btn_use_gps), style = MaterialTheme.typography.labelMedium)
                        }
                    }

                    CustomTextField(
                        value = state.city,
                        onValueChange = onCityChange,
                        label = stringResource(R.string.label_weather_city),
                        placeholder = stringResource(R.string.placeholder_weather_city)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedTextField(
                            value = state.latitude,
                            onValueChange = onLatChange,
                            label = { Text(stringResource(R.string.label_weather_lat), style = MaterialTheme.typography.bodyMedium) },
                            placeholder = { Text(stringResource(R.string.placeholder_weather_lat), style = MaterialTheme.typography.bodyMedium) },
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            textStyle = MaterialTheme.typography.bodyLarge,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            colors = OutlinedTextFieldDefaults.colors(
                                unfocusedContainerColor = Color.Transparent,
                                focusedContainerColor = Color.Transparent,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                                focusedBorderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
                                unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                            )
                        )

                        OutlinedTextField(
                            value = state.longitude,
                            onValueChange = onLonChange,
                            label = { Text(stringResource(R.string.label_weather_lon), style = MaterialTheme.typography.bodyMedium) },
                            placeholder = { Text(stringResource(R.string.placeholder_weather_lon), style = MaterialTheme.typography.bodyMedium) },
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            textStyle = MaterialTheme.typography.bodyLarge,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            colors = OutlinedTextFieldDefaults.colors(
                                unfocusedContainerColor = Color.Transparent,
                                focusedContainerColor = Color.Transparent,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                                focusedBorderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
                                unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                            )
                        )
                    }
                }

                // Section: Quick Presets
                ConfigSection(title = stringResource(R.string.label_city_presets), icon = Icons.Default.Public) {
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        com.leshoraa.kore.domain.model.WeatherLocationConfig.PRESETS.forEach { preset ->
                            val isSelected = state.city.equals(preset.city, ignoreCase = true)
                            FilterChip(
                                selected = isSelected,
                                onClick = { onPresetSelected(preset) },
                                label = { Text(preset.city, style = MaterialTheme.typography.labelSmall) },
                                shape = CircleShape,
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                    selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            )
                        }
                    }
                }

                // Section: Timezone Offset
                ConfigSection(title = stringResource(R.string.label_weather_tz), icon = Icons.Default.Schedule) {
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(
                            "WIB (UTC+7)" to 25200,
                            "WITA (UTC+8)" to 28800,
                            "WIT (UTC+9)" to 32400,
                            "GMT (UTC+0)" to 0,
                            "EST (UTC-5)" to -18000
                        ).forEach { (label, offsetSec) ->
                            val isSelected = state.timezoneOffsetSec == offsetSec
                            FilterChip(
                                selected = isSelected,
                                onClick = { onTzChange(offsetSec) },
                                label = { Text(label, style = MaterialTheme.typography.labelSmall) },
                                shape = CircleShape,
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                                    selectedLabelColor = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                            )
                        }
                    }
                }

                // Section: Spontaneous Ambient Glance Toggle
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(R.string.label_weather_enabled),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = "Periodically preview weather and clock on OLED automatically.",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = state.isEnabled,
                            onCheckedChange = onEnabledChange
                        )
                    }
                }

                // Actions Footer
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
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

                    OutlinedButton(
                        onClick = onSyncNow,
                        enabled = !state.isSaving && isBleConnected,
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.CloudSync, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(stringResource(R.string.btn_sync_now), style = MaterialTheme.typography.labelMedium)
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Button(
                        onClick = onSave,
                        enabled = !state.isSaving && isBleConnected,
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        if (state.isSaving) {
                            KoReInlineLoading(modifier = Modifier.size(18.dp))
                        } else {
                            Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(stringResource(R.string.btn_save_weather), style = MaterialTheme.typography.labelMedium)
                        }
                    }
                }
            }
        }
    }
}
