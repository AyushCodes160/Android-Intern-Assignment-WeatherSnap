package com.weathersnap.ui.report

import androidx.lifecycle.SavedStateHandle
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
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import java.io.File
import javax.inject.Inject

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
)

@OptIn(FlowPreview::class)
@HiltViewModel
class CreateReportViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    private val draftRepository: DraftRepository,
    private val reportRepository: ReportRepository,
    private val imageCompressor: ImageCompressor,
) : ViewModel() {

    private val _state = MutableStateFlow(CreateReportUiState())
    val state = _state.asStateFlow()

    private val _events = MutableSharedFlow<CreateReportEvent>(extraBufferCapacity = 1)
    val events = _events.asSharedFlow()

    private var notesPersistJob: Job? = null

    /**
     * Called by the screen on first composition with the snapshot that was passed in
     * via navigation. We freeze that snapshot into a draft (one per draftId) so it
     * survives rotation and process death without re-fetching.
     */
    fun init(draftId: String, incomingSnapshotJson: String) {
        if (_state.value.draftId == draftId && _state.value.snapshot != null) return

        viewModelScope.launch {
            val incoming: WeatherSnapshot =
                Json { ignoreUnknownKeys = true }.decodeFromString(incomingSnapshotJson)
            val draft = draftRepository.getOrCreate(draftId, incoming)
            val frozen = draftRepository.decodeSnapshot(draft)
            _state.value = CreateReportUiState(
                draftId = draft.id,
                snapshot = frozen,
                notes = draft.notes,
                imagePath = draft.imagePath,
                originalImageBytes = draft.originalImageBytes,
                compressedImageBytes = draft.compressedImageBytes,
            )
            wireNotesPersistence()
            observeCapturedImage()
        }
    }

    fun onNotesChange(newNotes: String) {
        _state.value = _state.value.copy(notes = newNotes)
    }

    /**
     * Compresses the captured photo and updates the draft. The result of the camera
     * screen is also written to savedStateHandle["captured_image_path"] by the nav
     * graph; this is the single entry point so duplicate captures cannot occur.
     */
    fun onPhotoCaptured(rawFilePath: String) {
        viewModelScope.launch {
            val draftId = _state.value.draftId
            if (draftId.isEmpty()) return@launch
            val raw = File(rawFilePath)
            if (!raw.exists()) return@launch
            runCatching { imageCompressor.compress(raw) }
                .onSuccess { result ->
                    runCatching { raw.delete() }
                    draftRepository.updateImage(
                        draftId = draftId,
                        imagePath = result.compressedFile.absolutePath,
                        originalBytes = result.originalBytes,
                        compressedBytes = result.compressedBytes,
                    )
                    _state.value = _state.value.copy(
                        imagePath = result.compressedFile.absolutePath,
                        originalImageBytes = result.originalBytes,
                        compressedImageBytes = result.compressedBytes,
                    )
                }
                .onFailure {
                    _events.tryEmit(CreateReportEvent.Error(it.message ?: "Image processing failed"))
                }
        }
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
                // Persist any final notes that may not yet have been written.
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
                if (s.draftId.isNotEmpty()) draftRepository.updateNotes(s.draftId, s.notes)
            }
            .launchIn(viewModelScope)
    }

    private fun observeCapturedImage() {
        savedStateHandle.getStateFlow<String?>(KEY_CAPTURED_IMAGE, null)
            .onEach { path ->
                if (!path.isNullOrEmpty()) {
                    savedStateHandle[KEY_CAPTURED_IMAGE] = null
                    onPhotoCaptured(path)
                }
            }
            .launchIn(viewModelScope)
    }

    companion object { const val KEY_CAPTURED_IMAGE = "captured_image_path" }
}
