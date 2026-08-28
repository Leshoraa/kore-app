package com.leshoraa.kore.presentation.moments

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.leshoraa.kore.domain.model.DeskMoment
import com.leshoraa.kore.domain.repository.UserPreferencesRepository
import com.leshoraa.kore.domain.usecase.CaptureMomentUseCase
import com.leshoraa.kore.domain.usecase.DeleteMomentUseCase
import com.leshoraa.kore.domain.usecase.GetDeskMomentsUseCase
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class MomentsUiState(
    val moments: List<DeskMoment> = emptyList(),
    val isCapturing: Boolean = false,
    val selectedMoment: DeskMoment? = null,
    val isDetailSheetOpen: Boolean = false,
    val snackbarMessage: String? = null
)

class MomentsViewModel(
    private val getDeskMomentsUseCase: GetDeskMomentsUseCase,
    private val captureMomentUseCase: CaptureMomentUseCase,
    private val deleteMomentUseCase: DeleteMomentUseCase,
    private val userPreferencesRepository: UserPreferencesRepository
) : ViewModel() {

    companion object {
        private const val TAG = "MomentsVM"
    }

    private val _uiState = MutableStateFlow(MomentsUiState())
    val uiState: StateFlow<MomentsUiState> = _uiState.asStateFlow()

    val cameraHost = userPreferencesRepository.cameraHost
    val isAutoCaptureEnabled = userPreferencesRepository.momentAutoCaptureEnabled
    val captureIntervalMinutes = userPreferencesRepository.momentCaptureIntervalMinutes

    private var autoCaptureJob: Job? = null

    init {
        viewModelScope.launch {
            getDeskMomentsUseCase().collect { list ->
                _uiState.update { it.copy(moments = list) }
            }
        }

        // Manage periodic background capture worker
        viewModelScope.launch {
            combine(
                isAutoCaptureEnabled,
                captureIntervalMinutes,
                cameraHost
            ) { enabled, intervalMin, host ->
                Triple(enabled, intervalMin, host)
            }.collect { (enabled, intervalMin, host) ->
                manageAutoCaptureSchedule(enabled, intervalMin, host)
            }
        }
    }

    private fun manageAutoCaptureSchedule(enabled: Boolean, intervalMin: Int, host: String) {
        autoCaptureJob?.cancel()
        autoCaptureJob = null

        if (!enabled || host.isBlank()) return

        autoCaptureJob = viewModelScope.launch {
            val intervalMs = (intervalMin.coerceAtLeast(15) * 60 * 1000L)
            Log.i(TAG, "Started desk moment auto-capture schedule: every ${intervalMin}m")

            while (true) {
                delay(intervalMs)
                try {
                    captureMomentUseCase(host).onSuccess {
                        Log.i(TAG, "Periodic desk moment captured successfully: ID ${it.id}")
                    }.onFailure { e ->
                        Log.d(TAG, "Periodic moment capture skipped/failed: ${e.message}")
                    }
                } catch (e: Exception) {
                    Log.d(TAG, "Periodic auto capture exception: ${e.message}")
                }
            }
        }
    }

    fun captureInstantMoment() {
        if (_uiState.value.isCapturing) return
        val host = cameraHost.value
        if (host.isBlank()) {
            _uiState.update { it.copy(snackbarMessage = "Camera host is not configured.") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isCapturing = true, snackbarMessage = null) }
            captureMomentUseCase(host).fold(
                onSuccess = { moment ->
                    _uiState.update {
                        it.copy(
                            isCapturing = false,
                            snackbarMessage = "Moment captured!"
                        )
                    }
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(
                            isCapturing = false,
                            snackbarMessage = error.message ?: "Failed to capture snapshot from KoRe."
                        )
                    }
                }
            )
        }
    }

    fun selectMoment(moment: DeskMoment) {
        _uiState.update { it.copy(selectedMoment = moment, isDetailSheetOpen = true) }
    }

    fun closeDetailSheet() {
        _uiState.update { it.copy(isDetailSheetOpen = false, selectedMoment = null) }
    }

    fun deleteMoment(moment: DeskMoment) {
        viewModelScope.launch {
            deleteMomentUseCase(moment.id, moment.filePath)
            _uiState.update {
                it.copy(
                    isDetailSheetOpen = false,
                    selectedMoment = null,
                    snackbarMessage = "Moment removed."
                )
            }
        }
    }

    fun deleteMultipleMoments(moments: List<DeskMoment>) {
        if (moments.isEmpty()) return
        viewModelScope.launch {
            moments.forEach { moment ->
                deleteMomentUseCase(moment.id, moment.filePath)
            }
            _uiState.update {
                it.copy(
                    isDetailSheetOpen = false,
                    selectedMoment = null,
                    snackbarMessage = "${moments.size} moments deleted."
                )
            }
        }
    }

    fun exportMomentsToGallery(context: Context, moments: List<DeskMoment>) {
        if (moments.isEmpty()) return
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            var exportedCount = 0
            val resolver = context.contentResolver

            moments.forEach { moment ->
                try {
                    val file = java.io.File(moment.filePath)
                    if (file.exists()) {
                        val displayName = "KoRe_Moment_${moment.timestamp}"
                        val values = android.content.ContentValues().apply {
                            put(android.provider.MediaStore.Images.Media.DISPLAY_NAME, "$displayName.jpg")
                            put(android.provider.MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
                            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                                put(android.provider.MediaStore.Images.Media.RELATIVE_PATH, "Pictures/KoRe")
                                put(android.provider.MediaStore.Images.Media.IS_PENDING, 1)
                            }
                        }

                        val uri = resolver.insert(android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
                        if (uri != null) {
                            resolver.openOutputStream(uri)?.use { out ->
                                file.inputStream().use { input ->
                                    input.copyTo(out)
                                }
                            }
                            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                                values.clear()
                                values.put(android.provider.MediaStore.Images.Media.IS_PENDING, 0)
                                resolver.update(uri, values, null, null)
                            }
                            exportedCount++
                        }
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to export moment ${moment.id}: ${e.message}")
                }
            }

            _uiState.update {
                it.copy(
                    snackbarMessage = if (exportedCount > 0) "$exportedCount photos saved to Gallery (Pictures/KoRe)!" else "Failed to save photos to Gallery."
                )
            }
        }
    }

    fun clearAllMoments() {
        viewModelScope.launch {
            deleteMomentUseCase.clearAll()
            _uiState.update {
                it.copy(
                    isDetailSheetOpen = false,
                    selectedMoment = null,
                    snackbarMessage = "All moments cleared."
                )
            }
        }
    }

    fun clearSnackbarMessage() {
        _uiState.update { it.copy(snackbarMessage = null) }
    }
}
