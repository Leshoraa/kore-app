package com.leshoraa.kore.presentation.camera

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.leshoraa.kore.R
import com.leshoraa.kore.domain.model.CameraSensorParams
import com.leshoraa.kore.domain.model.StreamConnectionState
import com.leshoraa.kore.domain.model.TelemetryData
import com.leshoraa.kore.presentation.components.KoReInlineLoading
import com.leshoraa.kore.presentation.components.KoReLoadingScreen
import java.util.Locale


/**
 * Main Vision & Telemetry screen.
 * Displays real-time camera feed with Material 3 compliant HUD overlays.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CameraVisionScreen(
    viewModel: CameraVisionViewModel,
    isDarkTheme: Boolean = false,
    onToggleTheme: () -> Unit = {}
) {
    val connectionState by viewModel.connectionState.collectAsState()
    val isStreamActive by viewModel.isStreamActive.collectAsState()
    val isDiscovering by viewModel.isDiscovering.collectAsState()
    val lastErrorMessage by viewModel.lastErrorMessage.collectAsState()
    val currentFrame by viewModel.currentFrame.collectAsState()
    val telemetry by viewModel.telemetry.collectAsState()
    val hostIp by viewModel.hostIp.collectAsState()
    val sensorParams by viewModel.sensorParams.collectAsState()

    val colorScheme = MaterialTheme.colorScheme
    val shapes = MaterialTheme.shapes

    var showIpDialog by remember { mutableStateOf(false) }
    var showSensorSheet by remember { mutableStateOf(false) }

    val scrollState = rememberScrollState()

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.vision_title),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                },
                actions = {
                    IconButton(onClick = { showSensorSheet = true }) {
                        Icon(
                            imageVector = Icons.Default.Tune,
                            contentDescription = stringResource(R.string.vision_btn_settings)
                        )
                    }
                    IconButton(onClick = onToggleTheme) {
                        Icon(
                            imageVector = if (isDarkTheme) Icons.Default.LightMode else Icons.Default.DarkMode,
                            contentDescription = stringResource(R.string.toggle_theme)
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. Camera Viewport with Material HUD
            CameraViewportCard(
                frame = currentFrame,
                telemetry = telemetry,
                connectionState = connectionState,
                isStreamActive = isStreamActive,
                onToggleStream = { viewModel.toggleStream() }
            )

            // 2. Error/Troubleshooting Banner
            AnimatedVisibility(visible = connectionState == StreamConnectionState.ERROR || lastErrorMessage != null) {
                OutlinedCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = shapes.large,
                    colors = CardDefaults.outlinedCardColors(containerColor = colorScheme.errorContainer.copy(alpha = 0.1f))
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.ErrorOutline,
                                contentDescription = null,
                                tint = colorScheme.onErrorContainer
                            )
                            Text(
                                text = "KoRe Connection Failed",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = colorScheme.onErrorContainer
                            )
                        }
                        Text(
                            text = lastErrorMessage ?: "Unable to reach http://$hostIp:81. Please verify the IP address or perform an Auto-Scan.",
                            style = MaterialTheme.typography.bodySmall,
                            color = colorScheme.onErrorContainer
                        )
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Button(
                                onClick = { viewModel.autoDiscoverKoRe() },
                                enabled = !isDiscovering,
                                colors = ButtonDefaults.buttonColors(containerColor = colorScheme.error),
                                shape = shapes.medium
                            ) {
                                if (isDiscovering) {
                                    KoReInlineLoading(
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(stringResource(R.string.vision_status_discovering))
                                } else {
                                    Icon(imageVector = Icons.Default.Search, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(stringResource(R.string.vision_btn_auto_scan))
                                }
                            }
                            OutlinedButton(
                                onClick = { showIpDialog = true },
                                shape = shapes.medium
                            ) {
                                Text("Manual IP")
                            }
                        }
                    }
                }
            }

            // 3. Connection Information Pill
            StreamControlPill(
                connectionState = connectionState,
                isStreamActive = isStreamActive,
                hostIp = hostIp,
                onToggleStream = { viewModel.toggleStream() },
                onConfigureIp = { showIpDialog = true }
            )

            // 4. AI Metrics Grid
            VisionMetricsSection(telemetry = telemetry)

            // 5. Affective Dynamics Section
            AffectiveDynamicsSection(telemetry = telemetry)

            // 6. Hardware Diagnostics Section
            HardwareDiagnosticsSection(telemetry = telemetry)
        }
    }

    // IP Configuration Bottom Sheet
    if (showIpDialog) {
        IpConfigBottomSheet(
            currentIp = hostIp,
            isDiscovering = isDiscovering,
            onDismiss = { showIpDialog = false },
            onAutoDiscover = { viewModel.autoDiscoverKoRe() },
            onSave = { newIp ->
                viewModel.setHostIp(newIp)
                showIpDialog = false
            }
        )
    }

    // Camera Sensor Tuning Bottom Sheet
    if (showSensorSheet) {
        CameraSensorControlSheet(
            params = sensorParams,
            onDismiss = { showSensorSheet = false },
            onUpdateParam = { param, value ->
                viewModel.updateSensorParam(param, value)
            }
        )
    }
}

/**
 * Viewport container embedding the video frame image and the real-time HUD Canvas overlay.
 */
@Composable
private fun CameraViewportCard(
    frame: androidx.compose.ui.graphics.ImageBitmap?,
    telemetry: TelemetryData?,
    connectionState: StreamConnectionState,
    isStreamActive: Boolean,
    onToggleStream: () -> Unit
) {
    val textMeasurer = rememberTextMeasurer()
    val colorScheme = MaterialTheme.colorScheme

    OutlinedCard(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(4f / 3f),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.outlinedCardColors(containerColor = Color.Black),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            // A. Video Frame Surface
            if (frame != null && isStreamActive) {
                androidx.compose.foundation.Image(
                    bitmap = frame,
                    contentDescription = null,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                // Placeholder State
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier.padding(16.dp)
                ) {
                    Icon(
                        imageVector = if (connectionState == StreamConnectionState.CONNECTING) Icons.Default.Sensors else Icons.Default.VideocamOff,
                        contentDescription = null,
                        tint = colorScheme.primary.copy(alpha = 0.6f),
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = when (connectionState) {
                            StreamConnectionState.CONNECTING -> stringResource(R.string.vision_status_connecting)
                            StreamConnectionState.ERROR -> stringResource(R.string.vision_status_error)
                            else -> "Camera Feed Standby"
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.7f)
                    )
                    Spacer(modifier = Modifier.height(20.dp))
                    Button(
                        onClick = onToggleStream,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isStreamActive) colorScheme.error else colorScheme.primary,
                            contentColor = if (isStreamActive) colorScheme.onError else colorScheme.onPrimary
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(
                            imageVector = if (isStreamActive) Icons.Default.Stop else Icons.Default.PlayArrow,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isStreamActive) stringResource(R.string.vision_btn_stop) else stringResource(R.string.vision_btn_start),
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // B. HUD Canvas Overlay
            Canvas(
                modifier = Modifier.fillMaxSize()
            ) {
                val canvasWidth = size.width
                val canvasHeight = size.height

                if (canvasWidth <= 0f || canvasHeight <= 0f) return@Canvas

                val fw = (telemetry?.fw ?: 640).toFloat()
                val fh = (telemetry?.fh ?: 480).toFloat()
                val scaleX = canvasWidth / fw
                val scaleY = canvasHeight / fh

                // 1. Draw Optical Center Reticle
                drawOpticalCenterReticle(this, canvasWidth, canvasHeight)

                val data = telemetry
                if (data != null && data.detected && isStreamActive) {
                    val primaryColor = colorScheme.primary
                    val tertiaryColor = colorScheme.tertiary

                    // 2. Draw Secondary Candidate Target Indicators
                    for (cand in data.candidates) {
                        if (cand.index != 0 && cand.cx > 0f) {
                            drawCandidateTargetIndicator(
                                scope = this,
                                cx = cand.cx * scaleX,
                                cy = cand.cy * scaleY,
                                w = cand.w * scaleX,
                                h = cand.h * scaleY,
                                color = tertiaryColor
                            )
                        }
                    }

                    // 3. Draw Primary Target Focus Frame
                    val primCx = data.cx * scaleX
                    val primCy = data.cy * scaleY
                    val primW = (if (data.w > 0f) data.w else 160f) * scaleX
                    val primH = (if (data.h > 0f) data.h else 200f) * scaleY

                    drawPrimaryTargetFrame(
                        scope = this,
                        textMeasurer = textMeasurer,
                        cx = primCx,
                        cy = primCy,
                        w = primW,
                        h = primH,
                        color = primaryColor,
                        rawX = data.cx,
                        rawY = data.cy
                    )
                }
            }
        }
    }
}


/**
 * Draws the subtle central optical crosshairs reticle.
 */
private fun drawOpticalCenterReticle(scope: DrawScope, width: Float, height: Float) {
    val cx = width / 2f
    val cy = height / 2f
    val crossLen = 10f
    val reticleColor = Color.White.copy(alpha = 0.15f)

    // Center Cross
    scope.drawLine(reticleColor, Offset(cx - crossLen, cy), Offset(cx + crossLen, cy), strokeWidth = 1f)
    scope.drawLine(reticleColor, Offset(cx, cy - crossLen), Offset(cx, cy + crossLen), strokeWidth = 1f)

    // Outer subtle boundary frame ticks
    val tickSize = 14f
    val tickColor = Color.White.copy(alpha = 0.12f)
    scope.drawLine(tickColor, Offset(12f, 12f), Offset(12f + tickSize, 12f), strokeWidth = 1f)
    scope.drawLine(tickColor, Offset(12f, 12f), Offset(12f, 12f + tickSize), strokeWidth = 1f)

    scope.drawLine(tickColor, Offset(width - 12f, 12f), Offset(width - 12f - tickSize, 12f), strokeWidth = 1f)
    scope.drawLine(tickColor, Offset(width - 12f, 12f), Offset(width - 12f, 12f + tickSize), strokeWidth = 1f)

    scope.drawLine(tickColor, Offset(12f, height - 12f), Offset(12f + tickSize, height - 12f), strokeWidth = 1f)
    scope.drawLine(tickColor, Offset(12f, height - 12f), Offset(12f, height - 12f - tickSize), strokeWidth = 1f)

    scope.drawLine(tickColor, Offset(width - 12f, height - 12f), Offset(width - 12f - tickSize, height - 12f), strokeWidth = 1f)
    scope.drawLine(tickColor, Offset(width - 12f, height - 12f), Offset(width - 12f, height - 12f - tickSize), strokeWidth = 1f)
}


/**
 * Draws rounded indicators for candidate targets.
 */
private fun drawCandidateTargetIndicator(
    scope: DrawScope,
    cx: Float,
    cy: Float,
    w: Float,
    h: Float,
    color: Color
) {
    val left = cx - w / 2f
    val top = cy - h / 2f
    val cornerRadius = 0f

    // Full sharp body outline
    scope.drawRoundRect(
        color = color.copy(alpha = 0.5f),
        topLeft = Offset(left, top),
        size = Size(w, h),
        cornerRadius = androidx.compose.ui.geometry.CornerRadius(cornerRadius, cornerRadius),
        style = Stroke(width = 2f)
    )

    // Subtle fill
    scope.drawRoundRect(
        color = color.copy(alpha = 0.05f),
        topLeft = Offset(left, top),
        size = Size(w, h),
        cornerRadius = androidx.compose.ui.geometry.CornerRadius(cornerRadius, cornerRadius)
    )
}

/**
 * Draws the primary focus frame for the tracked target with high contrast labeling.
 */
private fun drawPrimaryTargetFrame(
    scope: DrawScope,
    textMeasurer: androidx.compose.ui.text.TextMeasurer,
    cx: Float,
    cy: Float,
    w: Float,
    h: Float,
    color: Color,
    rawX: Float,
    rawY: Float
) {
    val left = cx - w / 2f
    val top = cy - h / 2f
    val cornerRadius = 16f
    
    // 1. Target Area Background (High contrast scrim)
    scope.drawRoundRect(
        color = Color.Black.copy(alpha = 0.15f),
        topLeft = Offset(left, top),
        size = Size(w, h),
        cornerRadius = androidx.compose.ui.geometry.CornerRadius(cornerRadius, cornerRadius)
    )

    // 2. Main Rounded Frame
    scope.drawRoundRect(
        color = color,
        topLeft = Offset(left, top),
        size = Size(w, h),
        cornerRadius = androidx.compose.ui.geometry.CornerRadius(cornerRadius, cornerRadius),
        style = Stroke(width = 4f)
    )

    // 3. Coordinate Information Label (High Contrast)
    val label = String.format(Locale.US, "%.0f, %.0f", rawX, rawY)
    val textStyle = TextStyle(
        color = Color.Black,
        fontSize = 11.sp,
        fontWeight = FontWeight.Black
    )
    val textLayoutResult = textMeasurer.measure(label, textStyle)
    
    val labelPaddingH = 10f
    val labelPaddingV = 6f
    val labelWidth = textLayoutResult.size.width.toFloat() + (labelPaddingH * 2)
    val labelHeight = textLayoutResult.size.height.toFloat() + (labelPaddingV * 2)

    // Label container with solid primary background for contrast
    scope.drawRoundRect(
        color = color,
        topLeft = Offset(left, top - labelHeight - 6f),
        size = Size(labelWidth, labelHeight),
        cornerRadius = androidx.compose.ui.geometry.CornerRadius(10f, 10f)
    )
    
    scope.drawText(
        textMeasurer = textMeasurer,
        text = label,
        topLeft = Offset(left + labelPaddingH, top - labelHeight - labelPaddingV + 2f),
        style = textStyle
    )
}

/**
 * Connection status information pill with refined controls.
 */
@Composable
private fun StreamControlPill(
    connectionState: StreamConnectionState,
    isStreamActive: Boolean,
    hostIp: String,
    onToggleStream: () -> Unit,
    onConfigureIp: () -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme
    OutlinedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.outlinedCardColors(containerColor = colorScheme.surfaceContainer),
        border = androidx.compose.foundation.BorderStroke(1.dp, colorScheme.outlineVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = when (connectionState) {
                            StreamConnectionState.STREAMING -> stringResource(R.string.vision_status_streaming)
                            StreamConnectionState.CONNECTING -> stringResource(R.string.vision_status_connecting)
                            StreamConnectionState.ERROR -> stringResource(R.string.vision_status_error)
                            else -> "Camera Stream Ready"
                        },
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = when (connectionState) {
                            StreamConnectionState.STREAMING -> colorScheme.primary
                            StreamConnectionState.ERROR -> colorScheme.error
                            else -> colorScheme.primary
                        }
                    )
                    Text(
                        text = "http://$hostIp:81/stream",
                        style = MaterialTheme.typography.bodySmall,
                        color = colorScheme.onSurfaceVariant
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    IconButton(
                        onClick = onConfigureIp,
                        colors = IconButtonDefaults.iconButtonColors(contentColor = colorScheme.secondary)
                    ) {
                        Icon(imageVector = Icons.Default.Edit, contentDescription = "Edit IP")
                    }
                    
                    if (isStreamActive) {
                        FilledTonalButton(
                            onClick = onToggleStream,
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.filledTonalButtonColors(
                                containerColor = colorScheme.errorContainer,
                                contentColor = colorScheme.error
                            ),
                            contentPadding = PaddingValues(8.dp),
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(imageVector = Icons.Default.Stop, contentDescription = "Stop Stream", modifier = Modifier.size(20.dp))
                        }
                    }
                }
            }
        }
    }
}

/**
 * AI Metrics grid display.
 */
@Composable
private fun VisionMetricsSection(telemetry: TelemetryData?) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            text = "Vision Metrics",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.5.sp
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            MetricTile(
                title = stringResource(R.string.vision_hud_fps),
                value = String.format(Locale.US, "%.1f", telemetry?.fpsAi ?: 0f),
                unit = "FPS",
                modifier = Modifier.weight(1f)
            )
            MetricTile(
                title = stringResource(R.string.vision_hud_conf),
                value = String.format(Locale.US, "%d", ((telemetry?.conf ?: 0f) * 100).toInt()),
                unit = "%",
                modifier = Modifier.weight(1f)
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            MetricTile(
                title = stringResource(R.string.vision_hud_human),
                value = String.format(Locale.US, "%d", ((telemetry?.humanLikelihood ?: 0f) * 100).toInt()),
                unit = "%",
                modifier = Modifier.weight(1f)
            )
            MetricTile(
                title = stringResource(R.string.vision_hud_prox),
                value = String.format(Locale.US, "%d", ((telemetry?.prox ?: 0f) * 100).toInt()),
                unit = "%",
                modifier = Modifier.weight(1f)
            )
        }
    }
}

/**
 * Single metric tile card with filled container background.
 */
@Composable
private fun MetricTile(
    title: String,
    value: String,
    unit: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            Row(
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = value,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (unit.isNotEmpty()) {
                    Text(
                        text = unit,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 3.dp)
                    )
                }
            }
        }
    }
}

/**
 * Affective state and mood dynamics visualization with filled card background.
 */
@Composable
private fun AffectiveDynamicsSection(telemetry: TelemetryData?) {
    val colorScheme = MaterialTheme.colorScheme
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = colorScheme.surfaceContainer)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Mood",
                    style = MaterialTheme.typography.labelLarge,
                    color = colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp
                )
            }

            // Valence Indicator
            val valence = telemetry?.valence ?: 0f
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = stringResource(R.string.vision_affective_valence),
                        style = MaterialTheme.typography.bodyMedium,
                        color = colorScheme.onSurface
                    )
                    Text(
                        text = String.format(Locale.US, "%+.2f", valence),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = colorScheme.onSurface
                    )
                }
                LinearProgressIndicator(
                    progress = { ((valence + 1.0f) / 2.0f).coerceIn(0f, 1f) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp)),
                    color = if (valence >= 0) colorScheme.primary else colorScheme.error,
                    trackColor = colorScheme.surfaceContainerHighest
                )
            }

            // Arousal Indicator
            val arousal = telemetry?.arousal ?: 0f
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = stringResource(R.string.vision_affective_arousal),
                        style = MaterialTheme.typography.bodyMedium,
                        color = colorScheme.onSurface
                    )
                    Text(
                        text = String.format(Locale.US, "%.2f", arousal),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = colorScheme.onSurface
                    )
                }
                LinearProgressIndicator(
                    progress = { arousal.coerceIn(0f, 1f) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp)),
                    color = colorScheme.tertiary,
                    trackColor = colorScheme.surfaceContainerHighest
                )
            }
        }
    }
}

/**
 * Embedded hardware stats section with filled card and filled status chips.
 */
@Composable
private fun HardwareDiagnosticsSection(telemetry: TelemetryData?) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "System Diagnostics",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.5.sp
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                DiagnosticItem(
                    label = stringResource(R.string.vision_sys_cpu),
                    value = "${telemetry?.cpuMhz ?: 240} MHz",
                    modifier = Modifier.weight(1f)
                )
                DiagnosticItem(
                    label = stringResource(R.string.vision_sys_heap),
                    value = "${(telemetry?.heapFree ?: 0L) / 1024} KB",
                    modifier = Modifier.weight(1f)
                )
                DiagnosticItem(
                    label = stringResource(R.string.vision_sys_psram),
                    value = "${(telemetry?.psramFree ?: 0L) / 1024 / 1024} MB",
                    modifier = Modifier.weight(1f)
                )
                DiagnosticItem(
                    label = stringResource(R.string.vision_sys_uptime),
                    value = "${(telemetry?.uptimeSeconds ?: 0L) / 60}m",
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun DiagnosticItem(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 10.dp)
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

/**
 * Bottom sheet to configure ESP32 host IP with presets and Auto-Discovery.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun IpConfigBottomSheet(
    currentIp: String,
    isDiscovering: Boolean,
    onDismiss: () -> Unit,
    onAutoDiscover: () -> Unit,
    onSave: (String) -> Unit
) {
    var ipText by remember { mutableStateOf(currentIp) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        dragHandle = { BottomSheetDefaults.DragHandle() },
        containerColor = MaterialTheme.colorScheme.surface,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = stringResource(R.string.dialog_ip_title),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = stringResource(R.string.dialog_ip_message),
                style = MaterialTheme.typography.bodyMedium
            )

            OutlinedTextField(
                value = ipText,
                onValueChange = { ipText = it },
                placeholder = { Text(stringResource(R.string.vision_host_placeholder)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )

            Text(
                text = stringResource(R.string.label_quick_presets),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary
            )

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                AssistChip(
                    onClick = { ipText = "192.168.18.16" },
                    label = { Text("192.168.18.16") }
                )
                AssistChip(
                    onClick = { ipText = "kore.local" },
                    label = { Text("kore.local") }
                )
            }

            Button(
                onClick = onAutoDiscover,
                enabled = !isDiscovering,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                if (isDiscovering) {
                    KoReInlineLoading(
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(R.string.vision_status_discovering))
                } else {
                    Icon(imageVector = Icons.Default.Search, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(R.string.vision_btn_auto_scan))
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = onDismiss) {
                    Text(stringResource(R.string.btn_cancel))
                }

                Spacer(modifier = Modifier.width(8.dp))

                Button(
                    onClick = { onSave(ipText) },
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(stringResource(R.string.dialog_btn_save))
                }
            }
        }
    }
}

/**
 * Camera sensor adjustment bottom sheet.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CameraSensorControlSheet(
    params: CameraSensorParams,
    onDismiss: () -> Unit,
    onUpdateParam: (String, Int) -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = stringResource(R.string.vision_btn_settings),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            // Brightness Slider
            SensorSliderItem(
                title = stringResource(R.string.vision_param_brightness),
                value = params.brightness.toFloat(),
                valueRange = -2f..2f,
                steps = 3,
                onValueChange = { onUpdateParam("brightness", it.toInt()) }
            )

            // Contrast Slider
            SensorSliderItem(
                title = stringResource(R.string.vision_param_contrast),
                value = params.contrast.toFloat(),
                valueRange = -2f..2f,
                steps = 3,
                onValueChange = { onUpdateParam("contrast", it.toInt()) }
            )

            // Saturation Slider
            SensorSliderItem(
                title = stringResource(R.string.vision_param_saturation),
                value = params.saturation.toFloat(),
                valueRange = -2f..2f,
                steps = 3,
                onValueChange = { onUpdateParam("saturation", it.toInt()) }
            )

            HorizontalDivider()

            // Toggles
            SensorToggleItem(
                title = stringResource(R.string.vision_param_vflip),
                checked = params.vflip,
                onCheckedChange = { onUpdateParam("vflip", if (it) 1 else 0) }
            )
            SensorToggleItem(
                title = stringResource(R.string.vision_param_hmirror),
                checked = params.hmirror,
                onCheckedChange = { onUpdateParam("hmirror", if (it) 1 else 0) }
            )
            SensorToggleItem(
                title = stringResource(R.string.vision_param_aec),
                checked = params.aec,
                onCheckedChange = { onUpdateParam("aec", if (it) 1 else 0) }
            )
        }
    }
}

@Composable
private fun SensorSliderItem(
    title: String,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    steps: Int,
    onValueChange: (Float) -> Unit
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(text = title, style = MaterialTheme.typography.bodyMedium)
            Text(
                text = String.format(Locale.US, "%+d", value.toInt()),
                style = MaterialTheme.typography.bodyMedium
            )
        }
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = valueRange,
            steps = steps
        )
    }
}

@Composable
private fun SensorToggleItem(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = title, style = MaterialTheme.typography.bodyMedium)
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}
