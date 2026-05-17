package com.weathersnap.ui.report

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.weathersnap.data.repository.DraftRepository
import com.weathersnap.data.repository.ReportRepository
import com.weathersnap.domain.model.WeatherSnapshot
import com.weathersnap.util.ImageCompressor
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import java.io.File
import javax.inject.Inject

private const val TAG = "CreateReportVM"

sealed interface CreateReportEvent {
    data object Saved : CreateReportEvent
    data class Error(val message: String) : CreateReportEvent
}

data class CreateReportUiState(
    val draftId: String = "",
    val snapshot: WeatherSnapshot? = null,
    val notes: String = "",
    val imagePath: String? = null,
    val originalImageBytes: Long = 0L,
    val compressedImageBytes: Long = 0L,
    val isSaving: Boolean = false,
    val isProcessingPhoto: Boolean = false,
)

@OptIn(FlowPreview::class)
@HiltViewModel
class CreateReportViewModel @Inject constructor(
    private val draftRepository: DraftRepository,
    private val reportRepository: ReportRepository,
    private val imageCompressor: ImageCompressor,
) : ViewModel() {

    private val _state = MutableStateFlow(CreateReportUiState())
    val state = _state.asStateFlow()

    private val _events = MutableSharedFlow<CreateReportEvent>(extraBufferCapacity = 4)
    val events = _events.asSharedFlow()

    private var notesPersistJob: Job? = null
    private var lastConsumedCapturePath: String? = null

    init {
        wireNotesPersistence()
    }

    /**
     * Freezes the incoming weather snapshot into a draft row (one per draftId) so the
     * Create Report flow survives rotation, backgrounding, and process death without
     * re-fetching weather, and without producing duplicate saved reports on save.
     */
    fun init(draftId: String, incomingSnapshotJson: String) {
        if (_state.value.draftId == draftId && _state.value.snapshot != null) return

        viewModelScope.launch {
            runCatching {
                val incoming: WeatherSnapshot =
                    Json { ignoreUnknownKeys = true }.decodeFromString(incomingSnapshotJson)
                val draft = draftRepository.getOrCreate(draftId, incoming)
                val frozen = draftRepository.decodeSnapshot(draft)
                _state.value = _state.value.copy(
                    draftId = draft.id,
                    snapshot = frozen,
                    notes = draft.notes,
                    imagePath = draft.imagePath,
                    originalImageBytes = draft.originalImageBytes,
                    compressedImageBytes = draft.compressedImageBytes,
                )
            }.onFailure {
                Log.e(TAG, "init failed", it)
                _events.tryEmit(CreateReportEvent.Error("Could not load draft: ${it.message}"))
            }
        }
    }

    fun onNotesChange(newNotes: String) {
        _state.value = _state.value.copy(notes = newNotes)
    }

    /**
     * Called by the screen when the camera returns a captured file path. Idempotent —
     * processing the same path twice is a no-op so an over-eager re-emit from the
     * back-stack-entry SavedStateHandle (rotation, etc.) cannot double-compress.
     */
    fun onPhotoCaptured(rawFilePath: String) {
        if (rawFilePath.isEmpty() || rawFilePath == lastConsumedCapturePath) return
        lastConsumedCapturePath = rawFilePath
        Log.d(TAG, "captured image path arrived: $rawFilePath")
        viewModelScope.launch { handleCapturedPath(rawFilePath) }
    }

    fun onSave() {
        val s = _state.value
        if (s.isSaving) return
        val snapshot = s.snapshot ?: return
        val imagePath = s.imagePath
        if (imagePath.isNullOrEmpty()) {
            _events.tryEmit(CreateReportEvent.Error("Capture a photo before saving"))
            return
        }
        _state.value = s.copy(isSaving = true)
        viewModelScope.launch {
            runCatching {
                draftRepository.updateNotes(s.draftId, s.notes)
                reportRepository.saveReport(
                    draftId = s.draftId,
                    snapshot = snapshot,
                    notes = s.notes,
                    imagePath = imagePath,
                    originalImageBytes = s.originalImageBytes,
                    compressedImageBytes = s.compressedImageBytes,
                )
            }
                .onSuccess { _events.tryEmit(CreateReportEvent.Saved) }
                .onFailure {
                    Log.e(TAG, "save failed", it)
                    _state.value = _state.value.copy(isSaving = false)
                    _events.tryEmit(CreateReportEvent.Error(it.message ?: "Save failed"))
                }
        }
    }

    private fun wireNotesPersistence() {
        notesPersistJob?.cancel()
        notesPersistJob = _state
            .drop(1)
            .debounce(400L)
            .onEach { s ->
                if (s.draftId.isNotEmpty()) {
                    runCatching { draftRepository.updateNotes(s.draftId, s.notes) }
                        .onFailure { Log.e(TAG, "notes persist failed", it) }
                }
            }
            .launchIn(viewModelScope)
    }

    private suspend fun handleCapturedPath(rawFilePath: String) {
        val draftId = if (_state.value.draftId.isNotEmpty()) {
            _state.value.draftId
        } else {
            Log.d(TAG, "waiting for draftId before processing capture")
            _state.first { it.draftId.isNotEmpty() }.draftId
        }

        val raw = File(rawFilePath)
        if (!raw.exists()) {
            Log.e(TAG, "raw capture missing at $rawFilePath")
            _events.tryEmit(CreateReportEvent.Error("Captured photo file is missing"))
            return
        }

        _state.value = _state.value.copy(isProcessingPhoto = true)
        runCatching { imageCompressor.compress(raw) }
            .onSuccess { result ->
                runCatching { raw.delete() }
                runCatching {
                    draftRepository.updateImage(
                        draftId = draftId,
                        imagePath = result.compressedFile.absolutePath,
                        originalBytes = result.originalBytes,
                        compressedBytes = result.compressedBytes,
                    )
                }
                _state.value = _state.value.copy(
                    imagePath = result.compressedFile.absolutePath,
                    originalImageBytes = result.originalBytes,
                    compressedImageBytes = result.compressedBytes,
                    isProcessingPhoto = false,
                )
                Log.d(TAG, "photo processed: ${result.compressedFile.absolutePath}")
            }
            .onFailure {
                Log.e(TAG, "image processing failed", it)
                _state.value = _state.value.copy(isProcessingPhoto = false)
                _events.tryEmit(CreateReportEvent.Error("Image processing failed: ${it.message}"))
            }
    }

    companion object { const val KEY_CAPTURED_IMAGE = "captured_image_path" }
}
