package com.leshoraa.kore.presentation.dashboard

import android.bluetooth.BluetoothProfile
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BluetoothConnected
import androidx.compose.material.icons.filled.BluetoothDisabled
import androidx.compose.material.icons.filled.BluetoothSearching
import androidx.compose.material.icons.filled.BrightnessLow
import androidx.compose.material.icons.filled.BrightnessMedium
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.SettingsSuggest
import androidx.compose.material.icons.filled.Widgets
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import com.leshoraa.kore.R
import com.leshoraa.kore.core.common.PermissionManager
import com.leshoraa.kore.domain.model.Expression
import com.leshoraa.kore.domain.model.NotificationEvent
import com.leshoraa.kore.presentation.components.KoReInlineLoading
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Dashboard screen displaying hardware connection status, environmental metrics,
 * and recent notification logs.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel,
    isDarkTheme: Boolean = false,
    onToggleTheme: () -> Unit = {},
    onEnableBluetooth: () -> Unit = {},
    onNavigateToScanner: () -> Unit,
    onNavigateToSettings: (String?) -> Unit
) {
    val context = LocalContext.current
    val connectionState by viewModel.connectionState.collectAsState()
    val isBluetoothEnabled by viewModel.isBluetoothEnabled.collectAsState()
    val connectedDeviceName by viewModel.connectedDeviceName.collectAsState()
    val brightness by viewModel.brightness.collectAsState()
    val selectedExpression by viewModel.selectedExpression.collectAsState()
    val logs by viewModel.logs.collectAsState()
    
    val isSystemAccessTipDismissed by viewModel.isSystemAccessTipDismissed.collectAsState()
    val isBluetoothTipDismissed by viewModel.isBluetoothTipDismissed.collectAsState()

    // Periodically update permission status when the screen is visible
    LaunchedEffect(Unit) {
        viewModel.updatePermissionStatus(
            notificationAccess = PermissionManager.isNotificationListenerEnabled(context),
            batteryOptimization = PermissionManager.isBatteryOptimizationIgnored(context)
        )
    }

    
    val testTitle by viewModel.testTitle.collectAsState()
    val testMessage by viewModel.testMessage.collectAsState()
    val snackbarMessage by viewModel.snackbarMessage.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(snackbarMessage) {
        snackbarMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearSnackbarMessage()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(stringResource(R.string.dashboard_title), style = MaterialTheme.typography.titleMedium) },
                actions = {
                    IconButton(onClick = onToggleTheme) {
                        Icon(
                            if (isDarkTheme) Icons.Default.LightMode else Icons.Default.DarkMode,
                            contentDescription = stringResource(R.string.toggle_theme)
                        )
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
        ) {
            item(key = "tips_carousel", contentType = "carousel") {
                val bluetoothTipTitle = stringResource(R.string.tip_bluetooth_off_title)
                val bluetoothTipDesc = stringResource(R.string.tip_bluetooth_off_desc)
                val bluetoothActionLabel = stringResource(R.string.btn_turn_on)
                
                val systemAccessTipTitle = stringResource(R.string.tip_system_access_title)
                val systemAccessTipDesc = stringResource(R.string.tip_system_access_desc)
                
                val tertiaryContainer = MaterialTheme.colorScheme.tertiaryContainer
                val onTertiaryContainer = MaterialTheme.colorScheme.onTertiaryContainer
                
                val tips = remember(isBluetoothEnabled, isSystemAccessTipDismissed, isBluetoothTipDismissed, bluetoothTipTitle, systemAccessTipTitle) {
                    mutableListOf<TipData>().apply {
                        val needsSystemAccess = !PermissionManager.isNotificationListenerEnabled(context) || 
                                              !PermissionManager.isBatteryOptimizationIgnored(context)
                        
                        if (needsSystemAccess && !isSystemAccessTipDismissed) {
                            add(
                                TipData(
                                    title = systemAccessTipTitle,
                                    description = systemAccessTipDesc,
                                    icon = Icons.Default.SettingsSuggest,
                                    containerColor = tertiaryContainer,
                                    contentColor = onTertiaryContainer,
                                    onClick = { onNavigateToSettings("system_access") },
                                    onDismiss = { viewModel.dismissSystemAccessTip() }
                                )
                            )
                        }

                        if (!isBluetoothEnabled && !isBluetoothTipDismissed) {
                            add(
                                TipData(
                                    title = bluetoothTipTitle,
                                    description = bluetoothTipDesc,
                                    icon = Icons.Default.BluetoothDisabled,
                                    containerColor = Color(0xFFBA1A1A), // Error Red
                                    contentColor = Color.White,
                                    actionLabel = bluetoothActionLabel,
                                    onAction = onEnableBluetooth,
                                    onDismiss = { viewModel.dismissBluetoothTip() }
                                )
                            )
                        }
                    }
                }

                if (tips.isNotEmpty()) {
                    TipsCarousel(tips = tips)
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }

            item(key = "connection_card", contentType = "card") {
                ConnectionStatusCard(
                    state = connectionState,
                    deviceName = connectedDeviceName,
                    onConnectClick = onNavigateToScanner,
                    onDisconnectClick = { viewModel.disconnect() }
                )
                Spacer(modifier = Modifier.height(12.dp))
            }

            item(key = "brightness_card", contentType = "card") {
                BrightnessControlCard(
                    brightness = brightness,
                    connectionState = connectionState,
                    onBrightnessChange = { viewModel.onBrightnessChange(it) },
                    onBrightnessChangeFinished = { viewModel.onBrightnessChangeFinished() }
                )
                Spacer(modifier = Modifier.height(12.dp))
            }

            item(key = "expression_card", contentType = "card") {
                ExpressionControlCard(
                    selectedExpression = selectedExpression,
                    connectionState = connectionState,
                    onSelectExpression = { viewModel.selectExpression(it) }
                )
                Spacer(modifier = Modifier.height(12.dp))
            }



            item(key = "log_header", contentType = "header") {
                Text(
                    text = stringResource(R.string.live_event_log),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(top = 16.dp, bottom = 10.dp, start = 4.dp)
                )
            }

            item(key = "test_section", contentType = "card") {
                TestMessageSection(
                    title = testTitle,
                    message = testMessage,
                    onTitleChange = { viewModel.onTestTitleChange(it) },
                    onMessageChange = { viewModel.onTestMessageChange(it) },
                    onSendClick = { viewModel.sendTestMessage() }
                )
                Spacer(modifier = Modifier.height(12.dp))
            }

            if (logs.isEmpty()) {
                item(key = "empty_log", contentType = "placeholder") {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 200.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            stringResource(R.string.msg_waiting_events),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                items(
                    items = logs,
                    key = { it.id },
                    contentType = { "log_item" }
                ) { event ->
                    Box(modifier = Modifier.animateItem()) {
                        LogItem(event)
                    }
                }
            }
        }
    }
}

/**
 * A horizontal pager displaying a carousel of actionable tips or status warnings.
 *
 * @param tips List of data objects representing each tip in the carousel.
 */
@Composable
fun TipsCarousel(tips: List<TipData>) {
    val pagerState = rememberPagerState(pageCount = { tips.size })
    
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxWidth()
        ) { page ->
            val tip = tips[page]
            TipCard(
                data = tip,
                modifier = Modifier.padding(horizontal = 4.dp)
            )
        }
        
        if (tips.size > 1) {
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                Modifier
                    .height(8.dp)
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                repeat(tips.size) { iteration ->
                    val color = if (pagerState.currentPage == iteration) 
                        MaterialTheme.colorScheme.primary 
                    else 
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                    Box(
                        modifier = Modifier
                            .padding(horizontal = 4.dp)
                            .clip(CircleShape)
                            .background(color)
                            .size(6.dp)
                    )
                }
            }
        }
    }
}

/**
 * Data model for a single tip in the carousel.
 *
 * @param title Bold header text for the tip.
 * @param description Supporting body text explaining the tip.
 * @param icon Vector icon representing the tip category.
 * @param containerColor Background color of the card.
 * @param contentColor Color for text and icon foreground.
 * @param actionLabel Optional text for a primary action button.
 * @param onAction Callback for the primary action button.
 * @param onClick Callback for clicking the entire card.
 * @param onDismiss Callback for the close button.
 */
data class TipData(
    val title: String,
    val description: String,
    val icon: ImageVector,
    val containerColor: Color,
    val contentColor: Color,
    val actionLabel: String? = null,
    val onAction: (() -> Unit)? = null,
    val onClick: (() -> Unit)? = null,
    val onDismiss: (() -> Unit)? = null
)

@Composable
fun TipCard(
    data: TipData,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .then(if (data.onClick != null) Modifier.clickable(onClick = data.onClick) else Modifier),
        colors = CardDefaults.cardColors(
            containerColor = data.containerColor,
            contentColor = data.contentColor
        ),
        shape = RoundedCornerShape(24.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                color = data.contentColor.copy(alpha = 0.2f),
                shape = CircleShape,
                modifier = Modifier.size(40.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = data.icon,
                        contentDescription = null,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = data.title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = data.description,
                    style = MaterialTheme.typography.labelSmall
                )
            }
            
            if (data.actionLabel != null && data.onAction != null) {
                TextButton(
                    onClick = data.onAction,
                    colors = ButtonDefaults.textButtonColors(contentColor = data.contentColor)
                ) {
                    Text(data.actionLabel, fontWeight = FontWeight.Bold)
                }
            }
            
            if (data.onDismiss != null) {
                IconButton(onClick = data.onDismiss) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Dismiss",
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
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
                    isConnected -> MaterialTheme.colorScheme.primary
                    isConnecting -> MaterialTheme.colorScheme.secondaryContainer
                    else -> MaterialTheme.colorScheme.secondaryContainer
                },
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier.size(48.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    if (isConnecting) {
                        KoReInlineLoading(modifier = Modifier.size(22.dp))
                    } else {
                        Icon(
                            imageVector = if (isConnected) Icons.Default.BluetoothConnected else Icons.Default.BluetoothDisabled,
                            contentDescription = null,
                            tint = if (isConnected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = when {
                        isConnected -> deviceName ?: stringResource(R.string.status_connected)
                        isConnecting -> deviceName ?: stringResource(R.string.status_connecting)
                        else -> stringResource(R.string.status_disconnected)
                    },
                    style = MaterialTheme.typography.titleSmall
                )
                Text(
                    text = when {
                        isConnected -> stringResource(R.string.label_hardware_active)
                        isConnecting -> stringResource(R.string.status_connecting)
                        else -> stringResource(R.string.label_no_hardware_linked)
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            when {
                isConnected -> {
                    TextButton(onClick = onDisconnectClick) {
                        Text(stringResource(R.string.btn_stop))
                    }
                }
                isConnecting -> {
                    OutlinedButton(
                        onClick = onDisconnectClick,
                        contentPadding = PaddingValues(horizontal = 10.dp),
                        modifier = Modifier.height(34.dp)
                    ) {
                        Text(stringResource(R.string.btn_cancel), style = MaterialTheme.typography.labelSmall)
                    }
                }
                else -> {
                    Button(onClick = onConnectClick, contentPadding = PaddingValues(horizontal = 12.dp)) {
                        Text(stringResource(R.string.btn_link))
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
                        contentDescription = stringResource(R.string.label_brightness),
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.title_kore_brightness),
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                Text(
                    text = "${brightness}%",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            Slider(
                value = brightness.toFloat(),
                onValueChange = { newValue ->
                    onBrightnessChange(newValue.toInt())
                },
                onValueChangeFinished = onBrightnessChangeFinished,
                valueRange = 1f..100f,
                steps = 5,
                modifier = Modifier.fillMaxWidth()
            )

            if (!isConnected) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = if (isConnecting) 
                        stringResource(R.string.status_connecting) 
                    else 
                        stringResource(R.string.msg_hardware_offline),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun ExpressionControlCard(
    selectedExpression: Expression?,
    connectionState: Int,
    onSelectExpression: (Expression?) -> Unit
) {
    val isConnected = connectionState == BluetoothProfile.STATE_CONNECTED
    val isConnecting = connectionState == BluetoothProfile.STATE_CONNECTING
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
                    Icon(
                        imageVector = Icons.Default.Face,
                        contentDescription = stringResource(R.string.label_expression),
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.label_expression),
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Icon(
                    imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = if (isExpanded) stringResource(R.string.desc_collapse) else stringResource(R.string.desc_expand),
                    tint = MaterialTheme.colorScheme.primary
                )
            }

            AnimatedVisibility(visible = isExpanded) {
                Column {
                    Spacer(modifier = Modifier.height(12.dp))

                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Expression.entries.forEach { expr ->
                            val isSelected = selectedExpression == expr
                            ExpressionButton(
                                expression = expr,
                                isSelected = isSelected,
                                onClick = { onSelectExpression(expr) }
                            )
                        }

                        // Auto Mood as a reset chip
                        val isAutoMood = selectedExpression == null
                        FilterChip(
                            selected = isAutoMood,
                            onClick = { onSelectExpression(null) },
                            label = { 
                                Text(
                                    text = stringResource(if (isAutoMood) R.string.btn_default_auto_mood_selected else R.string.btn_default_auto_mood),
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = if (isAutoMood) FontWeight.Bold else FontWeight.Normal
                                ) 
                            },
                            shape = CircleShape,
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.tertiaryContainer,
                                selectedLabelColor = MaterialTheme.colorScheme.onTertiaryContainer
                            )
                        )
                    }

                    if (!isConnected) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = if (isConnecting)
                                stringResource(R.string.status_connecting)
                            else
                                stringResource(R.string.msg_hardware_offline),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ExpressionButton(
    expression: Expression,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    FilterChip(
        selected = isSelected,
        onClick = onClick,
        label = { 
            Text(
                text = expression.displayName,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
            )
        },
        shape = CircleShape,
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = MaterialTheme.colorScheme.tertiaryContainer,
            selectedLabelColor = MaterialTheme.colorScheme.onTertiaryContainer
        )
    )
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
                        text = stringResource(R.string.title_push_test),
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Icon(
                    imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = if (isExpanded) stringResource(R.string.desc_collapse) else stringResource(R.string.desc_expand),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
            
            AnimatedVisibility(visible = isExpanded) {
                Column {
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = title,
                        onValueChange = onTitleChange,
                        label = { Text(stringResource(R.string.label_title)) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.medium
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = message,
                        onValueChange = onMessageChange,
                        label = { Text(stringResource(R.string.label_message)) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.medium
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    FilledTonalButton(
                        onClick = onSendClick,
                        modifier = Modifier.fillMaxWidth(),
                        enabled = title.isNotBlank() && message.isNotBlank()
                    ) {
                        Text(stringResource(R.string.btn_send_notification))
                    }
                }
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
    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), thickness = 0.8.dp)
}


