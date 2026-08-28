package com.leshoraa.kore.presentation.moments

import android.graphics.BitmapFactory
import androidx.compose.animation.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.leshoraa.kore.R
import com.leshoraa.kore.domain.model.DeskMoment
import com.leshoraa.kore.presentation.components.KoReInlineLoading
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

/**
 * Material 3 screen for viewing and managing candid Desk Moments.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MomentsScreen(
    viewModel: MomentsViewModel,
    isDarkTheme: Boolean = false,
    onToggleTheme: () -> Unit = {},
    onNavigateBack: (() -> Unit)? = null
) {
    val uiState by viewModel.uiState.collectAsState()
    val isAutoCaptureEnabled by viewModel.isAutoCaptureEnabled.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = androidx.compose.ui.platform.LocalContext.current

    var isSelectionMode by remember { mutableStateOf(false) }
    var selectedMomentIds by remember { mutableStateOf(setOf<Long>()) }

    var showDeleteConfirmDialog by remember { mutableStateOf<DeskMoment?>(null) }
    var showBatchDeleteConfirmDialog by remember { mutableStateOf(false) }
    var showClearAllConfirmDialog by remember { mutableStateOf(false) }

    LaunchedEffect(uiState.snackbarMessage) {
        uiState.snackbarMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearSnackbarMessage()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = if (isSelectionMode) "${selectedMomentIds.size} Selected" else "Desk Moments",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    if (isSelectionMode) {
                        IconButton(onClick = {
                            isSelectionMode = false
                            selectedMomentIds = emptySet()
                        }) {
                            Icon(Icons.Default.Close, contentDescription = "Exit selection mode")
                        }
                    } else if (onNavigateBack != null) {
                        IconButton(onClick = onNavigateBack) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = stringResource(R.string.common_back)
                            )
                        }
                    }
                },
                actions = {
                    if (isSelectionMode) {
                        IconButton(onClick = {
                            selectedMomentIds = if (selectedMomentIds.size == uiState.moments.size) {
                                emptySet()
                            } else {
                                uiState.moments.map { it.id }.toSet()
                            }
                        }) {
                            Icon(
                                imageVector = if (selectedMomentIds.size == uiState.moments.size && uiState.moments.isNotEmpty()) {
                                    Icons.Default.Deselect
                                } else {
                                    Icons.Default.SelectAll
                                },
                                contentDescription = "Select all"
                            )
                        }
                    } else {
                        if (uiState.moments.isNotEmpty()) {
                            IconButton(onClick = { showClearAllConfirmDialog = true }) {
                                Icon(
                                    imageVector = Icons.Default.DeleteSweep,
                                    contentDescription = "Clear all moments"
                                )
                            }
                        }
                        IconButton(onClick = onToggleTheme) {
                            Icon(
                                imageVector = if (isDarkTheme) Icons.Default.LightMode else Icons.Default.DarkMode,
                                contentDescription = stringResource(R.string.toggle_theme)
                            )
                        }
                    }
                }
            )
        },
        floatingActionButton = {
            if (!isSelectionMode) {
                ExtendedFloatingActionButton(
                    onClick = { viewModel.captureInstantMoment() },
                    expanded = true,
                    icon = {
                        if (uiState.isCapturing) {
                            KoReInlineLoading(modifier = Modifier.size(20.dp))
                        } else {
                            Icon(Icons.Default.CameraAlt, contentDescription = null)
                        }
                    },
                    text = {
                        Text(if (uiState.isCapturing) "Capturing..." else "Capture Moment")
                    },
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        },
        bottomBar = {
            if (isSelectionMode) {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceContainer,
                    tonalElevation = 0.dp,
                    shadowElevation = 0.dp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Button(
                            onClick = {
                                val selected = uiState.moments.filter { selectedMomentIds.contains(it.id) }
                                viewModel.exportMomentsToGallery(context, selected)
                                isSelectionMode = false
                                selectedMomentIds = emptySet()
                            },
                            enabled = selectedMomentIds.isNotEmpty(),
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Save to Gallery (${selectedMomentIds.size})")
                        }

                        FilledTonalButton(
                            onClick = { showBatchDeleteConfirmDialog = true },
                            enabled = selectedMomentIds.isNotEmpty(),
                            colors = ButtonDefaults.filledTonalButtonColors(
                                containerColor = MaterialTheme.colorScheme.errorContainer,
                                contentColor = MaterialTheme.colorScheme.onErrorContainer
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.DeleteOutline, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Delete")
                        }
                    }
                }
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
        ) {
            if (uiState.moments.isEmpty()) {
                EmptyMomentsPlaceholder(
                    isAutoCaptureEnabled = isAutoCaptureEnabled
                )
            } else {
                MomentsGrid(
                    moments = uiState.moments,
                    isSelectionMode = isSelectionMode,
                    selectedIds = selectedMomentIds,
                    onToggleSelect = { id ->
                        selectedMomentIds = if (selectedMomentIds.contains(id)) {
                            selectedMomentIds - id
                        } else {
                            selectedMomentIds + id
                        }
                    },
                    onEnterSelectionMode = {
                        isSelectionMode = true
                    },
                    onExitSelectionMode = {
                        isSelectionMode = false
                        selectedMomentIds = emptySet()
                    },
                    onMomentClick = { moment ->
                        if (isSelectionMode) {
                            selectedMomentIds = if (selectedMomentIds.contains(moment.id)) {
                                selectedMomentIds - moment.id
                            } else {
                                selectedMomentIds + moment.id
                            }
                        } else {
                            viewModel.selectMoment(moment)
                        }
                    },
                    onDeleteClick = { showDeleteConfirmDialog = it }
                )
            }
        }
    }

    // Detail Bottom Sheet
    if (uiState.isDetailSheetOpen && uiState.selectedMoment != null) {
        MomentDetailSheet(
            moment = uiState.selectedMoment!!,
            onDismiss = { viewModel.closeDetailSheet() },
            onSaveToGallery = {
                viewModel.exportMomentsToGallery(context, listOf(uiState.selectedMoment!!))
            },
            onDelete = {
                viewModel.deleteMoment(uiState.selectedMoment!!)
            }
        )
    }

    // Batch Delete Dialog
    if (showBatchDeleteConfirmDialog) {
        val count = selectedMomentIds.size
        AlertDialog(
            onDismissRequest = { showBatchDeleteConfirmDialog = false },
            icon = { Icon(Icons.Default.DeleteOutline, contentDescription = null) },
            title = { Text("Delete $count Selected Moments?") },
            text = { Text("These $count photo snapshots will be permanently removed from your device storage.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        val targets = uiState.moments.filter { selectedMomentIds.contains(it.id) }
                        viewModel.deleteMultipleMoments(targets)
                        isSelectionMode = false
                        selectedMomentIds = emptySet()
                        showBatchDeleteConfirmDialog = false
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete All ($count)")
                }
            },
            dismissButton = {
                TextButton(onClick = { showBatchDeleteConfirmDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Single Delete Dialog
    if (showDeleteConfirmDialog != null) {
        val target = showDeleteConfirmDialog!!
        AlertDialog(
            onDismissRequest = { showDeleteConfirmDialog = null },
            icon = { Icon(Icons.Default.DeleteOutline, contentDescription = null) },
            title = { Text("Delete Moment?") },
            text = { Text("This desk moment photo will be permanently deleted from your device storage.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteMoment(target)
                        showDeleteConfirmDialog = null
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmDialog = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Clear All Dialog
    if (showClearAllConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showClearAllConfirmDialog = false },
            icon = { Icon(Icons.Default.DeleteForever, contentDescription = null) },
            title = { Text("Clear All Desk Moments?") },
            text = { Text("Are you sure you want to remove all saved desk moments? This action cannot be undone.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.clearAllMoments()
                        showClearAllConfirmDialog = false
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Clear All")
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearAllConfirmDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun MomentsGrid(
    moments: List<DeskMoment>,
    isSelectionMode: Boolean,
    selectedIds: Set<Long>,
    onToggleSelect: (Long) -> Unit,
    onEnterSelectionMode: () -> Unit,
    onExitSelectionMode: () -> Unit,
    onMomentClick: (DeskMoment) -> Unit,
    onDeleteClick: (DeskMoment) -> Unit
) {
    val totalCount = moments.size
    val timeFormat = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }
    val dateFormat = remember { SimpleDateFormat("EEE, dd MMM yyyy", Locale.getDefault()) }

    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 96.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        item(span = { GridItemSpan(2) }) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceContainer,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Desk Memory Vault",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = if (isSelectionMode) "${selectedIds.size} of $totalCount selected" else "$totalCount moments captured",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    if (!isSelectionMode) {
                        FilledTonalIconButton(
                            onClick = onEnterSelectionMode,
                            colors = IconButtonDefaults.filledTonalIconButtonColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer,
                                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Default.Checklist,
                                contentDescription = "Select moments",
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    } else {
                        FilledTonalButton(
                            onClick = onExitSelectionMode,
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("Done", style = MaterialTheme.typography.labelMedium)
                        }
                    }
                }
            }
        }

        items(moments, key = { it.id }) { moment ->
            val isSelected = selectedIds.contains(moment.id)
            MomentCard(
                moment = moment,
                isSelectionMode = isSelectionMode,
                isSelected = isSelected,
                timeText = timeFormat.format(Date(moment.timestamp)),
                dateText = dateFormat.format(Date(moment.timestamp)),
                onClick = { onMomentClick(moment) },
                onDelete = { onDeleteClick(moment) }
            )
        }
    }
}

@Composable
private fun MomentCard(
    moment: DeskMoment,
    isSelectionMode: Boolean,
    isSelected: Boolean,
    timeText: String,
    dateText: String,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme

    OutlinedCard(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        border = if (isSelectionMode && isSelected) {
            androidx.compose.foundation.BorderStroke(2.5.dp, colorScheme.primary)
        } else {
            CardDefaults.outlinedCardBorder()
        },
        colors = CardDefaults.outlinedCardColors(
            containerColor = if (isSelectionMode && isSelected) {
                colorScheme.primaryContainer.copy(alpha = 0.25f)
            } else {
                colorScheme.surfaceContainerLow
            }
        )
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(4f / 3f)
                    .background(Color.Black.copy(alpha = 0.05f)),
                contentAlignment = Alignment.Center
            ) {
                com.leshoraa.kore.core.common.LazyDeskMomentImage(
                    filePath = moment.filePath,
                    contentDescription = "Desk snapshot",
                    contentScale = ContentScale.Crop,
                    isThumbnail = true,
                    modifier = Modifier.fillMaxSize()
                )

                // Mood pill overlay
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = colorScheme.surface.copy(alpha = 0.85f),
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(8.dp)
                ) {
                    Text(
                        text = moment.expressionName.ifBlank { "IDLE" },
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = colorScheme.primary,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }

                // Selection checkmark indicator
                if (isSelectionMode) {
                    Surface(
                        shape = CircleShape,
                        color = if (isSelected) colorScheme.primary else colorScheme.surface.copy(alpha = 0.8f),
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(8.dp)
                            .size(24.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            if (isSelected) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = "Selected",
                                    tint = colorScheme.onPrimary,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
            }

            Column(modifier = Modifier.padding(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = timeText,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = colorScheme.onSurface
                    )
                    Text(
                        text = String.format(Locale.US, "V: %+.1f", moment.valence),
                        style = MaterialTheme.typography.labelSmall,
                        color = if (moment.valence >= 0) colorScheme.primary else colorScheme.error
                    )
                }
                Text(
                    text = dateText,
                    style = MaterialTheme.typography.bodySmall,
                    color = colorScheme.onSurfaceVariant,
                    fontSize = 11.sp
                )
            }
        }
    }
}

@Composable
private fun EmptyMomentsPlaceholder(
    isAutoCaptureEnabled: Boolean
) {
    val colorScheme = MaterialTheme.colorScheme

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Surface(
                shape = CircleShape,
                color = colorScheme.primaryContainer.copy(alpha = 0.5f),
                modifier = Modifier.size(72.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.CameraAlt,
                        contentDescription = null,
                        tint = colorScheme.primary,
                        modifier = Modifier.size(36.dp)
                    )
                }
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = "No Desk Moments Yet",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = colorScheme.onSurface
                )
                Text(
                    text = "Capture candid memories of your desk work and study sessions from KoRe's perspective.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = colorScheme.onSurfaceVariant,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }

            if (!isAutoCaptureEnabled) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = colorScheme.surfaceContainer.copy(alpha = 0.6f),
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = "Auto-capture is currently off. You can enable periodic capture anytime in Settings.",
                            style = MaterialTheme.typography.bodySmall,
                            color = colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MomentDetailSheet(
    moment: DeskMoment,
    onDismiss: () -> Unit,
    onSaveToGallery: () -> Unit,
    onDelete: () -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme
    val dateTimeFormat = remember { SimpleDateFormat("EEEE, dd MMMM yyyy • HH:mm:ss", Locale.getDefault()) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = colorScheme.surfaceContainerLow
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Desk Moment Detail",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = colorScheme.onSurface
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    IconButton(onClick = onSaveToGallery) {
                        Icon(
                            imageVector = Icons.Default.Download,
                            contentDescription = "Save to Gallery",
                            tint = colorScheme.primary
                        )
                    }
                    IconButton(onClick = onDelete) {
                        Icon(
                            imageVector = Icons.Default.DeleteOutline,
                            contentDescription = "Delete",
                            tint = colorScheme.error
                        )
                    }
                }
            }

            // Preview Full Image
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(4f / 3f)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color.Black),
                contentAlignment = Alignment.Center
            ) {
                com.leshoraa.kore.core.common.LazyDeskMomentImage(
                    filePath = moment.filePath,
                    contentDescription = "Desk Moment Full Preview",
                    contentScale = ContentScale.Fit,
                    isThumbnail = false,
                    modifier = Modifier.fillMaxSize()
                )
            }

            // Metadata Card
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = colorScheme.surfaceContainer
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Captured Time", style = MaterialTheme.typography.bodyMedium, color = colorScheme.onSurfaceVariant)
                        Text(
                            text = dateTimeFormat.format(Date(moment.timestamp)),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = colorScheme.onSurface
                        )
                    }

                    HorizontalDivider(color = colorScheme.outlineVariant.copy(alpha = 0.3f))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Expression / Mood", style = MaterialTheme.typography.bodyMedium, color = colorScheme.onSurfaceVariant)
                        Text(
                            text = moment.expressionName.ifBlank { "IDLE" },
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = colorScheme.primary
                        )
                    }

                    HorizontalDivider(color = colorScheme.outlineVariant.copy(alpha = 0.3f))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Emotional Valence", style = MaterialTheme.typography.bodyMedium, color = colorScheme.onSurfaceVariant)
                        Text(
                            text = String.format(Locale.US, "%+.2f", moment.valence),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = if (moment.valence >= 0) colorScheme.primary else colorScheme.error
                        )
                    }
                }
            }
        }
    }
}
